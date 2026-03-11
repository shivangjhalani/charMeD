package com.charmed.renderer;

import com.charmed.model.MarkdownNode;
import com.charmed.visitor.PlainTextVisitor;

/** Renders AST to unstyled plain text. */
public class PlainTextRenderer implements Renderer {

    @Override
    public String render(MarkdownNode root) {
        PlainTextVisitor visitor = new PlainTextVisitor();
        root.accept(visitor);
        return visitor.getText();
    }
}
