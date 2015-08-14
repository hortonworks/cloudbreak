package com.sequenceiq.cloudbreak.converter.spi;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Template;

@Component
public class InstanceMetaDataToCloudInstanceConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, CloudInstance> {

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Override
    public CloudInstance convert(InstanceMetaData metaData) {
        InstanceGroup group = metaData.getInstanceGroup();
        Template template = metaData.getInstanceGroup().getTemplate();
        InstanceTemplate instance = stackToCloudStackConverter.buildInstanceTemplate(template, group.getGroupName(), metaData.getPrivateId());
        CloudInstanceMetaData md = new CloudInstanceMetaData(metaData.getPrivateIp(), metaData.getPublicIp());
        return new CloudInstance(metaData.getInstanceId(), md, instance);
    }

}
