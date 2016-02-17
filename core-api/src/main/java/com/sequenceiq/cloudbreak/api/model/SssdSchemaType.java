package com.sequenceiq.cloudbreak.api.model;

public enum SssdSchemaType {

    RFC2307("rfc2307"),
    RFC2307BIS("rfc2307bis"),
    IPA("IPA"),
    AD("AD");

    private String representation;

    SssdSchemaType(String representation) {
        this.representation = representation;
    }

    public String getRepresentation() {
        return representation;
    }
}
