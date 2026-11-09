package dev.explorationcore.book;

import java.util.ArrayList;
import java.util.List;

public final class GuideBook {
    public static final String TITLE = "Guides";
    public static final String INDEX_MARK = "⌖ ";
    public static final String RETURN_LINK = "< Guides";
    public static final int INDEX_PAGE = 1;

    private GuideBook() {
    }

    public static PreparedBook prepare(String serverName, List<GuideChapter> chapters) throws GuidesException {
        String name = serverName == null ? "" : serverName;
        List<GuideChapter> source = chapters == null ? List.of() : chapters;
        List<String> titles = new ArrayList<>();
        List<List<List<BookLine>>> chapterPages = new ArrayList<>();
        for (GuideChapter chapter : source) {
            String title = chapter.title().replace("{SERVER_NAME}", name).trim();
            if (title.isBlank()) {
                throw new GuidesException("chapter title is blank");
            }
            titles.add(title);
            chapterPages.add(paginateChapter(title, chapter.text().replace("{SERVER_NAME}", name)));
        }

        int indexPages = paginateLines(indexLines(titles, ones(titles.size()))).size();
        int[] starts = new int[titles.size()];
        int next = indexPages + 1;
        for (int index = 0; index < starts.length; index++) {
            starts[index] = next;
            next += chapterPages.get(index).size();
        }

        List<List<BookLine>> pages = new ArrayList<>(paginateLines(indexLines(titles, starts)));
        for (List<List<BookLine>> chapter : chapterPages) {
            pages.addAll(chapter);
        }
        if (pages.size() > ExplorationBook.MAX_PAGES) {
            throw new GuidesException("guides.yml has " + pages.size() + " pages");
        }
        return new PreparedBook(pages, List.of());
    }

    private static int[] ones(int count) {
        int[] pages = new int[count];
        for (int index = 0; index < count; index++) {
            pages[index] = 1;
        }
        return pages;
    }

    private static List<BookLine> indexLines(List<String> titles, int[] starts) {
        List<BookLine> lines = new ArrayList<>();
        lines.add(BookLine.heading(TITLE));
        if (!titles.isEmpty()) {
            lines.add(BookLine.of(""));
        }
        for (int index = 0; index < titles.size(); index++) {
            for (String line : BookText.displayedLines(INDEX_MARK + titles.get(index), false)) {
                lines.add(BookLine.link(line, starts[index]));
            }
        }
        return lines;
    }

    private static List<List<BookLine>> paginateChapter(String title, String text) {
        List<BookLine> content = new ArrayList<>();
        for (String line : BookText.displayedLines(title, true)) {
            content.add(BookLine.heading(line));
        }
        if (!text.isEmpty()) {
            content.add(BookLine.of(""));
            for (String line : BookText.displayedLines(text, false)) {
                content.add(BookLine.of(line));
            }
        }
        List<List<BookLine>> pages = new ArrayList<>();
        List<BookLine> current = new ArrayList<>();
        int index = 0;
        while (index < content.size()) {
            BookLine line = content.get(index);
            if (!current.isEmpty() && !fits(current, line)) {
                pages.add(finish(current));
                current = new ArrayList<>();
                continue;
            }
            current.add(line);
            index++;
        }
        pages.add(finish(current));
        return pages;
    }

    private static boolean fits(List<BookLine> current, BookLine extra) {
        int visual = 1 + BookText.visualLines(RETURN_LINK);
        int characters = RETURN_LINK.length();
        int count = 0;
        for (int index = 0; index <= current.size(); index++) {
            BookLine line = index == current.size() ? extra : current.get(index);
            visual += BookText.visualLines(line.text());
            if (count > 0) {
                characters++;
            }
            characters += line.text().length();
            count++;
        }
        if (count > 0) {
            characters++;
        }
        characters++;
        return visual <= BookText.LINES_PER_PAGE && characters <= BookText.MAX_PAGE_CHARACTERS;
    }

    private static List<BookLine> finish(List<BookLine> content) {
        List<BookLine> lines = new ArrayList<>(content);
        int used = 0;
        for (BookLine line : lines) {
            used += BookText.visualLines(line.text());
        }
        int blanks = BookText.LINES_PER_PAGE - used - BookText.visualLines(RETURN_LINK);
        while (blanks > 0 && pageCharacters(lines, blanks) > BookText.MAX_PAGE_CHARACTERS) {
            blanks--;
        }
        for (int index = 0; index < Math.max(0, blanks); index++) {
            lines.add(BookLine.of(""));
        }
        lines.add(BookLine.link(RETURN_LINK, INDEX_PAGE));
        return List.copyOf(lines);
    }

    private static int pageCharacters(List<BookLine> lines, int blanks) {
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
        return characters + RETURN_LINK.length();
    }

    private static List<List<BookLine>> paginateLines(List<BookLine> lines) {
        List<String> texts = new ArrayList<>();
        for (BookLine line : lines) {
            texts.add(line.text());
        }
        List<String> paged = BookText.paginate(texts);
        if (paged.isEmpty()) {
            return List.of(List.copyOf(lines));
        }
        List<List<BookLine>> pages = new ArrayList<>();
        int cursor = 0;
        for (String page : paged) {
            List<BookLine> drafted = new ArrayList<>();
            for (String part : page.split("\n", -1)) {
                BookLine line = lines.get(cursor++);
                if (!line.text().equals(part)) {
                    throw new IllegalStateException("Page layout drifted");
                }
                drafted.add(line);
            }
            pages.add(List.copyOf(drafted));
        }
        if (cursor != lines.size()) {
            throw new IllegalStateException("Page layout drifted");
        }
        return pages;
    }
}
