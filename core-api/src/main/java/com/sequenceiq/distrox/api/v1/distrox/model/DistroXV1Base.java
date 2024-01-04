package com.sequenceiq.distrox.api.v1.distrox.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class DistroXV1Base implements Serializable, CloudPlatformProvider {

    @ValidStackNameFormat
    @ValidStackNameLength
    @NotNull
    @Schema(description = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @Schema(description = StackModelDescription.AWS_PARAMETERS)
    private AwsDistroXV1Parameters aws;

    @Schema(description = StackModelDescription.AZURE_PARAMETERS)
    private AzureDistroXV1Parameters azure;

    @Schema
    private GcpDistroXV1Parameters gcp;

    @Schema
    private YarnDistroXV1Parameters yarn;

    @Schema
    private Long timeToLive;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAws(AwsDistroXV1Parameters aws) {
        this.aws = aws;
    }

    public void setAzure(AzureDistroXV1Parameters azure) {
        this.azure = azure;
    }

    public void setYarn(YarnDistroXV1Parameters yarn) {
        this.yarn = yarn;
    }

    public void setGcp(GcpDistroXV1Parameters gcp) {
        this.gcp = gcp;
    }

    @Override
    public AwsDistroXV1Parameters getAws() {
        return aws;
    }

    @Override
    public AzureDistroXV1Parameters getAzure() {
        return azure;
    }

    @Override
    public YarnDistroXV1Parameters getYarn() {
        return yarn;
    }

    @Override
    public GcpDistroXV1Parameters getGcp() {
        return gcp;
    }

    public Long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Long timeToLive) {
        this.timeToLive = timeToLive;
    }

}
