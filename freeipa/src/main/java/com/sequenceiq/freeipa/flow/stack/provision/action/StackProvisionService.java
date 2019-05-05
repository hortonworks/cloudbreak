package com.sequenceiq.freeipa.flow.stack.provision.action;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.TlsSetupService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.MetadataSetupService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

import reactor.bus.EventBus;

@Component
public class StackProvisionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackProvisionService.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ImageService imageService;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private TlsSetupService tlsSetupService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    public void setupProvision(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISION_SETUP, "Provisioning setup");
    }

    public void startProvisioning(StackContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CREATING_INFRASTRUCTURE, "Creating infrastructure");
        instanceMetaDataService.saveInstanceRequests(stack, context.getCloudStack().getGroups());
    }

    public Stack provisioningFinished(StackContext context, LaunchStackResult result, Map<Object, Object> variables) {
        Stack stack = context.getStack();
        validateResourceResults(context.getCloudContext(), result);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.METADATA_COLLECTION, "Metadata collection");
        return stackService.getByIdWithListsInTransaction(stack.getId());
    }

    public Stack setupMetadata(StackContext context, CollectMetadataResult collectMetadataResult) {
        Stack stack = context.getStack();
        metadataSetupService.saveInstanceMetaData(stack, collectMetadataResult.getResults(), InstanceStatus.CREATED);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.TLS_SETUP, "TLS setup");
        LOGGER.debug("Metadata setup DONE.");
        return stackService.getByIdWithListsInTransaction(stack.getId());
    }

    public Stack saveTlsInfo(StackContext context, TlsInfo tlsInfo) {
        boolean usePrivateIpToTls = tlsInfo.usePrivateIpToTls();
        Stack stack = context.getStack();
        if (usePrivateIpToTls) {
            SecurityConfig securityConfig = stack.getSecurityConfig();
            securityConfig.setUsePrivateIpToTls(true);
            stackUpdater.updateStackSecurityConfig(stack, securityConfig);
            stack = stackService.getByIdWithListsInTransaction(stack.getId());
            LOGGER.debug("Update Stack and it's SecurityConfig to use private ip when TLS is built.");
        }
        return stack;
    }

    public void setupTls(StackContext context) throws CloudbreakException {
        Stack stack = context.getStack();
        Set<InstanceMetaData> instanceMetaDataSet =
                stack.getInstanceGroups().stream().flatMap(instanceGroup -> instanceGroup.getAllInstanceMetaData().stream()).collect(Collectors.toSet());
        for (InstanceMetaData gwInstance : instanceMetaDataSet) {
            tlsSetupService.setupTls(stack.getId());
        }
    }

    public void stackCreationFinished(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISIONED, "Stack provisioned.");
    }

    public void handleStackCreationFailure(Stack stack, Exception errorDetails) {
        LOGGER.info("Error during stack creation flow:", errorDetails);
        String errorReason = errorDetails == null ? "Unknown error" : errorDetails.getMessage();
        if (errorDetails instanceof CancellationException || ExceptionUtils.getRootCause(errorDetails) instanceof CancellationException) {
            LOGGER.debug("The flow has been cancelled.");
        } else {
            if (!stack.isStackInDeletionPhase()) {
//                handleFailure(stack, errorReason);
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISION_FAILED, errorReason);
            }
        }
    }

    //FIXME
    /*private void handleFailure(Stack stack, String errorReason) {
        try {
            if (!stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
                LOGGER.debug("Nothing to do. OnFailureAction {}", stack.getOnFailureActionAction());
            } else {
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ROLLING_BACK);
                connector.rollback(stack, stack.getResources());
            }
        } catch (Exception ex) {
            LOGGER.info("Stack rollback failed on stack id : {}. Exception:", stack.getId(), ex);
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISION_FAILED, format("Rollback failed: %s", ex.getMessage()));
        }
    }*/

    private long calculateStackCreationTime(Date startDate) {
        long result = 0;
        if (startDate != null) {
            return (new Date().getTime() - startDate.getTime()) / DateUtils.MILLIS_PER_SECOND;
        }
        return result;
    }

    private void validateResourceResults(CloudContext cloudContext, LaunchStackResult res) {
        validateResourceResults(cloudContext, res.getErrorDetails(), res.getResults());
    }

    private void validateResourceResults(CloudContext cloudContext, Exception exception, List<CloudResourceStatus> results) {
        String action = "create";
        if (exception != null) {
            LOGGER.info(format("Failed to %s stack: %s", action, cloudContext), exception);
            throw new OperationException(exception);
        }
        if (results.size() == 1 && (results.get(0).isFailed() || results.get(0).isDeleted())) {
            throw new OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext, results.get(0).getStatusReason()));
        }
        List<CloudResourceStatus> failedResources = results.stream().filter(r -> r.isFailed() || r.isDeleted()).collect(Collectors.toList());
        if (!failedResources.isEmpty()) {
            throw new OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext, failedResources));
        }
    }

    private List<CloudResourceStatus> removeFailedMetadata(Long stackId, List<CloudResourceStatus> statuses, Group group) {
        Map<Long, CloudResourceStatus> failedResources = new HashMap<>();
        Set<Long> groupPrivateIds = getPrivateIds(group);
        for (CloudResourceStatus status : statuses) {
            Long privateId = status.getPrivateId();
            if (privateId != null && status.isFailed() && !failedResources.containsKey(privateId) && groupPrivateIds.contains(privateId)) {
                failedResources.put(privateId, status);
                instanceMetaDataService.deleteInstanceRequest(stackId, privateId);
            }
        }
        return new ArrayList<>(failedResources.values());
    }

    private Set<Long> getPrivateIds(Group group) {
        Set<Long> ids = new HashSet<>();
        for (CloudInstance cloudInstance : group.getInstances()) {
            ids.add(cloudInstance.getTemplate().getPrivateId());
        }
        return ids;
    }
}
