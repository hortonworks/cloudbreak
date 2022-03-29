package com.sequenceiq.environment.api.doc;

public class ModelDescriptions {
    public static final String ID = "id of the resource";
    public static final String NAME = "name of the resource";
    public static final String ORIGINAL_NAME = "original name of the resource";
    public static final String DESCRIPTION = "description of the resource";

    public static final String CONNECTOR_NOTES = "Each cloud provider has it's own specific resources like instance types and disk types."
            + " These endpoints are collecting them.";
    public static final String CRN = "global identifiers of the resource";
    public static final String CREATOR = "crn of the creator";

    private ModelDescriptions() {
    }
}
