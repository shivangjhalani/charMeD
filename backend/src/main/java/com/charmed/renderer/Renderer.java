package com.charmed.renderer;

import com.charmed.model.MarkdownNode;

/** Strategy interface for rendering the AST to different output formats. */
public interface Renderer {
    String render(MarkdownNode root);
}
