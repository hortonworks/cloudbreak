package com.sequenceiq.freeipa.converter.instance;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.converter.image.ImageToImageSettingsResponseConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

@Component
public class InstanceMetaDataToInstanceMetaDataResponseConverter implements Converter<InstanceMetaData, InstanceMetaDataResponse> {

    private static final String NOT_AVAILABLE = "N/A";

    @Inject
    private ImageToImageSettingsResponseConverter imageConverter;

    @Override
    public InstanceMetaDataResponse convert(InstanceMetaData source) {
        InstanceMetaDataResponse metaDataJson = new InstanceMetaDataResponse();
        metaDataJson.setPrivateIp(source.getPrivateIp());
        if (source.getPublicIp() != null) {
            metaDataJson.setPublicIp(source.getPublicIp());
        } else if (source.getPrivateIp() != null) {
            metaDataJson.setPublicIp(NOT_AVAILABLE);
        }
        metaDataJson.setSshPort(source.getSshPort());
        metaDataJson.setInstanceId(source.getInstanceId());
        metaDataJson.setDiscoveryFQDN(source.getDiscoveryFQDN());
        metaDataJson.setInstanceGroup(source.getInstanceGroup().getGroupName());
        metaDataJson.setInstanceStatus(source.getInstanceStatus());
        metaDataJson.setInstanceType(source.getInstanceMetadataType());
        metaDataJson.setLifeCycle(source.getLifeCycle());
        metaDataJson.setAvailabilityZone(source.getAvailabilityZone());
        metaDataJson.setSubnetId(source.getSubnetId());
        if (source.getImage() != null && StringUtils.isNotBlank(source.getImage().getValue())) {
            Image image = source.getImage().getUnchecked(Image.class);
            metaDataJson.setImage(imageConverter.convert(image));
        }
        return metaDataJson;
    }

    public Set<InstanceMetaDataResponse> convert(Iterable<InstanceMetaData> source) {
        return StreamSupport.stream(source.spliterator(), false).map(this::convert).collect(Collectors.toSet());
    }
}
