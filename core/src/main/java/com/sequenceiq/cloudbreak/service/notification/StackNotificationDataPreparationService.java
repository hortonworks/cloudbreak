package com.sequenceiq.cloudbreak.service.notification;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.common.api.type.CdpResourceType.fromStackType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.notification.config.CDPConsoleUrlProvider;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.ClusterHealthNotificationAdditionalDataDto;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.InstanceStatusDto;

@Service
public class StackNotificationDataPreparationService {

    private final InstanceMetaDataService instanceMetaDataService;

    private final TransactionService transactionService;

    private final CDPConsoleUrlProvider cdpConsoleUrlProvider;

    private final StackNotificationTypePreparationService stackNotificationTypePreparationService;

    private final EnvironmentService environmentService;

    private final CredentialClientService credentialClientService;

    private final CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    public StackNotificationDataPreparationService(
            InstanceMetaDataService instanceMetaDataService,
            TransactionService transactionService,
            CDPConsoleUrlProvider cdpConsoleUrlProvider,
            CloudPlatformConnectors cloudPlatformConnectors,
            CredentialClientService credentialClientService,
            CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter,
            StackNotificationTypePreparationService stackNotificationTypePreparationService, EnvironmentService environmentService) {
        this.instanceMetaDataService = instanceMetaDataService;
        this.transactionService = transactionService;
        this.cdpConsoleUrlProvider = cdpConsoleUrlProvider;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.credentialClientService = credentialClientService;
        this.extendedCloudCredentialConverter = extendedCloudCredentialConverter;
        this.stackNotificationTypePreparationService = stackNotificationTypePreparationService;
        this.environmentService = environmentService;
    }

    public NotificationGeneratorDtos notificationGeneratorDtos(Stack stack, Status newStatus,
        DetailedStackStatus newDetailedStatus, String statusReason, String accountId) throws TransactionService.TransactionExecutionException {
        return transactionService.required(() ->
                NotificationGeneratorDtos.builder()
                        .notification(Set.of(prepareResults(stack, newStatus, newDetailedStatus, statusReason, accountId)))
                        .notificationType(stackNotificationTypePreparationService.notificationType(newStatus))
                        .build()
        );
    }

    private NotificationGeneratorDto prepareResults(Stack stack, Status newStatus,
        DetailedStackStatus newDetailedStatus, String statusReason, String accountId) {
        DetailedEnvironmentResponse environment = environmentService.getByCrn(stack.getEnvironmentCrn());
        Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
        ExtendedCloudCredential extendedCloudCredential = extendedCloudCredentialConverter.convert(credential);
        return NotificationGeneratorDto.builder()
                .resourceCrn(stack.getResourceCrn())
                .name(stack.getName())
                .resourceName(stack.getName() + "_" + stack.getResourceCrn())
                .additionalData(
                        BaseNotificationRegisterAdditionalDataDtos.builder()
                                .results(List.of(
                                        ClusterHealthNotificationAdditionalDataDto
                                                .builder()
                                                .status(newStatus.name())
                                                .creator(stack.getCreator().getUserName())
                                                .crn(stack.getResourceCrn())
                                                .name(stack.getName())
                                                .stackType(stack.getType().equals(StackType.DATALAKE) ? "Data Lake" : "Data Hub")
                                                .statusReason(statusReason)
                                                .detailedStatus(newDetailedStatus.name())
                                                .dateTimeString(getCurrentDateTimeForEmail())
                                                .instances(instanceStatus(environment, extendedCloudCredential, stack))
                                                .controlPlaneUrl(getClusterUrl(environment, stack))
                                                .build()
                                ))
                                .build())
                .accountId(accountId)
                .build();
    }

    private String getClusterUrl(DetailedEnvironmentResponse environment, Stack stack) {
        return cdpConsoleUrlProvider.getClusterUrl(fromStackType(stack.getType().name()), environment.getName(), stack.getName());
    }

    Set<InstanceStatusDto> instanceStatus(DetailedEnvironmentResponse environmentResponse, ExtendedCloudCredential extendedCloudCredential, Stack stack) {
        CloudConnector cloudConnector = cloudPlatformConnectors.getDefault(Platform.platform(stack.cloudPlatform()));
        return instanceMetaDataService.findAllByStackIdAndStatusGroup(
                        stack.getId(),
                        stackNotificationTypePreparationService.instanceNotificationTargets())
                .stream()
                .map(e -> InstanceStatusDto
                        .builder()
                        .status(e.getInstanceStatus().name())
                        .instanceType(e.getProviderInstanceType())
                        .groupName(e.getInstanceGroupName())
                        .url(getInstanceUrl(environmentResponse, extendedCloudCredential, stack, e.getInstanceId(), cloudConnector))
                        .name(e.getInstanceId())
                        .build())
                .collect(Collectors.toSet());
    }

    private String getInstanceUrl(DetailedEnvironmentResponse environment, ExtendedCloudCredential credential,
        Stack stack, String instanceId, CloudConnector cloudConnector) {
        return cloudConnector
                .platformResources()
                .getVirtualMachineUrl(credential, region(stack.getRegion()), instanceId, Map.of())
                .orElse(getClusterUrl(environment, stack));
    }

    public String getCurrentDateTimeForEmail() {
        return DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' hh:mm a z")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }
}
