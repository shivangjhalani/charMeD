package com.charmed.renderer;

import com.charmed.model.MarkdownNode;

/** Concrete decorator — prepends line numbers to rendered output. */
public class LineNumberDecorator extends StyleDecorator {

    public LineNumberDecorator(Renderer wrapped) {
        super(wrapped);
    }

    @Override
    public String render(MarkdownNode root) {
        String base = wrapped.render(root);
        String[] lines = base.split("\n", -1);
        StringBuilder result = new StringBuilder();
        int width = String.valueOf(lines.length).length();

        for (int i = 0; i < lines.length; i++) {
            result.append(String.format("%" + width + "d │ %s", i + 1, lines[i]));
            if (i < lines.length - 1) result.append("\n");
        }
        return result.toString();
    }
}
