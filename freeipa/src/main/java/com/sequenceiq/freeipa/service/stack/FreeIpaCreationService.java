package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.constant.ImdsConstants.AWS_IMDS_VERSION_V1;
import static com.sequenceiq.cloudbreak.constant.ImdsConstants.AWS_IMDS_VERSION_V2;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.CloudStorageFolderResolverService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.CreateFreeIpaV1Response;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.converter.stack.CreateFreeIpaRequestToStackConverter;
import com.sequenceiq.freeipa.converter.stack.StackToDescribeFreeIpaResponseConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.ProvisionTriggerEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
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
import com.sequenceiq.freeipa.service.validation.SeLinuxValidationService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class FreeIpaCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationService.class);

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
    private AccountTelemetryService accountTelemetryService;

    @Inject
    private MultiAzCalculatorService multiAzCalculatorService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private MultiAzValidator multiAzValidator;

    @Inject
    private FreeIpaRecommendationService freeIpaRecommendationService;

    @Inject
    private FreeIpaRecipeService freeIpaRecipeService;

    @Inject
    private ImageConverter imageConverter;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SeLinuxValidationService seLinuxValidationService;

    @Value("${info.app.version:}")
    private String appVersion;

    public CreateFreeIpaV1Response launchFreeIpa(CreateFreeIpaRequest request, String accountId) {
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
        multiAzValidator.validateMultiAzForStack(stack.getPlatformvariant(), stack.getInstanceGroups());
        ImageSettingsRequest imageSettingsRequest = request.getImage() == null ? new ImageSettingsRequest() : request.getImage();
        Pair<ImageWrapper, String> imageWrapperStringPair = imageService.fetchImageWrapperAndName(stack, imageSettingsRequest);
        if (stack.getArchitecture() == null) {
            stack.setArchitecture(Architecture.fromStringWithFallback(imageWrapperStringPair.getKey().getImage().getArchitecture()));
        }
        measure(() -> freeIpaRecommendationService.validateCustomInstanceType(stack, credential), LOGGER,
                "Validating custom instance type took {} ms for {}", stack.getName());
        validateSeLinux(stack, imageWrapperStringPair.getLeft().getImage());
        try {
            Triple<Stack, ImageEntity, FreeIpa> stackImageFreeIpaTuple = transactionService.required(() -> {
                Stack savedStack = stackService.save(stack);
                securityConfigService.create(stack, request.getSecurity());
                freeIpaRecipeService.saveRecipes(request.getRecipes(), savedStack.getId());
                freeIpaRecipeService.sendCreationUsageReport(stack.getResourceCrn(), CollectionUtils.emptyIfNull(request.getRecipes()).size());
                ImageEntity image = imageService.create(savedStack, imageWrapperStringPair);
                Image imageForIm = imageConverter.convert(image);
                if (!Objects.equals(savedStack.getArchitecture(), Architecture.fromStringWithFallback(image.getArchitecture()))) {
                    throw new BadRequestException(String.format("Image architecture doesn't match the selected FreeIPA architecture. Image architecture: %s, " +
                            "FreeIPA architecture: %s.", imageForIm.getArchitecture(), savedStack.getArchitecture()));
                }
                if (!entitlementService.isEntitledToUseOS(accountId, OsType.getByOsTypeString(image.getOsType()))) {
                    throw new BadRequestException(String.format("Your account is not entitled to use %s images.", image.getOs()));
                }
                stack.getAllInstanceMetaDataList().forEach(im -> im.setImage(new Json(imageForIm)));
                savedStack = setSupportedImdsVersionForStackIfNecessary(savedStack, image).orElse(savedStack);
                FreeIpa freeIpa = freeIpaService.create(savedStack, request.getFreeIpa(), image.getOsType());
                return Triple.of(savedStack, image, freeIpa);
            });
            FlowIdentifier flowIdentifier = flowManager.notify(FlowChainTriggers.PROVISION_TRIGGER_EVENT,
                    new ProvisionTriggerEvent(FlowChainTriggers.PROVISION_TRIGGER_EVENT,
                            stackImageFreeIpaTuple.getLeft().getId(),
                            getFreeIpaLoadBalancerType(request)));
            InMemoryStateStore.putStack(stack.getId(), PollGroup.POLLABLE);
            return stackToDescribeFreeIpaResponseConverter.convertToCreate(
                    stackImageFreeIpaTuple.getLeft(),
                    stackImageFreeIpaTuple.getMiddle(),
                    stackImageFreeIpaTuple.getRight(),
                    Optional.empty(),
                    false,
                    environment,
                    flowIdentifier
            );
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Creation of FreeIPA failed", e);
            throw new BadRequestException("Creation of FreeIPA failed: " + e.getCause().getMessage(), e);
        }
    }

    private void validateSeLinux(Stack stack, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image image) {
        try {
            seLinuxValidationService.validateSeLinuxEntitlementGranted(stack);
            seLinuxValidationService.validateSeLinuxSupportedOnTargetImage(stack, image);
        } catch (CloudbreakServiceException e) {
            throw new BadRequestException(e);
        }
    }

    private FreeIpaLoadBalancerType getFreeIpaLoadBalancerType(CreateFreeIpaRequest request) {
        try {
            return FreeIpaLoadBalancerType.valueOf(request.getLoadBalancerType().toUpperCase(Locale.US));
        } catch (IllegalArgumentException | NullPointerException e) {
            return FreeIpaLoadBalancerType.getDefault();
        }
    }

    private Future<String> initiateOwnerFetching(String userCrn) {
        Future<String> ownerFuture;
        if (Crn.safeFromString(userCrn).getResourceType().equals(Crn.ResourceType.MACHINE_USER)) {
            ownerFuture = intermediateBuilderExecutor.submit(() ->
                    umsClient.getMachineUserDetails(userCrn, Crn.fromString(userCrn).getAccountId()).getMachineUserName());
        } else {
            ownerFuture = intermediateBuilderExecutor.submit(() -> umsClient.getUserDetails(userCrn).getEmail());
        }
        return ownerFuture;
    }

    private Optional<Stack> setSupportedImdsVersionForStackIfNecessary(Stack stack, ImageEntity image) {
        if (CloudPlatform.AWS.equals(CloudPlatform.valueOf(stack.getCloudPlatform()))) {
            if (StringUtils.equals(image.getImdsVersion(), AWS_IMDS_VERSION_V2)) {
                stack.setSupportedImdsVersion(AWS_IMDS_VERSION_V2);
            } else {
                stack.setSupportedImdsVersion(AWS_IMDS_VERSION_V1);
            }
            return Optional.of(stackService.save(stack));
        }
        return Optional.empty();
    }

    private void fillInstanceMetadata(Stack stack, DetailedEnvironmentResponse environment) {
        long privateIdNumber = 0;
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                instanceMetaData.setPrivateId(privateIdNumber++);
                instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
            }
            multiAzCalculatorService.calculateByRoundRobin(multiAzCalculatorService.prepareSubnetAzMap(environment), instanceGroup, stack);
            multiAzCalculatorService.populateAvailabilityZonesForInstances(stack, instanceGroup);
        }
    }
}
