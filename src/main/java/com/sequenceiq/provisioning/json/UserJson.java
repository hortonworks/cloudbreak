package com.sequenceiq.provisioning.json;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.controller.json.InfraRequest;


public class UserJson implements JsonEntity {


    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("roleArn")
    private String roleArn;

    @JsonProperty("subscriptionId")
    private String subscriptionId;

    @JsonProperty("jks")
    private String jks;

    @JsonProperty("azureInfraList")
    private Set<InfraRequest> azureInfraList = new HashSet<>();

    @JsonProperty("awsInfraList")
    private Set<InfraRequest> awsInfraList = new HashSet<>();

    @JsonProperty("azureCloudList")
    private Set<CloudInstanceRequest> azureCloudList = new HashSet<>();

    @JsonProperty("awsCloudList")
    private Set<CloudInstanceRequest> awsCloudList = new HashSet<>();

    public UserJson() {

    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getJks() {
        return jks;
    }

    public void setJks(String jks) {
        this.jks = jks;
    }

    public Set<InfraRequest> getAzureInfraList() {
        return azureInfraList;
    }

    public void setAzureInfraList(Set<InfraRequest> azureInfraList) {
        this.azureInfraList = azureInfraList;
    }

    public Set<InfraRequest> getAwsInfraList() {
        return awsInfraList;
    }

    public void setAwsInfraList(Set<InfraRequest> awsInfraList) {
        this.awsInfraList = awsInfraList;
    }

    public Set<CloudInstanceRequest> getAzureCloudList() {
        return azureCloudList;
    }

    public void setAzureCloudList(Set<CloudInstanceRequest> azureCloudList) {
        this.azureCloudList = azureCloudList;
    }

    public Set<CloudInstanceRequest> getAwsCloudList() {
        return awsCloudList;
    }

    public void setAwsCloudList(Set<CloudInstanceRequest> awsCloudList) {
        this.awsCloudList = awsCloudList;
    }
}