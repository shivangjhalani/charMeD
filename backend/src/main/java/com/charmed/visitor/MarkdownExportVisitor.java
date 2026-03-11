package com.charmed.visitor;

import com.charmed.model.*;

/** Re-serializes AST back to markdown (useful after transformations). */
public class MarkdownExportVisitor implements NodeVisitor {

    private final StringBuilder md = new StringBuilder();

    public String getOutput() { return md.toString(); }

    @Override public void visit(DocumentNode node) { visitChildren(node); }

    @Override public void visit(HeadingNode node) {
        md.append("#".repeat(node.level())).append(" ");
        visitChildren(node);
        md.append("\n\n");
    }

    @Override public void visit(ParagraphNode node) {
        visitChildren(node);
        md.append("\n\n");
    }

    @Override public void visit(CodeBlockNode node) {
        md.append("```").append(node.language()).append("\n");
        md.append(node.code()).append("\n");
        md.append("```\n\n");
    }

    @Override public void visit(InlineCodeNode node) {
        md.append("`").append(node.code()).append("`");
    }

    @Override public void visit(ListNode node) {
        visitChildren(node);
        md.append("\n");
    }

    @Override public void visit(ListItemNode node) {
        md.append("- ");
        visitChildren(node);
        md.append("\n");
    }

    @Override public void visit(BlockquoteNode node) {
        // Prefix each line with >
        MarkdownExportVisitor inner = new MarkdownExportVisitor();
        for (MarkdownNode child : node.children()) {
            child.accept(inner);
        }
        for (String line : inner.getOutput().split("\n")) {
            md.append("> ").append(line).append("\n");
        }
        md.append("\n");
    }

    @Override public void visit(BoldNode node) {
        md.append("**");
        visitChildren(node);
        md.append("**");
    }

    @Override public void visit(ItalicNode node) {
        md.append("*");
        visitChildren(node);
        md.append("*");
    }

    @Override public void visit(LinkNode node) {
        md.append("[").append(node.text()).append("](").append(node.url()).append(")");
    }

    @Override public void visit(HorizontalRuleNode node) {
        md.append("---\n\n");
    }

    @Override public void visit(TextNode node) {
        md.append(node.content());
    }

    private void visitChildren(MarkdownNode node) {
        for (MarkdownNode child : node.children()) {
            child.accept(this);
        }
    }
}
