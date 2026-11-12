package dev.explorationcore.book;

import java.util.List;

public record BookLine(
        String text,
        Integer page,
        boolean bold,
        List<Integer> green,
        Integer color,
        boolean italic
) {
    public BookLine {
        if (text == null) {
            throw new IllegalArgumentException("line text is missing");
        }
        green = green == null || green.isEmpty() ? List.of() : List.copyOf(green);
    }

    public static BookLine of(String text) {
        return new BookLine(text, null, false, List.of(), null, false);
    }

    public static BookLine of(String text, List<Integer> green) {
        return new BookLine(text, null, false, green, null, false);
    }

    public static BookLine link(String text, int page) {
        return new BookLine(text, page, false, List.of(), null, false);
    }

    public static BookLine heading(String text) {
        return new BookLine(text, null, true, List.of(), null, false);
    }

    public static BookLine styled(String text, Integer color, boolean bold, boolean italic) {
        return new BookLine(text, null, bold, List.of(), color, italic);
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
