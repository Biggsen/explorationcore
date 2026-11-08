package dev.explorationcore.detect;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DetectionDropsTest {
    @Test
    void redstoneDustBecomesOneCoal() {
        assertEquals(
                List.of(Material.COAL),
                DetectionDrops.coalInsteadOfRedstone(
                        List.of(Material.REDSTONE, Material.REDSTONE, Material.REDSTONE),
                        Material.REDSTONE,
                        Material.COAL
                )
        );
    }

    @Test
    void otherDropsStayBesideTheCoal() {
        assertEquals(
                List.of(Material.COAL, Material.FLINT),
                DetectionDrops.coalInsteadOfRedstone(
                        List.of(Material.REDSTONE, Material.FLINT, Material.REDSTONE),
                        Material.REDSTONE,
                        Material.COAL
                )
        );
    }

    @Test
    void silkTouchKeepsTheOre() {
        List<Material> ore = List.of(Material.REDSTONE_ORE);
        assertSame(ore, DetectionDrops.coalInsteadOfRedstone(ore, Material.REDSTONE, Material.COAL));
        List<Material> deepslate = List.of(Material.DEEPSLATE_REDSTONE_ORE);
        assertSame(deepslate, DetectionDrops.coalInsteadOfRedstone(deepslate, Material.REDSTONE, Material.COAL));
    }
}
