package com.sequenceiq.environment.environment.service.notification;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.environment.api.v1.terms.model.TermType;
import com.sequenceiq.environment.environment.dto.CompactViewDto;
import com.sequenceiq.environment.environment.service.EnvironmentOutboundService;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.environment.terms.service.TermsService;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.Subscription;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDto;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.scheduled.register.dto.azureoutbound.AzureOutboundNotificationAdditionalDataDto;

@Service
public class EnvironmentNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentNotificationService.class);

    private final EnvironmentViewService environmentViewService;

    private final DatahubService datahubService;

    private final TermsService termsService;

    private final EnvironmentOutboundService outboundService;

    private final ParametersService parametersService;

    public EnvironmentNotificationService(
            EnvironmentViewService environmentViewService,
            DatahubService datahubService,
            TermsService termsService,
            EnvironmentOutboundService outboundService,
            ParametersService parametersService) {
        this.environmentViewService = environmentViewService;
        this.datahubService = datahubService;
        this.termsService = termsService;
        this.outboundService = outboundService;
        this.parametersService = parametersService;
    }

    public List<NotificationGeneratorDto> filterForOutboundUpgradeNotifications() {
        return environmentViewService.findAllResourceCrnsByArchivedIsFalseAndCloudPlatform(CloudPlatform.AZURE.name())
                .stream()
                .filter(this::notificationSendingIsRequired)
                .map(actualEnvironmentCrn -> {
                    Crn crn = Crn.safeFromString(actualEnvironmentCrn);
                    String environmentViewName = environmentViewService.getNameByCrn(actualEnvironmentCrn);
                    return build(actualEnvironmentCrn, environmentViewName, crn);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private NotificationGeneratorDto build(String actualEnvironmentCrn, String environmentViewName, Crn crn) {
        return NotificationGeneratorDto.builder()
                .resourceCrn(actualEnvironmentCrn)
                .name(environmentViewName)
                .resourceName(environmentViewName + "_" + actualEnvironmentCrn)
                .additionalData(targets(actualEnvironmentCrn))
                .accountId(crn.getAccountId())
                .build();
    }

    private BaseNotificationRegisterAdditionalDataDtos targets(String actualEnvironmentCrn) {
        List<BaseNotificationRegisterAdditionalDataDto> list = datahubService.listInternal(actualEnvironmentCrn).getResponses()
                .stream()
                .filter(s -> s.getProviderSyncStates().contains(ProviderSyncState.OUTBOUND_UPGRADE_NEEDED))
                .map(s -> AzureOutboundNotificationAdditionalDataDto.builder()
                        .crn(s.getCrn())
                        .name(s.getName())
                        .status(s.getStatus().name())
                        .creator(s.getUser().getUserName())
                        .build())
                .map(dto -> (BaseNotificationRegisterAdditionalDataDto) dto)
                .toList();
        return BaseNotificationRegisterAdditionalDataDtos.builder()
                .results(list)
                .build();
    }

    private boolean notificationSendingIsRequired(String actualEnvironmentCrn) {
        if (termsService.get(Crn.safeFromString(actualEnvironmentCrn).getAccountId(), TermType.AZURE_DEFAULT_OUTBOUND_TERMS)) {
            LOGGER.debug("Notification sending is suppressed for environment {}.", actualEnvironmentCrn);
            return false;
        } else {
            Map<String, OutboundType> outboundTypeMap = outboundService.validateOutboundTypes(actualEnvironmentCrn)
                    .getStackOutboundTypeMap();
            boolean hasClusterWithDefaultOutbound = outboundTypeMap
                    .values()
                    .stream()
                    .anyMatch(OutboundType::isDefault);
            LOGGER.debug("Environment cluster(s) with default outbound type: {}", hasClusterWithDefaultOutbound);
            return hasClusterWithDefaultOutbound;
        }
    }

    public void processDistributionListSync(List<? extends Subscription> subscriptions, List<DistributionList> distributionLists) {
        int listCount = distributionLists.size();
        LOGGER.info("Received {} distribution list(s) after sending Azure default outbound notifications", listCount);

        if (listCount == 0) {
            LOGGER.info("No distribution lists were created for Azure default outbound notifications, subscriptions: {}", subscriptions);
        } else if (listCount == 1) {
            DistributionList distributionList = distributionLists.getFirst();
            LOGGER.info("1 distribution list was created for Azure default outbound notifications {}", distributionList);
            updateDistributionList(distributionList.getResourceCrn(), distributionList);
        } else {
            LOGGER.warn("Multiple ({}) distribution lists were created for Azure default outbound notifications, this should not happen, subscriptions: {}",
                    listCount, subscriptions);
        }
    }

    private void updateDistributionList(String envCrn, DistributionList list) {
        Optional<CompactViewDto> envView = environmentViewService.getCompactViewByCrn(envCrn);
        envView.ifPresentOrElse(compactView -> {
            parametersService.updateDistributionListDetails(compactView.getId(), list);
        }, () -> LOGGER.warn("Cannot update distribution list details, environment not found for CRN: {}", envCrn));

    }
}
