package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Blockquote — `> quoted text`, children are block nodes. */
public final class BlockquoteNode implements MarkdownNode {

    private final List<MarkdownNode> children;

    public BlockquoteNode(List<MarkdownNode> children) {
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return children; }
    @Override public NodeType nodeType() { return NodeType.BLOCKQUOTE; }
}
