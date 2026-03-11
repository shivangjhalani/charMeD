package com.charmed.config;

/**
 * Editor configuration — Builder pattern with sensible defaults.
 * Holds all editor settings: tab size, auto-indent, word wrap, key bindings.
 */
public class EditorConfig {

    private final int tabSize;
    private final boolean autoIndent;
    private final int wrapWidth;
    private final KeyBindingScheme keyBindings;

    private EditorConfig(Builder builder) {
        this.tabSize = builder.tabSize;
        this.autoIndent = builder.autoIndent;
        this.wrapWidth = builder.wrapWidth;
        this.keyBindings = builder.keyBindings;
    }

    public int tabSize() { return tabSize; }
    public boolean autoIndent() { return autoIndent; }
    public int wrapWidth() { return wrapWidth; }
    public KeyBindingScheme keyBindings() { return keyBindings; }

    public static class Builder {
        private int tabSize = 4;
        private boolean autoIndent = true;
        private int wrapWidth = 80;
        private KeyBindingScheme keyBindings = null;

        public Builder tabSize(int size) { this.tabSize = size; return this; }
        public Builder autoIndent(boolean enabled) { this.autoIndent = enabled; return this; }
        public Builder wrapWidth(int width) { this.wrapWidth = width; return this; }
        public Builder keyBindings(KeyBindingScheme scheme) { this.keyBindings = scheme; return this; }

        public EditorConfig build() {
            return new EditorConfig(this);
        }
    }
}
