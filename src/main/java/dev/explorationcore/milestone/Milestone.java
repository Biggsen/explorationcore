package dev.explorationcore.milestone;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

public record Milestone(
        String id,
        String counter,
        long threshold,
        String displayName,
        String message,
        int xp,
        List<ItemReward> items,
        int claimblocks,
        String promotionCommand,
        String broadcast
) {
    public Milestone {
        items = List.copyOf(items);
    }

    public String recoveryText() {
        StringBuilder builder = new StringBuilder();
        builder.append("milestone=").append(id);
        if (xp > 0) {
            builder.append(" xp=").append(xp);
        }
        if (!items.isEmpty()) {
            builder.append(" items=");
            for (int index = 0; index < items.size(); index++) {
                if (index > 0) {
                    builder.append(',');
                }
                ItemReward item = items.get(index);
                builder.append(item.amount()).append(' ').append(item.name());
            }
        }
        if (claimblocks > 0) {
            builder.append(" claimblocks=").append(claimblocks);
        }
        if (promotionCommand != null) {
            builder.append(" promotion=\"").append(promotionCommand).append('"');
        }
        if (broadcast != null) {
            builder.append(" broadcast=\"").append(broadcast).append('"');
        }
        return builder.toString();
    }

    public record ItemReward(Material material, int amount, String displayName) {
        public ItemReward(Material material, int amount) {
            this(material, amount, null);
        }

        public String name() {
            return material.name().toLowerCase(Locale.ROOT);
        }

        public String label() {
            if (displayName != null) {
                return displayName;
            }
            StringBuilder title = new StringBuilder();
            for (String word : name().split("_")) {
                if (word.isEmpty()) {
                    continue;
                }
                if (title.length() > 0) {
                    title.append(' ');
                }
                title.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    title.append(word.substring(1));
                }
            }
            return title.toString();
        }
    }
}
