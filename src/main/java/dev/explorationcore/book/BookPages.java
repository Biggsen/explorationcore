package dev.explorationcore.book;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

public final class BookPages {
    private BookPages() {
    }

    public static List<Component> components(PreparedBook book) {
        List<Component> pages = new ArrayList<>();
        List<String> source = book.pages();
        for (int index = 0; index < source.size(); index++) {
            pages.add(index == 0 ? firstPage(source.get(index)) : body(source.get(index)));
        }
        return pages;
    }

    private static Component firstPage(String page) {
        int split = page.indexOf('\n');
        String serverName = split < 0 ? page : page.substring(0, split);
        Component name = Component.text(serverName)
                .color(TextColor.color(ExplorationBook.SERVER_NAME_COLOR))
                .decorate(TextDecoration.BOLD);
        if (split < 0) {
            return name;
        }
        // The server name is a sibling, so the rest of the page does not inherit its style.
        return Component.empty()
                .append(name)
                .append(Component.newline())
                .append(body(page.substring(split + 1)));
    }

    private static Component body(String text) {
        return Component.text(text).color(TextColor.color(ExplorationBook.BODY_COLOR));
    }
}
