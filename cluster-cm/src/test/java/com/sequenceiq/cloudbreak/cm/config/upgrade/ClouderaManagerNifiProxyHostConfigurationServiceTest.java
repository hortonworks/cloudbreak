package com.sequenceiq.cloudbreak.cm.config.upgrade;

import static com.sequenceiq.cloudbreak.cm.config.upgrade.ClouderaManagerNifiProxyHostConfigurationService.NIFI_PROPERTIES_SAFETY_VALVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupList;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.LoadBalancerUserFacingFqdnProvider;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerConfigService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerNifiProxyHostConfigurationServiceTest {

    private static final String STACK_NAME = "tb-cfm";

    private static final String BLUEPRINT = "{}";

    private static final String NIFI_SERVICE = "nifi-NIFI-BASE";

    private static final String COMPUTED_HOSTS = "gw0.site,gw1.site,tb-cfm.site,LB.elb.com,lb.elb.com";

    private static final String EXPECTED_PROXY_HOST_PROPERTY = proxyHostProperty(COMPUTED_HOSTS);

    @Mock
    private CmTemplateService cmTemplateService;

    @Mock
    private ClouderaManagerConfigService clouderaManagerConfigService;

    @Mock
    private LoadBalancerUserFacingFqdnProvider loadBalancerUserFacingFqdnProvider;

    @Mock
    private ApiClient apiClient;

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private ClusterView cluster;

    @Mock
    private InstanceMetadataView primaryGateway;

    @Mock
    private InstanceMetadataView otherGateway;

    @InjectMocks
    private ClouderaManagerNifiProxyHostConfigurationService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(stack.getName()).thenReturn(STACK_NAME);
        lenient().when(stack.getId()).thenReturn(1L);
        lenient().when(stack.getBlueprintJsonText()).thenReturn(BLUEPRINT);
        lenient().when(stack.hasGateway()).thenReturn(true);
        lenient().when(cmTemplateService.isServiceTypePresent("NIFI", BLUEPRINT)).thenReturn(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.12.0.10000", "4.12.0.10000-1", "4.12.0.10000-238", "4.12.0.20000-231", "4.13.0.0-1"})
    void testPatchNeededForTargetCfm(String cfmVersion) {
        assertTrue(underTest.isPatchNeeded(stack, products(cfmVersion)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.11.0.0-100", "4.12.0.0-914", "4.12.0.401-1", "4.12.0.9999-999"})
    void testPatchNotNeededForOlderTargetCfm(String cfmVersion) {
        assertFalse(underTest.isPatchNeeded(stack, products(cfmVersion)));
    }

    @Test
    void testPatchNotNeededWithoutCfmCandidate() {
        assertFalse(underTest.isPatchNeeded(stack, Set.of(new ClouderaManagerProduct().withName("CDH").withVersion("7.3.2-1.cdh7.3.2.p20000.83281140"))));
    }

    @Test
    void testPatchNotNeededWithoutGateway() {
        when(stack.hasGateway()).thenReturn(false);

        assertFalse(underTest.isPatchNeeded(stack, products("4.12.0.20000-231")));
    }

    @Test
    void testPatchNotNeededWithoutNifi() {
        when(cmTemplateService.isServiceTypePresent("NIFI", BLUEPRINT)).thenReturn(false);

        assertFalse(underTest.isPatchNeeded(stack, products("4.12.0.20000-231")));
    }

    @Test
    void testPatchWritesEveryNifiNodeRoleConfigGroupAndKeepsOtherSafetyValveEntries() {
        mockHosts();
        String customEntry = "<property><name>nifi.custom</name><value>x</value></property>";
        String oldProxyHost = "<property><name>nifi.web.proxy.host</name><value>old.host</value></property>";
        when(clouderaManagerConfigService.readRoleConfigGroupConfigs(apiClient, STACK_NAME, NIFI_SERVICE)).thenReturn(new ApiRoleConfigGroupList()
                .addItemsItem(roleConfigGroup("nifi-NIFI_NODE-nifi", "NIFI_NODE", null))
                .addItemsItem(roleConfigGroup("nifi-NIFI_NODE-nifi_scaling", "NIFI_NODE", customEntry + oldProxyHost))
                .addItemsItem(roleConfigGroup("nifi-NIFI-BASE-GATEWAY-BASE", "GATEWAY", null)));

        underTest.patchServiceConfig(apiClient, stack, products("4.12.0.20000-231"));

        verify(clouderaManagerConfigService).modifyRoleConfigGroup(apiClient, STACK_NAME, NIFI_SERVICE, "nifi-NIFI_NODE-nifi",
                Map.of(NIFI_PROPERTIES_SAFETY_VALVE, EXPECTED_PROXY_HOST_PROPERTY));
        verify(clouderaManagerConfigService).modifyRoleConfigGroup(apiClient, STACK_NAME, NIFI_SERVICE, "nifi-NIFI_NODE-nifi_scaling",
                Map.of(NIFI_PROPERTIES_SAFETY_VALVE, customEntry + proxyHostProperty(COMPUTED_HOSTS + ",old.host")));
        verify(clouderaManagerConfigService, never()).modifyRoleConfigGroup(any(), anyString(), anyString(), eq("nifi-NIFI-BASE-GATEWAY-BASE"), any());
    }

    @Test
    void testPatchMergesCustomHostsOfSafetyValveAndRoleConfigWithoutDuplicatesAndPlaceholders() {
        mockHosts();
        ApiRoleConfigGroup roleConfigGroup = roleConfigGroup("nifi-NIFI_NODE-nifi", "NIFI_NODE",
                proxyHostProperty(" custom.proxy , gw0.site,,${KNOX_HOST}"));
        roleConfigGroup.getConfig().addItemsItem(new ApiConfig().name("nifi.web.proxy.host").value("${KNOX_HOST},param.proxy:8443,LB.elb.com"));
        when(clouderaManagerConfigService.readRoleConfigGroupConfigs(apiClient, STACK_NAME, NIFI_SERVICE))
                .thenReturn(new ApiRoleConfigGroupList().addItemsItem(roleConfigGroup));

        underTest.patchServiceConfig(apiClient, stack, products("4.12.0.20000-231"));

        verify(clouderaManagerConfigService).modifyRoleConfigGroup(apiClient, STACK_NAME, NIFI_SERVICE, "nifi-NIFI_NODE-nifi",
                Map.of(NIFI_PROPERTIES_SAFETY_VALVE, proxyHostProperty(COMPUTED_HOSTS + ",custom.proxy,param.proxy:8443")));
    }

    @Test
    void testPatchSkipsRoleConfigGroupWhenRoleConfigAlreadyContainsEveryHost() {
        mockHosts();
        ApiRoleConfigGroup roleConfigGroup = roleConfigGroup("nifi-NIFI_NODE-nifi", "NIFI_NODE",
                "<property><name>nifi.custom</name><value>x</value></property>");
        roleConfigGroup.getConfig().addItemsItem(new ApiConfig().name("nifi.web.proxy.host").value(COMPUTED_HOSTS + ", custom.proxy"));
        when(clouderaManagerConfigService.readRoleConfigGroupConfigs(apiClient, STACK_NAME, NIFI_SERVICE))
                .thenReturn(new ApiRoleConfigGroupList().addItemsItem(roleConfigGroup));

        underTest.patchServiceConfig(apiClient, stack, products("4.12.0.20000-231"));

        verify(clouderaManagerConfigService, never()).modifyRoleConfigGroup(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testPatchFixesSafetyValveWhenItOverridesCompleteRoleConfig() {
        mockHosts();
        ApiRoleConfigGroup roleConfigGroup = roleConfigGroup("nifi-NIFI_NODE-nifi", "NIFI_NODE", proxyHostProperty("old.host"));
        roleConfigGroup.getConfig().addItemsItem(new ApiConfig().name("nifi.web.proxy.host").value(COMPUTED_HOSTS));
        when(clouderaManagerConfigService.readRoleConfigGroupConfigs(apiClient, STACK_NAME, NIFI_SERVICE))
                .thenReturn(new ApiRoleConfigGroupList().addItemsItem(roleConfigGroup));

        underTest.patchServiceConfig(apiClient, stack, products("4.12.0.20000-231"));

        verify(clouderaManagerConfigService).modifyRoleConfigGroup(apiClient, STACK_NAME, NIFI_SERVICE, "nifi-NIFI_NODE-nifi",
                Map.of(NIFI_PROPERTIES_SAFETY_VALVE, proxyHostProperty(COMPUTED_HOSTS + ",old.host")));
    }

    @Test
    void testPatchUpdatesRoleConfigGroupWhenRoleConfigMissesHost() {
        mockHosts();
        ApiRoleConfigGroup roleConfigGroup = roleConfigGroup("nifi-NIFI_NODE-nifi", "NIFI_NODE", null);
        roleConfigGroup.getConfig().addItemsItem(new ApiConfig().name("nifi.web.proxy.host").value("gw0.site,gw1.site,tb-cfm.site"));
        when(clouderaManagerConfigService.readRoleConfigGroupConfigs(apiClient, STACK_NAME, NIFI_SERVICE))
                .thenReturn(new ApiRoleConfigGroupList().addItemsItem(roleConfigGroup));

        underTest.patchServiceConfig(apiClient, stack, products("4.12.0.20000-231"));

        verify(clouderaManagerConfigService).modifyRoleConfigGroup(apiClient, STACK_NAME, NIFI_SERVICE, "nifi-NIFI_NODE-nifi",
                Map.of(NIFI_PROPERTIES_SAFETY_VALVE, EXPECTED_PROXY_HOST_PROPERTY));
    }

    @Test
    void testPatchDoesNotWriteWhenMergedValueIsUnchanged() {
        mockHosts();
        String current = proxyHostProperty(COMPUTED_HOSTS + ",custom.proxy");
        when(clouderaManagerConfigService.readRoleConfigGroupConfigs(apiClient, STACK_NAME, NIFI_SERVICE)).thenReturn(new ApiRoleConfigGroupList()
                .addItemsItem(roleConfigGroup("nifi-NIFI_NODE-nifi", "NIFI_NODE", current)));

        underTest.patchServiceConfig(apiClient, stack, products("4.12.0.20000-231"));

        verify(clouderaManagerConfigService, never()).modifyRoleConfigGroup(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testPatchSkipsUnchangedAndUnparsableSafetyValve() {
        mockHosts();
        when(clouderaManagerConfigService.readRoleConfigGroupConfigs(apiClient, STACK_NAME, NIFI_SERVICE)).thenReturn(new ApiRoleConfigGroupList()
                .addItemsItem(roleConfigGroup("nifi-NIFI_NODE-nifi", "NIFI_NODE", EXPECTED_PROXY_HOST_PROPERTY))
                .addItemsItem(roleConfigGroup("nifi-NIFI_NODE-nifi_scaling", "NIFI_NODE", "<property><name>broken")));

        underTest.patchServiceConfig(apiClient, stack, products("4.12.0.20000-231"));

        verify(clouderaManagerConfigService, never()).modifyRoleConfigGroup(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testPatchFailsWhenNifiServiceIsMissing() {
        mockHostsWithoutService();
        when(clouderaManagerConfigService.getServiceName(apiClient, STACK_NAME, "NIFI")).thenReturn(Optional.empty());

        assertThrows(CloudbreakServiceException.class, () -> underTest.patchServiceConfig(apiClient, stack, products("4.12.0.20000-231")));
    }

    @Test
    void testParseSafetyValveKeepsFormattingOfOtherEntries() {
        String other = "<property>\n  <name>nifi.custom</name>\n  <value>x</value>\n</property>";
        String current = other + "\n" + proxyHostProperty("a.host") + "\n";

        assertEquals(Optional.of(new ClouderaManagerNifiProxyHostConfigurationService.ParsedSafetyValve(other, List.of("a.host"))),
                ClouderaManagerNifiProxyHostConfigurationService.parseSafetyValve(current));
    }

    @Test
    void testParseSafetyValveOfBlankAndBrokenContent() {
        assertEquals(Optional.of(new ClouderaManagerNifiProxyHostConfigurationService.ParsedSafetyValve("", List.of())),
                ClouderaManagerNifiProxyHostConfigurationService.parseSafetyValve(" "));
        assertTrue(ClouderaManagerNifiProxyHostConfigurationService.parseSafetyValve("<property><name>broken").isEmpty());
    }

    @Test
    void testParseSafetyValveRejectsDoctypeDeclaration() {
        Optional<ClouderaManagerNifiProxyHostConfigurationService.ParsedSafetyValve> parsed = ClouderaManagerNifiProxyHostConfigurationService
                .parseSafetyValve("<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>"
                        + "<property><name>nifi.web.proxy.host</name><value>&e;</value></property>");

        // the DOCTYPE ends up inside the wrapper root element, which is never valid XML, so entities can not be declared at all
        assertTrue(parsed.isEmpty());
    }

    private void mockHosts() {
        mockHostsWithoutService();
        when(clouderaManagerConfigService.getServiceName(apiClient, STACK_NAME, "NIFI")).thenReturn(Optional.of(NIFI_SERVICE));
    }

    private void mockHostsWithoutService() {
        when(primaryGateway.getDiscoveryFQDN()).thenReturn("gw0.site");
        when(otherGateway.getDiscoveryFQDN()).thenReturn("gw1.site");
        when(stack.getPrimaryGatewayInstance()).thenReturn(primaryGateway);
        when(stack.getAllAvailableGatewayInstances()).thenReturn(List.of(primaryGateway, otherGateway));
        when(stack.getCluster()).thenReturn(cluster);
        when(cluster.getFqdn()).thenReturn("tb-cfm.site");
        when(loadBalancerUserFacingFqdnProvider.getLoadBalancerUserFacingFQDN(1L)).thenReturn("LB.elb.com");
    }

    private ApiRoleConfigGroup roleConfigGroup(String name, String roleType, String safetyValve) {
        ApiConfigList configList = new ApiConfigList();
        if (safetyValve != null) {
            configList.addItemsItem(new ApiConfig().name(NIFI_PROPERTIES_SAFETY_VALVE).value(safetyValve));
        }
        return new ApiRoleConfigGroup().name(name).roleType(roleType).config(configList);
    }

    private static String proxyHostProperty(String hosts) {
        return "<property><name>nifi.web.proxy.host</name><value>" + hosts + "</value></property>";
    }

    private Set<ClouderaManagerProduct> products(String cfmVersion) {
        return Set.of(new ClouderaManagerProduct().withName("CFM").withVersion(cfmVersion));
    }
}
