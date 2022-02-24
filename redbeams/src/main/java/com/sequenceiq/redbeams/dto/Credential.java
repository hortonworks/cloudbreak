package com.sequenceiq.redbeams.dto;

import java.util.Optional;

/**
 * Credential information extracted from a credential service response.
 */
public class Credential {

    private final String crn;

    private final String name;

    private final String attributes;

    private final String accountId;

    private final Optional<AzureParameters> azure;

    /**
     * Creates a new credential.
     *
     * @param  crn        credential CRN
     * @param  name       credential name
     * @param  attributes credential attributes
     */
    public Credential(String crn, String name, String attributes, String accountId) {
        this(crn, name, attributes, null, accountId);
    }

    /**
     * Creates a new Azure credential.
     *
     * @param  crn        credential CRN
     * @param  name       credential name
     * @param  attributes credential attributes
     * @param  Azure      Azure-specific credential information (null if a non-Azure credential)
     */
    public Credential(String crn, String name, String attributes, AzureParameters azure, String accountId) {
        this.crn = crn;
        this.name = name;
        this.attributes = attributes;
        this.azure = Optional.ofNullable(azure);
        this.accountId = accountId;
    }

    /**
     * Gets the credential CRN.
     *
     * @return credential CRN
     */
    public String getCrn() {
        return crn;
    }

    /**
     * Gets the credential name.
     *
     * @return credential name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the credential attributes.
     *
     * @return credential attributes
     */
    public String getAttributes() {
        return attributes;
    }

    /**
     * Gets the Azure parameters of the credential.
     *
     * @return Azure parameters of the credential
     */
    public Optional<AzureParameters> getAzure() {
        return azure;
    }

    public String getAccountId() {
        return accountId;
    }

    /**
     * Azure-specific credential parameters.
     */
    public static class AzureParameters {

        private final String subscriptionId;

        /**
         * Creates new Azure parameters.
         *
         * @param  subscriptionId Azure subscription ID
         */
        public AzureParameters(String subscriptionId) {
            this.subscriptionId = subscriptionId;
        }

        /**
         * Gets the subscription ID.
         *
         * @return subscription ID
         */
        public String getSubscriptionId() {
            return subscriptionId;
        }
    }

}
