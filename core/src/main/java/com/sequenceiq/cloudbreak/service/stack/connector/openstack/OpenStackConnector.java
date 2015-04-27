package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.heat.StackUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BuildStackFailureException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudResourceOperationFailedException;
import com.sequenceiq.cloudbreak.service.stack.connector.UpdateFailedException;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;

import jersey.repackaged.com.google.common.collect.Maps;
import reactor.core.Reactor;

@Service
public class OpenStackConnector implements CloudPlatformConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackConnector.class);
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;
    private static final long OPERATION_TIMEOUT = 5L;

    @Autowired
    private OpenStackUtil openStackUtil;

    @Autowired
    private HeatTemplateBuilder heatTemplateBuilder;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Autowired
    private PollingService<OpenStackContext> pollingService;

    @Autowired
    private OpenStackHeatStackStatusCheckerTask openStackHeatStackStatusCheckerTask;

    @Autowired
    private OpenStackHeatStackDeleteStatusCheckerTask openStackHeatStackDeleteStatusCheckerTask;

    @Autowired
    private OpenStackInstanceStatusCheckerTask openStackInstanceStatusCheckerTask;

    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String hostGroupUserData, Map<String, Object> setupProperties) {
        String stackName = stack.getName();
        OpenStackCredential credential = (OpenStackCredential) stack.getCredential();
        List<InstanceGroup> instanceGroups = stack.getInstanceGroupsAsList();
        String heatTemplate = heatTemplateBuilder.build(stack, gateWayUserData, hostGroupUserData);
        OSClient osClient = openStackUtil.createOSClient(credential);
        org.openstack4j.model.heat.Stack openStackStack = osClient
                .heat()
                .stacks()
                .create(Builders.stack().name(stackName).template(heatTemplate).disableRollback(false)
                        .parameters(buildParameters(getPublicNetId(instanceGroups), credential, stack.getImage())).timeoutMins(OPERATION_TIMEOUT).build());
        List<Resource> resources = new ArrayList<>();
        resources.add(new Resource(ResourceType.HEAT_STACK, openStackStack.getId(), stack));
        Stack updatedStack = stackUpdater.addStackResources(stack.getId(), resources);
        LOGGER.info("Heat stack creation request sent with stack name: '{}' for stack: '{}'", stackName, updatedStack.getId());
        try {
            PollingResult pollingResult = pollingService.pollWithTimeout(openStackHeatStackStatusCheckerTask,
                    new OpenStackContext(stack, asList(openStackStack.getId()), osClient, HeatStackStatus.CREATED.getStatus()),
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            if (!isSuccess(pollingResult)) {
                throw new CloudResourceOperationFailedException("Failed to create Heat stack, because polling reached an invalid end state.");
            }
        } catch (HeatStackFailedException e) {
            LOGGER.error(String.format("Failed to create Heat stack: %s", stack.getId()), e);
            stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_FAILED, "Creation of cluster infrastructure failed: " + e.getMessage());
            throw new BuildStackFailureException(e);
        }
        return new HashSet<>(resources);
    }

    @Override
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String hostGroupUserData, Integer adjustment, String instanceGroup) {
        InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        group.setNodeCount(group.getNodeCount() + adjustment);
        try {
            String heatTemplate = heatTemplateBuilder.add(stack, gateWayUserData, hostGroupUserData,
                    instanceMetaDataRepository.findAllInStack(stack.getId()), instanceGroup, adjustment, group);
            PollingResult pollingResult = updateHeatStack(stack, heatTemplate);
            if (!isSuccess(pollingResult)) {
                throw new UpdateFailedException("Failed to update Heat stack, because polling reached an invalid end state.");
            }
        } catch (UpdateFailedException e) {
            LOGGER.error("Failed to update the Heat stack", e);
            throw new BuildStackFailureException(e);
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup) {
        try {
            String hostGroupUserDataScript = userDataBuilder.buildUserData(stack.cloudPlatform(), stack.getHash(), stack.getConsulServers(),
                    new HashMap<String, String>(), InstanceGroupType.CORE);
            String gateWayUserDataScript = userDataBuilder.buildUserData(stack.cloudPlatform(), stack.getHash(), stack.getConsulServers(),
                    new HashMap<String, String>(), InstanceGroupType.GATEWAY);
            String heatTemplate = heatTemplateBuilder.remove(stack, gateWayUserDataScript, hostGroupUserDataScript,
                    instanceMetaDataRepository.findAllInStack(stack.getId()), instanceIds, instanceGroup);
            PollingResult pollingResult = updateHeatStack(stack, heatTemplate);
            if (!isSuccess(pollingResult)) {
                throw new UpdateFailedException("Failed to update Heat stack, because polling reached an invalid end state.");
            }
        } catch (UpdateFailedException e) {
            LOGGER.error("Failed to update the Heat stack", e);
            throw new BuildStackFailureException(e);
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
            try {
                PollingResult pollingResult = pollingService.pollWithTimeout(openStackHeatStackDeleteStatusCheckerTask,
                        new OpenStackContext(stack, asList(heatStackId), osClient, HeatStackStatus.DELETED.getStatus()),
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
                if (!isSuccess(pollingResult)) {
                    throw new CloudResourceOperationFailedException("Failed to delete Heat stack, because polling reached an invalid end state.");
                }
            } catch (HeatStackFailedException e) {
                LOGGER.error(String.format("Failed to delete Heat stack: %s", stack.getId()), e);
                throw e;
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
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String hostGroupUserData) throws UpdateFailedException {
        Set<InstanceMetaData> metadata = instanceMetaDataRepository.findAllInStack(stack.getId());
        String heatTemplate = heatTemplateBuilder.update(stack, gateWayUserData, hostGroupUserData, metadata);
        PollingResult pollingResult = updateHeatStack(stack, heatTemplate);
        if (isExited(pollingResult)) {
            throw new UpdateFailedException(new IllegalStateException());
        }
    }

    private PollingResult updateHeatStack(Stack stack, String heatTemplate) throws UpdateFailedException {
        OpenStackCredential credential = (OpenStackCredential) stack.getCredential();
        Resource heatStack = stack.getResourceByType(ResourceType.HEAT_STACK);
        String heatStackId = heatStack.getResourceName();
        OSClient osClient = openStackUtil.createOSClient(credential);
        StackUpdate updateRequest = Builders.stackUpdate().template(heatTemplate)
                .parameters(buildParameters(getPublicNetId(stack), credential, stack.getImage())).timeoutMins(OPERATION_TIMEOUT).build();
        String stackName = stack.getName();
        osClient.heat().stacks().update(stackName, heatStackId, updateRequest);
        LOGGER.info("Heat stack update request sent with stack name: '{}' for Heat stack: '{}'", stackName, heatStackId);
        try {
            return pollingService.pollWithTimeout(openStackHeatStackStatusCheckerTask,
                    new OpenStackContext(stack, asList(heatStackId), osClient, HeatStackStatus.UPDATED.getStatus()),
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (HeatStackFailedException e) {
            throw new UpdateFailedException(e);
        }
    }

    private String getPublicNetId(Stack stack) {
        return getPublicNetId(stack.getInstanceGroupsAsList());
    }

    private String getPublicNetId(List<InstanceGroup> instanceGroups) {
        return ((OpenStackTemplate) instanceGroups.get(0).getTemplate()).getPublicNetId();
    }

    private Map<String, String> buildParameters(String publicNetId, OpenStackCredential credential, String image) {
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("public_net_id", publicNetId);
        parameters.put("image_id", image);
        parameters.put("key_name", openStackUtil.getKeyPairName(credential));
        parameters.put("tenant_id", credential.getTenantName());
        return parameters;
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
