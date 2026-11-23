package io.github.qan4ur.vetmos.gui;

/** A text field that can receive keyboard input from the screen. */
public interface TextInput {
    boolean isFocused();

    void setFocused(boolean focused);

    String text();

    void setText(String text);

    /** Editing keys; returns true when consumed. */
    boolean keyPressed(int key);

    boolean charTyped(char chr);
}
