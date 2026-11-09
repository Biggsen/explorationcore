package dev.explorationcore.detect;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulCandlesTest {
    private static final List<Material> CANDLES = List.of(
            Material.CANDLE,
            Material.WHITE_CANDLE,
            Material.ORANGE_CANDLE,
            Material.MAGENTA_CANDLE,
            Material.LIGHT_BLUE_CANDLE,
            Material.YELLOW_CANDLE,
            Material.LIME_CANDLE,
            Material.PINK_CANDLE,
            Material.GRAY_CANDLE,
            Material.LIGHT_GRAY_CANDLE,
            Material.CYAN_CANDLE,
            Material.PURPLE_CANDLE,
            Material.BLUE_CANDLE,
            Material.BROWN_CANDLE,
            Material.GREEN_CANDLE,
            Material.RED_CANDLE,
            Material.BLACK_CANDLE
    );

    @Test
    void allSeventeenCandlesLightWithoutChangingColourOrCount() {
        assertEquals(Set.copyOf(CANDLES), candlesRecognised());
        for (Material material : CANDLES) {
            for (int count = 1; count <= 4; count++) {
                PlacedCandle lit = SoulCandles.light(new PlacedCandle(material, count, false));
                assertEquals(new PlacedCandle(material, count, true), lit);
            }
        }
    }

    @Test
    void cakesAndOtherBlocksStayUnlit() {
        PlacedCandle cake = new PlacedCandle(Material.WHITE_CANDLE_CAKE, 1, false);
        PlacedCandle plainCake = new PlacedCandle(Material.CANDLE_CAKE, 1, false);
        PlacedCandle grass = new PlacedCandle(Material.SHORT_GRASS, 1, false);
        assertSame(cake, SoulCandles.light(cake));
        assertSame(plainCake, SoulCandles.light(plainCake));
        assertSame(grass, SoulCandles.light(grass));
        assertFalse(SoulCandles.isCandle(Material.REDSTONE_ORE));
    }

    @Test
    void anAlreadyLitCandleIsLeftAlone() {
        PlacedCandle lit = new PlacedCandle(Material.BLUE_CANDLE, 3, true);
        assertSame(lit, SoulCandles.light(lit));
    }

    @Test
    void cancelledPlacementDoesNotReachDetection() throws Exception {
        EventHandler place = EnvironmentalDetectionListener.class
                .getDeclaredMethod("onPlace", BlockPlaceEvent.class)
                .getAnnotation(EventHandler.class);
        assertTrue(place.ignoreCancelled());
    }

    @Test
    void detectionListensOnlyToPlacementAndDrops() {
        List<String> events = Arrays.stream(EnvironmentalDetectionListener.class.getDeclaredMethods())
                .filter(method -> method.getAnnotation(EventHandler.class) != null)
                .map(Method::getName)
                .sorted()
                .toList();
        assertEquals(List.of("onDrop", "onPlace"), events);
    }

    private static Set<Material> candlesRecognised() {
        return Arrays.stream(Material.values()).filter(SoulCandles::isCandle).collect(Collectors.toSet());
    }
}
