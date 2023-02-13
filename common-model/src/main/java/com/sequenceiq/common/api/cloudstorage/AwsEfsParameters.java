package com.sequenceiq.common.api.cloudstorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsEfsParameters {
    private String name;

    private String backupPolicyStatus;

    private Boolean encrypted;

    private String fileSystemPolicy;

    private Map<String, String> fileSystemTags;

    private String kmsKeyId;

    private List<String> lifeCyclePolicies;

    private String performanceMode;

    private Double provisionedThroughputInMibps;

    private String throughputMode;

    // the following fields are only set at response
    private String filesystemId;

    private String lifeCycleState;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBackupPolicyStatus() {
        return backupPolicyStatus;
    }

    public void setBackupPolicyStatus(String backupPolicyStatus) {
        this.backupPolicyStatus = backupPolicyStatus;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getFileSystemPolicy() {
        return fileSystemPolicy;
    }

    public void setFileSystemPolicy(String fileSystemPolicy) {
        this.fileSystemPolicy = fileSystemPolicy;
    }

    public Map<String, String> getFileSystemTags() {
        return fileSystemTags;
    }

    public void setFileSystemTags(Map<String, String> fileSystemTags) {
        this.fileSystemTags = new HashMap<>(fileSystemTags);
    }

    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    public List<String> getLifeCyclePolicies() {
        return lifeCyclePolicies;
    }

    public void setLifeCyclePolicies(List<String> lifeCyclePolicies) {
        this.lifeCyclePolicies = new ArrayList<>(lifeCyclePolicies);
    }

    public String getPerformanceMode() {
        return performanceMode;
    }

    public void setPerformanceMode(String performanceMode) {
        this.performanceMode = performanceMode;
    }

    public Double getProvisionedThroughputInMibps() {
        return provisionedThroughputInMibps;
    }

    public void setProvisionedThroughputInMibps(Double provisionedThroughputInMibps) {
        this.provisionedThroughputInMibps = provisionedThroughputInMibps;
    }

    public String getThroughputMode() {
        return throughputMode;
    }

    public void setThroughputMode(String throughputMode) {
        this.throughputMode = throughputMode;
    }

    public String getFilesystemId() {
        return filesystemId;
    }

    public void setFilesystemId(String filesystemId) {
        this.filesystemId = filesystemId;
    }

    public String getLifeCycleState() {
        return lifeCycleState;
    }

    public void setLifeCycleState(String lifeCycleState) {
        this.lifeCycleState = lifeCycleState;
    }
}
