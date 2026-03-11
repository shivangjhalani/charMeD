package com.charmed.renderer;

import com.charmed.model.*;

/** Renders AST to ANSI-styled terminal text (for debug/status display). */
public class AnsiRenderer implements Renderer {

    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String ITALIC = "\033[3m";
    private static final String DIM = "\033[2m";
    private static final String CYAN = "\033[36m";
    private static final String YELLOW = "\033[33m";
    private static final String GREEN = "\033[32m";

    @Override
    public String render(MarkdownNode root) {
        StringBuilder sb = new StringBuilder();
        renderNode(root, sb);
        return sb.toString();
    }

    private void renderNode(MarkdownNode node, StringBuilder sb) {
        switch (node) {
            case DocumentNode d -> d.children().forEach(c -> renderNode(c, sb));
            case HeadingNode h -> {
                sb.append(BOLD).append(CYAN);
                sb.append("#".repeat(h.level())).append(" ");
                h.children().forEach(c -> renderNode(c, sb));
                sb.append(RESET).append("\n");
            }
            case ParagraphNode p -> {
                p.children().forEach(c -> renderNode(c, sb));
                sb.append("\n\n");
            }
            case CodeBlockNode c -> {
                sb.append(DIM).append("```").append(c.language()).append("\n");
                sb.append(c.code()).append("\n```").append(RESET).append("\n\n");
            }
            case InlineCodeNode c -> sb.append(DIM).append("`").append(c.code()).append("`").append(RESET);
            case ListNode l -> l.children().forEach(c -> renderNode(c, sb));
            case ListItemNode li -> {
                sb.append(YELLOW).append("  • ").append(RESET);
                li.children().forEach(c -> renderNode(c, sb));
                sb.append("\n");
            }
            case BlockquoteNode bq -> {
                sb.append(GREEN).append("│ ").append(RESET);
                bq.children().forEach(c -> renderNode(c, sb));
            }
            case BoldNode b -> {
                sb.append(BOLD);
                b.children().forEach(c -> renderNode(c, sb));
                sb.append(RESET);
            }
            case ItalicNode i -> {
                sb.append(ITALIC);
                i.children().forEach(c -> renderNode(c, sb));
                sb.append(RESET);
            }
            case LinkNode l -> sb.append(CYAN).append(l.text()).append(RESET)
                    .append(DIM).append("(").append(l.url()).append(")").append(RESET);
            case HorizontalRuleNode hr -> sb.append(DIM).append("─".repeat(40)).append(RESET).append("\n");
            case TextNode t -> sb.append(t.content());
        }
    }
}
