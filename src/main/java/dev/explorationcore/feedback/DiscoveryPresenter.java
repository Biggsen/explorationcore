package dev.explorationcore.feedback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class DiscoveryPresenter {
    private static final long FADE_IN_MILLIS = 500L;
    private static final long STAY_MILLIS = 3500L;
    private static final long FADE_OUT_MILLIS = 1000L;
    private static final Title.Times TIMES = Title.Times.times(
            Duration.ofMillis(FADE_IN_MILLIS),
            Duration.ofMillis(STAY_MILLIS),
            Duration.ofMillis(FADE_OUT_MILLIS)
    );

    private DiscoveryPresenter() {
    }

    public static long durationTicks() {
        return (FADE_IN_MILLIS + STAY_MILLIS + FADE_OUT_MILLIS) / 50L;
    }

    public static void show(Player player, DiscoveryNotice notice, String prefix) {
        player.showTitle(Title.title(
                Component.text(notice.title()),
                Component.text(notice.subtitle()),
                TIMES
        ));
        player.sendMessage(NoticeChat.line(prefix, notice.chat()));
        player.playSound(
                player.getLocation(),
                Sound.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.PLAYERS,
                1.0F,
                2.0F
        );
        shimmer(player);
    }

    private static void shimmer(Player player) {
        Location feet = player.getLocation();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 15; i++) {
            player.spawnParticle(
                    Particle.END_ROD,
                    feet.getX() + spread(random, 0.4),
                    feet.getY() + 1.5 + spread(random, 0.3),
                    feet.getZ() + spread(random, 0.4),
                    0,
                    (random.nextDouble() - 0.5) * 0.02,
                    0.03 + random.nextDouble() * 0.02,
                    (random.nextDouble() - 0.5) * 0.02,
                    1.0
            );
        }
    }

    private static double spread(ThreadLocalRandom random, double blocks) {
        return (random.nextDouble() - 0.5) * 2.0 * blocks;
    }
}
