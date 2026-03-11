package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Italic — `*italic*`, children are inline nodes. */
public final class ItalicNode implements MarkdownNode {

    private final List<MarkdownNode> children;

    public ItalicNode(List<MarkdownNode> children) {
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return children; }
    @Override public NodeType nodeType() { return NodeType.ITALIC; }
}
