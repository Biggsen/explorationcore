package dev.explorationcore.feedback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.time.Duration;

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
                Sound.UI_TOAST_CHALLENGE_COMPLETE,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
        Location at = player.getLocation();
        at.add(0.0, 1.0, 0.0);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, at, 30, 0.5, 0.5, 0.5, 0.1);
    }
}
