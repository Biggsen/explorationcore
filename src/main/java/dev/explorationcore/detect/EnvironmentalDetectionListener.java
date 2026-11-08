package dev.explorationcore.detect;

import org.bukkit.Material;
import org.bukkit.block.Block;
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
        HeartEffect heart = settings.heart();
        Block block = event.getBlock();
        if (block.getType() != heart.place()) {
            return;
        }
        if (!hit(block, "heart", heart.horizontalRadius(), heart.verticalRadius())) {
            return;
        }
        block.setType(heart.become(), false);
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
