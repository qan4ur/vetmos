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
}
