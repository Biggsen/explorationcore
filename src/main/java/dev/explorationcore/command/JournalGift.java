package dev.explorationcore.command;

public final class JournalGift {
    public static final String SELF_ONLY = "This command gives you a journal.";
    public static final String NAME_THE_PLAYER = "Name the player who should receive the journal.";

    private JournalGift() {
    }

    public static Decision decide(boolean player, String[] args) {
        if (player) {
            if (args == null || args.length == 0) {
                return new Self();
            }
            return new Rejected(SELF_ONLY);
        }
        if (args == null || args.length != 1 || !singleToken(args[0])) {
            return new Rejected(NAME_THE_PLAYER);
        }
        return new Named(args[0]);
    }

    private static boolean singleToken(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        for (int index = 0; index < name.length(); index++) {
            if (Character.isWhitespace(name.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    public sealed interface Decision {
    }

    public record Self() implements Decision {
    }

    public record Named(String name) implements Decision {
    }

    public record Rejected(String message) implements Decision {
    }
}
