package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

@Component
public class CloudVmMetadataStatusToCoreInstanceMetaDataConverter
        extends AbstractConversionServiceAwareConverter<CloudVmMetaDataStatus, CoreInstanceMetaData> {

    @Override
    public CoreInstanceMetaData convert(CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        CloudInstance cloudInstance = cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance();
        InstanceTemplate template = cloudInstance.getTemplate();
        return new CoreInstanceMetaData(cloudInstance.getInstanceId(), template.getPrivateId(),
                cloudVmMetaDataStatus.getMetaData().getPrivateIp(), cloudVmMetaDataStatus.getMetaData().getPublicIp(),
                template.getVolumes().size(), template.getGroupName());
    }

}
