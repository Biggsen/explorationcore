package dev.explorationcore.book;

import java.util.ArrayList;
import java.util.List;

public record PreparedBook(List<List<BookLine>> pages, List<String> notices) {
    public PreparedBook {
        List<List<BookLine>> copied = new ArrayList<>();
        for (List<BookLine> page : pages) {
            copied.add(List.copyOf(page));
        }
        pages = List.copyOf(copied);
        notices = List.copyOf(notices);
    }
}
