package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils;

@Component
public class InstanceMetaDataToCloudInstanceConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, CloudInstance> {


    @Override
    public CloudInstance convert(InstanceMetaData metaData) {
        InstanceGroup group = metaData.getInstanceGroup();
        Template template = metaData.getInstanceGroup().getTemplate();
        //TODO fix fake id
        InstanceTemplate instance = new InstanceTemplate(template.getInstanceTypeName(), group.getGroupName(), -1);
        for (int i = 0; i < template.getVolumeCount(); i++) {
            Volume volume = new Volume(VolumeUtils.VOLUME_PREFIX + (i + 1), template.getVolumeTypeName(), template.getVolumeSize());
            instance.addVolume(volume);
        }
        CloudInstanceMetaData md = new CloudInstanceMetaData(metaData.getPrivateIp(), metaData.getPublicIp());

        return new CloudInstance(metaData.getInstanceId(), md, instance);
    }

}
