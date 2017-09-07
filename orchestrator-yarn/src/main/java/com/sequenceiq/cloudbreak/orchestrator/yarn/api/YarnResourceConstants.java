package com.sequenceiq.cloudbreak.orchestrator.yarn.api;

public final class YarnResourceConstants {

    // Setup the base API path for rest client calls
    public static final String CONTEXT_ROOT = "services";

    public static final String API_VERSION = "v1";

    public static final String API_BASE_PATH = CONTEXT_ROOT + '/' + API_VERSION;

    public static final int RETRIES = 300;

    // Application API endpoint path
    public static final String APPLICATIONS_PATH = "applications";

    public static final int HTTP_SUCCESS = 200;

    public static final int HTTP_ACCEPTED = 202;

    public static final int HTTP_NO_CONTENT = 204;

    public static final int HTTP_NOT_FOUND = 404;

    public static final int NON_AGENT_MEMORY = 8192;

    public static final int NON_AGENT_CPUS = 2;

    public static final int ONE_SECOND = 1000;

    public static final int UNLIMITED = -1;

    private YarnResourceConstants() { }

}