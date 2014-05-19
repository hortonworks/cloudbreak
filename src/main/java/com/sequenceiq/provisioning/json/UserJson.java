package com.sequenceiq.provisioning.json;

import java.util.ArrayList;
import java.util.List;

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
    private List<InfraRequest> azureInfraList = new ArrayList<>();

    @JsonProperty("awsInfraList")
    private List<InfraRequest> awsInfraList = new ArrayList<>();

    @JsonProperty("azureCloudList")
    private List<CloudInstanceRequest> azureCloudList = new ArrayList<>();

    @JsonProperty("awsCloudList")
    private List<CloudInstanceRequest> awsCloudList = new ArrayList<>();

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

    public List<InfraRequest> getAzureInfraList() {
        return azureInfraList;
    }

    public void setAzureInfraList(List<InfraRequest> azureInfraList) {
        this.azureInfraList = azureInfraList;
    }

    public List<InfraRequest> getAwsInfraList() {
        return awsInfraList;
    }

    public void setAwsInfraList(List<InfraRequest> awsInfraList) {
        this.awsInfraList = awsInfraList;
    }

    public List<CloudInstanceRequest> getAzureCloudList() {
        return azureCloudList;
    }

    public void setAzureCloudList(List<CloudInstanceRequest> azureCloudList) {
        this.azureCloudList = azureCloudList;
    }

    public List<CloudInstanceRequest> getAwsCloudList() {
        return awsCloudList;
    }

    public void setAwsCloudList(List<CloudInstanceRequest> awsCloudList) {
        this.awsCloudList = awsCloudList;
    }
}