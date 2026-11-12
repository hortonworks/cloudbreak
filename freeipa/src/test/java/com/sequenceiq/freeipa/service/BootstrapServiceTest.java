package com.sequenceiq.freeipa.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.SaltMinimumAuthVersionCalculator;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.util.CompressUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackParameter;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.JumpgateTunnelStatusService;
import com.sequenceiq.freeipa.service.stack.StackParameterService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.util.SaltBootstrapVersionChecker;

@ExtendWith(MockitoExtension.class)
class BootstrapServiceTest {

    private static final long STACK_ID = 1L;

    private static final String INSTANCE_WITH_FQDN = "instanceWithFqdn";

    private static final String INSTANCE_WO_FQDN = "instanceWOFqdn";

    private static final String INSTANCE_WRONG_DOMAIN = "instanceWrongDomain";

    private static final String DOMAIN = "bootstrap.test";

    private static final String HOSTNAME = "ipaHostname";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private HostDiscoveryService hostDiscoveryService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private ImageService imageService;

    @Mock
    private CompressUtil compressUtil;

    @Mock
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Mock
    private JumpgateTunnelStatusService jumpgateTunnelStatusService;

    @Mock
    private StackParameterService stackParameterService;

    @Spy
    private SaltMinimumAuthVersionCalculator saltMinimumAuthVersionCalculator;

    @InjectMocks
    private BootstrapService underTest;

