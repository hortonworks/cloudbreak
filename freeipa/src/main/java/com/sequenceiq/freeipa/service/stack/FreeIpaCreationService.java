package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.CloudStorageFolderResolverService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.converter.stack.CreateFreeIpaRequestToStackConverter;
import com.sequenceiq.freeipa.converter.stack.StackToDescribeFreeIpaResponseConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.backup.BackupClusterType;
import com.sequenceiq.freeipa.service.freeipa.backup.cloud.CloudBackupFolderResolverService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.freeipa.service.multiaz.MultiAzValidator;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.service.telemetry.AccountTelemetryService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class FreeIpaCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationService.class);

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

    @Inject
    private CloudStorageFolderResolverService cloudStorageFolderResolverService;

    @Inject
    private CloudBackupFolderResolverService cloudBackupFolderResolverService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private AccountTelemetryService accountTelemetryService;

    @Inject
    private MultiAzCalculatorService multiAzCalculatorService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private MultiAzValidator multiAzValidator;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private FreeIpaRecommendationService freeIpaRecommendationService;

    @Inject
    private FreeIpaRecipeService freeIpaRecipeService;

    @Value("${info.app.version:}")
    private String appVersion;

    public DescribeFreeIpaResponse launchFreeIpa(CreateFreeIpaRequest request, String accountId) {
        String userCrn = crnService.getUserCrn();
        Future<String> ownerFuture = initiateOwnerFetching(userCrn);
        Credential credential = credentialService.getCredentialByEnvCrn(request.getEnvironmentCrn());
        DetailedEnvironmentResponse environment = measure(() -> cachedEnvironmentClientService.getByCrn(request.getEnvironmentCrn()),
                LOGGER, "Environment properties were queried under {} ms for environment {}", request.getEnvironmentCrn());

        Stack stack = stackConverter.convert(request, environment, accountId, ownerFuture, userCrn, credential.getCloudPlatform());

        stack.setAppVersion(appVersion);
        GetPlatformTemplateRequest getPlatformTemplateRequest = templateService.triggerGetTemplate(stack, credential);
        Telemetry telemetry = stack.getTelemetry();
        if (telemetry != null) {
            telemetry.setRules(accountTelemetryService.getAnonymizationRules(accountId));
        }
        cloudStorageFolderResolverService.updateStorageLocation(telemetry,
                FluentClusterType.FREEIPA.value(), stack.getName(), stack.getResourceCrn());

        stack.setTelemetry(telemetry);

        Backup backup = stack.getBackup();
        backup = cloudBackupFolderResolverService.updateStorageLocation(backup,
                BackupClusterType.FREEIPA.value(), stack.getName(), stack.getResourceCrn());
        stack.setBackup(backup);

        fillInstanceMetadata(stack, environment);

        String template = templateService.waitGetTemplate(getPlatformTemplateRequest);
        stack.setTemplate(template);
        SecurityConfig securityConfig = tlsSecurityService.generateSecurityKeys(accountId);
        multiAzValidator.validateMultiAzForStack(stack.getPlatformvariant(), stack.getInstanceGroups());
        freeIpaRecommendationService.validateCustomInstanceType(stack, credential);
        try {
            Triple<Stack, ImageEntity, FreeIpa> stackImageFreeIpaTuple = transactionService.required(() -> {
                SecurityConfig savedSecurityConfig = securityConfigService.save(securityConfig);
                stack.setSecurityConfig(savedSecurityConfig);
                Stack savedStack = stackService.save(stack);
                freeIpaRecipeService.saveRecipes(request.getRecipes(), savedStack.getId());
                ImageSettingsRequest imageSettingsRequest = request.getImage();
                ImageEntity image = imageService.create(savedStack, Objects.nonNull(imageSettingsRequest) ? imageSettingsRequest : new ImageSettingsRequest());
                FreeIpa freeIpa = freeIpaService.create(savedStack, request.getFreeIpa());
                return Triple.of(savedStack, image, freeIpa);
            });
            flowManager.notify(FlowChainTriggers.PROVISION_TRIGGER_EVENT,
                    new StackEvent(FlowChainTriggers.PROVISION_TRIGGER_EVENT, stackImageFreeIpaTuple.getLeft().getId()));
            InMemoryStateStore.putStack(stack.getId(), PollGroup.POLLABLE);
            return stackToDescribeFreeIpaResponseConverter
                    .convert(stackImageFreeIpaTuple.getLeft(), stackImageFreeIpaTuple.getMiddle(), stackImageFreeIpaTuple.getRight(), Optional.empty(), false);
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Creation of FreeIPA failed", e);
            throw new BadRequestException("Creation of FreeIPA failed: " + e.getCause().getMessage(), e);
        }
    }

    private Future<String> initiateOwnerFetching(String userCrn) {
        Future<String> ownerFuture;
        if (Crn.safeFromString(userCrn).getResourceType().equals(Crn.ResourceType.MACHINE_USER)) {
            ownerFuture = intermediateBuilderExecutor.submit(() ->
                    umsClient.getMachineUserDetails(userCrn, Crn.fromString(userCrn).getAccountId(),
                            regionAwareInternalCrnGeneratorFactory).getMachineUserName());
        } else {
            ownerFuture = intermediateBuilderExecutor.submit(() -> umsClient.getUserDetails(userCrn,
                    regionAwareInternalCrnGeneratorFactory).getEmail());
        }
        return ownerFuture;
    }

    private void fillInstanceMetadata(Stack stack, DetailedEnvironmentResponse environment) {
        long privateIdNumber = 0;
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                instanceMetaData.setPrivateId(privateIdNumber++);
                instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
            }
            multiAzCalculatorService.calculateByRoundRobin(multiAzCalculatorService.prepareSubnetAzMap(environment), instanceGroup);
        }
    }
}
