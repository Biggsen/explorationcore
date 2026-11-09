package dev.explorationcore.book;

import java.util.List;

public record BookLine(String text, Integer page, boolean bold) {
    public BookLine {
        if (text == null) {
            throw new IllegalArgumentException("line text is missing");
        }
    }

    public static BookLine of(String text) {
        return new BookLine(text, null, false);
    }

    public static BookLine link(String text, int page) {
        return new BookLine(text, page, false);
    }

    public static BookLine heading(String text) {
        return new BookLine(text, null, true);
    }

    public static String join(List<BookLine> lines) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                out.append('\n');
            }
            out.append(lines.get(index).text());
        }
        return out.toString();
    }
}
