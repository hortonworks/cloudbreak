package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.ClusterModelDescription;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ClusterRequest {

    @Size(max = 40, min = 5, message = "The length of the cluster's name has to be in range of 5 to 40")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name of the cluster can only contain lowercase alphanumeric characters and hyphens and has to start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;
    @NotNull
    @ApiModelProperty(value = ClusterModelDescription.BLUEPRINT_ID, required = true)
    private Long blueprintId;
    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;
    @Valid
    @NotNull
    @ApiModelProperty(required = true)
    private Set<HostGroupJson> hostGroups;
    @ApiModelProperty(ClusterModelDescription.EMAIL_NEEDED)
    private Boolean emailNeeded = Boolean.FALSE;
    private Boolean enableSecurity = Boolean.FALSE;
    @Size(max = 15, min = 5, message = "The length of the username has to be in range of 5 to 15")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The username can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.USERNAME, required = true)
    private String userName;
    @NotNull
    @Size(max = 50, min = 5, message = "The length of the password has to be in range of 5 to 50")
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.PASSWORD, required = true)
    private String password;
    private String kerberosMasterKey;
    private String kerberosAdmin;
    private String kerberosPassword;
    private Boolean validateBlueprint = true;
    @Valid
    private AmbariStackDetailsJson ambariStackDetails;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Set<HostGroupJson> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupJson> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Boolean getEmailNeeded() {
        return emailNeeded;
    }

    public void setEmailNeeded(Boolean emailNeeded) {
        this.emailNeeded = emailNeeded;
    }

    public Boolean getEnableSecurity() {
        return enableSecurity;
    }

    public void setEnableSecurity(Boolean enableSecurity) {
        this.enableSecurity = enableSecurity;
    }

    public String getKerberosMasterKey() {
        return kerberosMasterKey;
    }

    public void setKerberosMasterKey(String kerberosMasterKey) {
        this.kerberosMasterKey = kerberosMasterKey;
    }

    public String getKerberosAdmin() {
        return kerberosAdmin;
    }

    public void setKerberosAdmin(String kerberosAdmin) {
        this.kerberosAdmin = kerberosAdmin;
    }

    public String getKerberosPassword() {
        return kerberosPassword;
    }

    public void setKerberosPassword(String kerberosPassword) {
        this.kerberosPassword = kerberosPassword;
    }

    public boolean getValidateBlueprint() {
        return validateBlueprint == null ? false : validateBlueprint;
    }

    public void setValidateBlueprint(Boolean validateBlueprint) {
        this.validateBlueprint = validateBlueprint;
    }

    public AmbariStackDetailsJson getAmbariStackDetails() {
        return ambariStackDetails;
    }

    public void setAmbariStackDetails(AmbariStackDetailsJson ambariStackDetails) {
        this.ambariStackDetails = ambariStackDetails;
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
}
