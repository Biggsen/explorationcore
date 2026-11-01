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
        TextColor color = TextColor.color(ExplorationBook.BODY_COLOR);
        if (line.page() == null) {
            return Component.text(line.text()).color(color);
        }
        TextColor link = TextColor.color(ExplorationBook.LINK_COLOR);
        ClickEvent click = ClickEvent.changePage(line.page());
        int split = line.text().indexOf(": ");
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
}
