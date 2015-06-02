package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.heat.StackUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.domain.OpenStackNetwork;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;

@Service
public class OpenStackConnector implements CloudPlatformConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackConnector.class);

    private static final int POLLING_INTERVAL = 10000;
    private static final int MAX_POLLING_ATTEMPTS = 1000;
    private static final long OPERATION_TIMEOUT = 60L;
    public static final String DEFAULT_SSH_USER = "centos";

    @Inject
    private OpenStackUtil openStackUtil;

    @Inject
    private HeatTemplateBuilder heatTemplateBuilder;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private PollingService<OpenStackContext> pollingService;

    @Inject
    private OpenStackHeatStackStatusCheckerTask openStackHeatStackStatusCheckerTask;

    @Inject
    private OpenStackHeatStackDeleteStatusCheckerTask openStackHeatStackDeleteStatusCheckerTask;

    @Inject
    private OpenStackInstanceStatusCheckerTask openStackInstanceStatusCheckerTask;

    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties) {
        String stackName = stack.getName();
        OpenStackCredential credential = (OpenStackCredential) stack.getCredential();
        String heatTemplate = heatTemplateBuilder.build(stack, gateWayUserData, coreUserData);
        OSClient osClient = openStackUtil.createOSClient(credential);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(getPublicNetId(stack), credential, stack.getImage(), stack.getNetwork());
        org.openstack4j.model.heat.Stack openStackStack = osClient
                .heat()
                .stacks()
                .create(Builders.stack().name(stackName).template(heatTemplate).disableRollback(false)
                        .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build());
        List<Resource> resources = new ArrayList<>();
        resources.add(new Resource(ResourceType.HEAT_STACK, openStackStack.getId(), stack));
        Stack updatedStack = stackUpdater.addStackResources(stack.getId(), resources);
        LOGGER.info("Heat stack creation request sent with stack name: '{}' for stack: '{}'", stackName, updatedStack.getId());
        PollingResult pollingResult = pollingService.pollWithTimeout(openStackHeatStackStatusCheckerTask,
                new OpenStackContext(stack, asList(openStackStack.getId()), osClient, HeatStackStatus.CREATED.getStatus()),
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        if (!isSuccess(pollingResult)) {
            LOGGER.error(String.format("Failed to create Heat stack: %s", stack.getId()));
            throw new OpenStackResourceException(String.format("Failed to update Heat stack while building stack; polling reached an invalid end state: '%s'",
                    pollingResult.name()));
        }
        return new HashSet<>(resources);
    }

    @Override
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String coreUserData, Integer adjustment, String instanceGroup) {
        InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        group.setNodeCount(group.getNodeCount() + adjustment);
        String heatTemplate = heatTemplateBuilder.add(stack, gateWayUserData, coreUserData,
                instanceMetaDataRepository.findAllInStack(stack.getId()), instanceGroup, adjustment, group);
        PollingResult pollingResult = updateHeatStack(stack, heatTemplate);
        if (!isSuccess(pollingResult)) {
            throw new OpenStackResourceException(String.format("Failed to update Heat stack while adding instances; polling reached an invalid end state: '%s'",
                    pollingResult.name()));
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup) {
        Map<InstanceGroupType, String> userData = userDataBuilder.buildUserData(stack.cloudPlatform());
        String heatTemplate = heatTemplateBuilder.remove(stack, userData.get(InstanceGroupType.GATEWAY), userData.get(InstanceGroupType.CORE),
                instanceMetaDataRepository.findAllInStack(stack.getId()), instanceIds, instanceGroup);
        PollingResult pollingResult = updateHeatStack(stack, heatTemplate);
        if (!isSuccess(pollingResult)) {
            throw new OpenStackResourceException(
                    String.format("Failed to update Heat stack while removing instances; polling reached an invalid end state: '%s'", pollingResult.name()));
        }
        return instanceIds;
    }

    @Override
    public void deleteStack(Stack stack, Credential credential) {
        OSClient osClient = openStackUtil.createOSClient(stack);
        Resource heatStack = stack.getResourceByType(ResourceType.HEAT_STACK);
        if (heatStack != null) {
            String heatStackId = heatStack.getResourceName();
            osClient.heat().stacks().delete(stack.getName(), heatStackId);
            PollingResult pollingResult = pollingService.pollWithTimeout(openStackHeatStackDeleteStatusCheckerTask,
                    new OpenStackContext(stack, asList(heatStackId), osClient, HeatStackStatus.DELETED.getStatus()),
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            if (!isSuccess(pollingResult)) {
                LOGGER.error(String.format("Failed to delete Heat stack: %s", stack.getId()));
                throw new OpenStackResourceException(String.format("Failed to delete Heat stack; polling reached an invalid end state: '%s'",
                        pollingResult.name()));
            }
        } else {
            LOGGER.info("No Heat stack saved for stack.");
        }
    }

    @Override
    public void rollback(Stack stack, Set<Resource> resourceSet) {
        deleteStack(stack, stack.getCredential());
    }

    @Override
    public boolean startAll(Stack stack) {
        return setStackState(stack, false);
    }

    @Override
    public boolean stopAll(Stack stack) {
        return setStackState(stack, true);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData) {
        Set<InstanceMetaData> metadata = instanceMetaDataRepository.findAllInStack(stack.getId());
        String heatTemplate = heatTemplateBuilder.update(stack, gateWayUserData, coreUserData, metadata);
        PollingResult pollingResult = updateHeatStack(stack, heatTemplate);
        if (isExited(pollingResult)) {
            LOGGER.debug("polling exited during updating subnets. Failing the process; stack: {}", stack.getId());
            throw new OpenStackResourceException(String.format("Polling exited. Failed to update subnets. stackId '%s'", stack.getId()));
        }
    }

    @Override
    public String getSSHUser() {
        return DEFAULT_SSH_USER;
    }

    @Override
    public String getSSHThumbprint(Stack stack, String gateway) {
        return "THUMBPRINT";
    }

    private PollingResult updateHeatStack(Stack stack, String heatTemplate) {
        OpenStackCredential credential = (OpenStackCredential) stack.getCredential();
        Resource heatStack = stack.getResourceByType(ResourceType.HEAT_STACK);
        String heatStackId = heatStack.getResourceName();
        OSClient osClient = openStackUtil.createOSClient(credential);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(getPublicNetId(stack), credential, stack.getImage(), stack.getNetwork());
        StackUpdate updateRequest = Builders.stackUpdate().template(heatTemplate)
                .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build();
        String stackName = stack.getName();
        osClient.heat().stacks().update(stackName, heatStackId, updateRequest);
        LOGGER.info("Heat stack update request sent with stack name: '{}' for Heat stack: '{}'", stackName, heatStackId);
        return pollingService.pollWithTimeout(openStackHeatStackStatusCheckerTask,
                new OpenStackContext(stack, asList(heatStackId), osClient, HeatStackStatus.UPDATED.getStatus()),
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
    }

    private String getPublicNetId(Stack stack) {
        return ((OpenStackNetwork) stack.getNetwork()).getPublicNetId();
    }


    private boolean setStackState(Stack stack, boolean stopped) {
        boolean result = true;
        OSClient osClient = openStackUtil.createOSClient(stack);
        Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
        String heatStackId = heatResource.getResourceName();
        org.openstack4j.model.heat.Stack heatStack = osClient.heat().stacks().getDetails(stack.getName(), heatStackId);
        List<Map<String, Object>> outputs = heatStack.getOutputs();
        LOGGER.info("Attempting to {} the Heat stack", stopped ? "stop" : "start");
        List<String> instances = new ArrayList<>(outputs.size());
        for (Map<String, Object> map : outputs) {
            String instanceId = (String) map.get("output_value");
            instances.add(instanceId);
            if (stopped) {
                if (!executeAction(osClient, instanceId, Action.STOP)) {
                    result = false;
                    break;
                }
            } else {
                if (!executeAction(osClient, instanceId, Action.START)) {
                    result = false;
                    break;
                }
            }
        }
        if (!result) {
            return result;
        }
        String desiredState = stopped ? OpenStackInstanceStatus.STOPPED.getStatus() : OpenStackInstanceStatus.STARTED.getStatus();
        PollingResult pollingResult = pollingService.pollWithTimeout(openStackInstanceStatusCheckerTask,
                new OpenStackContext(stack, instances, osClient, desiredState),
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        if (isSuccess(pollingResult)) {
            return result;
        } else {
            return false;
        }
    }

    private boolean executeAction(OSClient osClient, String instanceId, Action action) {
        ActionResponse actionResponse = osClient.compute().servers().action(instanceId, action);
        if (!actionResponse.isSuccess()) {
            LOGGER.info("Failed to execute the action: {}", actionResponse.getFault());
            return false;
        }
        return true;
    }
}
