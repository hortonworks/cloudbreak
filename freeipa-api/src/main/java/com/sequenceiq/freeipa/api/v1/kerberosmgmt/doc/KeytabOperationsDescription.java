package com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc;

public class KeytabOperationsDescription {
    public static final String DESCRIBE_GENERATE_SERVICE_KEYTAB = "Create the host and the service principal and then get the keytab for the provided "
            + "service on a specific host";
    public static final String DESCRIBE_SERVICE_KEYTAB = "Get the keytab for the provided service on a specific host";
    public static final String DESCRIBE_DELETE_SERVICE_PRINCIPAL = "Delete the service principal";
    public static final String DESCRIBE_DELETE_HOST = "Delete the host";

    private KeytabOperationsDescription() {
    }
}
