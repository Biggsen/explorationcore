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
    private static final Title.Times TIMES = Title.Times.times(
            Duration.ofMillis(500),
            Duration.ofMillis(3500),
            Duration.ofMillis(1000)
    );

    private DiscoveryPresenter() {
    }

    public static void show(Player player, DiscoveryNotice notice) {
        player.showTitle(Title.title(
                Component.text(notice.title()),
                Component.text(notice.subtitle()),
                TIMES
        ));
        player.sendMessage(Component.text(notice.chat()));
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
