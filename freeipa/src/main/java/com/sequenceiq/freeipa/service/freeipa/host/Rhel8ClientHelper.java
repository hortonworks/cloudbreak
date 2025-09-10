package com.sequenceiq.freeipa.service.freeipa.host;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class Rhel8ClientHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Rhel8ClientHelper.class);

    public Optional<String> findRhel8Instance(Stack stack) {
        try {
            return stack.getNotDeletedInstanceMetaDataSet().stream()
                    .filter(im -> Objects.nonNull(im.getImage()))
                    .filter(im -> StringUtils.isNotBlank(im.getImage().getValue()))
                    .filter(im -> {
                        Image image = im.getImage().getUnchecked(Image.class);
                        return OsType.RHEL8.getOsType().equalsIgnoreCase(image.getOsType());
                    })
                    .map(InstanceMetaData::getDiscoveryFQDN)
                    .findFirst();
        } catch (Exception e) {
            LOGGER.warn("Couldn't find RHEL8 instance", e);
            return Optional.empty();
        }
    }

    public boolean isClientConnectedToRhel8(Stack stack, FreeIpaClient freeIpaClient) {
        try {
            return stack.getNotDeletedInstanceMetaDataSet().stream()
                    .filter(im -> StringUtils.isNotBlank(im.getDiscoveryFQDN()))
                    .filter(im -> im.getDiscoveryFQDN().equals(freeIpaClient.getHostname()))
                    .map(InstanceMetaData::getImage)
                    .filter(Objects::nonNull)
                    .filter(image -> StringUtils.isNotBlank(image.getValue()))
                    .map(image -> image.getUnchecked(Image.class))
                    .anyMatch(image -> OsType.RHEL8.getOsType().equalsIgnoreCase(image.getOsType()));
        } catch (Exception e) {
            LOGGER.warn("Couldn't identify if client is connected to RHEL8 or not for client: [{}]", freeIpaClient.getHostname(), e);
            return false;
        }
    }
}
