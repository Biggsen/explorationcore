package dev.explorationcore.record;

import java.util.Optional;

@FunctionalInterface
public interface OnlinePlayers {
    Optional<OnlinePlayer> find(String name);
}
