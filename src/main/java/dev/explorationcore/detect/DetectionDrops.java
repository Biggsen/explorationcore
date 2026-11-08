package dev.explorationcore.detect;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DetectionDrops {
    private DetectionDrops() {
    }

    public static List<Material> coalInsteadOfRedstone(List<Material> drops, Material dust, Material coal) {
        if (drops == null || drops.isEmpty() || !drops.contains(dust)) {
            return drops;
        }
        List<Material> rewritten = new ArrayList<>();
        boolean placed = false;
        for (Material drop : drops) {
            if (drop == dust) {
                if (!placed) {
                    rewritten.add(coal);
                    placed = true;
                }
                continue;
            }
            rewritten.add(drop);
        }
        return rewritten;
    }

    public static void apply(List<Item> items, Material dust, Material coal) {
        List<Material> types = new ArrayList<>(items.size());
        for (Item item : items) {
            types.add(item.getItemStack().getType());
        }
        List<Material> rewritten = coalInsteadOfRedstone(types, dust, coal);
        if (rewritten == types) {
            return;
        }
        int index = 0;
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (index >= rewritten.size()) {
                iterator.remove();
                continue;
            }
            Material want = rewritten.get(index);
            if (item.getItemStack().getType() != want) {
                item.setItemStack(new ItemStack(want, 1));
            }
            index++;
        }
    }
}
