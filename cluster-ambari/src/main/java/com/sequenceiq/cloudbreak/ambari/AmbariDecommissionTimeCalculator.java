package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_CLUSTER_DECOMMISSIONING_TIME;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.FlowMessageService;

@Component
public class AmbariDecommissionTimeCalculator {

    private static final int DECOMMISSIONING_MAGIC_NUMBER = 12;

    private static final int SECONDS_PER_MINUTE = 60;

    @Inject
    private FlowMessageService flowMessageService;

    public void calculateDecommissioningTime(Stack stack, Collection<InstanceMetaData> filteredHostList, Map<String, Map<Long, Long>> dfsSpace, long usedSpace,
            int defaultRootVolumeSize) {
        Optional<InstanceMetaData> instanceMetaData = filteredHostList.stream().findFirst();
        if (instanceMetaData.isPresent()) {
            Template template = instanceMetaData.get().getInstanceGroup().getTemplate();
            int rootVolumeSize = template.getRootVolumeSize() == null
                    ? defaultRootVolumeSize
                    : template.getRootVolumeSize();
            long eachNodeCapacityInGB = template.getVolumeTemplates().stream()
                    .mapToInt(volume -> volume.getVolumeCount() * volume.getVolumeSize()).sum() + rootVolumeSize;
            long usedGlobalDfsSpace = dfsSpace.values().stream()
                    .mapToLong(dfsSpaceByUsed -> dfsSpaceByUsed.values().stream().mapToLong(Long::longValue).sum())
                    .sum();
            long remainingGlobalDfsSpace = dfsSpace.values().stream()
                    .mapToLong(dfsSpaceByUsed -> dfsSpaceByUsed.keySet().stream().mapToLong(Long::longValue).sum())
                    .sum();
            double globalDfsSpace = usedGlobalDfsSpace + remainingGlobalDfsSpace;
            double clusterUtilizationFraction = usedGlobalDfsSpace / globalDfsSpace;
            double decommissionFraction = usedSpace / globalDfsSpace;
            double decommissionSeconds = DECOMMISSIONING_MAGIC_NUMBER * eachNodeCapacityInGB * clusterUtilizationFraction * decommissionFraction;
            long decommissionMinutes = Math.round(decommissionSeconds / SECONDS_PER_MINUTE);
            flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), CLUSTER_MANAGER_CLUSTER_DECOMMISSIONING_TIME,
                    concatenateTimeString(decommissionMinutes));
        }
    }

    private String concatenateTimeString(long decommissionMinutes) {
        String timeString;
        if (decommissionMinutes >= SECONDS_PER_MINUTE) {
            long decommissionHours = Math.round(decommissionMinutes / (double) SECONDS_PER_MINUTE);
            timeString = decommissionHours + " hours";
        } else {
            timeString = decommissionMinutes + " minutes";
        }
        return timeString;
    }

}
