package com.sequenceiq.cloudbreak.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

public class InstanceMetadataDeduplicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataDeduplicator.class);

    private static final Comparator<InstanceMetadataView> BY_START_DATE_THEN_INSTANCE_ID = Comparator
            .comparing(InstanceMetadataView::getStartDate, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(InstanceMetadataView::getInstanceId, Comparator.nullsFirst(Comparator.naturalOrder()));

    private InstanceMetadataDeduplicator() {
    }

    public static <T extends InstanceMetadataView> Set<T> deduplicateByPrivateIp(Collection<T> instanceMetadatas) {
        Set<T> deduplicated = instanceMetadatas.stream()
                .filter(im -> im.getPrivateIp() == null)
                .collect(Collectors.toCollection(HashSet::new));
        deduplicated.addAll(instanceMetadatas.stream()
                .filter(im -> im.getPrivateIp() != null)
                .collect(Collectors.toMap(InstanceMetadataView::getPrivateIp, im -> im, (im1, im2) -> {
                    T newer = BY_START_DATE_THEN_INSTANCE_ID.compare(im1, im2) >= 0 ? im1 : im2;
                    LOGGER.warn("We have the same ip address for two nodes, we will return with the newer node {}! Affected nodes: {}, {}",
                            newer.getInstanceId(), im1, im2);
                    return newer;
                })).values());
        return deduplicated;
    }
}