    @Test
    public void testBootstrapWithInstanceIds() throws CloudbreakOrchestratorException, IOException {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(
                createInstance(INSTANCE_WITH_FQDN, "instance1." + DOMAIN),
                createInstance(INSTANCE_WO_FQDN, null),
                createInstance(INSTANCE_WRONG_DOMAIN, "instance.wrong.domain"),
                createInstance("filterMe", "filtered" + DOMAIN)));
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("cloud");
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.of(stack));
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        freeIpa.setHostname(HOSTNAME);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        List<GatewayConfig> gatewayConfigs = List.of();
        when(gatewayConfigService.getGatewayConfigs(eq(stack), anySet())).thenReturn(gatewayConfigs);
        byte[] bytes = {};
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "freeipa-salt")).thenReturn(bytes);
        ImageEntity image = new ImageEntity();
        image.setOs("ZOS");
        when(imageService.getByStack(stack)).thenReturn(image);
        when(hostDiscoveryService.generateHostname(anyString(), any(), anyLong(), anyBoolean())).thenCallRealMethod();

        underTest.bootstrap(STACK_ID, List.of(INSTANCE_WITH_FQDN, INSTANCE_WO_FQDN, INSTANCE_WRONG_DOMAIN), false);

        ArgumentCaptor<Set<Node>> targetCaptor = ArgumentCaptor.forClass((Class) Set.class);
        ArgumentCaptor<Set<Node>> allCaptor = ArgumentCaptor.forClass((Class) Set.class);
        ArgumentCaptor<BootstrapParams> bootstrapParamsCaptor = ArgumentCaptor.forClass(BootstrapParams.class);
        ArgumentCaptor<ExitCriteriaModel> exitCriteriaModelCaptor = ArgumentCaptor.forClass(ExitCriteriaModel.class);
        verify(hostOrchestrator).bootstrapNewNodes(eq(gatewayConfigs), targetCaptor.capture(), allCaptor.capture(), eq(bytes), bootstrapParamsCaptor.capture(),
                exitCriteriaModelCaptor.capture(), eq(Boolean.FALSE));
        Set<Node> targetNodes = targetCaptor.getValue();
        Set<Node> allNodes = allCaptor.getValue();
        assertEquals(targetNodes, allNodes);
        assertEquals(3, allNodes.size());
        assertThat(allNodes, hasItem(
                allOf(hasProperty("instanceId", is(INSTANCE_WITH_FQDN)),
                        hasProperty("hostname", is("instance1")),
                        hasProperty("domain", is(DOMAIN)),
                        hasProperty("instanceType", is("GW")),
                        hasProperty("hostGroup", is("TADA"))
                )));
        assertThat(allNodes, hasItem(
                allOf(hasProperty("instanceId", is(INSTANCE_WO_FQDN)),
                        hasProperty("hostname", is(HOSTNAME + '1')),
                        hasProperty("domain", is(DOMAIN)),
                        hasProperty("instanceType", is("GW")),
                        hasProperty("hostGroup", is("TADA"))
                )));
        assertThat(allNodes, hasItem(
                allOf(hasProperty("instanceId", is(INSTANCE_WRONG_DOMAIN)),
                        hasProperty("hostname", is(HOSTNAME + '1')),
                        hasProperty("domain", is(DOMAIN)),
                        hasProperty("instanceType", is("GW")),
                        hasProperty("hostGroup", is("TADA"))
                )));
        BootstrapParams bootstrapParams = bootstrapParamsCaptor.getValue();
        assertTrue(bootstrapParams.isSaltBootstrapFpSupported());
        assertTrue(bootstrapParams.isRestartNeededFlagSupported());
        assertEquals(image.getOs(), bootstrapParams.getOs());
        assertEquals(stack.getCloudPlatform(), bootstrapParams.getCloud());
        StackBasedExitCriteriaModel exitCriteriaModel = (StackBasedExitCriteriaModel) exitCriteriaModelCaptor.getValue();
        assertEquals(STACK_ID, exitCriteriaModel.getStackId().get());
    }

    @Test
    public void testBootstrapWithoutInstanceIds() throws CloudbreakOrchestratorException, IOException {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(
                createInstance(INSTANCE_WITH_FQDN, "instance1." + DOMAIN),
                createInstance(INSTANCE_WO_FQDN, null),
                createInstance(INSTANCE_WRONG_DOMAIN, "instance.wrong.domain")));
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("cloud");
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.of(stack));
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        freeIpa.setHostname(HOSTNAME);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        List<GatewayConfig> gatewayConfigs = List.of();
        when(gatewayConfigService.getGatewayConfigs(eq(stack), anySet())).thenReturn(gatewayConfigs);
        byte[] bytes = {};
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "freeipa-salt")).thenReturn(bytes);
        ImageEntity image = new ImageEntity();
        image.setOs("ZOS");
        when(imageService.getByStack(stack)).thenReturn(image);
        when(hostDiscoveryService.generateHostname(anyString(), any(), anyLong(), anyBoolean())).thenCallRealMethod();

        underTest.bootstrap(STACK_ID);

        ArgumentCaptor<Set<Node>> targetCaptor = ArgumentCaptor.forClass((Class) Set.class);
        ArgumentCaptor<Set<Node>> allCaptor = ArgumentCaptor.forClass((Class) Set.class);
        ArgumentCaptor<BootstrapParams> bootstrapParamsCaptor = ArgumentCaptor.forClass(BootstrapParams.class);
        ArgumentCaptor<ExitCriteriaModel> exitCriteriaModelCaptor = ArgumentCaptor.forClass(ExitCriteriaModel.class);
        verify(hostOrchestrator).bootstrapNewNodes(eq(gatewayConfigs), targetCaptor.capture(), allCaptor.capture(), eq(bytes), bootstrapParamsCaptor.capture(),
                exitCriteriaModelCaptor.capture(), eq(Boolean.FALSE));
        Set<Node> targetNodes = targetCaptor.getValue();
        Set<Node> allNodes = allCaptor.getValue();
        assertEquals(targetNodes, allNodes);
        assertEquals(3, allNodes.size());
        assertThat(allNodes, hasItem(
                allOf(hasProperty("instanceId", is(INSTANCE_WITH_FQDN)),
                hasProperty("hostname", is("instance1")),
                hasProperty("domain", is(DOMAIN)),
                hasProperty("instanceType", is("GW")),
                hasProperty("hostGroup", is("TADA"))
                        )));
        assertThat(allNodes, hasItem(
                allOf(hasProperty("instanceId", is(INSTANCE_WO_FQDN)),
                hasProperty("hostname", is(HOSTNAME + '1')),
                hasProperty("domain", is(DOMAIN)),
                hasProperty("instanceType", is("GW")),
                hasProperty("hostGroup", is("TADA"))
                        )));
        assertThat(allNodes, hasItem(
                allOf(hasProperty("instanceId", is(INSTANCE_WRONG_DOMAIN)),
                hasProperty("hostname", is(HOSTNAME + '1')),
                hasProperty("domain", is(DOMAIN)),
                hasProperty("instanceType", is("GW")),
                hasProperty("hostGroup", is("TADA"))
                        )));
        BootstrapParams bootstrapParams = bootstrapParamsCaptor.getValue();
        assertTrue(bootstrapParams.isSaltBootstrapFpSupported());
        assertTrue(bootstrapParams.isRestartNeededFlagSupported());
        assertEquals(image.getOs(), bootstrapParams.getOs());
        assertEquals(stack.getCloudPlatform(), bootstrapParams.getCloud());
        StackBasedExitCriteriaModel exitCriteriaModel = (StackBasedExitCriteriaModel) exitCriteriaModelCaptor.getValue();
        assertEquals(STACK_ID, exitCriteriaModel.getStackId().get());
        InOrder inOrder = inOrder(jumpgateTunnelStatusService, hostOrchestrator);
        inOrder.verify(jumpgateTunnelStatusService).logTunnelStatus(eq(stack), anySet());
        inOrder.verify(hostOrchestrator).bootstrapNewNodes(any(), anySet(), anySet(), any(), any(), any(), anyBoolean());
    }

    @Test
    public void testBootstrapWithFqdnAsHostname() throws CloudbreakOrchestratorException, IOException {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(
                createInstance(INSTANCE_WITH_FQDN, "instance1." + DOMAIN),
                createInstance(INSTANCE_WO_FQDN, null),
                createInstance(INSTANCE_WRONG_DOMAIN, "instance.wrong.domain")));
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("cloud");
        when(saltBootstrapVersionChecker.isFqdnAsHostnameSupported(stack)).thenReturn(true);
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.of(stack));
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        freeIpa.setHostname(HOSTNAME);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        List<GatewayConfig> gatewayConfigs = List.of();
        when(gatewayConfigService.getGatewayConfigs(eq(stack), anySet())).thenReturn(gatewayConfigs);
        byte[] bytes = {};
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "freeipa-salt")).thenReturn(bytes);
        ImageEntity image = new ImageEntity();
        image.setOs("ZOS");
        when(imageService.getByStack(stack)).thenReturn(image);
        when(hostDiscoveryService.generateHostname(anyString(), any(), anyLong(), anyBoolean())).thenCallRealMethod();

        underTest.bootstrap(STACK_ID);

        ArgumentCaptor<Set<Node>> targetCaptor = ArgumentCaptor.forClass((Class) Set.class);
        ArgumentCaptor<Set<Node>> allCaptor = ArgumentCaptor.forClass((Class) Set.class);
        ArgumentCaptor<BootstrapParams> bootstrapParamsCaptor = ArgumentCaptor.forClass(BootstrapParams.class);
        ArgumentCaptor<ExitCriteriaModel> exitCriteriaModelCaptor = ArgumentCaptor.forClass(ExitCriteriaModel.class);
        verify(hostOrchestrator).bootstrapNewNodes(eq(gatewayConfigs), targetCaptor.capture(), allCaptor.capture(), eq(bytes), bootstrapParamsCaptor.capture(),
                exitCriteriaModelCaptor.capture(), eq(Boolean.FALSE));
        Set<Node> targetNodes = targetCaptor.getValue();
        Set<Node> allNodes = allCaptor.getValue();
        assertEquals(targetNodes, allNodes);
        assertEquals(3, allNodes.size());
        assertThat(allNodes, hasItem(
                allOf(hasProperty("instanceId", is(INSTANCE_WITH_FQDN)),
                hasProperty("hostname", is("instance1." + DOMAIN)),
                hasProperty("domain", is(DOMAIN)),
                hasProperty("instanceType", is("GW")),
                hasProperty("hostGroup", is("TADA"))
                        )));
        assertThat(allNodes, hasItem(
                allOf(hasProperty("instanceId", is(INSTANCE_WO_FQDN)),
                hasProperty("hostname", is(HOSTNAME + "1." + DOMAIN)),
                hasProperty("domain", is(DOMAIN)),
                hasProperty("instanceType", is("GW")),
                hasProperty("hostGroup", is("TADA"))
                        )));
        assertThat(allNodes, hasItem(
                allOf(hasProperty("instanceId", is(INSTANCE_WRONG_DOMAIN)),
                hasProperty("hostname", is(HOSTNAME + "1." + DOMAIN)),
                hasProperty("domain", is(DOMAIN)),
                hasProperty("instanceType", is("GW")),
                hasProperty("hostGroup", is("TADA"))
                        )));
        BootstrapParams bootstrapParams = bootstrapParamsCaptor.getValue();
        assertTrue(bootstrapParams.isSaltBootstrapFpSupported());
        assertTrue(bootstrapParams.isRestartNeededFlagSupported());
        assertEquals(image.getOs(), bootstrapParams.getOs());
        assertEquals(stack.getCloudPlatform(), bootstrapParams.getCloud());
        StackBasedExitCriteriaModel exitCriteriaModel = (StackBasedExitCriteriaModel) exitCriteriaModelCaptor.getValue();
        assertEquals(STACK_ID, exitCriteriaModel.getStackId().get());
    }

    @Test
    public void testReBootstrap() throws Exception {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("cloud");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(new Template());
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setPrivateIp("8.8.8.8");
        instanceMetaData.setDiscoveryFQDN("node." + DOMAIN);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        freeIpa.setHostname(HOSTNAME);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        List<GatewayConfig> gatewayConfigs = List.of();
        when(gatewayConfigService.getGatewayConfigs(eq(stack), anySet())).thenReturn(gatewayConfigs);
        ImageEntity image = new ImageEntity();
        image.setOs("ZOS");
        when(imageService.getByStack(stack)).thenReturn(image);

        underTest.reBootstrap(stack);

        ArgumentCaptor<Set<Node>> targetCaptor = ArgumentCaptor.forClass((Class) Set.class);
        ArgumentCaptor<BootstrapParams> bootstrapParamsCaptor = ArgumentCaptor.forClass(BootstrapParams.class);
        ArgumentCaptor<ExitCriteriaModel> exitCriteriaModelCaptor = ArgumentCaptor.forClass(ExitCriteriaModel.class);
        verify(hostOrchestrator).reBootstrapExistingNodes(eq(gatewayConfigs), targetCaptor.capture(), bootstrapParamsCaptor.capture(),
                exitCriteriaModelCaptor.capture());
        Set<Node> targetNodes = targetCaptor.getValue();
        assertEquals(1, targetNodes.size());
        assertTrue(targetNodes.stream().allMatch(node -> node.getPrivateIp().equals(instanceMetaData.getPrivateIp())));
        BootstrapParams bootstrapParams = bootstrapParamsCaptor.getValue();
        assertTrue(bootstrapParams.isSaltBootstrapFpSupported());
        assertTrue(bootstrapParams.isRestartNeededFlagSupported());
        assertFalse(bootstrapParams.isRestartNeeded());
        assertEquals(image.getOs(), bootstrapParams.getOs());
        assertEquals(stack.getCloudPlatform(), bootstrapParams.getCloud());
        StackBasedExitCriteriaModel exitCriteriaModel = (StackBasedExitCriteriaModel) exitCriteriaModelCaptor.getValue();
        assertEquals(STACK_ID, exitCriteriaModel.getStackId().get());
    }

    @Test
    public void testReBootstrapWithInstances() throws Exception {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("cloud");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(new Template());
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setPrivateIp("8.8.8.8");
        instanceMetaData.setDiscoveryFQDN("node." + DOMAIN);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        freeIpa.setHostname(HOSTNAME);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        List<GatewayConfig> gatewayConfigs = List.of();
        when(gatewayConfigService.getGatewayConfigs(eq(stack), anySet())).thenReturn(gatewayConfigs);
        ImageEntity image = new ImageEntity();
        image.setOs("ZOS");
        when(imageService.getByStack(stack)).thenReturn(image);

        underTest.reBootstrap(stack, Set.of(instanceMetaData));

        ArgumentCaptor<Set<Node>> targetCaptor = ArgumentCaptor.forClass((Class) Set.class);
        ArgumentCaptor<BootstrapParams> bootstrapParamsCaptor = ArgumentCaptor.forClass(BootstrapParams.class);
        ArgumentCaptor<ExitCriteriaModel> exitCriteriaModelCaptor = ArgumentCaptor.forClass(ExitCriteriaModel.class);
        verify(hostOrchestrator).reBootstrapExistingNodes(eq(gatewayConfigs), targetCaptor.capture(), bootstrapParamsCaptor.capture(),
                exitCriteriaModelCaptor.capture());
        Set<Node> targetNodes = targetCaptor.getValue();
        assertEquals(1, targetNodes.size());
        assertTrue(targetNodes.stream().allMatch(node -> node.getPrivateIp().equals(instanceMetaData.getPrivateIp())));
        BootstrapParams bootstrapParams = bootstrapParamsCaptor.getValue();
        assertTrue(bootstrapParams.isSaltBootstrapFpSupported());
        assertTrue(bootstrapParams.isRestartNeededFlagSupported());
        assertTrue(bootstrapParams.isRestartNeeded());
        assertEquals(image.getOs(), bootstrapParams.getOs());
        assertEquals(stack.getCloudPlatform(), bootstrapParams.getCloud());
        StackBasedExitCriteriaModel exitCriteriaModel = (StackBasedExitCriteriaModel) exitCriteriaModelCaptor.getValue();
        assertEquals(STACK_ID, exitCriteriaModel.getStackId().get());
        InOrder inOrder = inOrder(jumpgateTunnelStatusService, hostOrchestrator);
        inOrder.verify(jumpgateTunnelStatusService).logTunnelStatus(stack, Set.of(instanceMetaData));
        inOrder.verify(hostOrchestrator).reBootstrapExistingNodes(any(), anySet(), any(), any());
    }

    @Test
    public void testIOExceptionConverted() throws CloudbreakOrchestratorException, IOException {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(
                createInstance(INSTANCE_WITH_FQDN, "instance1" + DOMAIN),
                createInstance(INSTANCE_WO_FQDN, null),
                createInstance(INSTANCE_WRONG_DOMAIN, "instance.wrong.domain")));
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("cloud");
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.of(stack));
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        freeIpa.setHostname(HOSTNAME);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        List<GatewayConfig> gatewayConfigs = List.of();
        when(gatewayConfigService.getGatewayConfigs(eq(stack), anySet())).thenReturn(gatewayConfigs);
        byte[] bytes = {};
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "freeipa-salt")).thenReturn(bytes);
        ImageEntity image = new ImageEntity();
        image.setOs("ZOS");
        when(imageService.getByStack(stack)).thenReturn(image);
        when(hostDiscoveryService.generateHostname(anyString(), any(), anyLong(), anyBoolean())).thenCallRealMethod();
        doThrow(new CloudbreakOrchestratorCancelledException("wow"))
                .when(hostOrchestrator).bootstrapNewNodes(any(), anySet(), anySet(), any(), any(), any(), anyBoolean());

        assertThrows(CloudbreakOrchestratorException.class, () -> underTest.bootstrap(STACK_ID));
    }

    @Test
    public void testCbOrchestratorExRethrown() throws CloudbreakOrchestratorException, IOException {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(
                createInstance(INSTANCE_WITH_FQDN, "instance1" + DOMAIN),
                createInstance(INSTANCE_WO_FQDN, null),
                createInstance(INSTANCE_WRONG_DOMAIN, "instance.wrong.domain")));
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("cloud");
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.of(stack));
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        freeIpa.setHostname(HOSTNAME);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        List<GatewayConfig> gatewayConfigs = List.of();
        when(gatewayConfigService.getGatewayConfigs(eq(stack), anySet())).thenReturn(gatewayConfigs);
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "freeipa-salt")).thenThrow(new IOException("wow"));
        ImageEntity image = new ImageEntity();
        image.setOs("ZOS");
        when(imageService.getByStack(stack)).thenReturn(image);
        when(hostDiscoveryService.generateHostname(anyString(), any(), anyLong(), anyBoolean())).thenCallRealMethod();

        assertThrows(CloudbreakOrchestratorException.class, () -> underTest.bootstrap(STACK_ID));
    }

    static Stream<Arguments> saltVersionsOfFreeIpa() {
        return Stream.of(
                Arguments.of("3006.25", List.of("3006.25", "3006.10"), 2),
                Arguments.of("3006.25", List.of("3006.25", "3006.25"), 3),
                Arguments.of("3006.25", List.of("3006.25", ""), 0)
        );
    }

    @ParameterizedTest
    @MethodSource("saltVersionsOfFreeIpa")
    void bootstrapRestartsEverySaltMasterWithTheMinimumAuthVersionOfTheOldestInstance(String imageSaltVersion, List<String> instanceSaltVersions,
            int expectedMinimumAuthVersion) throws Exception {
        BootstrapParams bootstrapParams = bootstrapFreeIpaWithSaltVersions(imageSaltVersion, instanceSaltVersions);

        assertEquals(expectedMinimumAuthVersion, bootstrapParams.getSaltMasterMinimumAuthVersion());
        assertTrue(bootstrapParams.isSaltMasterRestartNeeded());
    }

    @Test
    void firstBootstrapSavesTheValueWithoutStoppingNewMasters() throws Exception {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(replicas("replica1"));
        List<GatewayConfig> gatewayConfigs = freeIpaWithSaltVersions("3006.25", List.of("3006.25"));
        Stack stack = stackRepository.findOneWithLists(STACK_ID).orElseThrow();
        stack.setStackStatus(new StackStatus(stack, "Bootstrapping machines", DetailedStackStatus.BOOTSTRAPPING_MACHINES));

        underTest.bootstrap(STACK_ID);

        BootstrapParams params = capturedBootstrapParams(gatewayConfigs);
        assertEquals(3, params.getSaltMasterMinimumAuthVersion());
        assertFalse(params.isSaltMasterRestartNeeded());
        InOrder order = inOrder(stackParameterService, hostOrchestrator);
        order.verify(stackParameterService).setStackParameter(STACK_ID, "SALT_MASTER_MINIMUM_AUTH_VERSION", "unknown");
        order.verify(hostOrchestrator).bootstrapNewNodes(any(), anySet(), anySet(), any(), any(), any(), anyBoolean());
        order.verify(stackParameterService).setStackParameter(STACK_ID, "SALT_MASTER_MINIMUM_AUTH_VERSION", "3");
    }

    @Test
    void failedFirstBootstrapRestartsTheMastersOnRetry() throws Exception {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(replicas("replica1"));
        List<GatewayConfig> gatewayConfigs = freeIpaWithSaltVersions("3006.25", List.of("3006.25"));
        Stack stack = stackRepository.findOneWithLists(STACK_ID).orElseThrow();
        stack.setStackStatus(new StackStatus(stack, "Bootstrapping machines", DetailedStackStatus.BOOTSTRAPPING_MACHINES));
        appliedMinimumAuthVersion("unknown");

        underTest.bootstrap(STACK_ID);

        assertTrue(capturedBootstrapParams(gatewayConfigs).isSaltMasterRestartNeeded());
    }

    @Test
    void rebuiltStackWithUnknownAppliedValueStillRestartsTheMasters() throws Exception {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(replicas("replica1"));
        List<GatewayConfig> gatewayConfigs = freeIpaWithSaltVersions("3006.25", List.of("3006.25"));
        Stack stack = stackRepository.findOneWithLists(STACK_ID).orElseThrow();
        stack.setStackStatus(new StackStatus(stack, "Bootstrapping machines", DetailedStackStatus.REBUILD_IN_PROGRESS));

        underTest.bootstrap(STACK_ID);

        assertTrue(capturedBootstrapParams(gatewayConfigs).isSaltMasterRestartNeeded());
    }

    @Test
    void unchangedAppliedValueNeedsNoMasterRestartAndNoWrite() throws Exception {
        appliedMinimumAuthVersion("2");

        BootstrapParams params = bootstrapFreeIpaWithSaltVersions("3006.25", List.of("3006.25", "3006.10"));

        assertEquals(2, params.getSaltMasterMinimumAuthVersion());
        assertFalse(params.isSaltMasterRestartNeeded());
        verify(stackParameterService, never()).setStackParameter(anyLong(), anyString(), anyString());
    }

    @Test
    void failedBootstrapKeepsTheValueUnknownAndRestartsAgainOnRetry() throws Exception {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(replicas("replica1", "replica2"));
        List<GatewayConfig> gatewayConfigs = freeIpaWithSaltVersions("3006.25", List.of("3006.25", "3006.25"));
        StackParameter parameter = appliedMinimumAuthVersion("2");
        doAnswer(invocation -> {
            parameter.setParamValue(invocation.getArgument(2));
            return null;
        }).when(stackParameterService).setStackParameter(eq(STACK_ID), eq("SALT_MASTER_MINIMUM_AUTH_VERSION"), anyString());
        doThrow(new CloudbreakOrchestratorCancelledException("failed"))
                .doNothing().when(hostOrchestrator).bootstrapNewNodes(any(), anySet(), anySet(), any(), any(), any(), anyBoolean());

        assertThrows(CloudbreakOrchestratorException.class, () -> underTest.bootstrap(STACK_ID));
        assertEquals("unknown", parameter.getParamValue());

        underTest.bootstrap(STACK_ID);

        ArgumentCaptor<BootstrapParams> params = ArgumentCaptor.forClass(BootstrapParams.class);
        verify(hostOrchestrator, times(2)).bootstrapNewNodes(eq(gatewayConfigs), anySet(), anySet(), any(), params.capture(), any(),
                eq(false));
        assertTrue(params.getAllValues().stream().allMatch(BootstrapParams::isSaltMasterRestartNeeded));
        assertEquals("3", parameter.getParamValue());
    }

    @Test
    void bootstrapLeavesSaltMasterConfigAloneWhenOnlyTheStackImageHasNewSalt() throws Exception {
        BootstrapParams params = bootstrapFreeIpaWithSaltVersions("3006.25", List.of("3006.10", "3006.10"));

        assertNull(params.getSaltMasterMinimumAuthVersion());
        assertFalse(params.isSaltMasterRestartNeeded());
        verify(stackParameterService, never()).setStackParameter(anyLong(), anyString(), anyString());
    }

    @Test
    void bootstrapLeavesSaltMasterConfigAloneWithoutNewSalt() throws Exception {
        BootstrapParams bootstrapParams = bootstrapFreeIpaWithSaltVersions("3006.10", List.of("3006.10", "3006.10"));

        assertNull(bootstrapParams.getSaltMasterMinimumAuthVersion());
        assertFalse(bootstrapParams.isSaltMasterRestartNeeded());
    }

    @Test
    void reBootstrapLeavesSaltMasterConfigAlone() throws Exception {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setInstanceGroups(Set.of());
        when(freeIpaService.findByStack(stack)).thenReturn(new FreeIpa());
        List<GatewayConfig> gatewayConfigs = gatewayConfigsWithSaltVersions(List.of("3006.25", "3006.10"));
        when(gatewayConfigService.getGatewayConfigs(eq(stack), anySet())).thenReturn(gatewayConfigs);
        ImageEntity image = new ImageEntity();
        image.setSaltVersion("3006.25");
        when(imageService.getByStack(stack)).thenReturn(image);

        underTest.reBootstrap(stack);

        ArgumentCaptor<BootstrapParams> bootstrapParamsCaptor = ArgumentCaptor.forClass(BootstrapParams.class);
        verify(hostOrchestrator).reBootstrapExistingNodes(eq(gatewayConfigs), anySet(), bootstrapParamsCaptor.capture(), any());
        assertNull(bootstrapParamsCaptor.getValue().getSaltMasterMinimumAuthVersion());
        assertFalse(bootstrapParamsCaptor.getValue().isSaltMasterRestartNeeded());
    }

    @Test
    void bootstrapOfSomeInstancesLeavesSaltMasterConfigAlone() throws Exception {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID))
                .thenReturn(Set.of(createInstance(INSTANCE_WITH_FQDN, "instance1." + DOMAIN), createInstance(INSTANCE_WO_FQDN, "instance2." + DOMAIN)));
        List<GatewayConfig> gatewayConfigs = freeIpaWithSaltVersions("3006.25", List.of("3006.25"));

        underTest.bootstrap(STACK_ID, List.of(INSTANCE_WITH_FQDN), false);

        BootstrapParams bootstrapParams = capturedBootstrapParams(gatewayConfigs);
        assertNull(bootstrapParams.getSaltMasterMinimumAuthVersion());
        assertFalse(bootstrapParams.isSaltMasterRestartNeeded());
    }

    @Test
    void bootstrapOfEveryReplicaByIdRestartsEverySaltMasterWithTheMinimumAuthVersionOfTheOldestReplica() throws Exception {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(replicas("replica1", "replica2", "replica3"));
        List<GatewayConfig> gatewayConfigs = freeIpaWithSaltVersions("3006.25", List.of("3006.25", "3006.10", "3006.10"));

        underTest.bootstrap(STACK_ID, List.of("replica1", "replica2", "replica3"), false);

        BootstrapParams bootstrapParams = capturedBootstrapParams(gatewayConfigs);
        assertEquals(2, bootstrapParams.getSaltMasterMinimumAuthVersion());
        assertTrue(bootstrapParams.isSaltMasterRestartNeeded());
    }

    @Test
    void replicaAddedDuringTheBootstrapLeavesSaltMasterConfigAlone() throws Exception {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID))
                .thenReturn(replicas("replica1", "replica2"), replicas("replica1", "replica2", "replica3"));
        List<GatewayConfig> gatewayConfigs = freeIpaWithSaltVersions("3006.25", List.of("3006.25", "3006.25"));

        underTest.bootstrap(STACK_ID);

        BootstrapParams bootstrapParams = capturedBootstrapParams(gatewayConfigs);
        assertNull(bootstrapParams.getSaltMasterMinimumAuthVersion());
        assertFalse(bootstrapParams.isSaltMasterRestartNeeded());
    }

    @Test
    void replicaAddedAfterParamsWereConfiguredPreventsSavingTheAppliedValue() throws Exception {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID))
                .thenReturn(replicas("replica1", "replica2"), replicas("replica1", "replica2"), replicas("replica1", "replica2", "replica3"));
        List<GatewayConfig> gatewayConfigs = freeIpaWithSaltVersions("3006.25", List.of("3006.25", "3006.25"));

        underTest.bootstrap(STACK_ID);

        assertEquals(3, capturedBootstrapParams(gatewayConfigs).getSaltMasterMinimumAuthVersion());
        verify(stackParameterService).setStackParameter(STACK_ID, "SALT_MASTER_MINIMUM_AUTH_VERSION", "unknown");
        verify(stackParameterService, never()).setStackParameter(STACK_ID, "SALT_MASTER_MINIMUM_AUTH_VERSION", "3");
    }

    private StackParameter appliedMinimumAuthVersion(String value) {
        StackParameter parameter = new StackParameter();
        parameter.setParamValue(value);
        when(stackParameterService.findStackParameterByStackIdAndParamKey(STACK_ID, "SALT_MASTER_MINIMUM_AUTH_VERSION"))
                .thenReturn(Optional.of(parameter));
        return parameter;
    }

    private BootstrapParams bootstrapFreeIpaWithSaltVersions(String imageSaltVersion, List<String> instanceSaltVersions) throws Exception {
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(createInstance(INSTANCE_WITH_FQDN, "instance1." + DOMAIN)));
        List<GatewayConfig> gatewayConfigs = freeIpaWithSaltVersions(imageSaltVersion, instanceSaltVersions);

        underTest.bootstrap(STACK_ID);

        return capturedBootstrapParams(gatewayConfigs);
    }

    private List<GatewayConfig> freeIpaWithSaltVersions(String imageSaltVersion, List<String> instanceSaltVersions) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.of(stack));
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        List<GatewayConfig> gatewayConfigs = gatewayConfigsWithSaltVersions(instanceSaltVersions);
        when(gatewayConfigService.getGatewayConfigs(eq(stack), anySet())).thenReturn(gatewayConfigs);
        ImageEntity image = new ImageEntity();
        image.setSaltVersion(imageSaltVersion);
        when(imageService.getByStack(stack)).thenReturn(image);
        return gatewayConfigs;
    }

    private BootstrapParams capturedBootstrapParams(List<GatewayConfig> gatewayConfigs) throws Exception {
        ArgumentCaptor<BootstrapParams> bootstrapParamsCaptor = ArgumentCaptor.forClass(BootstrapParams.class);
        verify(hostOrchestrator).bootstrapNewNodes(eq(gatewayConfigs), anySet(), anySet(), any(), bootstrapParamsCaptor.capture(), any(), eq(false));
        return bootstrapParamsCaptor.getValue();
    }

    private List<GatewayConfig> gatewayConfigsWithSaltVersions(List<String> saltVersions) {
        return saltVersions.stream()
                .map(saltVersion -> GatewayConfig.builder().withSaltVersion(saltVersion).build())
                .toList();
    }

    private Set<InstanceMetaData> replicas(String... instanceIds) {
        return Stream.of(instanceIds)
                .map(instanceId -> createInstance(instanceId, instanceId + "." + DOMAIN))
                .collect(Collectors.toSet());
    }

    private InstanceMetaData createInstance(String instanceId, String fqdn) {
        InstanceMetaData im = new InstanceMetaData();
        im.setInstanceId(instanceId);
        im.setDiscoveryFQDN(fqdn);
        im.setPrivateId(1L);
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        template.setInstanceType("GW");
        instanceGroup.setTemplate(template);
        instanceGroup.setGroupName("TADA");
        im.setInstanceGroup(instanceGroup);
        return im;
    }
}