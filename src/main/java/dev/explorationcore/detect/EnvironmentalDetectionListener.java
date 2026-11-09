package dev.explorationcore.detect;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Candle;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

public final class EnvironmentalDetectionListener implements Listener {
    private static final Material REDSTONE_DUST = Material.REDSTONE;

    private final DetectionSettings settings;
    private final List<DetectionSite> sites;
    private final DoubleSupplier rolls;

    public EnvironmentalDetectionListener(DetectionSettings settings, DetectionCatalogue catalogue) {
        this(settings, catalogue, () -> ThreadLocalRandom.current().nextDouble());
    }

    EnvironmentalDetectionListener(DetectionSettings settings, DetectionCatalogue catalogue, DoubleSupplier rolls) {
        this.settings = settings;
        this.sites = catalogue.sites();
        this.rolls = rolls;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        HeartEffect heart = settings.heart();
        if (block.getType() == heart.place()) {
            if (hit(block, "heart", heart.horizontalRadius(), heart.verticalRadius())) {
                block.setType(heart.become(), false);
            }
            return;
        }
        lightCandle(block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        NerveEffect nerve = settings.nerve();
        if (!nerve.ores().contains(event.getBlockState().getType())) {
            return;
        }
        List<Item> items = event.getItems();
        if (!containsDust(items)) {
            return;
        }
        if (!hit(event.getBlock(), "nerve", nerve.horizontalRadius(), nerve.verticalRadius())) {
            return;
        }
        DetectionDrops.apply(items, REDSTONE_DUST, nerve.drop());
    }

    private void lightCandle(Block block) {
        if (!SoulCandles.isCandle(block.getType())) {
            return;
        }
        if (!(block.getBlockData() instanceof Candle candle) || candle.isLit()) {
            return;
        }
        SoulEffect soul = settings.soul();
        if (!hit(block, "soul", soul.horizontalRadius(), soul.verticalRadius())) {
            return;
        }
        PlacedCandle placed = new PlacedCandle(block.getType(), candle.getCandles(), candle.isLit());
        candle.setLit(SoulCandles.light(placed).lit());
        block.setBlockData(candle, false);
    }

    private boolean hit(Block block, String type, double horizontalRadius, double verticalRadius) {
        Optional<DetectionHit> nearest = DetectionQuery.nearest(
                sites,
                type,
                block.getWorld().getName(),
                settings.overworldFolder(),
                block.getX(),
                block.getY(),
                block.getZ(),
                horizontalRadius,
                verticalRadius
        );
        if (nearest.isEmpty()) {
            return false;
        }
        double chance = DetectionChance.chance(
                nearest.get().distance(),
                settings.maxProbability(),
                settings.falloff()
        );
        return DetectionChance.succeeds(chance, rolls.getAsDouble());
    }

    private static boolean containsDust(List<Item> items) {
        for (Item item : items) {
            if (item.getItemStack().getType() == REDSTONE_DUST) {
                return true;
            }
        }
        return false;
    }
}
