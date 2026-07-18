package dev.genesi.match5;

import dev.genesi.games.GenesiGamePlugin;
import dev.genesi.games.economy.EconomyService;
import dev.genesi.games.economy.PointsService;
import dev.genesi.games.message.MessageService;
import dev.genesi.match5.command.Match5AdminCommand;
import dev.genesi.match5.command.Match5Command;
import dev.genesi.match5.listener.GameListener;
import dev.genesi.match5.manager.ArenaManager;
import dev.genesi.match5.manager.DisplayService;
import dev.genesi.match5.manager.GameManager;
import dev.genesi.match5.manager.SidebarService;
import dev.genesi.match5.manager.SignService;
import dev.genesi.match5.util.ItemFactory;

public final class Match5Plugin extends GenesiGamePlugin {

    private ArenaManager arenaManager;
    private GameManager gameManager;
    private PointsService pointsService;
    private EconomyService economyService;
    private MessageService messageService;
    private DisplayService displayService;
    private SignService signService;
    private SidebarService sidebarService;
    private ItemFactory itemFactory;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messageService = new MessageService(this, "&8[&6Match 5&8] &r");
        this.economyService = new EconomyService(this, "match5.bypass.fee");
        this.pointsService = new PointsService(this);
        this.itemFactory = new ItemFactory(this);
        this.signService = new SignService(this);
        this.displayService = new DisplayService(this);
        this.sidebarService = new SidebarService(this);
        this.arenaManager = new ArenaManager(this);
        this.gameManager = new GameManager(this);

        arenaManager.load();
        pointsService.load();
        economyService.hook();

        Match5Command playerCommand = new Match5Command(this);
        Match5AdminCommand adminCommand = new Match5AdminCommand(this);
        getCommand("match5").setExecutor(playerCommand);
        getCommand("match5").setTabCompleter(playerCommand);
        getCommand("match5admin").setExecutor(adminCommand);
        getCommand("match5admin").setTabCompleter(adminCommand);

        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getLogger().info("Match5 enabled. Economy: " + economyService.describe()
                + " | Board: floor signs + ItemDisplay"
                + " | Data: " + getDataFolder().getPath());
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (arenaManager != null) {
            arenaManager.save();
        }
        if (pointsService != null) {
            pointsService.save();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        messageService.reload();
        signService.reload();
        arenaManager.load();
        pointsService.load();
        economyService.hook();
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public PointsService getPointsService() {
        return pointsService;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public DisplayService getDisplayService() {
        return displayService;
    }

    public SignService getSignService() {
        return signService;
    }

    public SidebarService getSidebarService() {
        return sidebarService;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }
}
