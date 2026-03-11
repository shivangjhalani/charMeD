package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.List;

/** Horizontal rule — `---` or `***`. Leaf node with no content. */
public final class HorizontalRuleNode implements MarkdownNode {

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return List.of(); }
    @Override public String rawText() { return ""; }
    @Override public NodeType nodeType() { return NodeType.HORIZONTAL_RULE; }
}
