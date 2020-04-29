package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_EVENT;
import static java.util.function.Predicate.not;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.instance.InstanceEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

@Service
public class RebootInstancesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebootInstancesService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private FreeIpaHealthDetailsService healthDetailsService;

    private Map<String, InstanceStatus> getInstanceHealthMap(String accountId, RebootInstancesRequest request) {
        return healthDetailsService.getHealthDetails(request.getEnvironmentCrn(), accountId).getNodeHealthDetails().stream()
                .collect(Collectors.toMap(NodeHealthDetails::getInstanceId, NodeHealthDetails::getStatus));
    }

    private Collection<String> getValidInstanceIds(Collection<String> allInstances, Collection<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return allInstances;
        } else {
            Collection<String> validInstanceIds = instanceIds.stream().filter(allInstances::contains).collect(Collectors.toSet());
            if (validInstanceIds.size() != instanceIds.size()) {
                String badIds = instanceIds.stream()
                        .filter(not(allInstances::contains)).collect(Collectors.joining(","));
                String msg = MessageFormat.format("Invalid instanceIds in request {0}.", badIds);
                LOGGER.error(msg);
                throw new BadRequestException(msg);
            }
            return validInstanceIds;
        }
    }

    private Map<String, InstanceMetaData> getInstancesToReboot(Map<String, InstanceMetaData> allInstances,
            String accountId, RebootInstancesRequest request) {
        Collection<String> validInstanceIds = getValidInstanceIds(allInstances.keySet(), request.getInstanceIds());
        Map<String, InstanceStatus> healthMap =
                request.isForceReboot() ? Collections.emptyMap() : getInstanceHealthMap(accountId, request);

        Map<String, InstanceMetaData> instancesToReboot = validInstanceIds.stream()
                .filter(instanceId -> request.isForceReboot() || (healthMap.get(instanceId) != null && !healthMap.get(instanceId).isAvailable()))
                .collect(Collectors.toMap(Function.identity(), instanceId -> allInstances.get(instanceId)));
        if (instancesToReboot.keySet().size() != validInstanceIds.size()) {
            LOGGER.info("Not rebooting instances {} because force reboot was not selected.", validInstanceIds.stream()
                    .filter(instance -> !instancesToReboot.keySet().contains(instance)).collect(Collectors.joining(",")));
        }
        return instancesToReboot;
    }

    private Map<String, InstanceMetaData> getAllInstancesFromStack(Stack stack) {
        return stack.getInstanceGroups().stream().flatMap(instanceGroup -> instanceGroup.getInstanceMetaData().stream())
                .collect(Collectors.toMap(InstanceMetaData::getInstanceId, Function.identity()));
    }

    /**
     * If no instance passed in request, reboot all bad instances
     * If instances passed in request, reboot all valid passed bad instances
     * If force and instances passed in request, reboot all valid passed instances
     * If force and no instances passed in request, reboot all instances
     * @param accountId - The account id for the instance to reboot.
     * @param request - A RebootInstanceRequest containing request parameters.
     */
    public void rebootInstances(String accountId, RebootInstancesRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        Map<String, InstanceMetaData> allInstancesByInstanceId = getAllInstancesFromStack(stack);
        Map<String, InstanceMetaData> instancesToReboot = getInstancesToReboot(allInstancesByInstanceId, accountId, request);

        if (instancesToReboot.keySet().size() > 0) {
            flowManager.notify(REBOOT_EVENT.event(), new InstanceEvent(REBOOT_EVENT.event(), stack.getId(),
                    instancesToReboot.keySet().stream().collect(Collectors.toList())));
        } else {
            throw new NotFoundException("No unhealthy instances to reboot.  Maybe use the force option.");
        }
    }
}
