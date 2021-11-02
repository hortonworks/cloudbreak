package com.sequenceiq.redbeams.api;

import io.swagger.annotations.ApiKeyAuthDefinition;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;

@SwaggerDefinition(securityDefinition = @SecurityDefinition(
    apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = RedbeamsApi.CRN_HEADER_API_KEY, in = ApiKeyAuthDefinition.ApiKeyLocation.HEADER, name = "x-cdp-actor-crn")
    }))
public class RedbeamsApi {

    public static final String API_ROOT_CONTEXT = "/api";

    public static final String CRN_HEADER_API_KEY = "crnHeader";

    private RedbeamsApi() {
    }
}

