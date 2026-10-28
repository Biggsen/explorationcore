package dev.explorationcore.feedback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscoveryPresenterTest {
    @Test
    void titleDurationIsOneHundredTicks() {
        assertEquals(100L, DiscoveryPresenter.durationTicks());
    }
}
