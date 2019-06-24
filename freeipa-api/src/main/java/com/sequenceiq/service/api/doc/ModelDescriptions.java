package com.sequenceiq.service.api.doc;

public class ModelDescriptions {
    public static final String ID = "id of the resource";
    public static final String CRN = "CRN of the resource";
    public static final String NAME = "name of the resource";
    public static final String DESCRIPTION = "description of the resource";
    public static final String ENVIRONMENT_NAME = "The name of the environment";
    public static final String ENVIRONMENT_CRN = "CRN of the environment";
    public static final String USER_CRN = "CRN of a user. It is optional as it will be available in \"x-cdp-actor-crn\" header in the request.";

    private ModelDescriptions() {
    }
}
