package com.sequenceiq.datalake.controller.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.sdx.api.model.event.SdxEventResponse;

@Service
public class SdxEventConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEventConverter.class);

    public SdxEventResponse convert(CloudbreakEventV4Response from) {
        SdxEventResponse to = new SdxEventResponse();
        to.setStackName(from.getStackName());
        to.setAvailabilityZone(from.getAvailabilityZone());
        to.setBlueprintId(from.getBlueprintId());
        to.setEventMessage(from.getEventMessage());
        to.setUserId(from.getUserId());
        to.setWorkspaceId(from.getWorkspaceId());
        to.setLdapDetails(from.getLdapDetails());
        to.setRdsDetails(from.getRdsDetails());
        to.setNodeCount(from.getNodeCount());
        to.setClusterId(from.getClusterId());
        to.setClusterName(from.getClusterName());
        to.setTenantName(from.getTenantName());
        to.setBlueprintName(from.getBlueprintName());
        to.setStackCrn(from.getStackCrn());
        to.setStackStatus(from.getStackStatus());
        to.setClusterStatus(from.getClusterStatus());
        to.setInstanceGroup(from.getInstanceGroup());
        to.setCloud(from.getCloud());
        to.setNotificationType(from.getNotificationType());
        to.setEventTimestamp(from.getEventTimestamp());
        to.setRegion(from.getRegion());
        to.setEventType(from.getEventType());
        return to;
    }
}
