package com.sequenceiq.cloudbreak.cloud.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.microsoft.azure.management.resources.SubscriptionPolicies;
import com.microsoft.azure.management.resources.SubscriptionState;

public class AzureSubscription {

    /**
     * The fully qualified ID for the subscription. For example,
     * /subscriptions/00000000-0000-0000-0000-000000000000.
     */
    @JsonProperty(value = "id", access = Access.WRITE_ONLY)
    private String id;

    /**
     * The subscription ID.
     */
    @JsonProperty(value = "subscriptionId", access = Access.WRITE_ONLY)
    private String subscriptionId;

    /**
     * The tenant ID.
     */
    @JsonProperty(value = "tenantId", access = Access.WRITE_ONLY)
    private String tenantId;

    /**
     * The subscription display name.
     */
    @JsonProperty(value = "displayName", access = Access.WRITE_ONLY)
    private String displayName;

    /**
     * The subscription state. Possible values are Enabled, Warned, PastDue,
     * Disabled, and Deleted. Possible values include: 'Enabled', 'Warned',
     * 'PastDue', 'Disabled', 'Deleted'.
     */
    @JsonProperty(value = "state", access = Access.WRITE_ONLY)
    private SubscriptionState state;

    /**
     * The subscription policies.
     */
    @JsonProperty("subscriptionPolicies")
    private SubscriptionPolicies subscriptionPolicies;

    /**
     * The authorization source of the request. Valid values are one or more
     * combinations of Legacy, RoleBased, Bypassed, Direct and Management. For
     * example, 'Legacy, RoleBased'.
     */
    @JsonProperty("authorizationSource")
    private String authorizationSource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public SubscriptionState getState() {
        return state;
    }

    public void setState(SubscriptionState state) {
        this.state = state;
    }

    public SubscriptionPolicies getSubscriptionPolicies() {
        return subscriptionPolicies;
    }

    public void setSubscriptionPolicies(SubscriptionPolicies subscriptionPolicies) {
        this.subscriptionPolicies = subscriptionPolicies;
    }

    public String getAuthorizationSource() {
        return authorizationSource;
    }

    public void setAuthorizationSource(String authorizationSource) {
        this.authorizationSource = authorizationSource;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "AzureSubscription{" +
                "id='" + id + '\'' +
                ", subscriptionId='" + subscriptionId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", state=" + state +
                ", subscriptionPolicies=" + subscriptionPolicies +
                ", authorizationSource='" + authorizationSource + '\'' +
                '}';
    }
    //END GENERATED CODE
}

