package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.List;

/** Raw text content — the terminal leaf node. */
public final class TextNode implements MarkdownNode {

    private final String content;

    public TextNode(String content) {
        this.content = content;
    }

    public String content() { return content; }

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return List.of(); }
    @Override public String rawText() { return content; }
    @Override public NodeType nodeType() { return NodeType.TEXT; }
}
