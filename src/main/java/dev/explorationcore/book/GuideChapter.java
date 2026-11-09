package dev.explorationcore.book;

public record GuideChapter(String title, String text) {
    public GuideChapter {
        if (title == null) {
            throw new IllegalArgumentException("chapter title is missing");
        }
        if (text == null) {
            throw new IllegalArgumentException("chapter text is missing");
        }
    }
}
