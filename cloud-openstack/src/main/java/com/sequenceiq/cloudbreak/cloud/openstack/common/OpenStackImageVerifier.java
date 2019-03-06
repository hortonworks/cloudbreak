package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.BasicResource;
import org.openstack4j.model.image.Image.Status;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.image.v2.Image.ImageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Component
public class OpenStackImageVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackImageVerifier.class);

    public boolean exist(OSClient<?> osClient, String name) {
        return getStatus(osClient, name).isPresent();
    }

    public Optional<ImageStatus> getStatus(OSClient<?> osClient, String name) {
        try {
            List<? extends BasicResource> imagesV2 = osClient.imagesV2().list(Collections.singletonMap("name", name));
            return getStatusFromImages(osClient, imagesV2, name, true);
        } catch (ProcessingException e) {
            LOGGER.debug("Exception occured during listing openstack images on V2 API. Falling back to V1 API.", e);
            List<? extends BasicResource> imagesV1 = osClient.images().list(Collections.singletonMap("name", name));
            return getStatusFromImages(osClient, imagesV1, name, false);
        }

    }

    private Optional<ImageStatus> getStatusFromImages(OSClient<?> osClient, List<? extends BasicResource> images, String name, boolean v2Resouce) {
        if (CollectionUtils.isEmpty(images)) {
            return handleImageNotFound(osClient, name);
        } else if (images.size() > 1) {
            handleMultipleImagesFound(images, name);
        } else {
            return getStatusForSingleResult(images, name, v2Resouce);
        }
        return Optional.empty();
    }

    private Optional<ImageStatus> handleImageNotFound(OSClient<?> osClient, String name) {
        LOGGER.error("OpenStack image: {} not found", name);
        List<? extends BasicResource> allImages;
        try {
            allImages = osClient.imagesV2().list();
        } catch (ProcessingException e) {
            LOGGER.debug("Exception occured during listing openstack images on V2 API. Falling back to V1 API.", e);
            allImages = osClient.images().list();
        }
        if (allImages != null) {
            for (BasicResource image : allImages) {
                LOGGER.debug("Available images: {}, entry: {}", image.getName(), image);
            }
        }
        LOGGER.debug("OpenStack image: {} not found", name);
        return Optional.empty();
    }

    private void handleMultipleImagesFound(List<? extends BasicResource> images, String name) {
        for (BasicResource image : images) {
            LOGGER.debug("Multiple images found: {}, entry: {}", image.getName(), image);
        }
        List<String> imageIds = images.stream().map(BasicResource::getId).collect(Collectors.toList());
        throw new CloudConnectorException(String.format("Multiple OpenStack images found with ids: %s, image name: %s",
                String.join(", ", imageIds), name));
    }

    private Optional<ImageStatus> getStatusForSingleResult(List<? extends BasicResource> images, String name, boolean v2Resource) {
        ImageStatus imageStatus;
        LOGGER.debug("OpenStack Image found: {}, entry: {}", name, images);
        BasicResource foundImage = images.get(0);
        imageStatus = v2Resource ? ((Image) foundImage).getStatus() : v1StatusToV2Status(((org.openstack4j.model.image.Image) foundImage).getStatus());
        return Optional.of(imageStatus);
    }

    private ImageStatus v1StatusToV2Status(Status status) {
        return ImageStatus.valueOf(status.name());
    }
}
