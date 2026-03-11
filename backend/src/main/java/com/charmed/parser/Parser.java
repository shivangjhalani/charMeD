package com.charmed.parser;

import com.charmed.model.MarkdownNode;

/** Strategy interface for parsing implementations. */
public interface Parser {
    MarkdownNode parse(String input);
}
