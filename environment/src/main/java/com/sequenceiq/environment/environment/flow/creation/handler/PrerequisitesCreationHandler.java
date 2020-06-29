package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_PREREQUISITES_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;
import static com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation.CREATE_NEW;
import static com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern.USE_MULTIPLE;

import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.prerequisite.AzurePrerequisiteCreateRequest;
import com.sequenceiq.cloudbreak.cloud.model.prerequisite.EnvironmentPrerequisitesCreateRequest;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameters.dto.AzureParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class PrerequisitesCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrerequisitesCreationHandler.class);

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final EntitlementService entitlementService;

    private final CostTagging costTagging;

    private final EventBus eventBus;

    private final EnvironmentService environmentService;

    private final ParametersService parameterService;

    private final Clock clock;

    protected PrerequisitesCreationHandler(EventSender eventSender,
            CloudPlatformConnectors cloudPlatformConnectors,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            EntitlementService entitlementService,
            CostTagging costTagging, EventBus eventBus, EnvironmentService environmentService,
            ParametersService parameterService,
            Clock clock) {
        super(eventSender);
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.entitlementService = entitlementService;
        this.costTagging = costTagging;
        this.eventBus = eventBus;
        this.environmentService = environmentService;
        this.parameterService = parameterService;
        this.clock = clock;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        environmentService.findEnvironmentById(environmentDto.getId())
                .ifPresentOrElse(environment -> {
                            try {
                                if (AZURE.name().equals(environmentDto.getCloudPlatform())) {
                                    createResourceGroup(environmentDto, environment);
                                } else {
                                    LOGGER.debug("Cloudplatform not Azure, not creating resource group.");
                                }
                                goToNetworkCreationState(environmentDtoEvent);
                            } catch (Exception e) {
                                goToFailedState(environmentDtoEvent, e.getMessage());
                            }
                        }, () -> goToFailedState(environmentDtoEvent, String.format("Environment was not found with id '%s'.", environmentDto.getId()))
                );
    }

    private void createResourceGroup(EnvironmentDto environmentDto, Environment environment) {
        if (!entitlementService.azureSingleResourceGroupDeploymentEnabled(INTERNAL_ACTOR_CRN, environment.getAccountId())) {
            LOGGER.debug("Azure single resource group feature turned off, not creating resourcegroup.");
            return;
        }

        Optional<AzureResourceGroupDto> azureResourceGroupDtoOptional = getAzureResourceGroupDto(environmentDto);
        if (azureResourceGroupDtoOptional.isEmpty()) {
            LOGGER.debug("No azure resource group dto defined, not creating resource group.");
            return;
        }
        AzureResourceGroupDto azureResourceGroupDto = azureResourceGroupDtoOptional.get();
        LOGGER.debug("Azure resource group: {}", azureResourceGroupDto);
        if (USE_MULTIPLE.equals(azureResourceGroupDto.getResourceGroupUsagePattern()) || !CREATE_NEW.equals(azureResourceGroupDto.getResourceGroupCreation())) {
            LOGGER.debug("New single azure resource group creation not requested, not creating resource group.");
            return;
        }

        String resourceGroupName = String.format("%s_%d", environmentDto.getName(), clock.getCurrentTimeMillis());
        LOGGER.debug("Azure generated resource group name: {}.", resourceGroupName);
        createResourceGroup(environmentDto, resourceGroupName);
        parameterService.updateResourceGroupName(environment, resourceGroupName);
        LOGGER.debug("Azure resource group created successfully.");
    }

    private Optional<Setup> getSetupConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).setup());
    }

    private void createResourceGroup(EnvironmentDto environmentDto, String resourceGroupName) {
        try {
            Optional<Setup> setupOptional = getSetupConnector(environmentDto.getCloudPlatform());
            if (setupOptional.isEmpty()) {
                LOGGER.debug("No setup defined for platform {}, resource group not created.", environmentDto.getCloudPlatform());
                return;
            }

            EnvironmentPrerequisitesCreateRequest environmentPrerequisitesCreateRequest = new EnvironmentPrerequisitesCreateRequest(
                    credentialToCloudCredentialConverter.convert(environmentDto.getCredential()),
                    AzurePrerequisiteCreateRequest.builder()
                            .withResourceGroupName(resourceGroupName)
                            .withLocation(environmentDto.getLocation().getName())
                            .withTags(environmentDto.mergeTags(costTagging))
                            .build());
            setupOptional.get().createEnvironmentPrerequisites(environmentPrerequisitesCreateRequest);
        } catch (Exception e) {
            throw new CloudConnectorException("Could not create resource group" + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    private Optional<AzureResourceGroupDto> getAzureResourceGroupDto(EnvironmentDto environmentDto) {
        return Optional.ofNullable(environmentDto.getParameters())
                .map(ParametersDto::getAzureParametersDto)
                .map(AzureParametersDto::getAzureResourceGroupDto);
    }

    private void goToFailedState(Event<EnvironmentDto> environmentDtoEvent, String message) {
        LOGGER.debug("Going to failed state, message: {}.", message);
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                environmentDto.getId(),
                environmentDto.getName(),
                new BadRequestException(message),
                environmentDto.getResourceCrn());

        eventBus.notify(failureEvent.selector(), new Event<>(environmentDtoEvent.getHeaders(), failureEvent));
    }

    private void goToNetworkCreationState(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        EnvCreationEvent envCreationEvent = EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(START_NETWORK_CREATION_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
        LOGGER.debug("Proceeding to next flow step.");
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }

    @Override
    public String selector() {
        return CREATE_PREREQUISITES_EVENT.selector();
    }

}
