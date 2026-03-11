package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** List node — ordered or unordered, children are ListItemNode. */
public final class ListNode implements MarkdownNode {

    private final boolean ordered;
    private final List<MarkdownNode> children;

    public ListNode(boolean ordered, List<MarkdownNode> children) {
        this.ordered = ordered;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    public boolean ordered() { return ordered; }

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return children; }
    @Override public NodeType nodeType() { return NodeType.LIST; }
}
