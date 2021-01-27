package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.structuredevent.event.SyncDetails;

@Component
public class StackToSyncDetailsConverter extends AbstractConversionServiceAwareConverter<Stack, SyncDetails> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToSyncDetailsConverter.class);

    @Override
    public SyncDetails convert(Stack source) {
        SyncDetails syncDetails = new SyncDetails();
        syncDetails.setId(source.getId());
        syncDetails.setName(source.getName());
        syncDetails.setType(source.getType().name());
        syncDetails.setDescription(source.getDescription());
        syncDetails.setTunnel(source.getTunnel().name());
        syncDetails.setRegion(source.getRegion());
        syncDetails.setAvailabilityZone(source.getAvailabilityZone());
        syncDetails.setCloudPlatform(source.getCloudPlatform());
        syncDetails.setStatus(source.getStatus().name());
        if (source.getStackStatus() != null && source.getStackStatus().getDetailedStackStatus() != null) {
            syncDetails.setDetailedStatus(source.getStackStatus().getDetailedStackStatus().name());
        }
        syncDetails.setStatusReason(source.getStatusReason());
        syncDetails.setDatalakeResourceId(source.getDatalakeResourceId());
        Cluster cluster = source.getCluster();
        if (cluster != null) {
            syncDetails.setClusterCreationStarted(cluster.getCreationStarted());
            syncDetails.setClusterCreationFinished(cluster.getCreationFinished());
            syncDetails.setUpSince(cluster.getUpSince());
        }
        return syncDetails;
    }
}
