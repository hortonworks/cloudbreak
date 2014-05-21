package com.sequenceiq.provisioning.controller.json;

import java.util.HashSet;
import java.util.Set;

public class UserJson implements JsonEntity {

    private String firstName;

    private String lastName;

    private String email;

    private String roleArn;

    private String subscriptionId;

    private String jks;

    private Set<InfraJson> azureInfras = new HashSet<>();

    private Set<InfraJson> awsInfras = new HashSet<>();

    private Set<CloudInstanceJson> cloudInstances = new HashSet<>();

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

    public Set<InfraJson> getAwsInfras() {
        return awsInfras;
    }

    public void setAwsInfras(Set<InfraJson> awsInfras) {
        this.awsInfras = awsInfras;
    }

    public Set<InfraJson> getAzureInfras() {
        return azureInfras;
    }

    public void setAzureInfras(Set<InfraJson> azureInfras) {
        this.azureInfras = azureInfras;
    }

    public Set<CloudInstanceJson> getCloudInstances() {
        return cloudInstances;
    }

    public void setCloudInstances(Set<CloudInstanceJson> cloudInstances) {
        this.cloudInstances = cloudInstances;
    }

}