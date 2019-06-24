package com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc;

public class KeytabModelDescription {
    public static final String SERVICE_NAME = "Service requesting keytab";
    public static final String SERVICE_HOST = "Hostname where the service is running";
    public static final String ID = "Unique Request Id";
    public static final String PRINCIPAL = "Kerberos Service Principal Name";
    public static final String KEYTAB = "Keytab that was requested";

    private KeytabModelDescription() {
    }
}
