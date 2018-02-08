package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.image.v2.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Component
public class OpenStackImageVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackImageVerifier.class);

    public void exist(OSClient osClient, String name) {
        List<? extends Image> images = osClient.imagesV2().list(Collections.singletonMap("name", name));
        if (images == null || images.isEmpty()) {
            LOGGER.error("OpenStack image: {} not found", name);
            List<? extends Image> allImages = osClient.imagesV2().list();
            if (allImages != null) {
                for (Image image : allImages) {
                    LOGGER.info("Available images: {}, entry: {}", image.getName(), image);
                }
            }
            throw new CloudConnectorException(String.format("OpenStack image: %s not found", name));

        } else if (images.size() > 1) {
            for (Image image : images) {
                LOGGER.info("Multiple images found: {}, entry: {}", image.getName(), image);
            }
            List<String> imageIds = images.stream().map(Image::getId).collect(Collectors.toList());
            throw new CloudConnectorException(String.format("OpenStack image: %s not found with ids: %s", name, String.join(", ", imageIds)));
        } else {
            LOGGER.info("OpenStack Image found: {}, entry: {}", name, images);
        }
    }
}
