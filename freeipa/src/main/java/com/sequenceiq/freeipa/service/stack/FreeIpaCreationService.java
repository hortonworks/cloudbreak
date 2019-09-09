package com.sequenceiq.freeipa.service.stack;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.stack.CreateFreeIpaRequestToStackConverter;
import com.sequenceiq.freeipa.converter.stack.StackToDescribeFreeIpaResponseConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class FreeIpaCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationService.class);

    private static final int CCM_KEY_ID_LENGTH = 36;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackService stackService;

    @Inject
    private CreateFreeIpaRequestToStackConverter stackConverter;

    @Inject
    private StackTemplateService templateService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private ImageService imageService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private StackToDescribeFreeIpaResponseConverter stackToDescribeFreeIpaResponseConverter;

    @Inject
    private CrnService crnService;

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private TransactionService transactionService;

    @Autowired(required = false)
    private CcmParameterSupplier ccmParameterSupplier;

    @Value("${info.app.version:}")
    private String appVersion;

    public DescribeFreeIpaResponse launchFreeIpa(CreateFreeIpaRequest request, String accountId) {
        String userCrn = crnService.getUserCrn();
        Future<User> userFuture = intermediateBuilderExecutor.submit(() -> umsClient.getUserDetails(userCrn, userCrn, Optional.empty()));
        Credential credential = credentialService.getCredentialByEnvCrn(request.getEnvironmentCrn());
        Stack stack = stackConverter.convert(request, accountId, userFuture, credential.getCloudPlatform());
        stack.setResourceCrn(crnService.createCrn(accountId, Crn.ResourceType.FREEIPA));
        stack.setAppVersion(appVersion);
        GetPlatformTemplateRequest getPlatformTemplateRequest = templateService.triggerGetTemplate(stack, credential);

        SecurityConfig securityConfig = tlsSecurityService.generateSecurityKeys();
        stack.setSecurityConfig(securityConfig);

        fillInstanceMetadata(stack);

        String template = templateService.waitGetTemplate(stack, getPlatformTemplateRequest);
        stack.setTemplate(template);
        try {
            Triple<Stack, Image, FreeIpa> stackImageFreeIpaTuple = transactionService.required(() -> {
                Stack savedStack = stackService.save(stack);
                ImageSettingsRequest imageSettingsRequest = request.getImage();

                CcmParameters ccmParameters = null;
                if ((ccmParameterSupplier != null) && Boolean.TRUE.equals(stack.getUseCcm())) {
                    int gatewayPort = Optional.ofNullable(stack.getGatewayport()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
                    Map<KnownServiceIdentifier, Integer> tunneledServicePorts = Collections.singletonMap(KnownServiceIdentifier.GATEWAY, gatewayPort);
                    // JSA TODO Use stack ID or something else instead?
                    String keyId = StringUtils.right(stack.getResourceCrn(), CCM_KEY_ID_LENGTH);
                    String actorCrn = Objects.requireNonNull(userCrn, "userCrn is null");
                    ccmParameters = ccmParameterSupplier.getCcmParameters(actorCrn, accountId, keyId, tunneledServicePorts).orElse(null);
                }

                Image image = imageService.create(savedStack, Objects.nonNull(imageSettingsRequest) ? imageSettingsRequest
                        : new ImageSettingsRequest(), credential, ccmParameters);
                FreeIpa freeIpa = freeIpaService.create(savedStack, request.getFreeIpa());
                return Triple.of(savedStack, image, freeIpa);
            });
            flowManager.notify(FlowChainTriggers.PROVISION_TRIGGER_EVENT,
                    new StackEvent(FlowChainTriggers.PROVISION_TRIGGER_EVENT, stackImageFreeIpaTuple.getLeft().getId()));
            InMemoryStateStore.putStack(stack.getId(), PollGroup.POLLABLE);
            return stackToDescribeFreeIpaResponseConverter
                    .convert(stackImageFreeIpaTuple.getLeft(), stackImageFreeIpaTuple.getMiddle(), stackImageFreeIpaTuple.getRight());
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Creation of FreeIPA failed", e);
            throw new BadRequestException("Creation of FreeIPA failed: " + e.getCause().getMessage(), e);
        }
    }

    private void fillInstanceMetadata(Stack stack) {
        long privateIdNumber = 0;
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                instanceMetaData.setPrivateId(privateIdNumber++);
                instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
            }
        }
    }
}
