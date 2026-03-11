package com.charmed.visitor;

import com.charmed.model.*;

/**
 * Visitor interface — one visit() method per sealed node type.
 * Double dispatch via accept()/visit() for extensible AST operations.
 */
public interface NodeVisitor {
    void visit(DocumentNode node);
    void visit(HeadingNode node);
    void visit(ParagraphNode node);
    void visit(CodeBlockNode node);
    void visit(InlineCodeNode node);
    void visit(ListNode node);
    void visit(ListItemNode node);
    void visit(BlockquoteNode node);
    void visit(BoldNode node);
    void visit(ItalicNode node);
    void visit(LinkNode node);
    void visit(HorizontalRuleNode node);
    void visit(TextNode node);
}
