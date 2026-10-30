package dev.explorationcore.reward;

import java.util.Optional;

public final class CrateGrant {
    private final boolean accepted;
    private final Optional<String> playerMessage;

    private CrateGrant(boolean accepted, Optional<String> playerMessage) {
        this.accepted = accepted;
        this.playerMessage = playerMessage;
    }

    public static CrateGrant fromDispatch(boolean accepted) {
        return new CrateGrant(accepted, Optional.empty());
    }

    public boolean accepted() {
        return accepted;
    }

    public Optional<String> playerMessage() {
        return playerMessage;
    }
}
