package com.sequenceiq.cloudbreak.reactor.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleService;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.cluster.InstanceStorageInfo;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScalePreparationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScalePreparationResult;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@Component
public class CoreVerticalScalePreparationHandler implements CloudPlatformEventHandler<CoreVerticalScalePreparationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreVerticalScalePreparationHandler.class);

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private CoreVerticalScaleService coreVerticalScaleService;

    @Override
    public Class<CoreVerticalScalePreparationRequest> type() {
        return CoreVerticalScalePreparationRequest.class;
    }

    @Override
    public void accept(Event<CoreVerticalScalePreparationRequest> coreVerticalScalePreparationRequestEvent) {
        CoreVerticalScalePreparationRequest request = coreVerticalScalePreparationRequestEvent.getData();
        try {
            CloudContext ctx = request.getCloudContext();
            CloudConnector connector = cloudPlatformConnectors.get(ctx.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request.getCloudCredential(), ctx, connector);
            String requestGroup = request.getStackVerticalScaleV4Request().getGroup();
            String requestInstanceType = request.getStackVerticalScaleV4Request().getTemplate().getInstanceType();
            InstanceStoreMetadata instanceStoreMetadata = connector.metadata().collectInstanceStorageCount(ac,
                    List.of(requestInstanceType));
            List<InstanceStorageInfo> instanceStorageInfoList = convert(instanceStoreMetadata, requestInstanceType);
            StackDto stackDto = request.getStack();
            Template template = stackDto.getInstanceGroupDtos().stream()
                    .filter(instance -> instance.getInstanceGroup().getGroupName().equals(requestGroup))
                    .findFirst().get().getInstanceGroup().getTemplate();
            setTemporaryStorageOnTemplate(template, instanceStorageInfoList);
            String blueprintText = stackDto.getBlueprint().getBlueprintText();
            CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
            Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
            List<String> hostTemplateRoleGroupNames = processor.getHostTemplateRoleNames(requestGroup);
            if (!stackDto.isStackInStopPhase()) {
                coreVerticalScaleService.stopClouderaManagerServicesAndUpdateClusterConfigs(stackDto, hostTemplateServiceComponents,
                        instanceStorageInfoList);
                coreVerticalScaleService.stopInstances(connector, request.getCloudResources(), request.getInstanceGroup(),
                        stackDto, ac);
            }
            CoreVerticalScalePreparationResult result = new CoreVerticalScalePreparationResult(hostTemplateServiceComponents,
                    instanceStorageInfoList,
                    ctx,
                    request.getCloudCredential(),
                    request.getCloudStack(),
                    request.getStackVerticalScaleV4Request(),
                    hostTemplateRoleGroupNames);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(coreVerticalScalePreparationRequestEvent.getHeaders(), result));
            LOGGER.debug("Vertical scaling Preparation successfully finished for {}, and the result is: {}", ctx, result);
        } catch (Exception e) {
            LOGGER.error("Vertical scaling Preparation error: ", e);
            CoreVerticalScalePreparationResult result = new CoreVerticalScalePreparationResult(
                    e.getMessage(),
                    e,
                    request.getStackVerticalScaleV4Request());
            request.getResult().onNext(result);
            eventBus.notify(CloudPlatformResult.failureSelector(CoreVerticalScalePreparationResult.class),
                    new Event<>(coreVerticalScalePreparationRequestEvent.getHeaders(), result));
        }
    }

    private AuthenticatedContext getAuthenticatedContext(CloudCredential cloudCredential,
            CloudContext cloudContext, CloudConnector connector) {
        return connector.authentication().authenticate(cloudContext, cloudCredential);
    }

    private void setTemporaryStorageOnTemplate(Template template, List<InstanceStorageInfo> instanceStoreInfo) {
        if (instanceStoreInfo != null && !instanceStoreInfo.isEmpty()) {
            template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES);
            template.setInstanceStorageCount(instanceStoreInfo.get(0).getInstanceStorageCount());
            template.setInstanceStorageSize(instanceStoreInfo.get(0).getInstanceStorageSize());
        } else {
            template.setTemporaryStorage(TemporaryStorage.ATTACHED_VOLUMES);
            template.setInstanceStorageCount(0);
            template.setInstanceStorageSize(0);
        }
    }

    private List<InstanceStorageInfo> convert(InstanceStoreMetadata instanceStoreMetadata, String requestInstanceType) {
        List<InstanceStorageInfo> instanceStorageInfoList = new ArrayList<>();
        Integer instanceStorageCount = instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled(requestInstanceType);
        Integer instanceStorageSize = instanceStoreMetadata.mapInstanceTypeToInstanceSizeNullHandled(requestInstanceType);
        instanceStorageInfoList.add(new InstanceStorageInfo(instanceStorageCount > 0, instanceStorageCount,
                instanceStorageSize));
        return instanceStorageInfoList;
    }
}
