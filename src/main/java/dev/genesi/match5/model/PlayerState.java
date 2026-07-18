package dev.genesi.match5.model;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
    private Material mob = Material.PIG_SPAWN_EGG;
    private String mobLabel = "Pig";
    private int score;
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

    public Material getMob() {
        return mob;
    }

    public void setMob(Material mob) {
        this.mob = mob == null ? Material.PIG_SPAWN_EGG : mob;
    }

    public String getMobLabel() {
        return mobLabel;
    }

    public void setMobLabel(String mobLabel) {
        this.mobLabel = mobLabel == null || mobLabel.isBlank() ? "Mob" : mobLabel;
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
