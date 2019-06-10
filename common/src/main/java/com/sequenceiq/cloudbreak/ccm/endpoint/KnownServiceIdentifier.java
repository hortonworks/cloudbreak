package com.sequenceiq.cloudbreak.ccm.endpoint;

/**
 * Identifiers for known services.
 */
public enum KnownServiceIdentifier {

    /**
     * A service identifier for the nginx web server that runs on gateway nodes.
     */
    GATEWAY,

    /**
     * A service identifier for the Apache Knox proxy server that runs on gateway nodes.
     */
    KNOX;
}
