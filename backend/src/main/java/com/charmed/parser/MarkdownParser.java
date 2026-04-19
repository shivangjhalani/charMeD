package com.charmed.parser;

import com.charmed.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recursive-descent markdown parser. Tokenizes input then builds the AST.
 * Strategy pattern — implements the Parser interface.
 */
public class MarkdownParser implements Parser {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern HR_PATTERN = Pattern.compile("^(---+|\\*\\*\\*+|___+)\\s*$");
    private static final Pattern UL_PATTERN = Pattern.compile("^(\\s*)[*\\-+]\\s+(.*)$");
    private static final Pattern OL_PATTERN = Pattern.compile("^(\\s*)\\d+\\.\\s+(.*)$");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s?(.*)$");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("^```(\\w*)\\s*$");

    private final NodeFactory factory;

    public MarkdownParser() {
        this.factory = new NodeFactory();
    }

    public MarkdownParser(NodeFactory factory) {
        this.factory = factory;
    }

    @Override
    public MarkdownNode parse(String input) {
        if (input == null || input.isEmpty()) {
            return factory.createDocument(List.of());
        }
        String[] lines = input.split("\n", -1);
        List<MarkdownNode> blocks = parseBlocks(lines, 0, lines.length);
        return factory.createDocument(blocks);
    }

    // --- Block-level parsing ---

    private List<MarkdownNode> parseBlocks(String[] lines, int start, int end) {
        List<MarkdownNode> blocks = new ArrayList<>();
        int i = start;

        while (i < end) {
            String line = lines[i];

            // Blank line — skip
            if (line.trim().isEmpty()) {
                i++;
                continue;
            }

            // Horizontal rule
            if (HR_PATTERN.matcher(line).matches()) {
                blocks.add(factory.createHorizontalRule());
                i++;
                continue;
            }

            // Code fence
            Matcher codeFence = CODE_FENCE_PATTERN.matcher(line);
            if (codeFence.matches()) {
                String lang = codeFence.group(1);
                StringBuilder code = new StringBuilder();
                i++;
                while (i < end && !CODE_FENCE_PATTERN.matcher(lines[i]).matches()) {
                    if (code.length() > 0) code.append("\n");
                    code.append(lines[i]);
                    i++;
                }
                blocks.add(factory.createCodeBlock(lang, code.toString()));
                if (i < end) i++; // skip closing fence
                continue;
            }

            // Heading
            Matcher heading = HEADING_PATTERN.matcher(line);
            if (heading.matches()) {
                int level = heading.group(1).length();
                List<MarkdownNode> inlines = parseInlines(heading.group(2));
                blocks.add(factory.createHeading(level, inlines));
                i++;
                continue;
            }

            // Blockquote
            if (BLOCKQUOTE_PATTERN.matcher(line).matches()) {
                List<String> quoteLines = new ArrayList<>();
                while (i < end) {
                    Matcher bq = BLOCKQUOTE_PATTERN.matcher(lines[i]);
                    if (bq.matches()) {
                        quoteLines.add(bq.group(1));
                        i++;
                    } else {
                        break;
                    }
                }
                String quoteContent = String.join("\n", quoteLines);
                // Recursively parse the blockquote content
                String[] innerLines = quoteContent.split("\n", -1);
                List<MarkdownNode> innerBlocks = parseBlocks(innerLines, 0, innerLines.length);
                blocks.add(factory.createBlockquote(innerBlocks));
                continue;
            }

            // Unordered list
            if (UL_PATTERN.matcher(line).matches()) {
                List<MarkdownNode> items = new ArrayList<>();
                while (i < end && UL_PATTERN.matcher(lines[i]).matches()) {
                    Matcher m = UL_PATTERN.matcher(lines[i]);
                    m.matches();
                    List<MarkdownNode> inlines = parseInlines(m.group(2));
                    items.add(factory.createListItem(inlines));
                    i++;
                }
                blocks.add(factory.createList(false, items));
                continue;
            }

            // Ordered list
            if (OL_PATTERN.matcher(line).matches()) {
                List<MarkdownNode> items = new ArrayList<>();
                while (i < end && OL_PATTERN.matcher(lines[i]).matches()) {
                    Matcher m = OL_PATTERN.matcher(lines[i]);
                    m.matches();
                    List<MarkdownNode> inlines = parseInlines(m.group(2));
                    items.add(factory.createListItem(inlines));
                    i++;
                }
                blocks.add(factory.createList(true, items));
                continue;
            }

            // Paragraph — default: gather contiguous non-blank, non-special lines
            StringBuilder para = new StringBuilder();
            while (i < end && !lines[i].trim().isEmpty()
                    && !HR_PATTERN.matcher(lines[i]).matches()
                    && !HEADING_PATTERN.matcher(lines[i]).matches()
                    && !CODE_FENCE_PATTERN.matcher(lines[i]).matches()
                    && !BLOCKQUOTE_PATTERN.matcher(lines[i]).matches()
                    && !UL_PATTERN.matcher(lines[i]).matches()
                    && !OL_PATTERN.matcher(lines[i]).matches()) {
                if (para.length() > 0) para.append(" ");
                para.append(lines[i].trim());
                i++;
            }
            if (para.length() > 0) {
                List<MarkdownNode> inlines = parseInlines(para.toString());
                blocks.add(factory.createParagraph(inlines));
            }
        }
        return blocks;
    }

    // --- Inline-level parsing ---

    private List<MarkdownNode> parseInlines(String text) {
        List<MarkdownNode> nodes = new ArrayList<>();
        int i = 0;
        StringBuilder buffer = new StringBuilder();

        while (i < text.length()) {
            char c = text.charAt(i);

            // Bold (**...** or __...__)
            if ((c == '*' || c == '_') && i + 1 < text.length() && text.charAt(i + 1) == c) {
                flushText(buffer, nodes);
                String marker = "" + c + c;
                int close = text.indexOf(marker, i + 2);
                if (close != -1) {
                    String inner = text.substring(i + 2, close);
                    nodes.add(factory.createBold(parseInlines(inner)));
                    i = close + 2;
                    continue;
                }
            }

            // Italic (*...* or _..._)
            if ((c == '*' || c == '_') && (i + 1 >= text.length() || text.charAt(i + 1) != c)) {
                flushText(buffer, nodes);
                int close = text.indexOf(c, i + 1);
                if (close != -1) {
                    String inner = text.substring(i + 1, close);
                    nodes.add(factory.createItalic(parseInlines(inner)));
                    i = close + 1;
                    continue;
                }
            }

            // Inline code (`...`)
            if (c == '`') {
                flushText(buffer, nodes);
                int close = text.indexOf('`', i + 1);
                if (close != -1) {
                    nodes.add(factory.createInlineCode(text.substring(i + 1, close)));
                    i = close + 1;
                    continue;
                }
            }

            // Link [text](url)
            if (c == '[') {
                int closeBracket = text.indexOf(']', i + 1);
                if (closeBracket != -1 && closeBracket + 1 < text.length()
                        && text.charAt(closeBracket + 1) == '(') {
                    int closeParen = text.indexOf(')', closeBracket + 2);
                    if (closeParen != -1) {
                        flushText(buffer, nodes);
                        String linkText = text.substring(i + 1, closeBracket);
                        String linkUrl = text.substring(closeBracket + 2, closeParen);
                        nodes.add(factory.createLink(linkText, linkUrl));
                        i = closeParen + 1;
                        continue;
                    }
                }
            }

            buffer.append(c);
            i++;
        }
        flushText(buffer, nodes);
        return nodes;
    }

    private void flushText(StringBuilder buffer, List<MarkdownNode> nodes) {
        if (buffer.length() > 0) {
            nodes.add(factory.createText(buffer.toString()));
            buffer.setLength(0);
        }
    }
}
