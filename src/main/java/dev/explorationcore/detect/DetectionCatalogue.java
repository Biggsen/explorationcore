package dev.explorationcore.detect;

import java.util.List;

public final class DetectionCatalogue {
    private final List<DetectionSite> sites;
    private final int hearts;
    private final int nerves;

    public DetectionCatalogue(List<DetectionSite> sites) {
        this.sites = List.copyOf(sites);
        int heartCount = 0;
        int nerveCount = 0;
        for (DetectionSite site : this.sites) {
            if ("heart".equals(site.type())) {
                heartCount++;
            } else if ("nerve".equals(site.type())) {
                nerveCount++;
            }
        }
        this.hearts = heartCount;
        this.nerves = nerveCount;
    }

    public List<DetectionSite> sites() {
        return sites;
    }

    public int hearts() {
        return hearts;
    }

    public int nerves() {
        return nerves;
    }

    public boolean isEmpty() {
        return sites.isEmpty();
    }
}
