package com.sequenceiq.freeipa.service.stack;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaSafeInstanceHealthDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSafeInstanceHealthDetailsService.class);

    @Inject
    private FreeIpaInstanceHealthDetailsService healthDetailsService;

    public NodeHealthDetails getInstanceHealthDetails(Stack stack, InstanceMetaData instance) {
        NodeHealthDetails nodeHealthDetails;
        try {
            nodeHealthDetails = healthDetailsService.getInstanceHealthDetails(stack, instance);
        } catch (Exception e) {
            LOGGER.error("Unable to check the health of FreeIPA instance: {}", instance.getInstanceId(), e);
            nodeHealthDetails = createNodeResponseWithStatusAndIssue(instance, InstanceStatus.UNREACHABLE, e.getLocalizedMessage());
        }
        return nodeHealthDetails;
    }

    public NodeHealthDetails createNodeResponseWithStatusAndIssue(InstanceMetaData instance, InstanceStatus status, String issue) {
        NodeHealthDetails nodeResponse = new NodeHealthDetails();
        nodeResponse.setName(instance.getDiscoveryFQDN());
        nodeResponse.setStatus(status);
        nodeResponse.setInstanceId(instance.getInstanceId());
        nodeResponse.addIssue(issue);
        return nodeResponse;
    }
}
