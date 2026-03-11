package com.charmed.visitor;

import com.charmed.model.*;
import java.util.ArrayList;
import java.util.List;

/** Finds all occurrences of a search term, returns line/column positions. */
public class SearchVisitor implements NodeVisitor {

    private final String query;
    private final boolean caseSensitive;
    private final List<SearchMatch> matches = new ArrayList<>();
    private int currentLine = 0;

    public record SearchMatch(int line, int column, int length, String context) {}

    public SearchVisitor(String query, boolean caseSensitive) {
        this.query = caseSensitive ? query : query.toLowerCase();
        this.caseSensitive = caseSensitive;
    }

    public List<SearchMatch> getMatches() { return matches; }

    @Override public void visit(DocumentNode node) { visitChildren(node); }
    @Override public void visit(HeadingNode node) { visitChildren(node); currentLine++; }
    @Override public void visit(ParagraphNode node) { visitChildren(node); currentLine += 2; }
    @Override public void visit(CodeBlockNode node) {
        searchInText(node.code());
        currentLine += node.code().split("\n").length + 2;
    }
    @Override public void visit(InlineCodeNode node) { searchInText(node.code()); }
    @Override public void visit(ListNode node) { visitChildren(node); }
    @Override public void visit(ListItemNode node) { visitChildren(node); currentLine++; }
    @Override public void visit(BlockquoteNode node) { visitChildren(node); }
    @Override public void visit(BoldNode node) { visitChildren(node); }
    @Override public void visit(ItalicNode node) { visitChildren(node); }
    @Override public void visit(LinkNode node) { searchInText(node.text()); }
    @Override public void visit(HorizontalRuleNode node) { currentLine++; }
    @Override public void visit(TextNode node) { searchInText(node.content()); }

    private void visitChildren(MarkdownNode node) {
        for (MarkdownNode child : node.children()) {
            child.accept(this);
        }
    }

    private void searchInText(String text) {
        if (text == null || text.isEmpty()) return;
        String searchText = caseSensitive ? text : text.toLowerCase();
        int idx = 0;
        while ((idx = searchText.indexOf(query, idx)) != -1) {
            matches.add(new SearchMatch(currentLine, idx, query.length(), text));
            idx += query.length();
        }
    }
}
