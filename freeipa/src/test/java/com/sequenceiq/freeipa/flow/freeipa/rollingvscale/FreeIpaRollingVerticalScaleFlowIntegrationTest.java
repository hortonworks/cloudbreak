package com.sequenceiq.freeipa.flow.freeipa.rollingvscale;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.VERTICAL_SCALE_IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.handler.StartStackHandler;
import com.sequenceiq.cloudbreak.cloud.handler.StopStackHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformInitializer;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailedFlowAnalyzer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaValidationProperties;
import com.sequenceiq.freeipa.flow.freeipa.downscale.handler.StopHealthAgentHandler;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.action.FreeIpaRollingVerticalScaleActions;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.handler.RollingVerticalScaleDescribeHandler;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.handler.RollingVerticalScaleHealthCheckHandler;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.handler.RollingVerticalScaleResizeHandler;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.flow.stack.start.AttemptMakerFactory;
import com.sequenceiq.freeipa.flow.stack.stop.handler.StopFreeIpaServicesHandler;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaServicesStopService;
import com.sequenceiq.freeipa.service.healthagent.HealthAgentService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceAttributeUtil;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.FreeIpaInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "freeipa.rolling-vertical-scale.health-check.max-attempts=5",
        "freeipa.rolling-vertical-scale.health-check.interval-seconds=1",
        "freeipa.rolling-vertical-scale.health-check.consecutive-success-required=1"
})
class FreeIpaRollingVerticalScaleFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String INSTANCE_ID = "i-0abc123";

    private static final String ACCOUNT_ID = "testaccountid";

    private static final String ENVIRONMENT_CRN = "env-crn";

    private static final String STACK_CRN = "crn:cdp:freeipa:us-west-1:" + ACCOUNT_ID + ":freeipa:test-freeipa-resource";

    @MockBean(reset = MockReset.NONE)
    private StackService stackService;

    @MockBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockBean
    private StackUpdater stackUpdater;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @MockBean
    private StackToCloudStackConverter stackToCloudStackConverter;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private InstanceMetaDataService instanceMetaDataService;

    @MockBean
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private OperationService operationService;

    @MockBean
    private FreeIpaServicesStopService freeIpaServicesStopService;

    @MockBean
    private FreeIpaInstanceHealthDetailsService healthDetailsService;

    @MockBean
    private FreeIpaLoadBalancerService loadBalancerService;

    @MockBean
    private HealthAgentService healthAgentService;

    @MockBean
    private DelayedExecutorService delayedExecutorService;

    @MockBean
    private EventSenderService eventSenderService;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private NodeConfig nodeConfig;

    @MockBean
    private NodeValidator nodeValidator;

    @MockBean
    private FlowCancelService flowCancelService;

    @MockBean
    private FlowUsageSender flowUsageSender;

    @MockBean
    private FlowEventListener flowEventListener;

    @MockBean
    private StackStatusFinalizer stackStatusFinalizer;

    @MockBean
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @BeforeEach
    public void setup() throws Exception {
        Template template = new Template();
        template.setInstanceType("m5.xlarge");

        InstanceGroup ig = new InstanceGroup();
        ig.setGroupName("master");
        ig.setTemplate(template);

        InstanceMetaData instance = new InstanceMetaData();
        instance.setInstanceId(INSTANCE_ID);
        instance.setDiscoveryFQDN("freeipa-0.example.com");
        instance.setInstanceGroup(ig);
        ig.setInstanceMetaData(Set.of(instance));

        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setCloudPlatform("AWS");
        stack.setPlatformvariant("AWS");
        stack.setRegion("us-east-1");
        stack.setAvailabilityZone("us-east-1a");
        stack.setName("test-stack");
        stack.setResourceCrn(STACK_CRN);
        stack.setInstanceGroups(Set.of(ig));

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);

        Credential credential = mock(Credential.class);
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(mock(CloudCredential.class));
        when(stackToCloudStackConverter.convert(stack)).thenReturn(CloudStack.builder().build());
        when(instanceMetaDataToCloudInstanceConverter.convert(any(InstanceMetaData.class))).thenReturn(mock(CloudInstance.class));
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(List.of());
        when(instanceMetaDataService.getByInstanceIds(eq(STACK_ID), any())).thenReturn(Set.of(instance));

        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        Authenticator authenticator = mock(Authenticator.class);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(mock(AuthenticatedContext.class));
        InstanceConnector instanceConnector = mock(InstanceConnector.class);
        when(connector.instances()).thenReturn(instanceConnector);
        when(instanceConnector.stop(any(), any(), any())).thenReturn(List.of());
        when(instanceConnector.start(any(), any(), any())).thenReturn(List.of());
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.update(any(), any(), any(), any(), any())).thenReturn(List.of());

        RPCResponse<Boolean> healthyResponse = new RPCResponse<>();
        healthyResponse.setResult(Boolean.TRUE);
        when(healthDetailsService.checkFreeIpaHealth(any(), any())).thenReturn(healthyResponse);

        doNothing().when(nodeValidator).checkForRecentHeartbeat();
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());
    }

    @Test
    public void testRollingVerticalScaleFlowCompletesSuccessfully() throws Exception {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        assertTrue(flowRegister.getRunningFlowIds().isEmpty(), "Flow should have finished");
        ArgumentCaptor<FlowLog> flowLogCaptor = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, atLeastOnce()).save(flowLogCaptor.capture());
        assertTrue(flowLogCaptor.getAllValues().stream().anyMatch(FlowLog::getFinalized), "Flow should be finalized");
        verify(stackUpdater, atLeastOnce()).updateStackStatus(eq(STACK_ID), eq(VERTICAL_SCALE_IN_PROGRESS), anyString());
    }

    private FlowIdentifier triggerFlow() {
        FreeIpaVerticalScaleParameters scaleConfig =
                new FreeIpaVerticalScaleParameters("master", "m5.2xlarge", "m5.xlarge", null);
        FreeIpaRollingVerticalScaleTriggerEvent event =
                new FreeIpaRollingVerticalScaleTriggerEvent(STACK_ID, INSTANCE_ID, scaleConfig);
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> freeIpaFlowManager.notify(event.selector(), event));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 50);
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            FreeIpaRollingVerticalScaleActions.class,
            FreeIpaRollingVerticalScaleFlowConfig.class,
            FlowIntegrationTestConfig.class,
            ResourceToCloudResourceConverter.class,
            ResourceAttributeUtil.class,
            WebApplicationExceptionMessageExtractor.class,
            FreeIpaFailedFlowAnalyzer.class,
            FreeIpaValidationProperties.class,
            AttemptMakerFactory.class,
            RollingVerticalScaleDescribeHandler.class,
            StopHealthAgentHandler.class,
            StopFreeIpaServicesHandler.class,
            StopStackHandler.class,
            RollingVerticalScaleResizeHandler.class,
            StartStackHandler.class,
            RollingVerticalScaleHealthCheckHandler.class,
            CloudPlatformInitializer.class
    })
    static class TestConfig {

        @MockBean
        private FreeipaJobService jobService;
    }
}
