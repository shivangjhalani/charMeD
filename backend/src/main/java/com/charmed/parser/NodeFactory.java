package com.charmed.parser;

import com.charmed.model.*;
import java.util.List;

/**
 * Factory for creating AST nodes. Centralizes node construction,
 * decoupling parser logic from concrete constructors.
 */
public class NodeFactory {

    public DocumentNode createDocument(List<MarkdownNode> children) {
        return new DocumentNode(children);
    }

    public HeadingNode createHeading(int level, List<MarkdownNode> children) {
        return new HeadingNode(level, children);
    }

    public ParagraphNode createParagraph(List<MarkdownNode> children) {
        return new ParagraphNode(children);
    }

    public CodeBlockNode createCodeBlock(String language, String code) {
        return new CodeBlockNode(language, code);
    }

    public InlineCodeNode createInlineCode(String code) {
        return new InlineCodeNode(code);
    }

    public ListNode createList(boolean ordered, List<MarkdownNode> children) {
        return new ListNode(ordered, children);
    }

    public ListItemNode createListItem(List<MarkdownNode> children) {
        return new ListItemNode(children);
    }

    public BlockquoteNode createBlockquote(List<MarkdownNode> children) {
        return new BlockquoteNode(children);
    }

    public BoldNode createBold(List<MarkdownNode> children) {
        return new BoldNode(children);
    }

    public ItalicNode createItalic(List<MarkdownNode> children) {
        return new ItalicNode(children);
    }

    public LinkNode createLink(String text, String url) {
        return new LinkNode(text, url);
    }

    public HorizontalRuleNode createHorizontalRule() {
        return new HorizontalRuleNode();
    }

    public TextNode createText(String content) {
        return new TextNode(content);
    }
}
