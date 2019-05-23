package com.sequenceiq.freeipa.api.v1.kerberos.model.create;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
    private FreeIPAKerberosDescriptor freeIpa;

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

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentId;

    public ActiveDirectoryKerberosDescriptor getActiveDirectory() {
        return activeDirectory;
    }

    public void setActiveDirectory(ActiveDirectoryKerberosDescriptor activeDirectoryKerberosDescriptor) {
        activeDirectory = activeDirectoryKerberosDescriptor;
    }

    public FreeIPAKerberosDescriptor getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(FreeIPAKerberosDescriptor freeIpaKerberosDescriptor) {
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

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }
}
