package com.charmed.visitor;

import com.charmed.model.*;

/** Produces HTML output from the AST. */
public class HtmlExportVisitor implements NodeVisitor {

    private final StringBuilder html = new StringBuilder();

    public String getOutput() { return html.toString(); }

    @Override public void visit(DocumentNode node) { visitChildren(node); }

    @Override public void visit(HeadingNode node) {
        html.append("<h").append(node.level()).append(">");
        visitChildren(node);
        html.append("</h").append(node.level()).append(">\n");
    }

    @Override public void visit(ParagraphNode node) {
        html.append("<p>");
        visitChildren(node);
        html.append("</p>\n");
    }

    @Override public void visit(CodeBlockNode node) {
        html.append("<pre><code");
        if (!node.language().isEmpty()) {
            html.append(" class=\"language-").append(escapeHtml(node.language())).append("\"");
        }
        html.append(">").append(escapeHtml(node.code())).append("</code></pre>\n");
    }

    @Override public void visit(InlineCodeNode node) {
        html.append("<code>").append(escapeHtml(node.code())).append("</code>");
    }

    @Override public void visit(ListNode node) {
        html.append(node.ordered() ? "<ol>\n" : "<ul>\n");
        visitChildren(node);
        html.append(node.ordered() ? "</ol>\n" : "</ul>\n");
    }

    @Override public void visit(ListItemNode node) {
        html.append("<li>");
        visitChildren(node);
        html.append("</li>\n");
    }

    @Override public void visit(BlockquoteNode node) {
        html.append("<blockquote>\n");
        visitChildren(node);
        html.append("</blockquote>\n");
    }

    @Override public void visit(BoldNode node) {
        html.append("<strong>");
        visitChildren(node);
        html.append("</strong>");
    }

    @Override public void visit(ItalicNode node) {
        html.append("<em>");
        visitChildren(node);
        html.append("</em>");
    }

    @Override public void visit(LinkNode node) {
        html.append("<a href=\"").append(escapeHtml(node.url())).append("\">");
        html.append(escapeHtml(node.text()));
        html.append("</a>");
    }

    @Override public void visit(HorizontalRuleNode node) {
        html.append("<hr>\n");
    }

    @Override public void visit(TextNode node) {
        html.append(escapeHtml(node.content()));
    }

    private void visitChildren(MarkdownNode node) {
        for (MarkdownNode child : node.children()) {
            child.accept(this);
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
