package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class InstanceMetadataToImageIdConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataToImageIdConverter.class);

    public String convert(InstanceMetaData source) {
        Image image = null;
        if (source.getImage() != null && StringUtils.isNotBlank(source.getImage().getValue())) {
            try {
                image = source.getImage().get(Image.class);
            } catch (IOException e) {
                LOGGER.error("InstanceMetadata's image could not be converted to Image class", e);
            }
        } else if (StringUtils.isNotBlank(source.getInstanceId())) {
            LOGGER.debug("No image for instance [{}] in instancemetadata", source.getInstanceId());
        }
        return image == null ? null : image.getImageName();
    }
}
