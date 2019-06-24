package com.sequenceiq.freeipa.api.v1.keytab.doc;

public class KeytabModelDescription {
    public static final String SERVICE_NAME = "Service requesting keytab";
    public static final String USER_NAME = "User for which keytab is requested";
    public static final String SERVICE_HOST = "Hostname where the service is running";
    public static final String USER_HOST = "Hostname for which user priciapl needs to be created";
    public static final String ID = "Unique Request Id";
    public static final String PRICIPAL = "Kerberos Service Principal Name";
    public static final String KEYTAB = "Keytab that was requested";
}
