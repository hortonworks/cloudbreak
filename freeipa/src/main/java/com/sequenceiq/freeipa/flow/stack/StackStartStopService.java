package com.sequenceiq.freeipa.flow.stack;

import static java.lang.String.format;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.service.OperationException;

@Component
public class StackStartStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartStopService.class);

    public void validateResourceResults(CloudContext cloudContext, Exception exception, InstancesStatusResult results, boolean start) {
        String action = start ? "start" : "stop";
        if (exception != null) {
            LOGGER.info(format("Failed to %s stack: %s", action, cloudContext), exception);
            throw new OperationException(exception);
        }
        List<CloudVmInstanceStatus> failedInstances =
                results.getResults().stream().filter(r -> r.getStatus() == InstanceStatus.FAILED).collect(Collectors.toList());
        if (!failedInstances.isEmpty()) {
            String statusReason = failedInstances.stream().map(
                    fi -> "Instance " + fi.getCloudInstance().getInstanceId() + ": " + fi.getStatus() + '(' + fi.getStatusReason() + ')')
                    .collect(Collectors.joining(","));
            throw new OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext.getName(), statusReason));
        }
    }
}
