package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static org.joda.time.DateTimeConstants.SECONDS_PER_MINUTE;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;

@Component
public class AmbariDecommissionTimeCalculator {

    private static final int DECOMMISSIONING_MAGIC_NUMBER = 12;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    public void calculateDecommissioningTime(Stack stack, Collection<HostMetadata> filteredHostList, Map<String, Map<Long, Long>> dfsSpace, long usedSpace) {
        Optional<HostMetadata> aHostMetadata = filteredHostList.stream().findFirst();
        if (aHostMetadata.isPresent()) {
            Template template = aHostMetadata.get().getHostGroup().getConstraint().getInstanceGroup().getTemplate();
            int rootVolumeSize = template.getRootVolumeSize() == null
                    ? defaultRootVolumeSizeProvider.getForPlatform(stack.cloudPlatform())
                    : template.getRootVolumeSize();
            long eachNodeCapacityInGB = template.getVolumeCount() * template.getVolumeSize() + rootVolumeSize;
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
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_DECOMMISSIONING_TIME, AVAILABLE.name(),
                    concatenateTimeString(decommissionMinutes));
        }
    }

    private String concatenateTimeString(long decommissionMinutes) {
        String timeString;
        if (decommissionMinutes >= SECONDS_PER_MINUTE) {
            long decommissionHours = Math.round(decommissionMinutes / (double) SECONDS_PER_MINUTE);
            timeString = String.valueOf(decommissionHours + " hours");
        } else {
            timeString = String.valueOf(decommissionMinutes) + " minutes";
        }
        return timeString;
    }

}
