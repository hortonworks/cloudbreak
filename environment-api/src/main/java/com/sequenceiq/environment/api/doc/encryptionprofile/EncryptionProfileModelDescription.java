package com.sequenceiq.environment.api.doc.encryptionprofile;

// This class provides descriptions for the fields in the EncryptionProfile models.
public class EncryptionProfileModelDescription {

    public static final String CREATED = "creation time of the encryption profile in long";

    public static final String TLS_VERSIONS = "TLS versions supported by the encryption profile, represented as a set of strings";

    public static final String CIPHER_SUITES = "Cipher suites supported by the encryption profile, represented as a set of strings";

    public static final String TLS_CIPHER_SUITES = "A map of TLS versions to their corresponding sets of cipher suites.";

    public static final String CLOUDERA_TLS_CIPHER_SUITES = "A map of TLS versions to their corresponding sets of cipher suites.";

    public static final String STATUS = "Status of the encryption profile, represented as a string (e.g. 'DEFAULT', 'USER_MANAGED')";

    public static final String COMPLIANCE_STANDARDS = "Compliance standards the encryption profile satisfies, represented as a set of strings "
            + "(currently 'FIPS_140_3'). It is computed by the service from the enabled TLS versions and cipher suites of the profile, "
            + "it is not a user-set flag. An empty set means that no known compliance standard is satisfied. "
            + "New values may be added in the future, clients must ignore values they do not know.";

    public static final String RECOMMENDED_CIPHER_SUITES = "The cipher suites recommended to use, represented as a set of strings";

    private EncryptionProfileModelDescription() {
    }
}
