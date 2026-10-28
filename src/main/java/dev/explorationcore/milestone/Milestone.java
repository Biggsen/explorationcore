package dev.explorationcore.milestone;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
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
                builder.append(items.get(index).recoveryToken());
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

    public record ItemReward(
            Material material,
            int amount,
            String displayName,
            List<StoredEnchant> enchantments,
            PotionType potionType
    ) {
        private static final String[] ROMAN = {
                "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
        };

        public ItemReward {
            enchantments = enchantments == null ? List.of() : List.copyOf(enchantments);
        }

        public ItemReward(Material material, int amount) {
            this(material, amount, null, List.of(), null);
        }

        public ItemReward(Material material, int amount, String displayName) {
            this(material, amount, displayName, List.of(), null);
        }

        public record StoredEnchant(String id, int level) {
        }

        public String name() {
            return material.name().toLowerCase(Locale.ROOT);
        }

        public String detail() {
            if (!enchantments.isEmpty()) {
                StringBuilder builder = new StringBuilder("[");
                for (int index = 0; index < enchantments.size(); index++) {
                    if (index > 0) {
                        builder.append('+');
                    }
                    StoredEnchant enchantment = enchantments.get(index);
                    builder.append(enchantment.id()).append('=').append(enchantment.level());
                }
                builder.append(']');
                return builder.toString();
            }
            if (potionType != null) {
                return "[" + potionType.name().toLowerCase(Locale.ROOT) + "]";
            }
            return "";
        }

        public String recoveryToken() {
            return amount + " " + name() + detail();
        }

        public String label() {
            if (displayName != null) {
                return displayName;
            }
            if (potionType != null) {
                return potionLabel();
            }
            if (!enchantments.isEmpty()) {
                return bookLabel();
            }
            return titleCase(name());
        }

        public List<ItemStack> stacks() {
            ItemStack sample = new ItemStack(material, 1);
            writeMeta(sample);
            int maxStack = Math.max(1, sample.getMaxStackSize());
            List<ItemStack> stacks = new ArrayList<>();
            int remaining = amount;
            while (remaining > 0) {
                int size = Math.min(maxStack, remaining);
                ItemStack stack = sample.clone();
                stack.setAmount(size);
                stacks.add(stack);
                remaining -= size;
            }
            return stacks;
        }

        private void writeMeta(ItemStack sample) {
            if (potionType != null) {
                PotionMeta meta = (PotionMeta) sample.getItemMeta();
                if (meta == null) {
                    throw new IllegalStateException("potion has no item meta");
                }
                meta.setBasePotionType(potionType);
                sample.setItemMeta(meta);
                return;
            }
            if (!enchantments.isEmpty()) {
                EnchantmentBooks.apply(sample, enchantments);
            }
        }

        private String bookLabel() {
            StringBuilder builder = new StringBuilder("Enchanted Book (");
            for (int index = 0; index < enchantments.size(); index++) {
                if (index > 0) {
                    builder.append(", ");
                }
                StoredEnchant enchantment = enchantments.get(index);
                builder.append(titleCase(enchantment.id()));
                builder.append(' ');
                builder.append(roman(enchantment.level()));
            }
            builder.append(')');
            return builder.toString();
        }

        private String potionLabel() {
            String id = potionType.name().toLowerCase(Locale.ROOT);
            String effect = id;
            String suffix = "";
            if (id.startsWith("strong_")) {
                effect = id.substring("strong_".length());
                suffix = " II";
            } else if (id.startsWith("long_")) {
                effect = id.substring("long_".length());
                suffix = " (extended)";
            }
            return container() + " of " + titleCase(effect) + suffix;
        }

        private String container() {
            return switch (material) {
                case SPLASH_POTION -> "Splash Potion";
                case LINGERING_POTION -> "Lingering Potion";
                default -> "Potion";
            };
        }

        private static String titleCase(String token) {
            StringBuilder title = new StringBuilder();
            for (String word : token.split("_")) {
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

        private static String roman(int level) {
            if (level >= 1 && level < ROMAN.length) {
                return ROMAN[level];
            }
            return Integer.toString(level);
        }
    }
}
