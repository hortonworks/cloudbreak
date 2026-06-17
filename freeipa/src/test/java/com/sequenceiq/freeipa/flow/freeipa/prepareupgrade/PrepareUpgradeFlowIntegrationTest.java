package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.CLUSTER_OPERATION;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailedFlowAnalyzer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaValidationProperties;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.handler.PrepareUpgradeFailureCleanupHandler;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.handler.PrepareUpgradeLbDeletionHandler;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.handler.PrepareUpgradeLbProvisionHandler;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.handler.PrepareUpgradeMetadataCollectionHandler;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerConfigurationService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerCreationService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerMetadataCollectionService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceAttributeUtil;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class PrepareUpgradeFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:env-id";

    private static final String OPERATION_ID = UUID.randomUUID().toString();

    private static final String ACCOUNT_ID = "accountId";

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @MockitoBean
    private StackService stackService;

    @MockitoBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockitoBean
    private StackToCloudStackConverter stackToCloudStackConverter;

    @MockitoBean
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @MockitoBean
    private CredentialService credentialService;

    @MockitoBean
    private ResourceService resourceService;

    @MockitoBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockitoBean
    private ResourceConnector resourceConnector;

    @MockitoBean
    private StackUpdater stackUpdater;

    @MockitoBean
    private NodeConfig nodeConfig;

    @MockitoBean
    private MeterRegistry meterRegistry;

    @MockitoBean
    private OperationService operationService;

    @MockitoBean
    private FreeIpaLoadBalancerConfigurationService freeIpaLoadBalancerConfigurationService;

    @MockitoBean
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @MockitoBean
    private FreeIpaLoadBalancerCreationService freeIpaLoadBalancerCreationService;

    @MockitoBean
    private FreeIpaLoadBalancerMetadataCollectionService freeIpaLoadBalancerMetadataCollectionService;

    @MockitoBean
    private NodeValidator nodeValidator;

    @MockitoBean
    private FlowCancelService flowCancelService;

    @MockitoBean
    private FlowUsageSender flowUsageSender;

    @MockitoBean
    private StackStatusFinalizer stackStatusFinalizer;

    @MockitoBean
    private EventSenderService eventSenderService;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudStack cloudStack;

    private Stack stack;

    @BeforeEach
    public void setup() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setAccountId(ACCOUNT_ID);
        InstanceGroup ig = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        ig.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(ig));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stackToCloudStackConverter.convert(stack)).thenReturn(cloudStack);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any(), any())).thenReturn(ac);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testSuccessfulAwsFlowWithLbCreation() throws Exception {
        stack.setCloudPlatform("AWS");
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());

        LoadBalancer loadBalancer = new LoadBalancer();
        when(freeIpaLoadBalancerConfigurationService.createLoadBalancerConfiguration(eq(STACK_ID), any())).thenReturn(loadBalancer);
        when(resourceService.findAllByStackId(STACK_ID)).thenReturn(List.of());

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, CLUSTER_OPERATION,
                "Preparing FreeIPA upgrade: creating temporary load balancer");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, CLUSTER_OPERATION,
                "Preparing FreeIPA upgrade: provisioning temporary load balancer");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, CLUSTER_OPERATION,
                "Preparing FreeIPA upgrade: collecting load balancer metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, CLUSTER_OPERATION,
                "Preparing FreeIPA upgrade: removing temporary load balancer");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, AVAILABLE,
                "FreeIPA upgrade preparation completed");
        stackStatusVerify.verifyNoMoreInteractions();

        verify(freeIpaLoadBalancerConfigurationService).createLoadBalancerConfiguration(eq(STACK_ID), any());
        verify(freeIpaLoadBalancerService).save(loadBalancer);
        verify(freeIpaLoadBalancerCreationService).createLoadBalancer(any());
        verify(freeIpaLoadBalancerMetadataCollectionService).collectLoadBalancerMetadata(any());
        verify(freeIpaLoadBalancerService).delete(STACK_ID);
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), eq(List.of()), eq(List.of()));
    }

    @Test
    public void testNonAwsFlowSkipsLbCreation() {
        stack.setCloudPlatform("AZURE");

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, AVAILABLE,
                "FreeIPA upgrade preparation completed");
        stackStatusVerify.verifyNoMoreInteractions();

        verifyNoInteractions(freeIpaLoadBalancerConfigurationService);
        verify(freeIpaLoadBalancerService, never()).save(any());
        verifyNoInteractions(freeIpaLoadBalancerCreationService);
        verifyNoInteractions(freeIpaLoadBalancerMetadataCollectionService);
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), eq(List.of()), eq(List.of()));
    }

    @Test
    public void testMetadataCollectionFailsAndCleanupHappens() throws Exception {
        stack.setCloudPlatform("AWS");
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());

        LoadBalancer loadBalancer = new LoadBalancer();
        when(freeIpaLoadBalancerConfigurationService.createLoadBalancerConfiguration(eq(STACK_ID), any())).thenReturn(loadBalancer);

        doThrow(new CloudbreakServiceException("Metadata collection failed"))
                .when(freeIpaLoadBalancerMetadataCollectionService).collectLoadBalancerMetadata(any());

        Resource lbResource = createLbResource();
        when(resourceService.findAllByStackId(STACK_ID)).thenReturn(List.of(lbResource));

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, CLUSTER_OPERATION,
                "Preparing FreeIPA upgrade: creating temporary load balancer");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, CLUSTER_OPERATION,
                "Preparing FreeIPA upgrade: provisioning temporary load balancer");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, CLUSTER_OPERATION,
                "Preparing FreeIPA upgrade: collecting load balancer metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(eq(stack), eq(AVAILABLE), any(String.class));
        stackStatusVerify.verifyNoMoreInteractions();

        verify(freeIpaLoadBalancerCreationService).createLoadBalancer(any());
        verify(freeIpaLoadBalancerMetadataCollectionService).collectLoadBalancerMetadata(any());
        verify(resourceConnector).terminate(eq(ac), eq(cloudStack), any());
        verify(freeIpaLoadBalancerService).delete(STACK_ID);
        verify(resourceService).deleteByStackIdAndNameAndType(eq(STACK_ID), eq("elb-1"),
                eq(com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER));
        verify(operationService).failOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), any(String.class), eq(List.of()), any());
    }

    @Test
    public void testAwsWithExistingLbSkipsValidation() {
        stack.setCloudPlatform("AWS");
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(new LoadBalancer()));

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, AVAILABLE,
                "FreeIPA upgrade preparation completed");
        stackStatusVerify.verifyNoMoreInteractions();

        verifyNoInteractions(freeIpaLoadBalancerConfigurationService);
        verify(freeIpaLoadBalancerService, never()).save(any());
        verifyNoInteractions(freeIpaLoadBalancerCreationService);
        verifyNoInteractions(freeIpaLoadBalancerMetadataCollectionService);
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), eq(List.of()), eq(List.of()));
    }

    @Test
    public void testLbDeletionSuccessPerformsDbCleanup() throws Exception {
        stack.setCloudPlatform("AWS");
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());

        LoadBalancer loadBalancer = new LoadBalancer();
        when(freeIpaLoadBalancerConfigurationService.createLoadBalancerConfiguration(eq(STACK_ID), any())).thenReturn(loadBalancer);

        Resource lbResource = createLbResource();
        when(resourceService.findAllByStackId(STACK_ID)).thenReturn(List.of(lbResource));

        testFlow();

        verify(freeIpaLoadBalancerService).delete(STACK_ID);
        verify(resourceService).deleteByStackIdAndNameAndType(eq(STACK_ID), eq("elb-1"),
                eq(com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER));
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), eq(List.of()), eq(List.of()));
    }

    private void testFlow() {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);
        flowFinishedSuccessfully();
    }

    private void flowFinishedSuccessfully() {
        assertTrue(flowRegister.getRunningFlowIds().isEmpty(), "flow has not finished");
    }

    private FlowIdentifier triggerFlow() {
        PrepareUpgradeTriggerEvent triggerEvent = new PrepareUpgradeTriggerEvent(
                PrepareUpgradeEvent.PREPARE_UPGRADE_EVENT.event(), STACK_ID, OPERATION_ID);
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(triggerEvent.selector(), triggerEvent));
    }

    private Resource createLbResource() {
        Resource resource = new Resource();
        resource.setResourceType(com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER);
        resource.setResourceName("elb-1");
        resource.setResourceStatus(com.sequenceiq.common.api.type.CommonStatus.CREATED);
        resource.setAttributes(new Json("{}"));
        return resource;
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 30);
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import(value = {
            PrepareUpgradeActions.class,
            PrepareUpgradeLbProvisionHandler.class,
            PrepareUpgradeMetadataCollectionHandler.class,
            PrepareUpgradeLbDeletionHandler.class,
            PrepareUpgradeFailureCleanupHandler.class,
            PrepareUpgradeFlowConfig.class,
            FlowIntegrationTestConfig.class,
            ResourceToCloudResourceConverter.class,
            ResourceAttributeUtil.class,
            FreeIpaFailedFlowAnalyzer.class,
            FreeIpaValidationProperties.class
    })
    static class Config {

    }
}
