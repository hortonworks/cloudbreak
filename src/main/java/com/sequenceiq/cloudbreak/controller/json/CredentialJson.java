package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.controller.validation.ValidCredentialRequest;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@ValidCredentialRequest
public class CredentialJson implements JsonEntity {

    private Long id;
    @NotNull
    @Size(max = 20, min = 5)
    private String name;
    private CloudPlatform cloudPlatform;
    private Map<String, Object> parameters = new HashMap<>();
    @Size(max = 50)
    private String description;
    @NotNull
    private String publicKey;
    @NotNull
    private Boolean publicInAccount;

    public CredentialJson() {

    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(Boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }
}
