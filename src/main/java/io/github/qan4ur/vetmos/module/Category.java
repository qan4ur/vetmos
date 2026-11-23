package io.github.qan4ur.vetmos.module;

public enum Category {
    COMBAT("Бой"),
    HUD("HUD"),
    RENDER("Рендер"),
    WORLD("Мир"),
    CLIENT("Клиент");

    public final String title;

    Category(String title) {
        this.title = title;
    }

    /** Title in the selected interface language. */
    public String displayTitle() {
        return io.github.qan4ur.vetmos.util.Lang.tr("category:" + name(), title);
    }
}
