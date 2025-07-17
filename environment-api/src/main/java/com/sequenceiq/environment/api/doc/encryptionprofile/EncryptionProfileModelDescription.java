package com.sequenceiq.environment.api.doc.encryptionprofile;

// This class provides descriptions for the fields in the EncryptionProfile models.
public class EncryptionProfileModelDescription {

    public static final String CREATED = "creation time of the encryption profile in long";

    public static final String TLS_VERSIONS = "TLS versions supported by the encryption profile, represented as a set of strings";

    public static final String CIPHER_SUITES = "Cipher suites supported by the encryption profile, represented as a set of strings";

    public static final String TLS_CIPHER_SUITES = "A map of TLS versions to their corresponding sets of cipher suites.";

    public static final String STATUS = "Status of the encryption profile, represented as a string (e.g. 'DEFAULT', 'CUSTOM')";

    private EncryptionProfileModelDescription() {
    }
}
