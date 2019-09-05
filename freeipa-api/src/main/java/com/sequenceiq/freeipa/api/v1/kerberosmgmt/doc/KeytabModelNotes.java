package com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc;

public class KeytabModelNotes {
    public static final String GENERATE_SERVICE_KEYTAB_NOTES = "Creates the host if it doesn't exist and also creates the service principal for the given "
            + "service before generating keytab for the principal. Resets the secret for the Kerberos principal redering all other keytabs for that principal"
            + " invalid. Calling the API multiple times for the same principal renders the keytab already generated for that principal invalid. The keytab in "
            + "the response is base64 encoded.";
    public static final String GET_SERVICE_KEYTAB_NOTES = "Retrieves the existing keytab for the service principal derived from the host and service provided. "
            + "Gets the existing keytab without modification and not effecting the prior keytab. The keytab in the response is base64 encoded.";
    public static final String GENERATE_HOST_KEYTAB_NOTES = "Creates the host if it doesn't exist and also generates keytab for the host. Resets the secret "
            + "for the Kerberos principal redering all other keytabs for that principal invalid. Calling the API multiple times for the same principal renders "
            + "the keytab already generated for that principal invalid. The keytab in the response is base64 encoded.";
    public static final String GET_HOST_KEYTAB_NOTES = "Retrieves the existing keytab for the host provided. Gets the existing keytab without modification "
            + "and not effecting the prior keytab. The keytab in the response is base64 encoded.";
    public static final String DELETE_SERVICE_PRINCIPAL_NOTES = "Deletes the principal from the FreeIPA. It also deletes vault secrets associated with the "
            + "principal";
    public static final String DELETE_HOST_NOTES = "Deletes the host and all the principals associated with the host in the FreeIPA. It also deletes vault "
            + "secrets associated with the host.";
    public static final String CLEANUP_NOTES = "Deletes all the secrets that are associated for the given cluster.";

    private KeytabModelNotes() {

    }
}
