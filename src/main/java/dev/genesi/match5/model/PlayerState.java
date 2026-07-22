package dev.genesi.match5.model;

import dev.genesi.match5.util.IconDef;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public final class PlayerState {

    private final UUID uuid;
    private final String name;
    private Seat seat;
    private IconDef icon;
    private int score;
    private int chances;
    private Location returnLocation;
    private GameMode gameMode;
    private boolean flying;
    private boolean allowFlight;
    private double health;
    private int foodLevel;
    private float saturation;
    private float exhaustion;
    private ItemStack[] inventory;
    private ItemStack[] armor;
    private ItemStack offhand;
    private Collection<PotionEffect> effects = new ArrayList<>();

    public PlayerState(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public IconDef getIcon() {
        return icon;
    }

    public void setIcon(IconDef icon) {
        this.icon = icon;
    }

    public String getIconLabel() {
        return icon == null ? "Icon" : icon.label();
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = Math.max(0, score);
    }

    public void addScore(int amount) {
        this.score = Math.max(0, this.score + amount);
    }

    public int getChances() {
        return chances;
    }

    public void setChances(int chances) {
        this.chances = Math.max(0, chances);
    }

    public void useChance() {
        if (chances > 0) {
            chances--;
        }
    }

    public Location getReturnLocation() {
        return returnLocation == null ? null : returnLocation.clone();
    }

    public void setReturnLocation(Location returnLocation) {
        this.returnLocation = returnLocation == null ? null : returnLocation.clone();
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public boolean isAllowFlight() {
        return allowFlight;
    }

    public void setAllowFlight(boolean allowFlight) {
        this.allowFlight = allowFlight;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public float getExhaustion() {
        return exhaustion;
    }

    public void setExhaustion(float exhaustion) {
        this.exhaustion = exhaustion;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public void setInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public void setArmor(ItemStack[] armor) {
        this.armor = armor;
    }

    public ItemStack getOffhand() {
        return offhand;
    }

    public void setOffhand(ItemStack offhand) {
        this.offhand = offhand;
    }

    public Collection<PotionEffect> getEffects() {
        return effects;
    }

    public void setEffects(Collection<PotionEffect> effects) {
        this.effects = effects == null ? new ArrayList<>() : new ArrayList<>(effects);
    }
}
