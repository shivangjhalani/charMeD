package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.List;

/** Inline code — `` `code` ``. Leaf node. */
public final class InlineCodeNode implements MarkdownNode {

    private final String code;

    public InlineCodeNode(String code) {
        this.code = code;
    }

    public String code() { return code; }

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return List.of(); }
    @Override public String rawText() { return code; }
    @Override public NodeType nodeType() { return NodeType.INLINE_CODE; }
}
