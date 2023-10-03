package com.sequenceiq.environment.api.v1.environment.model.base;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.doc.dataservices.DataServicesModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.AwsDataServicesV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.AzureDataServicesV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.GcpDataServicesV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.request.DataServicesRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DataServicesResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = { DataServicesRequest.class, DataServicesResponse.class })
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DataServicesBase implements Serializable {

    @ApiModelProperty(DataServicesModelDescription.AZURE_PARAMETERS)
    @Valid
    private AzureDataServicesV1Parameters azure;

    @ApiModelProperty(DataServicesModelDescription.AWS_PARAMETERS)
    @Valid
    private AwsDataServicesV1Parameters aws;

    @ApiModelProperty(DataServicesModelDescription.GCP_PARAMETERS)
    @Valid
    private GcpDataServicesV1Parameters gcp;

    public AzureDataServicesV1Parameters getAzure() {
        return azure;
    }

    public void setAzure(AzureDataServicesV1Parameters azure) {
        this.azure = azure;
    }

    public AwsDataServicesV1Parameters getAws() {
        return aws;
    }

    public void setAws(AwsDataServicesV1Parameters aws) {
        this.aws = aws;
    }

    public GcpDataServicesV1Parameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpDataServicesV1Parameters gcp) {
        this.gcp = gcp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataServicesBase that)) {
            return false;
        }
        return Objects.equals(azure, that.azure) && Objects.equals(aws, that.aws) && Objects.equals(gcp, that.gcp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(azure, aws, gcp);
    }

    @Override
    public String toString() {
        return "DataServicesBase{" +
                "azure=" + azure +
                ", aws=" + aws +
                ", gcp=" + gcp +
                '}';
    }

}
