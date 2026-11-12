package dev.explorationcore.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        return width(text, false);
    }

    static int width(String text, boolean bold) {
        int total = 0;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            total += advance(codePoint) + (bold ? 1 : 0);
            index += Character.charCount(codePoint);
        }
        return total;
    }

    static String center(String text, boolean bold) {
        int textWidth = width(text, bold);
        int space = advance(' ') + (bold ? 1 : 0);
        int slack = PAGE_WIDTH - textWidth;
        int spaces = slack <= 0 ? 0 : (int) Math.round((slack / 2.0) / space);
        while (spaces > 0 && spaces * space + textWidth > PAGE_WIDTH) {
            spaces--;
        }
        return spaces == 0 ? text : " ".repeat(spaces) + text;
    }

    static String countedLine(String label, String count) {
        int spaces = Math.max(1, (PAGE_WIDTH - width(label) - width(count)) / advance(' '));
        return label + " ".repeat(spaces) + count;
    }

    // Where a right-aligned discovered/total suffix begins, padding included.
    // -1 when the line is not a counted line, so a link covers the whole line.
    static int countSplit(String text) {
        int countStart = text.length();
        while (countStart > 0 && text.charAt(countStart - 1) != ' ') {
            countStart--;
        }
        if (countStart == 0 || countStart == text.length() || !isCount(text.substring(countStart))) {
            return -1;
        }
        int labelEnd = countStart;
        while (labelEnd > 0 && text.charAt(labelEnd - 1) == ' ') {
            labelEnd--;
        }
        return labelEnd == 0 ? -1 : labelEnd;
    }

    private static boolean isCount(String token) {
        int slash = token.indexOf('/');
        if (slash <= 0 || slash == token.length() - 1) {
            return false;
        }
        for (int index = 0; index < token.length(); index++) {
            if (index != slash && !Character.isDigit(token.charAt(index))) {
                return false;
            }
        }
        return true;
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

    // Header on the first page, and on later pages when repeatHeader is set.
    // The footer link is the last line, with one blank line above it. Further blank
    // lines keep it on the bottom line when the names do not fill the page. A name
    // that does not fit moves intact onto the next page.
    static List<List<BookLine>> paginateNamed(
            String header,
            List<String> names,
            String footer,
            int footerPage,
            boolean repeatHeader
    ) {
        if (names.isEmpty()) {
            return List.of();
        }
        List<List<BookLine>> pages = new ArrayList<>();
        List<String> current = new ArrayList<>();
        boolean firstPage = true;
        int index = 0;
        while (index < names.size()) {
            String name = names.get(index);
            boolean headerOnPage = firstPage || repeatHeader;
            if (!current.isEmpty() && !fits(header, plus(current, name), headerOnPage, footer)) {
                pages.add(finish(header, current, headerOnPage, footer, footerPage));
                current = new ArrayList<>();
                firstPage = false;
                continue;
            }
            current.add(name);
            index++;
        }
        pages.add(finish(header, current, firstPage || repeatHeader, footer, footerPage));
        return pages;
    }

    private static List<String> plus(List<String> names, String name) {
        List<String> copy = new ArrayList<>(names);
        copy.add(name);
        return copy;
    }

    private static boolean fits(String header, List<String> names, boolean headerShown, String footer) {
        int visual = 0;
        int characters = 0;
        int lines = 0;
        if (headerShown) {
            visual += visualLines(header);
            characters += header.length();
            lines++;
            String divider = rule();
            visual += visualLines(divider);
            characters += divider.length();
            if (lines > 0) {
                characters++;
            }
            lines++;
        }
        for (String name : names) {
            visual += visualLines(name);
            characters += name.length();
            if (lines > 0) {
                characters++;
            }
            lines++;
        }
        visual += 1;
        if (lines > 0) {
            characters++;
        }
        lines++;
        visual += visualLines(footer);
        characters += footer.length();
        if (lines > 0) {
            characters++;
        }
        return visual <= LINES_PER_PAGE && characters <= MAX_PAGE_CHARACTERS;
    }

    private static List<BookLine> finish(
            String header,
            List<String> names,
            boolean headerShown,
            String footer,
            int footerPage
    ) {
        List<BookLine> lines = new ArrayList<>();
        if (headerShown) {
            lines.add(BookLine.of(header));
            lines.add(BookLine.of(rule()));
        }
        for (String name : names) {
            lines.add(BookLine.of(name));
        }
        int used = 0;
        for (BookLine line : lines) {
            used += visualLines(line.text());
        }
        int blanks = LINES_PER_PAGE - used - visualLines(footer);
        while (blanks > 0 && pageCharacters(lines, blanks, footer) > MAX_PAGE_CHARACTERS) {
            blanks--;
        }
        for (int index = 0; index < Math.max(0, blanks); index++) {
            lines.add(BookLine.of(""));
        }
        lines.add(BookLine.link(footer, footerPage));
        return List.copyOf(lines);
    }

    private static int pageCharacters(List<BookLine> lines, int blanks, String footer) {
        int characters = 0;
        int count = 0;
        for (BookLine line : lines) {
            if (count > 0) {
                characters++;
            }
            characters += line.text().length();
            count++;
        }
        for (int index = 0; index < blanks; index++) {
            if (count > 0) {
                characters++;
            }
            count++;
        }
        if (count > 0) {
            characters++;
        }
        return characters + footer.length();
    }

    record Wrapped(String text, List<Integer> green) {
    }

    static List<Wrapped> wrapPlain(String text) {
        List<Wrapped> lines = new ArrayList<>();
        int start = 0;
        for (int index = 0; index <= text.length(); index++) {
            if (index == text.length() || text.charAt(index) == '\n') {
                String segment = text.substring(start, index);
                int[] spans = commandSpans(segment);
                int offset = 0;
                for (String piece : wrapSegment(segment, false)) {
                    lines.add(new Wrapped(piece, localSpans(spans, offset, piece.length())));
                    offset += piece.length();
                }
                if (offset != segment.length()) {
                    throw new IllegalStateException("Wrapped text drifted");
                }
                start = index + 1;
            }
        }
        return lines;
    }

    private static int[] commandSpans(String text) {
        List<Integer> spans = new ArrayList<>();
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) != '/') {
                continue;
            }
            if (index > 0 && Character.isLetterOrDigit(text.charAt(index - 1))) {
                continue;
            }
            int end = commandEnd(text, index);
            if (end == index + 1) {
                continue;
            }
            spans.add(index);
            spans.add(end);
            index = end - 1;
        }
        int[] ranges = new int[spans.size()];
        for (int index = 0; index < spans.size(); index++) {
            ranges[index] = spans.get(index);
        }
        return ranges;
    }

    private static int commandEnd(String text, int slash) {
        int index = slash + 1;
        if (index >= text.length() || !isCommandChar(text.charAt(index))) {
            return slash;
        }
        while (index < text.length() && isCommandChar(text.charAt(index))) {
            index++;
        }
        while (true) {
            if (index >= text.length() || text.charAt(index) != ' ') {
                break;
            }
            int next = index + 1;
            if (next >= text.length()) {
                break;
            }
            char marker = text.charAt(next);
            if (marker == '<') {
                int close = text.indexOf('>', next);
                if (close < 0) {
                    break;
                }
                index = close + 1;
                continue;
            }
            if (marker == '[') {
                int close = text.indexOf(']', next);
                if (close < 0) {
                    break;
                }
                index = close + 1;
                continue;
            }
            if (!isCommandChar(marker) || isSentenceWord(text, next)) {
                break;
            }
            index = next;
            while (index < text.length() && isCommandChar(text.charAt(index))) {
                index++;
            }
        }
        return index;
    }

    private static boolean isCommandChar(char character) {
        return Character.isLetterOrDigit(character) || character == '_' || character == '-';
    }

    private static boolean isSentenceWord(String text, int start) {
        int end = start;
        while (end < text.length() && isCommandChar(text.charAt(end))) {
            end++;
        }
        return switch (text.substring(start, end).toLowerCase(Locale.ROOT)) {
            case "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "is", "it",
                    "of", "on", "or", "the", "them", "to", "with", "you", "your" -> true;
            default -> false;
        };
    }

    private static List<Integer> localSpans(int[] spans, int offset, int length) {
        if (spans.length == 0 || length == 0) {
            return List.of();
        }
        List<Integer> local = new ArrayList<>();
        int end = offset + length;
        for (int index = 0; index + 1 < spans.length; index += 2) {
            int from = Math.max(spans[index], offset);
            int to = Math.min(spans[index + 1], end);
            if (from < to) {
                local.add(from - offset);
                local.add(to - offset);
            }
        }
        return local;
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
