package dev.explorationcore.atlas;

import java.util.Objects;

public final class AtlasClicks {
    private AtlasClicks() {
    }

    public static Decision place(AtlasBoard board, Offer offer) {
        if (offer == null || !offer.levelFour() || offer.amount() < 1 || offer.token() == null || offer.token().isBlank()) {
            return refuse(board, offer, AtlasMessages.NOT_LEVEL_FOUR);
        }
        if (offer.world() == null || offer.world().isBlank()) {
            return refuse(board, offer, AtlasMessages.OTHER_WORLD);
        }
        AtlasLayout.Origin origin = board.origin() == null
                ? AtlasLayout.origin(offer.gridX(), offer.gridZ())
                : board.origin();
        String world = board.world() == null ? offer.world() : board.world();
        if (!world.equals(offer.world())) {
            return refuse(board, offer, AtlasMessages.OTHER_WORLD);
        }
        if (!AtlasLayout.inside(origin, offer.gridX(), offer.gridZ())) {
            return refuse(board, offer, AtlasMessages.OUTSIDE);
        }
        int index = AtlasLayout.index(origin, offer.gridX(), offer.gridZ());
        if (board.cell(index) != null) {
            return refuse(board, offer, AtlasMessages.OCCUPIED);
        }
        return new Decision(
                board.place(index, offer.token(), world, origin),
                offer.amount() - 1,
                null,
                null
        );
    }

    public static Decision press(AtlasBoard board, Offer offer, int clickedGuiSlot, boolean clickedCellEmpty) {
        int amount = offer == null ? 0 : offer.amount();
        if (!clickedCellEmpty || !AtlasLayout.mapGuiSlot(clickedGuiSlot)) {
            return Decision.keep(board, amount);
        }
        return place(board, offer);
    }

    public static Decision shiftPlace(AtlasBoard board, Offer offer) {
        return place(board, offer);
    }

    public static Decision take(AtlasBoard board, int index) {
        if (index < 0 || index >= AtlasLayout.CELLS || board.cell(index) == null) {
            return Decision.keep(board, 0);
        }
        String token = board.cell(index);
        return new Decision(board.clear(index), 1, token, null);
    }

    public static Decision shiftTake(AtlasBoard board, int index, boolean room) {
        if (!room) {
            return Decision.keep(board, 0);
        }
        return take(board, index);
    }

    public static Decision number(
            AtlasBoard board,
            boolean pressedEmptyMapCell,
            int pressedIndex,
            boolean hotbarIsAtlas,
            Offer hotbar,
            boolean hotbarEmpty
    ) {
        int amount = hotbar == null ? 0 : hotbar.amount();
        if (hotbarIsAtlas) {
            return Decision.keep(board, amount);
        }
        if (!pressedEmptyMapCell) {
            if (hotbarEmpty) {
                return take(board, pressedIndex);
            }
            return Decision.keep(board, amount);
        }
        if (hotbar == null || hotbarEmpty) {
            return Decision.keep(board, amount);
        }
        Decision placed = place(board, hotbar);
        if (AtlasMessages.OCCUPIED.equals(placed.message())) {
            return Decision.keep(board, amount);
        }
        return placed;
    }

    public static Decision drag(AtlasBoard board, int mapCells, boolean touchesFiller, Offer offer) {
        int amount = offer == null ? 0 : offer.amount();
        if (touchesFiller || mapCells != 1) {
            return Decision.keep(board, amount);
        }
        return place(board, offer);
    }

    public static Decision finish(AtlasBoard original, int originalAmount, Decision applied, boolean writeFailed) {
        if (!writeFailed) {
            return applied;
        }
        return new Decision(original, originalAmount, null, AtlasMessages.FAILED);
    }

    public static int changedCell(AtlasBoard before, AtlasBoard after) {
        for (int index = 0; index < AtlasLayout.CELLS; index++) {
            if (!Objects.equals(before.cell(index), after.cell(index))) {
                return index;
            }
        }
        return -1;
    }

    private static Decision refuse(AtlasBoard board, Offer offer, String message) {
        int amount = offer == null ? 0 : offer.amount();
        return new Decision(board, amount, null, message);
    }

    public record Offer(boolean levelFour, String world, int gridX, int gridZ, String token, int amount) {
    }

    public record Decision(AtlasBoard board, int sourceAmount, String given, String message) {
        public static Decision keep(AtlasBoard board, int sourceAmount) {
            return new Decision(board, sourceAmount, null, null);
        }
    }
}
