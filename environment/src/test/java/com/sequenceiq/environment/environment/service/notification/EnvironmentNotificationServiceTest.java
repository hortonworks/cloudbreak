package com.sequenceiq.environment.environment.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.environment.api.v1.environment.model.response.OutboundTypeValidationResponse;
import com.sequenceiq.environment.api.v1.terms.model.TermType;
import com.sequenceiq.environment.environment.dto.CompactViewDto;
import com.sequenceiq.environment.environment.service.EnvironmentOutboundService;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.environment.terms.service.TermsService;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.DistributionListManagementType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;

@ExtendWith(MockitoExtension.class)
class EnvironmentNotificationServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:resource-123";

    private static final String ENV_CRN_2 = "crn:cdp:environments:us-west-1:tenant:environment:resource-456";

    private static final String ENV_NAME = "test-env";

    private static final String ACCOUNT_ID = "tenant";

    private static final Long ENV_ID = 123L;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private DatahubService datahubService;

    @Mock
    private TermsService termsService;

    @Mock
    private EnvironmentOutboundService outboundService;

    @Mock
    private ParametersService parametersService;

    @InjectMocks
    private EnvironmentNotificationService underTest;

    private StackViewV4Responses stackViewResponses;

    @BeforeEach
    void setUp() {
        stackViewResponses = new StackViewV4Responses();
        stackViewResponses.setResponses(new ArrayList<>());
    }

    @Test
    void filterForOutboundUpgradeNotificationsReturnsEnvironments() {
        when(environmentViewService.findAllResourceCrnsByArchivedIsFalseAndCloudPlatform(CloudPlatform.AZURE.name()))
                .thenReturn(List.of(ENV_CRN));
        when(environmentViewService.getNameByCrn(ENV_CRN)).thenReturn(ENV_NAME);
        when(termsService.get(ACCOUNT_ID, TermType.AZURE_DEFAULT_OUTBOUND_TERMS)).thenReturn(false);
        when(outboundService.validateOutboundTypes(ENV_CRN))
                .thenReturn(createOutboundValidationResponse(Map.of("cluster1", OutboundType.DEFAULT)));
        when(datahubService.listInternal(ENV_CRN)).thenReturn(stackViewResponses);

        List<NotificationGeneratorDto> result = underTest.filterForOutboundUpgradeNotifications();

        assertNotNull(result);
        assertEquals(1, result.size());
        NotificationGeneratorDto dto = result.getFirst();
        assertEquals(ENV_CRN, dto.getResourceCrn());
        assertEquals(ENV_NAME, dto.getName());
        assertEquals(ACCOUNT_ID, dto.getAccountId());
    }

    @Test
    void filterForOutboundUpgradeNotificationsFiltersOutAcceptedTerms() {
        when(environmentViewService.findAllResourceCrnsByArchivedIsFalseAndCloudPlatform(CloudPlatform.AZURE.name()))
                .thenReturn(List.of(ENV_CRN));
        when(termsService.get(ACCOUNT_ID, TermType.AZURE_DEFAULT_OUTBOUND_TERMS)).thenReturn(true);

        List<NotificationGeneratorDto> result = underTest.filterForOutboundUpgradeNotifications();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(outboundService, never()).validateOutboundTypes(anyString());
    }

    @Test
    void filterForOutboundUpgradeNotificationsFiltersOutNonDefaultOutbound() {
        when(environmentViewService.findAllResourceCrnsByArchivedIsFalseAndCloudPlatform(CloudPlatform.AZURE.name()))
                .thenReturn(List.of(ENV_CRN));
        when(termsService.get(ACCOUNT_ID, TermType.AZURE_DEFAULT_OUTBOUND_TERMS)).thenReturn(false);
        when(outboundService.validateOutboundTypes(ENV_CRN)).thenReturn(createOutboundValidationResponse(Map.of("cluster1", OutboundType.PUBLIC_IP)));

        List<NotificationGeneratorDto> result = underTest.filterForOutboundUpgradeNotifications();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void filterForOutboundUpgradeNotificationsProcessesMultipleEnvironments() {
        when(environmentViewService.findAllResourceCrnsByArchivedIsFalseAndCloudPlatform(CloudPlatform.AZURE.name()))
                .thenReturn(List.of(ENV_CRN, ENV_CRN_2));
        when(environmentViewService.getNameByCrn(ENV_CRN)).thenReturn(ENV_NAME);
        when(environmentViewService.getNameByCrn(ENV_CRN_2)).thenReturn("env2");
        when(termsService.get(ACCOUNT_ID, TermType.AZURE_DEFAULT_OUTBOUND_TERMS)).thenReturn(false);
        when(outboundService.validateOutboundTypes(anyString()))
                .thenReturn(createOutboundValidationResponse(Map.of("cluster1", OutboundType.DEFAULT)));
        when(datahubService.listInternal(anyString())).thenReturn(stackViewResponses);

        List<NotificationGeneratorDto> result = underTest.filterForOutboundUpgradeNotifications();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void processDistributionListSyncWithNoDistributionLists() {
        List<DistributionList> distributionLists = List.of();

        underTest.processDistributionListSync(List.of(), distributionLists);

        verify(parametersService, never()).updateDistributionListDetails(anyLong(), any());
    }

    @Test
    void processDistributionListSyncWithOneDistributionList() {
        DistributionList distributionList = createDistributionList(ENV_CRN, "dl-123");
        CompactViewDto compactView = new CompactViewDto(ENV_ID, ENV_CRN);

        when(environmentViewService.getCompactViewByCrn(ENV_CRN)).thenReturn(Optional.of(compactView));

        underTest.processDistributionListSync(List.of(), List.of(distributionList));

        verify(parametersService).updateDistributionListDetails(ENV_ID, distributionList);
    }

    @Test
    void processDistributionListSyncWithMultipleDistributionLists() {
        DistributionList distributionList1 = createDistributionList(ENV_CRN, "dl-123");
        DistributionList distributionList2 = createDistributionList(ENV_CRN_2, "dl-456");

        underTest.processDistributionListSync(List.of(), List.of(distributionList1, distributionList2));

        verify(parametersService, never()).updateDistributionListDetails(anyLong(), any());
    }

    @Test
    void processDistributionListSyncWhenEnvironmentNotFound() {
        DistributionList distributionList = createDistributionList(ENV_CRN, "dl-123");

        when(environmentViewService.getCompactViewByCrn(ENV_CRN)).thenReturn(Optional.empty());

        underTest.processDistributionListSync(List.of(), List.of(distributionList));

        verify(parametersService, never()).updateDistributionListDetails(anyLong(), any());
    }

    @Test
    void filterForOutboundUpgradeNotificationsIncludesDatahubTargets() {
        when(environmentViewService.findAllResourceCrnsByArchivedIsFalseAndCloudPlatform(CloudPlatform.AZURE.name()))
                .thenReturn(List.of(ENV_CRN));
        when(environmentViewService.getNameByCrn(ENV_CRN)).thenReturn(ENV_NAME);
        when(termsService.get(ACCOUNT_ID, TermType.AZURE_DEFAULT_OUTBOUND_TERMS)).thenReturn(false);
        when(outboundService.validateOutboundTypes(ENV_CRN))
                .thenReturn(createOutboundValidationResponse(Map.of("cluster1", OutboundType.DEFAULT)));
        when(datahubService.listInternal(ENV_CRN)).thenReturn(stackViewResponses);

        List<NotificationGeneratorDto> result = underTest.filterForOutboundUpgradeNotifications();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(datahubService).listInternal(ENV_CRN);
    }

    @Test
    void filterForOutboundUpgradeNotificationsHandlesMixedOutboundTypes() {
        when(environmentViewService.findAllResourceCrnsByArchivedIsFalseAndCloudPlatform(CloudPlatform.AZURE.name()))
                .thenReturn(List.of(ENV_CRN));
        when(environmentViewService.getNameByCrn(ENV_CRN)).thenReturn(ENV_NAME);
        when(termsService.get(ACCOUNT_ID, TermType.AZURE_DEFAULT_OUTBOUND_TERMS)).thenReturn(false);
        when(outboundService.validateOutboundTypes(ENV_CRN))
                .thenReturn(createOutboundValidationResponse(Map.of(
                        "cluster1", OutboundType.DEFAULT,
                        "cluster2", OutboundType.PUBLIC_IP
                )));
        when(datahubService.listInternal(ENV_CRN)).thenReturn(stackViewResponses);

        List<NotificationGeneratorDto> result = underTest.filterForOutboundUpgradeNotifications();

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    private DistributionList createDistributionList(String resourceCrn, String externalId) {
        return DistributionList.builder()
                .externalDistributionListId(externalId)
                .resourceCrn(resourceCrn)
                .type(DistributionListManagementType.USER_MANAGED)
                .build();
    }

    private OutboundTypeValidationResponse createOutboundValidationResponse(Map<String, OutboundType> stackOutboundTypeMap) {
        OutboundTypeValidationResponse outboundTypeValidationResponse = new OutboundTypeValidationResponse();
        outboundTypeValidationResponse.setStackOutboundTypeMap(stackOutboundTypeMap);
        return outboundTypeValidationResponse;
    }
}