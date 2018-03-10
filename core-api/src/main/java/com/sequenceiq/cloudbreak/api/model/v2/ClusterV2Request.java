package com.sequenceiq.cloudbreak.api.model.v2;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClusterV2Request implements JsonEntity {

    @ApiModelProperty(hidden = true)
    private String name;

    @ApiModelProperty(ClusterModelDescription.EMAIL_NEEDED)
    private Boolean emailNeeded = Boolean.FALSE;

    @ApiModelProperty(ClusterModelDescription.EMAIL_TO)
    private String emailTo;

    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG_NAME)
    private String ldapConfigName;

    @ApiModelProperty(ClusterModelDescription.RDS_CONFIGS)
    private RdsConfigs rdsConfigs;

    @ApiModelProperty(ClusterModelDescription.PROXY_NAME)
    private String proxyName;

    @Valid
    @ApiModelProperty(StackModelDescription.FILE_SYSTEM)
    private FileSystemRequest fileSystem;

    @ApiModelProperty(ClusterModelDescription.EXECUTOR_TYPE)
    private ExecutorType executorType = ExecutorType.DEFAULT;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_REQUEST)
    private AmbariV2Request ambari;

    public Boolean getEmailNeeded() {
        return emailNeeded;
    }

    public void setEmailNeeded(Boolean emailNeeded) {
        this.emailNeeded = emailNeeded;
    }

    public FileSystemRequest getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystemRequest fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public String getLdapConfigName() {
        return ldapConfigName;
    }

    public void setLdapConfigName(String ldapConfigName) {
        this.ldapConfigName = ldapConfigName;
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

    public AmbariV2Request getAmbari() {
        return ambari;
    }

    public void setAmbari(AmbariV2Request ambari) {
        this.ambari = ambari;
    }

    public RdsConfigs getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(RdsConfigs rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }

    public String getProxyName() {
        return proxyName;
    }

    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }
}
