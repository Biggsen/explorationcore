package dev.explorationcore.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JournalGiftTest {
    @Test
    void playerWithNoExtraArgumentsReceivesTheirOwnJournal() {
        assertInstanceOf(JournalGift.Self.class, JournalGift.decide(true, new String[0]));
        assertInstanceOf(JournalGift.Self.class, JournalGift.decide(true, null));
    }

    @Test
    void playerCannotNameSomeoneElse() {
        JournalGift.Rejected named = assertInstanceOf(
                JournalGift.Rejected.class,
                JournalGift.decide(true, new String[]{"Lowothra"})
        );
        assertEquals(JournalGift.SELF_ONLY, named.message());

        JournalGift.Rejected extra = assertInstanceOf(
                JournalGift.Rejected.class,
                JournalGift.decide(true, new String[]{"Lowothra", "again"})
        );
        assertEquals(JournalGift.SELF_ONLY, extra.message());
    }

    @Test
    void nonPlayerRequiresOneNameToken() {
        JournalGift.Named named = assertInstanceOf(
                JournalGift.Named.class,
                JournalGift.decide(false, new String[]{"Lowothra"})
        );
        assertEquals("Lowothra", named.name());

        assertRejected(JournalGift.decide(false, null));
        assertRejected(JournalGift.decide(false, new String[0]));
        assertRejected(JournalGift.decide(false, new String[]{"Lowothra", "again"}));
        assertRejected(JournalGift.decide(false, new String[]{""}));
        assertRejected(JournalGift.decide(false, new String[]{"   "}));
        assertRejected(JournalGift.decide(false, new String[]{"Low othra"}));
    }

    private static void assertRejected(JournalGift.Decision decision) {
        JournalGift.Rejected rejected = assertInstanceOf(JournalGift.Rejected.class, decision);
        assertEquals(JournalGift.NAME_THE_PLAYER, rejected.message());
    }
}
