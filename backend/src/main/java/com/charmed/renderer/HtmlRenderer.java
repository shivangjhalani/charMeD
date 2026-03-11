package com.charmed.renderer;

import com.charmed.model.MarkdownNode;
import com.charmed.visitor.HtmlExportVisitor;

/** Renders AST to HTML. Delegates to HtmlExportVisitor. */
public class HtmlRenderer implements Renderer {

    @Override
    public String render(MarkdownNode root) {
        HtmlExportVisitor visitor = new HtmlExportVisitor();
        root.accept(visitor);
        return visitor.getOutput();
    }
}
