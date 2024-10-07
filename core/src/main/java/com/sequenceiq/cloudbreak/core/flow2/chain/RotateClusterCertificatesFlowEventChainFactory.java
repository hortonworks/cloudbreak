package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCertificatesRotationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class RotateClusterCertificatesFlowEventChainFactory implements FlowEventChainFactory<ClusterCertificatesRotationTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateClusterCertificatesFlowEventChainFactory.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.ROTATE_CLUSTER_CERTIFICATES_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterCertificatesRotationTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        addClusterScaleTriggerEventIfNeeded(event.getResourceId()).ifPresent(flowEventChain::add);
        if (!event.getSkipSaltUpdate()) {
            flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        }
        flowEventChain.add(new ClusterCertificatesRotationTriggerEvent(ClusterCertificatesRotationEvent.CLUSTER_CMCA_ROTATION_EVENT.event(),
                event.getResourceId(), event.accepted(), event.getCertificateRotationType(), event.getSkipSaltUpdate()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private Optional<StopStartUpscaleTriggerEvent> addClusterScaleTriggerEventIfNeeded(Long stackId) {
        String hostGroup = "";
        List<String> stoppedInstances = new ArrayList<>();
        for (InstanceMetadataView instanceMetadataView : instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(stackId)) {
            if (instanceMetadataView.getInstanceStatus().equals(InstanceStatus.STOPPED)) {
                stoppedInstances.add(instanceMetadataView.getInstanceId());
                hostGroup = instanceMetadataView.getInstanceGroupName();
            }
        }

        if (stoppedInstances.isEmpty()) {
            LOGGER.info("There are no stopped instances present.");
            return Optional.empty();
        } else {
            LOGGER.info("There are stopped instances present. Trying to start them before private certificate rotation. Stopped instances: {}",
                    stoppedInstances.size());
            return Optional.of(new StopStartUpscaleTriggerEvent(
                    StopStartUpscaleEvent.STOPSTART_UPSCALE_TRIGGER_EVENT.event(),
                    stackId,
                    hostGroup,
                    stoppedInstances.size(),
                    ClusterManagerType.CLOUDERA_MANAGER,
                    false));
        }
    }
}
