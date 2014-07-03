package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashSet;
import java.util.Set;

public class UserJson implements JsonEntity {

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private Set<CredentialJson> credentials;

    private Set<TemplateJson> azureTemplates = new HashSet<>();

    private Set<TemplateJson> awsTemplates = new HashSet<>();

    private Set<StackJson> stacks = new HashSet<>();

    private Set<BlueprintJson> blueprints = new HashSet<>();

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

    public Set<TemplateJson> getAwsTemplates() {
        return awsTemplates;
    }

    public void setAwsTemplates(Set<TemplateJson> awsTemplates) {
        this.awsTemplates = awsTemplates;
    }

    public Set<TemplateJson> getAzureTemplates() {
        return azureTemplates;
    }

    public void setAzureTemplates(Set<TemplateJson> azureTemplates) {
        this.azureTemplates = azureTemplates;
    }

    public Set<StackJson> getStacks() {
        return stacks;
    }

    public void setStacks(Set<StackJson> stacks) {
        this.stacks = stacks;
    }


    public Set<BlueprintJson> getBlueprints() {
        return blueprints;
    }

    public void setBlueprints(Set<BlueprintJson> blueprints) {
        this.blueprints = blueprints;
    }

    public Set<CredentialJson> getCredentials() {
        return credentials;
    }

    public void setCredentials(Set<CredentialJson> credentials) {
        this.credentials = credentials;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}