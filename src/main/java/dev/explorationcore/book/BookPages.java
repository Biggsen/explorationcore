package dev.explorationcore.book;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

public final class BookPages {
    private BookPages() {
    }

    public static List<Component> components(PreparedBook book) {
        List<Component> pages = new ArrayList<>();
        List<List<BookLine>> source = book.pages();
        for (int index = 0; index < source.size(); index++) {
            pages.add(page(source.get(index), index == 0));
        }
        return pages;
    }

    private static Component page(List<BookLine> lines, boolean firstPage) {
        Component page = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                page = page.append(Component.newline());
            }
            BookLine line = lines.get(index);
            if (firstPage && index == 0) {
                page = page.append(Component.text(line.text())
                        .color(TextColor.color(ExplorationBook.SERVER_NAME_COLOR))
                        .decorate(TextDecoration.BOLD));
            } else {
                page = page.append(body(line));
            }
        }
        return page;
    }

    private static Component body(BookLine line) {
        if (line.page() == null) {
            int rgb = line.color() != null
                    ? line.color()
                    : (line.bold() ? ExplorationBook.SERVER_NAME_COLOR : ExplorationBook.BODY_COLOR);
            Component text = colored(line.text(), line.green(), TextColor.color(rgb));
            if (line.bold()) {
                text = text.decorate(TextDecoration.BOLD);
            }
            if (line.italic()) {
                text = text.decorate(TextDecoration.ITALIC);
            }
            return text;
        }
        TextColor color = TextColor.color(ExplorationBook.BODY_COLOR);
        TextColor link = TextColor.color(ExplorationBook.LINK_COLOR);
        ClickEvent click = ClickEvent.changePage(line.page());
        int split = BookText.countSplit(line.text());
        if (split < 0) {
            return Component.text(line.text())
                    .color(link)
                    .decoration(TextDecoration.UNDERLINED, false)
                    .clickEvent(click);
        }
        return Component.empty()
                .append(Component.text(line.text().substring(0, split))
                        .color(link)
                        .decoration(TextDecoration.UNDERLINED, false)
                        .clickEvent(click))
                .append(Component.text(line.text().substring(split))
                        .color(color)
                        .decoration(TextDecoration.UNDERLINED, false));
    }

    private static Component colored(String text, List<Integer> green, TextColor color) {
        if (green.isEmpty()) {
            return Component.text(text).color(color);
        }
        TextColor command = TextColor.color(GuideBook.COMMAND_COLOR);
        Component out = Component.empty();
        int cursor = 0;
        for (int index = 0; index + 1 < green.size(); index += 2) {
            int start = green.get(index);
            int end = green.get(index + 1);
            if (start > cursor) {
                out = out.append(Component.text(text.substring(cursor, start)).color(color));
            }
            out = out.append(Component.text(text.substring(start, end)).color(command));
            cursor = end;
        }
        if (cursor < text.length()) {
            out = out.append(Component.text(text.substring(cursor)).color(color));
        }
        return out;
    }
}
