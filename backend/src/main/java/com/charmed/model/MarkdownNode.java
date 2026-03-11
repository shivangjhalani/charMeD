package com.charmed.model;

import com.charmed.visitor.NodeVisitor;
import java.util.List;

/**
 * Sealed interface for the markdown AST. Every markdown element implements this.
 *
 * <p>Patterns: Composite (uniform tree traversal), Visitor (accept hook),
 * Sealed Types (exhaustive pattern matching).</p>
 */
public sealed interface MarkdownNode
        permits DocumentNode, HeadingNode, ParagraphNode, CodeBlockNode,
                InlineCodeNode, ListNode, ListItemNode, BlockquoteNode,
                BoldNode, ItalicNode, LinkNode, HorizontalRuleNode, TextNode {

    /** Visitor pattern hook — double dispatch. */
    void accept(NodeVisitor visitor);

    /** Composite pattern — children list (empty for leaves). */
    List<MarkdownNode> children();

    /** Plain text content (recursive for containers). */
    default String rawText() {
        return children().stream()
                .map(MarkdownNode::rawText)
                .collect(java.util.stream.Collectors.joining());
    }

    /** Enum discriminator for switch expressions. */
    NodeType nodeType();
}
