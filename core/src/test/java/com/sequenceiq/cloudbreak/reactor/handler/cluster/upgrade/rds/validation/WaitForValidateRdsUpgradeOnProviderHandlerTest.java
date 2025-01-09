package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
class WaitForValidateRdsUpgradeOnProviderHandlerTest {

    private static final Long STACK_ID = 42L;

    private static final String FLOW_ID = "flow_id";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private HandlerEvent<WaitForValidateRdsUpgradeOnCloudProviderRequest> event;

    @Mock
    private ExternalDatabaseService externalDatabaseService;

    @InjectMocks
    private WaitForValidateRdsUpgradeOnProviderHandler underTest;

    private FlowIdentifier flowIdentifier;

    @BeforeEach
    void setup() {
        flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("WAITFORVALIDATERDSUPGRADEONCLOUDPROVIDERREQUEST");
    }

    @Test
    void testDoAccept() {
        WaitForValidateRdsUpgradeOnCloudProviderRequest request = new WaitForValidateRdsUpgradeOnCloudProviderRequest(
                STACK_ID, new FlowIdentifier(FlowType.FLOW, FLOW_ID), new DatabaseConnectionProperties());
        when(event.getData()).thenReturn(request);

        ClusterView clusterView = mock(ClusterView.class);
        initStackDto(clusterView);

        Selectable result = underTest.doAccept(event);

        assertThat(result.selector()).isEqualTo("WAITFORVALIDATERDSUPGRADEONCLOUDPROVIDERRESULT");
        assertThat(result).isInstanceOf(WaitForValidateRdsUpgradeOnCloudProviderResult.class);
        assertThat(((WaitForValidateRdsUpgradeOnCloudProviderResult) result).getReason()).isNullOrEmpty();
    }

    private void initStackDto(ClusterView clusterView) {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(clusterView);
    }

    @Test
    void testDoAcceptWhenValidationFailed() {
        DatabaseServerV4Response serverResponse = new DatabaseServerV4Response();
        serverResponse.setId(1L);
        serverResponse.setStatusReason("status_reason");

        WaitForValidateRdsUpgradeOnCloudProviderRequest request = new WaitForValidateRdsUpgradeOnCloudProviderRequest(STACK_ID,
                new FlowIdentifier(FlowType.FLOW, FLOW_ID), new DatabaseConnectionProperties());
        when(event.getData()).thenReturn(request);

        ClusterView clusterView = mock(ClusterView.class);
        initStackDto(clusterView);
        when(externalDatabaseService.waitForDatabaseFlowToBeFinished(eq(clusterView), eq(flowIdentifier))).thenReturn(serverResponse);

        Selectable result = underTest.doAccept(event);

        assertThat(result.selector()).isEqualTo("WAITFORVALIDATERDSUPGRADEONCLOUDPROVIDERRESULT");
        assertThat(result).isInstanceOf(WaitForValidateRdsUpgradeOnCloudProviderResult.class);
        assertThat(((WaitForValidateRdsUpgradeOnCloudProviderResult) result).getReason()).isEqualTo("status_reason");
    }

    @Test
    void testDoAcceptWhenException() {
        WaitForValidateRdsUpgradeOnCloudProviderRequest request = new WaitForValidateRdsUpgradeOnCloudProviderRequest(STACK_ID,
                flowIdentifier, new DatabaseConnectionProperties());
        when(event.getData()).thenReturn(request);
        ClusterView clusterView = mock(ClusterView.class);
        UserBreakException userBreakException = new UserBreakException("exception");
        initStackDto(clusterView);
        doThrow(userBreakException).when(externalDatabaseService).waitForDatabaseFlowToBeFinished(eq(clusterView), eq(flowIdentifier));

        Selectable result = underTest.doAccept(event);

        assertThat(result).isInstanceOf(ValidateRdsUpgradeFailedEvent.class);
        assertThat(result.getException()).isEqualTo(userBreakException);
        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEFAILEDEVENT");
    }
}