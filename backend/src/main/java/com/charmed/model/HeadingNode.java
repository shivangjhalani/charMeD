package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Heading node — `# Heading` with level 1-6, children are inline nodes. */
public final class HeadingNode implements MarkdownNode {

    private final int level;
    private final List<MarkdownNode> children;

    public HeadingNode(int level, List<MarkdownNode> children) {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Heading level must be 1-6, got: " + level);
        }
        this.level = level;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    public int level() { return level; }

    @Override public void accept(NodeVisitor visitor) { visitor.visit(this); }
    @Override public List<MarkdownNode> children() { return children; }
    @Override public NodeType nodeType() { return NodeType.HEADING; }
}
