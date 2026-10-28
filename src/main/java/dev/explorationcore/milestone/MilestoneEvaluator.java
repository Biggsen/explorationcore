package dev.explorationcore.milestone;

import dev.explorationcore.counter.DiscoveryCounters;
import dev.explorationcore.db.PlayerTally;

import java.util.ArrayList;
import java.util.List;

public final class MilestoneEvaluator {
    private MilestoneEvaluator() {
    }

    public static List<Milestone> crossed(
            Milestones milestones,
            String world,
            String entityType,
            String structureType,
            List<PlayerTally> tallies
    ) {
        List<Milestone> crossed = new ArrayList<>();
        for (String counter : DiscoveryCounters.affected(world, entityType, structureType)) {
            long updated = DiscoveryCounters.count(tallies, counter);
            long previous = updated - 1;
            for (Milestone milestone : milestones.of(counter)) {
                if (previous < milestone.threshold() && updated >= milestone.threshold()) {
                    crossed.add(milestone);
                }
            }
        }
        return List.copyOf(crossed);
    }
}
