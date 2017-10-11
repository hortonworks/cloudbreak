package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterV2Request implements JsonEntity {

    @ApiModelProperty(hidden = true)
    private String name;

    @ApiModelProperty(ClusterModelDescription.EMAIL_NEEDED)
    private Boolean emailNeeded = Boolean.FALSE;

    @ApiModelProperty(ClusterModelDescription.EMAIL_TO)
    private String emailTo;

    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG_ID)
    private Long ldapConfigId;

    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_IDS)
    private Set<Long> rdsConfigIds = new HashSet<>();

    @Valid
    @ApiModelProperty(ClusterModelDescription.RDS_CONFIGS)
    private Set<RDSConfigRequest> rdsConfigJsons = new HashSet<>();

    @Valid
    @ApiModelProperty(StackModelDescription.FILE_SYSTEM)
    private FileSystemRequest fileSystem;

    @ApiModelProperty(ClusterModelDescription.EXECUTOR_TYPE)
    private ExecutorType executorType = ExecutorType.DEFAULT;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_REQUEST)
    private AmbariV2Request ambariRequest;

    @Valid
    @ApiModelProperty(ClusterModelDescription.BYOS_REQUEST)
    private ByosV2Request byosRequest;

    public Boolean getEmailNeeded() {
        return emailNeeded;
    }

    public void setEmailNeeded(Boolean emailNeeded) {
        this.emailNeeded = emailNeeded;
    }

    public Set<Long> getRdsConfigIds() {
        return rdsConfigIds;
    }

    public void setRdsConfigIds(Set<Long> rdsConfigIds) {
        this.rdsConfigIds = rdsConfigIds;
    }

    public Set<RDSConfigRequest> getRdsConfigJsons() {
        return rdsConfigJsons;
    }

    public void setRdsConfigJsons(Set<RDSConfigRequest> rdsConfigJsons) {
        this.rdsConfigJsons = rdsConfigJsons;
    }

    public FileSystemRequest getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystemRequest fileSystem) {
        this.fileSystem = fileSystem;
    }

    public Long getLdapConfigId() {
        return ldapConfigId;
    }

    public void setLdapConfigId(Long ldapConfigId) {
        this.ldapConfigId = ldapConfigId;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public void setExecutorType(ExecutorType executorType) {
        this.executorType = executorType;
    }

    @JsonIgnore
    public String getName() {
        return name;
    }

    @JsonIgnore
    public void setName(String name) {
        this.name = name;
    }

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public AmbariV2Request getAmbariRequest() {
        return ambariRequest;
    }

    public void setAmbariRequest(AmbariV2Request ambariRequest) {
        this.ambariRequest = ambariRequest;
    }

    public ByosV2Request getByosRequest() {
        return byosRequest;
    }

    public void setByosRequest(ByosV2Request byosRequest) {
        this.byosRequest = byosRequest;
    }
}
