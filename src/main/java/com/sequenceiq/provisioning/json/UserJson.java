package com.sequenceiq.provisioning.json;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
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

    @JsonProperty("password")
    private String password;

    @JsonProperty("azureStackList")
    private List<AzureStackJson> azureStackList = new ArrayList<>();

    @JsonProperty("awsStackList")
    private List<AwsStackJson> awsStackList = new ArrayList<>();

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<AzureStackJson> getAzureStackList() {
        return azureStackList;
    }

    public void setAzureStackList(List<AzureStackJson> azureStackList) {
        this.azureStackList = azureStackList;
    }

    public List<AwsStackJson> getAwsStackList() {
        return awsStackList;
    }

    public void setAwsStackList(List<AwsStackJson> awsStackList) {
        this.awsStackList = awsStackList;
    }
}