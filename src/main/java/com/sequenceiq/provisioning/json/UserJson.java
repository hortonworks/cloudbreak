package com.sequenceiq.provisioning.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private List<InfraRequest> azureStackList = new ArrayList<>();

    @JsonProperty("awsInfraList")
    private List<InfraRequest> awsStackList = new ArrayList<>();

    @JsonProperty("azureCloudList")
    private List<InfraRequest> azureCloudList = new ArrayList<>();

    @JsonProperty("awsCloudList")
    private List<InfraRequest> awsCloudList = new ArrayList<>();

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

    public List<InfraRequest> getAzureStackList() {
        return azureStackList;
    }

    public void setAzureStackList(List<InfraRequest> azureStackList) {
        this.azureStackList = azureStackList;
    }

    public List<InfraRequest> getAwsStackList() {
        return awsStackList;
    }

    public void setAwsStackList(List<InfraRequest> awsStackList) {
        this.awsStackList = awsStackList;
    }

    public List<InfraRequest> getAzureCloudList() {
        return azureCloudList;
    }

    public void setAzureCloudList(List<InfraRequest> azureCloudList) {
        this.azureCloudList = azureCloudList;
    }
}