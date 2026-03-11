package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.List;

/** Fenced code block — ` ```language ... ``` `. Leaf node. */
public final class CodeBlockNode implements MarkdownNode {

    private final String language;
    private final String code;

    public CodeBlockNode(String language, String code) {
        this.language = language != null ? language : "";
        this.code = code;
    }

    public String language() { return language; }
    public String code() { return code; }

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return List.of(); }
    @Override public String rawText() { return code; }
    @Override public NodeType nodeType() { return NodeType.CODE_BLOCK; }
}
