package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class HardwareInfoResponse {

    @ApiModelProperty(ModelDescriptions.InstanceGroupModelDescription.METADATA)
    private InstanceMetaDataJson instanceMetaData;

    @ApiModelProperty(value = ModelDescriptions.HostGroupModelDescription.METADATA)
    private HostMetadataResponse hostMetadata;

    public InstanceMetaDataJson getInstanceMetaData() {
        return instanceMetaData;
    }

    public void setInstanceMetaData(InstanceMetaDataJson instanceMetaData) {
        this.instanceMetaData = instanceMetaData;
    }

    public HostMetadataResponse getHostMetadata() {
        return hostMetadata;
    }

    public void setHostMetadata(HostMetadataResponse hostMetadata) {
        this.hostMetadata = hostMetadata;
    }
}
