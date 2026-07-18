package dev.genesi.match5.command;

import dev.genesi.match5.Match5Plugin;
import dev.genesi.match5.manager.GameManager;
import dev.genesi.match5.model.Arena;
import dev.genesi.match5.model.Seat;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class Match5AdminCommand implements CommandExecutor, TabCompleter {

    private final Match5Plugin plugin;

    public Match5AdminCommand(Match5Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("match5.admin")) {
            plugin.getMessageService().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "create" -> handleCreate(sender, args);
            case "delete", "remove" -> handleDelete(sender, args);
            case "setlobby", "lobby" -> handleSetLobby(sender, args);
            case "setorigin", "origin" -> handleSetOrigin(sender, args);
            case "setfacing", "facing" -> handleSetFacing(sender, args);
            case "setjoin", "join" -> handleSetJoin(sender, args);
            case "setsize", "size" -> handleSetSize(sender, args);
            case "setfee", "fee" -> handleSetFee(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "forcestart" -> handleForceStart(sender, args);
            case "forcestop", "cancel" -> handleForceStop(sender, args);
            case "givepoints" -> handleGivePoints(sender, args);
            case "takepoints" -> handleTakePoints(sender, args);
            case "setplayerpoints" -> handleSetPlayerPoints(sender, args);
            case "redeem" -> handleRedeem(sender, args);
            case "reload" -> {
                plugin.reloadPlugin();
                plugin.getMessageService().send(sender, "reloaded");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin create <name>");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (plugin.getArenaManager().exists(name)) {
            plugin.getMessageService().sendRaw(sender, "&cArena already exists.");
            return;
        }
        plugin.getArenaManager().create(name);
        plugin.getMessageService().send(sender, "arena-created", Map.of("arena", name));
        plugin.getMessageService().sendRaw(sender,
                "&7Next: setlobby, setorigin (corner of table), setjoin a|b (look at seat blocks)");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin delete <name>");
            return;
        }
        String name = args[1];
        if (plugin.getGameManager().isActive(name)) {
            plugin.getMessageService().sendRaw(sender, "&cStop the active game first: /m5admin forcestop " + name);
            return;
        }
        if (!plugin.getArenaManager().delete(name)) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", name));
            return;
        }
        plugin.getMessageService().send(sender, "arena-deleted", Map.of("arena", name.toLowerCase(Locale.ROOT)));
    }

    private void handleSetLobby(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "players-only");
            return;
        }
        if (args.length < 2) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin setlobby <arena>");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(args[1]);
        if (arena.isEmpty()) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", args[1]));
            return;
        }
        arena.get().setLobby(player.getLocation());
        plugin.getArenaManager().save();
        plugin.getMessageService().send(sender, "lobby-set", Map.of("arena", arena.get().getName()));
    }

    private void handleSetOrigin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "players-only");
            return;
        }
        if (args.length < 2) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin setorigin <arena>");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(args[1]);
        if (arena.isEmpty()) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", args[1]));
            return;
        }
        Block target = player.getTargetBlockExact(6);
        if (target == null) {
            plugin.getMessageService().sendRaw(sender, "&cLook at the corner block of the tabletop.");
            return;
        }
        arena.get().setOrigin(target.getLocation());
        arena.get().setFacing(GameManager.yawToFacing(player.getLocation().getYaw()));
        plugin.getArenaManager().save();
        plugin.getMessageService().send(sender, "origin-set", Map.of(
                "arena", arena.get().getName(),
                "facing", arena.get().getFacing().name()
        ));
    }

    private void handleSetFacing(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin setfacing <arena> <NORTH|SOUTH|EAST|WEST>");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(args[1]);
        if (arena.isEmpty()) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", args[1]));
            return;
        }
        BlockFace face;
        try {
            face = BlockFace.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getMessageService().sendRaw(sender, "&cFacing must be NORTH, SOUTH, EAST, or WEST.");
            return;
        }
        if (!face.isCartesian() || face == BlockFace.UP || face == BlockFace.DOWN) {
            plugin.getMessageService().sendRaw(sender, "&cFacing must be NORTH, SOUTH, EAST, or WEST.");
            return;
        }
        arena.get().setFacing(face);
        plugin.getArenaManager().save();
        plugin.getMessageService().send(sender, "facing-set", Map.of(
                "arena", arena.get().getName(),
                "facing", face.name()
        ));
    }

    private void handleSetJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "players-only");
            return;
        }
        if (args.length < 3) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin setjoin <arena> <a|b>");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(args[1]);
        if (arena.isEmpty()) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", args[1]));
            return;
        }
        Seat seat = Seat.fromString(args[2]);
        if (seat == null) {
            plugin.getMessageService().sendRaw(sender, "&cSeat must be a or b.");
            return;
        }
        Block target = player.getTargetBlockExact(6);
        if (target == null) {
            plugin.getMessageService().sendRaw(sender, "&cLook at the join seat block.");
            return;
        }
        arena.get().setJoin(seat, target.getLocation());
        plugin.getArenaManager().save();
        plugin.getMessageService().send(sender, "join-set", Map.of(
                "arena", arena.get().getName(),
                "seat", seat.display()
        ));
    }

    private void handleSetSize(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin setsize <arena> <columns> <rows>");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(args[1]);
        if (arena.isEmpty()) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", args[1]));
            return;
        }
        int columns;
        int rows;
        try {
            columns = Integer.parseInt(args[2]);
            rows = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            plugin.getMessageService().sendRaw(sender, "&cColumns and rows must be numbers.");
            return;
        }
        if (columns < 2 || rows < 2 || columns * rows > 144) {
            plugin.getMessageService().sendRaw(sender, "&cBoard size must be at least 2x2 and at most 144 cells.");
            return;
        }
        arena.get().setColumns(columns);
        arena.get().setRows(rows);
        plugin.getArenaManager().save();
        plugin.getMessageService().send(sender, "size-set", Map.of(
                "arena", arena.get().getName(),
                "columns", String.valueOf(columns),
                "rows", String.valueOf(rows)
        ));
    }

    private void handleSetFee(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin setfee <arena> <amount>");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(args[1]);
        if (arena.isEmpty()) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", args[1]));
            return;
        }
        double fee;
        try {
            fee = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessageService().sendRaw(sender, "&cInvalid amount.");
            return;
        }
        arena.get().setEntryFeeOverride(fee);
        plugin.getArenaManager().save();
        plugin.getMessageService().send(sender, "fee-set", Map.of(
                "arena", arena.get().getName(),
                "fee", plugin.getEconomyService().format(fee)
        ));
    }

    private void handleList(CommandSender sender) {
        var arenas = plugin.getArenaManager().getArenas();
        if (arenas.isEmpty()) {
            plugin.getMessageService().sendRaw(sender, "&7No arenas.");
            return;
        }
        for (Arena arena : arenas) {
            plugin.getMessageService().sendRaw(sender, "&e" + arena.getName()
                    + " &7ready=&f" + arena.isReady()
                    + " &7board=&f" + arena.getColumns() + "x" + arena.getRows());
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin info <arena>");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(args[1]);
        if (arena.isEmpty()) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", args[1]));
            return;
        }
        Arena a = arena.get();
        plugin.getMessageService().sendRaw(sender, "&e" + a.getName()
                + " &7ready=&f" + a.isReady()
                + " &7board=&f" + a.getColumns() + "x" + a.getRows()
                + " &7facing=&f" + a.getFacing().name()
                + " &7fee=&f" + plugin.getArenaManager().resolveEntryFee(a)
                + " &7busy=&f" + plugin.getGameManager().isBusy(a.getName()));
    }

    private void handleForceStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin forcestart <arena>");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(args[1]);
        if (arena.isEmpty()) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", args[1]));
            return;
        }
        plugin.getGameManager().forceStart(arena.get());
        plugin.getMessageService().send(sender, "force-started", Map.of("arena", arena.get().getName()));
    }

    private void handleForceStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin forcestop <arena>");
            return;
        }
        plugin.getGameManager().forceStop(args[1]);
        plugin.getMessageService().send(sender, "force-stopped", Map.of("arena", args[1].toLowerCase(Locale.ROOT)));
    }

    private void handleGivePoints(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin givepoints <player> <amount>");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageService().sendRaw(sender, "&cPlayer not found.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessageService().sendRaw(sender, "&cInvalid amount.");
            return;
        }
        int points = plugin.getPointsService().addPoints(target, amount);
        plugin.getMessageService().send(sender, "points-added", Map.of(
                "amount", String.valueOf(amount),
                "player", target.getName(),
                "points", String.valueOf(points)
        ));
    }

    private void handleTakePoints(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin takepoints <player> <amount>");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageService().sendRaw(sender, "&cPlayer not found.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessageService().sendRaw(sender, "&cInvalid amount.");
            return;
        }
        if (!plugin.getPointsService().removePoints(target, amount)) {
            plugin.getMessageService().send(sender, "not-enough-points", Map.of(
                    "player", target.getName(),
                    "points", String.valueOf(plugin.getPointsService().getPoints(target))
            ));
            return;
        }
        plugin.getMessageService().send(sender, "points-removed", Map.of(
                "amount", String.valueOf(amount),
                "player", target.getName(),
                "points", String.valueOf(plugin.getPointsService().getPoints(target))
        ));
    }

    private void handleSetPlayerPoints(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin setplayerpoints <player> <amount>");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageService().sendRaw(sender, "&cPlayer not found.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessageService().sendRaw(sender, "&cInvalid amount.");
            return;
        }
        plugin.getPointsService().setPoints(target, amount);
        plugin.getMessageService().send(sender, "points-set", Map.of(
                "player", target.getName(),
                "points", String.valueOf(plugin.getPointsService().getPoints(target))
        ));
    }

    private void handleRedeem(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /m5admin redeem <player> <amount>");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageService().sendRaw(sender, "&cPlayer not found.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessageService().sendRaw(sender, "&cInvalid amount.");
            return;
        }
        if (!plugin.getPointsService().removePoints(target, amount)) {
            plugin.getMessageService().send(sender, "not-enough-points", Map.of(
                    "player", target.getName(),
                    "points", String.valueOf(plugin.getPointsService().getPoints(target))
            ));
            return;
        }
        plugin.getMessageService().send(sender, "redeem-success", Map.of(
                "amount", String.valueOf(amount),
                "player", target.getName(),
                "points", String.valueOf(plugin.getPointsService().getPoints(target))
        ));
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessageService().sendRaw(sender, "&e/m5admin create|delete <arena>");
        plugin.getMessageService().sendRaw(sender, "&e/m5admin setlobby <arena>");
        plugin.getMessageService().sendRaw(sender, "&e/m5admin setorigin <arena> &7(look at corner)");
        plugin.getMessageService().sendRaw(sender, "&e/m5admin setjoin <arena> <a|b>");
        plugin.getMessageService().sendRaw(sender, "&e/m5admin setsize <arena> <cols> <rows>");
        plugin.getMessageService().sendRaw(sender, "&e/m5admin forcestart|forcestop <arena>");
        plugin.getMessageService().sendRaw(sender, "&e/m5admin reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("match5.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(Arrays.asList(
                    "help", "create", "delete", "setlobby", "setorigin", "setfacing", "setjoin",
                    "setsize", "setfee", "list", "info", "forcestart", "forcestop",
                    "givepoints", "takepoints", "setplayerpoints", "redeem", "reload"
            ), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (List.of("delete", "setlobby", "setorigin", "setfacing", "setjoin", "setsize", "setfee",
                    "info", "forcestart", "forcestop").contains(sub)) {
                return filter(plugin.getArenaManager().getArenas().stream().map(Arena::getName).toList(), args[1]);
            }
            if (List.of("givepoints", "takepoints", "setplayerpoints", "redeem").contains(sub)) {
                return null;
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setjoin")) {
            return filter(List.of("a", "b"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setfacing")) {
            return filter(List.of("NORTH", "SOUTH", "EAST", "WEST"), args[2]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
