package com.sequenceiq.environment.api.doc.encryptionprofile;

// This class provides a descriptor for the encryption profile API, including notes and descriptions for operations.
public class EncryptionProfileOpDescription {

    public static final String CREATE = "create encryption profile";

    public static final String GET_BY_NAME = "get encryption profile by name";

    public static final String GET_BY_CRN = "get encryption profile by crn";

    public static final String LIST = "list encryption profiles";

    public static final String DELETE_BY_NAME = "delete encryption profile by name";

    public static final String DELETE_BY_CRN = "delete encryption profile by crn";

    private EncryptionProfileOpDescription() {
    }
}
