package dev.genesi.match5.command;

import dev.genesi.match5.Match5Plugin;
import dev.genesi.match5.model.Arena;
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
import java.util.stream.Collectors;

public final class Match5Command implements CommandExecutor, TabCompleter {

    private final Match5Plugin plugin;

    public Match5Command(Match5Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "join", "play" -> handleJoin(sender, args);
            case "leave", "quit" -> handleLeave(sender);
            case "start" -> handleStart(sender);
            case "points", "balance" -> handlePoints(sender, args);
            case "arenas", "list" -> handleArenas(sender);
            case "info" -> handleInfo(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "players-only");
            return;
        }
        if (!player.hasPermission("match5.use")) {
            plugin.getMessageService().send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /match5 join <arena>");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(args[1]);
        if (arena.isEmpty()) {
            plugin.getMessageService().send(sender, "arena-not-found", Map.of("arena", args[1]));
            return;
        }
        String result = plugin.getGameManager().join(player, arena.get());
        switch (result) {
            case "ok", "handled" -> {
            }
            case "already-playing" -> plugin.getMessageService().send(player, "already-playing");
            case "arena-not-ready" -> plugin.getMessageService().send(player, "arena-not-ready",
                    Map.of("arena", arena.get().getName()));
            case "arena-busy" -> plugin.getMessageService().send(player, "arena-busy",
                    Map.of("arena", arena.get().getName()));
            case "economy-missing" -> plugin.getMessageService().send(player, "economy-missing");
            default -> plugin.getMessageService().sendRaw(player, "&cCould not join.");
        }
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "players-only");
            return;
        }
        plugin.getGameManager().leave(player, true);
    }

    private void handleStart(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "players-only");
            return;
        }
        plugin.getGameManager().tryStart(player);
    }

    private void handlePoints(CommandSender sender, String[] args) {
        if (!sender.hasPermission("match5.points") && !sender.hasPermission("match5.admin")) {
            plugin.getMessageService().send(sender, "no-permission");
            return;
        }
        if (args.length >= 2) {
            if (!sender.hasPermission("match5.admin")) {
                plugin.getMessageService().send(sender, "no-permission");
                return;
            }
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                plugin.getMessageService().sendRaw(sender, "&cPlayer not found.");
                return;
            }
            plugin.getMessageService().send(sender, "points-other", Map.of(
                    "player", target.getName(),
                    "points", String.valueOf(plugin.getPointsService().getPoints(target))
            ));
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "players-only");
            return;
        }
        plugin.getMessageService().send(player, "points-self", Map.of(
                "points", String.valueOf(plugin.getPointsService().getPoints(player))
        ));
    }

    private void handleArenas(CommandSender sender) {
        var arenas = plugin.getArenaManager().getArenas();
        if (arenas.isEmpty()) {
            plugin.getMessageService().sendRaw(sender, "&7No arenas configured.");
            return;
        }
        String list = arenas.stream()
                .map(a -> a.getName() + (a.isReady() ? "" : " &c(incomplete)"))
                .collect(Collectors.joining("&7, &e"));
        plugin.getMessageService().sendRaw(sender, "&7Arenas: &e" + list);
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageService().sendRaw(sender, "&cUsage: /match5 info <arena>");
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
                + " &7fee=&f" + plugin.getArenaManager().resolveEntryFee(a));
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessageService().sendRaw(sender, "&e/match5 join <arena>");
        plugin.getMessageService().sendRaw(sender, "&e/match5 leave");
        plugin.getMessageService().sendRaw(sender, "&e/match5 start");
        plugin.getMessageService().sendRaw(sender, "&e/match5 points");
        plugin.getMessageService().sendRaw(sender, "&e/match5 arenas");
        plugin.getMessageService().sendRaw(sender, "&7Or click a seat join block.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("join", "leave", "start", "points", "arenas", "info", "help"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("info"))) {
            return filter(plugin.getArenaManager().getArenas().stream().map(Arena::getName).toList(), args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
