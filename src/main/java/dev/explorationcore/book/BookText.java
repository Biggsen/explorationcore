package dev.explorationcore.book;

import java.util.ArrayList;
import java.util.List;

final class BookText {
    static final int PAGE_WIDTH = 114;
    static final int LINES_PER_PAGE = 14;
    static final int MAX_PAGE_CHARACTERS = 1024;

    // Advances for the default ascii.png font, code points 32 through 126.
    // A glyph's advance is its rightmost opaque pixel, counted from the left of the cell, plus one.
    // Space is the font's space provider, not the empty bitmap cell.
    private static final int[] ASCII_ADVANCE = {
            4, 2, 4, 6, 6, 6, 6, 2, 4, 4, 4, 6, 2, 6, 2, 6,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 2, 2, 5, 6, 5, 6,
            7, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 4, 6, 6,
            3, 6, 6, 6, 6, 6, 5, 6, 6, 2, 6, 5, 3, 6, 6, 6,
            6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 4, 2, 4, 7
    };
    private static final int UNKNOWN_ADVANCE = 9;

    private BookText() {
    }

    static String rule() {
        int hyphen = advance('-');
        return "-".repeat(PAGE_WIDTH / hyphen);
    }

    static int width(String text) {
        int total = 0;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            total += advance(codePoint);
            index += Character.charCount(codePoint);
        }
        return total;
    }

    static String countedLine(String label, String count) {
        String prefix = label + ": ";
        int spaces = Math.max(0, (PAGE_WIDTH - width(prefix) - width(count)) / advance(' '));
        return prefix + " ".repeat(spaces) + count;
    }

    static int visualLines(String text) {
        return displayedLines(text).size();
    }

    // The lines BookViewScreen draws. Wrapping happens here, once, so a preview
    // can paint the result without a second copy of the line break.
    static List<String> displayedLines(String text) {
        return displayedLines(text, false);
    }

    static List<String> displayedLines(String text, boolean boldFirstSegment) {
        List<String> lines = new ArrayList<>();
        for (DisplayedLine line : layout(text, boldFirstSegment)) {
            lines.add(line.text());
        }
        return lines;
    }

    static List<DisplayedLine> layout(String text, boolean boldFirstSegment) {
        List<DisplayedLine> lines = new ArrayList<>();
        int start = 0;
        boolean first = true;
        for (int index = 0; index <= text.length(); index++) {
            if (index == text.length() || text.charAt(index) == '\n') {
                boolean bold = first && boldFirstSegment;
                for (String line : wrapSegment(text.substring(start, index), bold)) {
                    lines.add(new DisplayedLine(line, bold));
                }
                first = false;
                start = index + 1;
            }
        }
        return lines;
    }

    static int[] asciiAdvances() {
        return ASCII_ADVANCE.clone();
    }

    static int unknownAdvance() {
        return UNKNOWN_ADVANCE;
    }

    static List<String> paginate(List<String> lines) {
        List<String> pages = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int linesUsed = 0;
        int charactersUsed = 0;
        for (String line : lines) {
            int needed = visualLines(line);
            int addedCharacters = line.length() + (current.isEmpty() ? 0 : 1);
            if (!current.isEmpty()
                    && (linesUsed + needed > LINES_PER_PAGE || charactersUsed + addedCharacters > MAX_PAGE_CHARACTERS)) {
                pages.add(String.join("\n", current));
                current.clear();
                linesUsed = 0;
                charactersUsed = 0;
                addedCharacters = line.length();
            }
            current.add(line);
            linesUsed += needed;
            charactersUsed += addedCharacters;
        }
        if (!current.isEmpty()) {
            pages.add(String.join("\n", current));
        }
        return pages;
    }

    private static List<String> wrapSegment(String text, boolean bold) {
        if (text.isEmpty()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        int lineStart = 0;
        int width = 0;
        int index = 0;
        int lastSpace = -1;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            int advance = advance(codePoint) + (bold ? 1 : 0);
            if (width > 0 && width + advance > PAGE_WIDTH) {
                if (lastSpace >= 0) {
                    lines.add(text.substring(lineStart, lastSpace + 1));
                    index = lastSpace + 1;
                    lineStart = index;
                    lastSpace = -1;
                    width = 0;
                    continue;
                }
                lines.add(text.substring(lineStart, index));
                lineStart = index;
                width = 0;
            }
            width += advance;
            if (codePoint == ' ') {
                lastSpace = index;
            }
            index += Character.charCount(codePoint);
        }
        lines.add(text.substring(lineStart));
        return lines;
    }

    private static int advance(int codePoint) {
        if (codePoint >= 32 && codePoint <= 126) {
            return ASCII_ADVANCE[codePoint - 32];
        }
        return UNKNOWN_ADVANCE;
    }
}
