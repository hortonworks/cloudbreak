package com.sequenceiq.freeipa.api.v1.kerberos.model.create;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigModelDescription;
import com.sequenceiq.freeipa.api.v1.kerberos.validation.ValidKerberosRequest;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CreateKerberosConfigV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@ValidKerberosRequest
public class CreateKerberosConfigRequest {
    @Valid
    @ApiModelProperty
    private ActiveDirectoryKerberosDescriptor activeDirectory;

    @Valid
    @ApiModelProperty
    private FreeIpaKerberosDescriptor freeIpa;

    @Valid
    @ApiModelProperty
    private MITKerberosDescriptor mit;

    @ApiModelProperty(value = KerberosConfigModelDescription.KERBEROS_CONFIG_NAME, required = true)
    @NotNull
    @NotEmpty
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    public ActiveDirectoryKerberosDescriptor getActiveDirectory() {
        return activeDirectory;
    }

    public void setActiveDirectory(ActiveDirectoryKerberosDescriptor activeDirectoryKerberosDescriptor) {
        activeDirectory = activeDirectoryKerberosDescriptor;
    }

    public FreeIpaKerberosDescriptor getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(FreeIpaKerberosDescriptor freeIpaKerberosDescriptor) {
        freeIpa = freeIpaKerberosDescriptor;
    }

    public MITKerberosDescriptor getMit() {
        return mit;
    }

    public void setMit(MITKerberosDescriptor mitKerberosDescriptor) {
        mit = mitKerberosDescriptor;
    }

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

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }
}
