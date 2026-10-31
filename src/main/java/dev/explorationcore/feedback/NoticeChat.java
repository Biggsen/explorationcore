package dev.explorationcore.feedback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class NoticeChat {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private NoticeChat() {
    }

    public static Component line(String prefix, String message) {
        if (prefix == null || prefix.isBlank()) {
            return LEGACY.deserialize(message);
        }
        return LEGACY.deserialize(prefix + message);
    }
}
