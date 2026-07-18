package dev.genesi.match5.manager;

import dev.genesi.match5.Match5Plugin;
import dev.genesi.match5.board.BoardGeometry;
import dev.genesi.match5.model.Arena;
import dev.genesi.match5.model.TileContent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;

/**
 * Floor signs are the click targets for each Match 5 cell.
 * Text shows "?" while hidden and the reveal result when opened.
 */
public final class SignService {

    private final Match5Plugin plugin;
    private Material signMaterial = Material.OAK_SIGN;
    private boolean writeText = true;
    private boolean glowing = false;
    private List<String> hiddenLines = List.of("", "  ?", "", "");
    private List<String> blankLines = List.of("", " ----", "", "");
    private DyeColor hiddenColor = DyeColor.GRAY;
    private DyeColor blankColor = DyeColor.LIGHT_GRAY;
    private DyeColor mobColor = DyeColor.YELLOW;

    public SignService(Match5Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("signs");
        writeText = section == null || section.getBoolean("write-text", true);
        glowing = section != null && section.getBoolean("glowing", false);

        String materialName = section == null ? "OAK_SIGN" : section.getString("material", "OAK_SIGN");
        Material matched = Material.matchMaterial(materialName == null ? "OAK_SIGN" : materialName);
        if (matched != null && matched.name().endsWith("_SIGN") && !matched.name().contains("WALL")
                && !matched.name().contains("HANGING")) {
            signMaterial = matched;
        } else {
            signMaterial = Material.OAK_SIGN;
        }

        hiddenLines = readLines(section, "hidden-lines", List.of("", "  ?", "", ""));
        blankLines = readLines(section, "blank-lines", List.of("", " ----", "", ""));
        hiddenColor = readColor(section, "hidden-color", DyeColor.GRAY);
        blankColor = readColor(section, "blank-color", DyeColor.LIGHT_GRAY);
        mobColor = readColor(section, "mob-color", DyeColor.YELLOW);
    }

    public boolean isSign(Block block) {
        if (block == null) {
            return false;
        }
        String name = block.getType().name();
        return name.endsWith("_SIGN") || name.endsWith("_HANGING_SIGN");
    }

    /**
     * Places a full grid of floor signs from the arena origin.
     * @return number of signs placed/updated
     */
    public int buildBoard(Arena arena) {
        BoardGeometry geometry = new BoardGeometry(arena);
        int placed = 0;
        for (int row = 0; row < geometry.getRows(); row++) {
            for (int column = 0; column < geometry.getColumns(); column++) {
                Block block = geometry.cellOrigin(column, row).getBlock();
                placeFloorSign(block, arena.getFacing());
                applyHidden(block);
                placed++;
            }
        }
        return placed;
    }

    public void resetBoard(Arena arena) {
        BoardGeometry geometry = new BoardGeometry(arena);
        for (int row = 0; row < geometry.getRows(); row++) {
            for (int column = 0; column < geometry.getColumns(); column++) {
                Block block = geometry.cellOrigin(column, row).getBlock();
                if (!isSign(block)) {
                    placeFloorSign(block, arena.getFacing());
                }
                applyHidden(block);
            }
        }
    }

    public void applyHidden(Block block) {
        apply(block, hiddenLines, hiddenColor, false);
    }

    public void applyBlank(Block block) {
        apply(block, blankLines, blankColor, false);
    }

    public void applyMob(Block block, String mobLabel) {
        String label = mobLabel == null || mobLabel.isBlank() ? "Mob" : mobLabel;
        if (label.length() > 14) {
            label = label.substring(0, 14);
        }
        apply(block, List.of("", " " + label, "", ""), mobColor, true);
    }

    public void applyReveal(Block block, TileContent content, String mobLabel) {
        if (content == TileContent.BLANK) {
            applyBlank(block);
        } else {
            applyMob(block, mobLabel);
        }
    }

    private void placeFloorSign(Block block, BlockFace boardFacing) {
        block.setType(signMaterial, false);
        if (block.getBlockData() instanceof Rotatable rotatable) {
            // Sign front faces players (same as board facing)
            rotatable.setRotation(boardFacing);
            block.setBlockData(rotatable, false);
        }
    }

    private void apply(Block block, List<String> lines, DyeColor color, boolean glow) {
        if (block == null || !isSign(block)) {
            return;
        }
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }
        var side = sign.getSide(Side.FRONT);
        if (writeText) {
            for (int i = 0; i < 4; i++) {
                String line = i < lines.size() ? lines.get(i) : "";
                side.line(i, legacy(line));
            }
        } else {
            for (int i = 0; i < 4; i++) {
                side.line(i, Component.empty());
            }
        }
        if (color != null) {
            side.setColor(color);
        }
        side.setGlowingText(glowing || glow);
        sign.setWaxed(true);
        sign.update(true, false);
    }

    private static List<String> readLines(ConfigurationSection section, String key, List<String> fallback) {
        if (section == null) {
            return fallback;
        }
        List<String> list = section.getStringList(key);
        return list == null || list.isEmpty() ? fallback : list;
    }

    private static DyeColor readColor(ConfigurationSection section, String key, DyeColor fallback) {
        if (section == null) {
            return fallback;
        }
        String raw = section.getString(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return DyeColor.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static Component legacy(String input) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input == null ? "" : input);
    }
}
