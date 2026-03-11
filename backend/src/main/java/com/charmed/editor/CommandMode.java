package com.charmed.editor;

/**
 * Command mode — command-line input (`:w`, `:q`, `:e`, `:/pattern`).
 * Enter executes, Esc returns to Normal.
 */
public final class CommandMode implements EditorMode {

    private final StringBuilder buffer = new StringBuilder();

    @Override
    public HandleResult handleKey(Editor editor, String key) {
        return switch (key) {
            case "esc", "escape" -> {
                buffer.setLength(0);
                editor.transitionTo(new NormalMode());
                yield HandleResult.modeChange("normal");
            }
            case "enter" -> {
                String command = buffer.toString();
                buffer.setLength(0);
                editor.transitionTo(new NormalMode());
                yield HandleResult.command(command);
            }
            case "backspace" -> {
                if (buffer.length() > 0) {
                    buffer.deleteCharAt(buffer.length() - 1);
                }
                yield HandleResult.none();
            }
            default -> {
                if (key.length() == 1) {
                    buffer.append(key);
                }
                yield HandleResult.none();
            }
        };
    }

    public String getBuffer() { return buffer.toString(); }

    @Override
    public String modeName() { return "COMMAND"; }
}
