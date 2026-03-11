package com.charmed.renderer;

import com.charmed.model.MarkdownNode;

/**
 * Abstract Decorator — wraps a Renderer, adds styling behavior.
 * Decorator pattern: composable rendering enhancements.
 */
public abstract class StyleDecorator implements Renderer {

    protected final Renderer wrapped;

    protected StyleDecorator(Renderer wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String render(MarkdownNode root) {
        return wrapped.render(root);
    }
}
