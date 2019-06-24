package com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc;

public class KeytabModelNotes {
    public static final String GENERATE_KEYTAB_NOTES = "Creates the host if it doesn't exist and also creates the service principal for the given "
            + "service before generating keytab for the principal. Resets the secret for the Kerberos principal redering all other keytabs for that principal"
            + " invalid. Calling the API multiple times for the same principal renders the keytab already generated for that principal invalid. The keytab in "
            + "the response is base64 encoded.";
    public static final String GET_KEYTAB_NOTES = "Retrieves the existing keytab for the service principal derived from the host and service provided. "
            + "Gets the existing keytab without modification not effeting the prior keytab. The keytab in the response is base64 encoded.";
    public static final String DELETE_SERVICE_PRINCIPAL_NOTES = "Deletes the pricipal";
    public static final String DELETE_HOST_NOTES = "Deletes the host and all the principals associated with the host";

    private KeytabModelNotes() {

    }
}
