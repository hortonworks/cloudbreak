package com.sequenceiq.cloudbreak.service.image;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

@Component
public class ImageConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageConverter.class);

    public Image convertJsonToImage(Json imageJson) {
        try {
            return imageJson.get(Image.class);
        } catch (IOException e) {
            String message = "Failed to convert Json to Image";
            LOGGER.error(message, e);
            throw new CloudbreakRuntimeException(message, e);
        }
    }
}
