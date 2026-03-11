package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.List;

/** Link — `[text](url)`. Leaf node with text and URL. */
public final class LinkNode implements MarkdownNode {

    private final String text;
    private final String url;

    public LinkNode(String text, String url) {
        this.text = text;
        this.url = url;
    }

    public String text() { return text; }
    public String url() { return url; }

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return List.of(); }
    @Override public String rawText() { return text; }
    @Override public NodeType nodeType() { return NodeType.LINK; }
}
