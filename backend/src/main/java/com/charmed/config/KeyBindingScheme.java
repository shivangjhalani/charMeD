package com.charmed.config;

/** Strategy interface for key binding configurations. */
public interface KeyBindingScheme {
    String getAction(String key, String mode);
    String schemeName();
}
