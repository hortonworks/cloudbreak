package com.sequenceiq.cloudbreak.api.model.kerberos;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_CONFIG_NAME;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidKerberosRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidKerberosRequest
public class KerberosRequest implements JsonEntity {

    @Valid
    @ApiModelProperty
    private ActiveDirectoryKerberosDescriptor activeDirectory;

    @Valid
    @ApiModelProperty
    private FreeIPAKerberosDescriptor freeIpa;

    @Valid
    @ApiModelProperty
    private MITKerberosDescriptor mit;

    @Valid
    @ApiModelProperty
    private AmbariKerberosDescriptor ambariKerberosDescriptor;

    @ApiModelProperty(value = KERBEROS_CONFIG_NAME, required = true)
    @NotNull
    @NotEmpty
    private String name;

    @ApiModelProperty(ModelDescriptions.ENVIRONMENTS)
    private Set<String> environments = new HashSet<>();

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    public Set<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<String> environments) {
        this.environments = environments;
    }

    public ActiveDirectoryKerberosDescriptor getActiveDirectory() {
        return activeDirectory;
    }

    public void setActiveDirectory(ActiveDirectoryKerberosDescriptor activeDirectoryKerberosDescriptor) {
        this.activeDirectory = activeDirectoryKerberosDescriptor;
    }

    public FreeIPAKerberosDescriptor getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(FreeIPAKerberosDescriptor freeIpaKerberosDescriptor) {
        this.freeIpa = freeIpaKerberosDescriptor;
    }

    public MITKerberosDescriptor getMit() {
        return mit;
    }

    public void setMit(MITKerberosDescriptor mitKerberosDescriptor) {
        this.mit = mitKerberosDescriptor;
    }

    public AmbariKerberosDescriptor getAmbariKerberosDescriptor() {
        return ambariKerberosDescriptor;
    }

    public void setAmbariKerberosDescriptor(AmbariKerberosDescriptor ambariKerberosDescriptor) {
        this.ambariKerberosDescriptor = ambariKerberosDescriptor;
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
}
