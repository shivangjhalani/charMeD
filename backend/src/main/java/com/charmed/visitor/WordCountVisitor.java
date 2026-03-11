package com.charmed.visitor;

import com.charmed.model.*;

/** Counts words across all text nodes in the AST. */
public class WordCountVisitor implements NodeVisitor {

    private int count = 0;

    public int getCount() { return count; }

    @Override public void visit(DocumentNode node) { visitChildren(node); }
    @Override public void visit(HeadingNode node) { visitChildren(node); }
    @Override public void visit(ParagraphNode node) { visitChildren(node); }
    @Override public void visit(CodeBlockNode node) { countWords(node.code()); }
    @Override public void visit(InlineCodeNode node) { countWords(node.code()); }
    @Override public void visit(ListNode node) { visitChildren(node); }
    @Override public void visit(ListItemNode node) { visitChildren(node); }
    @Override public void visit(BlockquoteNode node) { visitChildren(node); }
    @Override public void visit(BoldNode node) { visitChildren(node); }
    @Override public void visit(ItalicNode node) { visitChildren(node); }
    @Override public void visit(LinkNode node) { countWords(node.text()); }
    @Override public void visit(HorizontalRuleNode node) { /* no words */ }
    @Override public void visit(TextNode node) { countWords(node.content()); }

    private void visitChildren(MarkdownNode node) {
        for (MarkdownNode child : node.children()) {
            child.accept(this);
        }
    }

    private void countWords(String text) {
        if (text == null || text.isBlank()) return;
        count += text.trim().split("\\s+").length;
    }
}
