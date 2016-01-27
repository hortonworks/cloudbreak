package com.sequenceiq.cloudbreak.api.model;

public enum SssdSchemaType {

    RFC2307("rfc2307"),
    RFC2307BIS("rfc2307bis");

    private String representation;

    SssdSchemaType(String representation) {
        this.representation = representation;
    }

    public String getRepresentation() {
        return representation;
    }

    public static SssdSchemaType fromString(String representation) {
        for (SssdSchemaType type : SssdSchemaType.values()) {
            if (type.representation.equalsIgnoreCase(representation)) {
                return type;
            }
        }
        return null;
    }
}
