package com.sequenceiq.environment.api.doc.credential;

public class CredentialDescriptor {
    public static final String CREDENTIAL_NOTES = "Cloudbreak is launching Hadoop clusters on the user's behalf - "
            + "on different cloud providers. One key point is that Cloudbreak does not store your "
            + "Cloud provider account details (such as username, password, keys, private SSL certificates, etc). "
            + "We work around the concept that Identity and Access Management is fully controlled by you - the end user. "
            + "The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.";

    public static final String CREDENTIAL_DESCRIPTION = "Operations on credentials";

    public static final String CREDENTIAL = "Credential of the environment.";

    public static final String CREDENTIAL_VIEW = "Credential view of the environment that does not contain secrets.";

    private CredentialDescriptor() {
    }
}
