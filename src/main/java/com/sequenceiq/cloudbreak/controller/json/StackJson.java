package com.sequenceiq.cloudbreak.controller.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.controller.validation.ValidStackRequest;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OnFailureAction;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.SubnetJson;

@ValidStackRequest
public class StackJson implements JsonEntity {

    private Long id;
    @Size(max = 40, min = 5, message = "The length of the name has to be in range of 5 to 40")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    private String name;
    @NotNull
    private String region;
    private String owner;
    private String account;
    private boolean publicInAccount;
    private CloudPlatform cloudPlatform;
    @NotNull
    private Long credentialId;
    private Status status;
    private String ambariServerIp;
    @Size(max = 15, min = 5, message = "The length of the username has to be in range of 5 to 15")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The username can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    private String userName;
    @Size(max = 15, min = 5, message = "The length of the password has to be in range of 5 to 15")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The password can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    private String password;
    private String image;
    private String hash;
    private ClusterResponse cluster;
    private String statusReason;
    private OnFailureAction onFailureAction = OnFailureAction.ROLLBACK;
    private FailurePolicyJson failurePolicy;
    @Valid
    private List<InstanceGroupJson> instanceGroups = new ArrayList<>();
    private Integer consulServerCount;
    private List<SubnetJson> allowedSubnets = new ArrayList<>();

    private Map<String, String> parameters = new HashMap<>();

    public StackJson() {
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    public FailurePolicyJson getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicyJson failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public OnFailureAction getOnFailureAction() {
        return onFailureAction;
    }

    public void setOnFailureAction(OnFailureAction onFailureAction) {
        this.onFailureAction = onFailureAction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<InstanceGroupJson> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupJson> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    @JsonProperty("cloudPlatform")
    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    @JsonIgnore
    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public List<SubnetJson> getAllowedSubnets() {
        return allowedSubnets;
    }

    public void setAllowedSubnets(List<SubnetJson> allowedSubnets) {
        this.allowedSubnets = allowedSubnets;
    }

    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    @JsonIgnore
    public void setStatus(Status status) {
        this.status = status;
    }

    @JsonProperty("statusReason")
    public String getStatusReason() {
        return statusReason;
    }

    @JsonIgnore
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @JsonProperty("ambariServerIp")
    public String getAmbariServerIp() {
        return ambariServerIp;
    }

    @JsonIgnore
    public void setAmbariServerIp(String ambariServerIp) {
        this.ambariServerIp = ambariServerIp;
    }

    @JsonProperty("hash")
    public String getHash() {
        return hash;
    }

    @JsonIgnore
    public void setHash(String hash) {
        this.hash = hash;
    }

    @JsonProperty("cluster")
    public ClusterResponse getCluster() {
        return cluster;
    }

    @JsonIgnore
    public void setCluster(ClusterResponse cluster) {
        this.cluster = cluster;
    }

    @JsonProperty("owner")
    public String getOwner() {
        return owner;
    }

    @JsonIgnore
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @JsonProperty("account")
    public String getAccount() {
        return account;
    }

    @JsonIgnore
    public void setAccount(String account) {
        this.account = account;
    }

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @JsonIgnore
    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public Integer getConsulServerCount() {
        return consulServerCount;
    }

    public void setConsulServerCount(Integer consulServerCount) {
        this.consulServerCount = consulServerCount;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
