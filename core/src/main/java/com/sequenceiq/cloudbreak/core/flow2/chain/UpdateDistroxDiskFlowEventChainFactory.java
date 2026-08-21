package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.DISTROX_DISK_UPDATE_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.FULL_START_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.FULL_STOP_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_TRIGGER_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXDiskUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class UpdateDistroxDiskFlowEventChainFactory implements FlowEventChainFactory<DistroXDiskUpdateTriggerEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDistroxDiskFlowEventChainFactory.class);

    @Override
    public UsageProto.CDPClusterStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (SaltUpdateState.INIT_STATE.equals(flowState)) {
            return UPGRADE_STARTED;
        } else if (DistroXDiskUpdateState.DATAHUB_DISK_UPDATE_FINISHED_STATE.equals(flowState)) {
            return UPGRADE_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return UPGRADE_FAILED;
        } else {
            return UNSET;
        }
    }

    @Override
    public String initEvent() {
        return DISTROX_DISK_UPDATE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DistroXDiskUpdateTriggerEvent event) {
        LOGGER.debug("Creating flow trigger event queue for data hub disk update with event {}", event);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        boolean gcpDiskTypeChange = isGcpDiskTypeChangeRequested(event);
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        flowEventChain.add(getSaltUpdateTriggerEvent(event));
        if (gcpDiskTypeChange) {
            flowEventChain.add(new StackEvent(FULL_STOP_TRIGGER_EVENT, event.getResourceId()));
        }
        flowEventChain.add(getDistroXDiskUpdateEvent(event));
        if (gcpDiskTypeChange) {
            flowEventChain.add(new StackEvent(FULL_START_TRIGGER_EVENT, event.getResourceId()));
        }
        flowEventChain.add(getResizeDiskUpdateEvent(event));
        flowEventChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    /**
     * A GCP disk type change cannot be done in place, so it requires stop &rarr; recreate disks &rarr; start. True only
     * when the cluster is on GCP and the update is a type change (decided upstream in {@code StackOperationService}).
     */
    private boolean isGcpDiskTypeChangeRequested(DistroXDiskUpdateTriggerEvent event) {
        return CloudPlatform.GCP.name().equalsIgnoreCase(event.getCloudPlatform()) && event.isDiskTypeChangeRequested();
    }

    private StackEvent getSaltUpdateTriggerEvent(DistroXDiskUpdateTriggerEvent event) {
        return new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted());
    }

    private DistroXDiskUpdateEvent getDistroXDiskUpdateEvent(DistroXDiskUpdateTriggerEvent event) {
        return DistroXDiskUpdateEvent.builder()
                .withResourceId(event.getResourceId())
                .withStackId(event.getStackId())
                .withGroup(event.getGroup())
                .withVolumeType(event.getVolumeType())
                .withSize(event.getSize())
                .withDiskType(event.getDiskType())
                .withClusterName(event.getClusterName())
                .withAccountId(event.getAccountId())
                .withSelector(DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_VALIDATION_EVENT.selector())
                .withAccepted(event.accepted())
                .build();
    }

    private DiskResizeRequest getResizeDiskUpdateEvent(DistroXDiskUpdateTriggerEvent event) {
        return new DiskResizeRequest(DISK_RESIZE_TRIGGER_EVENT.event(), event.getResourceId(), event.getGroup());
    }
}
