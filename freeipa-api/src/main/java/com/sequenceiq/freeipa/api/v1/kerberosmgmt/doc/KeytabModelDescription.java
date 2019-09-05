package com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc;

public class KeytabModelDescription {
    public static final String SERVICE_NAME = "Service requesting keytab";
    public static final String SERVICE_HOST = "Hostname where the service is running";
    public static final String ID = "Unique Request Id";
    public static final String PRINCIPAL = "Kerberos Principal Name";
    public static final String KEYTAB = "Keytab that was requested";
    public static final String DO_NOT_RECREATE_KEYTAB = "If true existing keytab won't be overriden for service in normal scenario. "
            + "Preserving the keytab is best effort, it may invalidate prior keytabs.";
    public static final String ROLE = "Role request for adding roles and privileges to service";
    public static final String ROLE_NAME = "Name of the role to be created if not exists";
    public static final String PRIVILEGES = "Privileges for the role";

    private KeytabModelDescription() {
    }
}
