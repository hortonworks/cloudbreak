package com.sequenceiq.cloudbreak.shell.model;

/**
 * Holds information about the focus. Focus give you the ability to
 * provide context sensitive commands.
 */
public class Focus {

    private final String value;

    private final FocusType type;

    public Focus(String value, FocusType type) {
        this.value = value;
        this.type = type;
    }

    public String getPrefix() {
        return type.prefix();
    }

    public FocusType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    /**
     * Checks if the current focus exists with the provided one.
     *
     * @param type type to check with the current
     * @return true if they match false otherwise
     */
    public boolean isType(FocusType type) {
        return this.type == type;
    }
}
