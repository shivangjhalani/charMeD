package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Single list item — children are inline/block nodes. */
public final class ListItemNode implements MarkdownNode {

    private final List<MarkdownNode> children;

    public ListItemNode(List<MarkdownNode> children) {
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return children; }
    @Override public NodeType nodeType() { return NodeType.LIST_ITEM; }
}
