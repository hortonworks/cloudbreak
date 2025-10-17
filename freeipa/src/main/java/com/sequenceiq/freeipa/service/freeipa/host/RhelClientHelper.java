package com.sequenceiq.freeipa.service.freeipa.host;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class RhelClientHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RhelClientHelper.class);

    public Optional<String> findRhelInstance(Stack stack) {
        try {
            return getInstanceMetaDataStream(stack)
                    .filter(im -> OsType.isRhel(im.getImage().getUnchecked(Image.class).getOsType()))
                    .map(InstanceMetaData::getDiscoveryFQDN)
                    .findFirst();
        } catch (Exception e) {
            LOGGER.warn("Couldn't find RHEL instance", e);
            return Optional.empty();
        }
    }

    public boolean isClientConnectedToRhel(Stack stack, FreeIpaClient freeIpaClient) {
        try {
            Set<InstanceMetaData> instanceMetaData = getInstanceMetaDataStream(stack)
                    .filter(im -> im.getDiscoveryFQDN().equals(freeIpaClient.getHostname()))
                    .filter(im -> OsType.isRhel(im.getImage().getUnchecked(Image.class).getOsType()))
                    .collect(Collectors.toSet());
            return !instanceMetaData.isEmpty();
        } catch (Exception e) {
            LOGGER.warn("Couldn't identify if client is connected to RHEL or not for client: [{}]", freeIpaClient.getHostname(), e);
            return false;
        }
    }

    public boolean isClientConnectedToSpecificOs(Stack stack, OsType osType) {
        try {
            return getInstanceMetaDataStream(stack)
                    .anyMatch(im -> osType.getOs().equals(im.getImage().getUnchecked(Image.class).getOsType()));
        } catch (Exception e) {
            LOGGER.warn("Couldn't identify if client is connected to {} or not for client: [{}]", osType, e);
            return false;
        }
    }

    private Stream<InstanceMetaData> getInstanceMetaDataStream(Stack stack) {
        return stack.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> StringUtils.isNotBlank(im.getDiscoveryFQDN()))
                .filter(im -> Objects.nonNull(im.getImage()))
                .filter(im -> StringUtils.isNotBlank(im.getImage().getValue()));
    }
}
