package dev.explorationcore.atlas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class AtlasItem {
    public static final String NAME = "Exploration Atlas";
    private static final NamespacedKey MARKER = new NamespacedKey("explorationcore", "atlas");
    private static final NamespacedKey ID = new NamespacedKey("explorationcore", "atlas_id");
    private static final NamespacedKey WORLD = new NamespacedKey("explorationcore", "atlas_world");
    private static final NamespacedKey ORIGIN_X = new NamespacedKey("explorationcore", "atlas_origin_x");
    private static final NamespacedKey ORIGIN_Z = new NamespacedKey("explorationcore", "atlas_origin_z");
    private static final NamespacedKey MAPS = new NamespacedKey("explorationcore", "atlas_maps");
    private static final Component DISPLAY_NAME = Component.text(NAME, NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false);

    private AtlasItem() {
    }

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Material.KNOWLEDGE_BOOK, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("knowledge book has no item meta");
        }
        meta.displayName(DISPLAY_NAME);
        meta.getPersistentDataContainer().set(MARKER, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(ID, PersistentDataType.STRING, UUID.randomUUID().toString());
        stack.setItemMeta(meta);
        return stack;
    }

    public static boolean isAtlas(ItemStack stack) {
        return id(stack) != null;
    }

    public static UUID id(ItemStack stack) {
        if (stack == null || stack.getType() != Material.KNOWLEDGE_BOOK || stack.isEmpty()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        Byte mark = meta.getPersistentDataContainer().get(MARKER, PersistentDataType.BYTE);
        if (mark == null || mark != (byte) 1) {
            return null;
        }
        String id = meta.getPersistentDataContainer().get(ID, PersistentDataType.STRING);
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static Stored read(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("atlas has no item meta");
        }
        String world = meta.getPersistentDataContainer().get(WORLD, PersistentDataType.STRING);
        Integer originX = meta.getPersistentDataContainer().get(ORIGIN_X, PersistentDataType.INTEGER);
        Integer originZ = meta.getPersistentDataContainer().get(ORIGIN_Z, PersistentDataType.INTEGER);
        ItemStack[] maps = new ItemStack[AtlasLayout.CELLS];
        byte[] bytes = meta.getPersistentDataContainer().get(MAPS, PersistentDataType.BYTE_ARRAY);
        if (bytes != null && bytes.length > 0) {
            ItemStack[] decoded = ItemStack.deserializeItemsFromBytes(bytes);
            for (int index = 0; index < Math.min(maps.length, decoded.length); index++) {
                ItemStack map = decoded[index];
                if (map != null && !map.isEmpty()) {
                    maps[index] = map.clone();
                }
            }
        }
        AtlasLayout.Origin origin = originX == null || originZ == null ? null : new AtlasLayout.Origin(originX, originZ);
        Stored stored = new Stored(world, origin, maps);
        if (!stored.hasMap()) {
            return Stored.empty();
        }
        if (stored.world() == null || stored.origin() == null) {
            throw new IllegalStateException("atlas maps are missing their window");
        }
        return stored;
    }

    public static ItemStack write(ItemStack stack, Stored stored) {
        ItemStack copy = stack.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("atlas has no item meta");
        }
        if (!stored.hasMap()) {
            meta.getPersistentDataContainer().remove(WORLD);
            meta.getPersistentDataContainer().remove(ORIGIN_X);
            meta.getPersistentDataContainer().remove(ORIGIN_Z);
            meta.getPersistentDataContainer().remove(MAPS);
        } else {
            if (stored.world() == null || stored.origin() == null) {
                throw new IllegalStateException("atlas map is missing its window");
            }
            meta.getPersistentDataContainer().set(WORLD, PersistentDataType.STRING, stored.world());
            meta.getPersistentDataContainer().set(ORIGIN_X, PersistentDataType.INTEGER, stored.origin().x());
            meta.getPersistentDataContainer().set(ORIGIN_Z, PersistentDataType.INTEGER, stored.origin().z());
            meta.getPersistentDataContainer().set(MAPS, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(stored.maps()));
        }
        copy.setItemMeta(meta);
        return copy;
    }

    public static final class Stored {
        private final String world;
        private final AtlasLayout.Origin origin;
        private final ItemStack[] maps;

        public Stored(String world, AtlasLayout.Origin origin, ItemStack[] maps) {
            this.world = world;
            this.origin = origin;
            this.maps = new ItemStack[AtlasLayout.CELLS];
            if (maps != null) {
                for (int index = 0; index < this.maps.length && index < maps.length; index++) {
                    ItemStack map = maps[index];
                    if (map != null && !map.isEmpty()) {
                        this.maps[index] = map.clone();
                    }
                }
            }
        }

        public static Stored empty() {
            return new Stored(null, null, null);
        }

        public String world() {
            return world;
        }

        public AtlasLayout.Origin origin() {
            return origin;
        }

        public ItemStack[] maps() {
            ItemStack[] copy = new ItemStack[maps.length];
            for (int index = 0; index < maps.length; index++) {
                if (maps[index] != null) {
                    copy[index] = maps[index].clone();
                }
            }
            return copy;
        }

        public ItemStack map(int index) {
            ItemStack map = maps[index];
            return map == null ? null : map.clone();
        }

        public boolean hasMap() {
            for (ItemStack map : maps) {
                if (map != null && !map.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        public AtlasBoard board() {
            String[] tokens = new String[AtlasLayout.CELLS];
            for (int index = 0; index < tokens.length; index++) {
                if (maps[index] != null && !maps[index].isEmpty()) {
                    tokens[index] = Integer.toString(index);
                }
            }
            return AtlasBoard.of(world, origin, tokens);
        }
    }
}
