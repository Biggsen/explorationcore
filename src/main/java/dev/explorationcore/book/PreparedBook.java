package dev.explorationcore.book;

import java.util.List;

public record PreparedBook(List<String> pages, List<String> notices) {
    public PreparedBook {
        pages = List.copyOf(pages);
        notices = List.copyOf(notices);
    }
}
