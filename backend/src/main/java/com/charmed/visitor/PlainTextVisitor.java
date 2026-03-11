package com.charmed.visitor;

import com.charmed.model.*;

/** Extracts all text content from the AST as a flat string. */
public class PlainTextVisitor implements NodeVisitor {

    private final StringBuilder text = new StringBuilder();

    public String getText() { return text.toString(); }

    @Override public void visit(DocumentNode node) { visitChildren(node); }
    @Override public void visit(HeadingNode node) { visitChildren(node); text.append("\n"); }
    @Override public void visit(ParagraphNode node) { visitChildren(node); text.append("\n\n"); }
    @Override public void visit(CodeBlockNode node) { text.append(node.code()).append("\n\n"); }
    @Override public void visit(InlineCodeNode node) { text.append(node.code()); }
    @Override public void visit(ListNode node) { visitChildren(node); text.append("\n"); }
    @Override public void visit(ListItemNode node) { text.append("• "); visitChildren(node); text.append("\n"); }
    @Override public void visit(BlockquoteNode node) { visitChildren(node); }
    @Override public void visit(BoldNode node) { visitChildren(node); }
    @Override public void visit(ItalicNode node) { visitChildren(node); }
    @Override public void visit(LinkNode node) { text.append(node.text()); }
    @Override public void visit(HorizontalRuleNode node) { text.append("\n---\n"); }
    @Override public void visit(TextNode node) { text.append(node.content()); }

    private void visitChildren(MarkdownNode node) {
        for (MarkdownNode child : node.children()) {
            child.accept(this);
        }
    }
}
