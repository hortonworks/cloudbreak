package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil;
import com.sequenceiq.cloudbreak.cmtemplate.inifile.IniFile;
import com.sequenceiq.cloudbreak.cmtemplate.inifile.IniFileFactory;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@ExtendWith(MockitoExtension.class)
public class HueConfigProviderTest {

    private static final String HUE = "HUE";

    private static final String DB_PROVIDER = "dbProvider";

    private static final String HOST = "host";

    private static final String PORT = "12345";

    private static final String DB_NAME = "dbName";

    private static final String USER_NAME = "userName";

    private static final String PASSWORD = "password";

    @Mock
    private IniFileFactory iniFileFactory;

    @InjectMocks
    private HueConfigProvider underTest;

    @Mock
    private IniFile safetyValve;

    @Test
    public void getServiceConfigs() {
        BlueprintView blueprintView = getMockBlueprintView("7.1.0");

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);

        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig))
                .withBlueprintView(blueprintView)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .build();

        when(iniFileFactory.create()).thenReturn(safetyValve);
        when(safetyValve.print()).thenReturn("");

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        verify(safetyValve, never()).addContent(anyString());
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("database_host", HOST),
                entry("database_port", PORT),
                entry("database_name", DB_NAME),
                entry("database_type", DB_PROVIDER),
                entry("database_user", USER_NAME),
                entry("database_password", PASSWORD));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getServiceConfigsTestWhenGoodCmVersionButDbSslIsNotRequested() {
        BlueprintView blueprintView = getMockBlueprintView("7.2.2");

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.isUseSsl()).thenReturn(false);

        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig))
                .withBlueprintView(blueprintView)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();

        when(iniFileFactory.create()).thenReturn(safetyValve);
        when(safetyValve.print()).thenReturn("");

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        verify(safetyValve, never()).addContent(anyString());
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("database_host", HOST),
                entry("database_port", PORT),
                entry("database_name", DB_NAME),
                entry("database_type", DB_PROVIDER),
                entry("database_user", USER_NAME),
                entry("database_password", PASSWORD));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @ParameterizedTest(name = "connectionUrl={0}")
    @ValueSource(strings = {"sslmode=verify-full", "sslmode=verify-ca"})
    public void getServiceConfigsTestWhenDbSsl(String connectionUrl) {
        BlueprintView blueprintView = getMockBlueprintView("7.2.2");

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getConnectionURL()).thenReturn(connectionUrl);
        when(rdsConfig.isUseSsl()).thenReturn(true);
        when(rdsConfig.getSslCertificateFilePath()).thenReturn("/foo/bar.pem");

        TemplatePreparationObject tpo = new Builder()
                .withRdsViews(Set.of(rdsConfig))
                .withBlueprintView(blueprintView)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();

        when(iniFileFactory.create()).thenReturn(safetyValve);
        String expectedSslMode = connectionUrl.contains("verify-ca") ? "\"verify-ca\"" : "\"verify-full\"";
        String expectedSafetyValveValue = "[desktop]\n[[database]]\noptions='{\"sslmode\": " + expectedSslMode + ", \"sslrootcert\": \"/foo/bar.pem\"}'";
        when(safetyValve.print()).thenReturn(expectedSafetyValveValue);

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        verify(safetyValve).addContent(expectedSafetyValveValue);
        verifyNoMoreInteractions(safetyValve);
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("database_host", HOST),
                entry("database_port", PORT),
                entry("database_name", DB_NAME),
                entry("database_type", DB_PROVIDER),
                entry("database_user", USER_NAME),
                entry("database_password", PASSWORD),
                entry("hue_service_safety_valve", expectedSafetyValveValue));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenKnoxConfiguredToExternalDomain() {
        BlueprintView blueprintView = getMockBlueprintView("7.0.1");

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);

        String expectedExternalFQDN = "myaddress.cloudera.site";
        String expectedInternalFQDN = "private-gateway.cloudera.site";
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setExternalFQDN(expectedExternalFQDN);
        generalClusterConfigs.setKnoxUserFacingCertConfigured(true);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(expectedInternalFQDN));
        generalClusterConfigs.setOtherGatewayInstancesDiscoveryFQDN(Set.of(expectedInternalFQDN));

        TemplatePreparationObject tpo = new Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withGateway(new Gateway(), "", new HashSet<>())
                .withBlueprintView(blueprintView)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_1), null)
                .withRdsViews(Set.of(rdsConfig))
                .build();

        when(iniFileFactory.create()).thenReturn(safetyValve);
        String proxyHostsExpected = String.join(",", expectedInternalFQDN, expectedExternalFQDN);
        String expectedSafetyValveValue = "[desktop]\n[[knox]]\nknox_proxyhosts=".concat(proxyHostsExpected);
        when(safetyValve.print()).thenReturn(expectedSafetyValveValue);

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        verify(safetyValve).addContent(expectedSafetyValveValue);
        verifyNoMoreInteractions(safetyValve);
        verify(rdsConfig, never()).getSslCertificateFilePath();
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("database_host", HOST),
                entry("database_port", PORT),
                entry("database_name", DB_NAME),
                entry("database_type", DB_PROVIDER),
                entry("database_user", USER_NAME),
                entry("database_password", PASSWORD),
                entry("hue_service_safety_valve", expectedSafetyValveValue));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenKnoxConfiguredToExternalDomainWhenNoSafetyValve() {
        BlueprintView blueprintView = getMockBlueprintView("7.1.0");

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);

        String expectedExternalFQDN = "myaddress.cloudera.site";
        String expectedInternalFQDN = "private-gateway.cloudera.site";
        String expectedInternalOtherFQDN = "private-other-gateway.cloudera.site";
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setExternalFQDN(expectedExternalFQDN);
        generalClusterConfigs.setKnoxUserFacingCertConfigured(true);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(expectedInternalFQDN));
        generalClusterConfigs.setOtherGatewayInstancesDiscoveryFQDN(Set.of(expectedInternalFQDN, expectedInternalOtherFQDN));

        TemplatePreparationObject tpo = new Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withGateway(new Gateway(), "", new HashSet<>())
                .withBlueprintView(blueprintView)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .withRdsViews(Set.of(rdsConfig))
                .build();

        when(iniFileFactory.create()).thenReturn(safetyValve);
        when(safetyValve.print()).thenReturn("");

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        verify(safetyValve, never()).addContent(anyString());
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        String proxyHostsExpected = String.join(",", expectedInternalFQDN, expectedInternalOtherFQDN, expectedExternalFQDN);
        assertThat(configToValue).containsOnly(
                entry("database_host", HOST),
                entry("database_port", PORT),
                entry("database_name", DB_NAME),
                entry("database_type", DB_PROVIDER),
                entry("database_user", USER_NAME),
                entry("database_password", PASSWORD),
                entry("knox_proxyhosts", proxyHostsExpected));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    // Note: Due to the conflicting CM version requirements, it is impossible to have both the Knox Proxy Hosts and DB SSL settings in the Safety Valve!
    @ParameterizedTest(name = "connectionUrl={0}")
    @ValueSource(strings = {"sslmode=verify-full", "sslmode=verify-ca"})
    public void getServiceConfigsWhenKnoxConfiguredToExternalDomainWhenNoSafetyValveAndDbSsl(String connectionUrl) {
        BlueprintView blueprintView = getMockBlueprintView("7.2.2");

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);
        when(rdsConfig.getConnectionURL()).thenReturn(connectionUrl);
        when(rdsConfig.isUseSsl()).thenReturn(true);
        when(rdsConfig.getSslCertificateFilePath()).thenReturn("/foo/bar.pem");

        String expectedExternalFQDN = "myaddress.cloudera.site";
        String expectedInternalFQDN = "private-gateway.cloudera.site";
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setExternalFQDN(expectedExternalFQDN);
        generalClusterConfigs.setKnoxUserFacingCertConfigured(true);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(expectedInternalFQDN));
        generalClusterConfigs.setOtherGatewayInstancesDiscoveryFQDN(Set.of(expectedInternalFQDN));

        TemplatePreparationObject tpo = new Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withGateway(new Gateway(), "", new HashSet<>())
                .withBlueprintView(blueprintView)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .withRdsViews(Set.of(rdsConfig))
                .build();

        when(iniFileFactory.create()).thenReturn(safetyValve);
        String expectedSslMode = connectionUrl.contains("verify-ca") ? "\"verify-ca\"" : "\"verify-full\"";
        String expectedSafetyValveValue = "[desktop]\n[[database]]\noptions='{\"sslmode\": " + expectedSslMode + ", \"sslrootcert\": \"/foo/bar.pem\"}'";
        when(safetyValve.print()).thenReturn(expectedSafetyValveValue);

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        verify(safetyValve).addContent(expectedSafetyValveValue);
        verifyNoMoreInteractions(safetyValve);
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        String proxyHostsExpected = String.join(",", expectedInternalFQDN, expectedExternalFQDN);
        assertThat(configToValue).containsOnly(
                entry("database_host", HOST),
                entry("database_port", PORT),
                entry("database_name", DB_NAME),
                entry("database_type", DB_PROVIDER),
                entry("database_user", USER_NAME),
                entry("database_password", PASSWORD),
                entry("knox_proxyhosts", proxyHostsExpected),
                entry("hue_service_safety_valve", expectedSafetyValveValue));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo(HUE);
    }

    @Test
    public void getRoleTypes() {
        assertThat(underTest.getRoleTypes()).containsOnly("HUE_SERVER", "HUE_LOAD_BALANCER");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededTrue() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(true);

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);

        TemplatePreparationObject tpo = new Builder().withRdsViews(Set.of(rdsConfig)).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);

        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoHueOnCluster() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(false);

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);

        TemplatePreparationObject tpo = new Builder().withRdsViews(Set.of(rdsConfig)).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);

        assertThat(result).isFalse();
    }

    @Test
    public void isConfigurationNeededFalseWhenNoDBRegistered() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);

        TemplatePreparationObject tpo = new Builder().build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);

        assertThat(result).isFalse();
        verifyNoInteractions(mockTemplateProcessor);
    }

    @Test
    public void getServiceConfigsWhenKnoxConfiguredWithLoadBalancer() {
        BlueprintView blueprintView = getMockBlueprintView("7.0.1");

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);

        String expectedExternalFQDN = "myaddress.cloudera.site";
        String expectedLBFQDN = "loadbalancer-gateway.cloudera.site";
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setExternalFQDN(expectedExternalFQDN);
        generalClusterConfigs.setKnoxUserFacingCertConfigured(true);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.empty());
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.of(expectedLBFQDN));

        TemplatePreparationObject tpo = new Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withGateway(new Gateway(), "", new HashSet<>())
                .withBlueprintView(blueprintView)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_1), null)
                .withRdsViews(Set.of(rdsConfig))
                .build();

        when(iniFileFactory.create()).thenReturn(safetyValve);
        String proxyHostsExpected = String.join(",", expectedExternalFQDN, expectedLBFQDN);
        String expectedSafetyValveValue = "[desktop]\n[[knox]]\nknox_proxyhosts=".concat(proxyHostsExpected);
        when(safetyValve.print()).thenReturn(expectedSafetyValveValue);

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        verify(safetyValve).addContent(expectedSafetyValveValue);
        verifyNoMoreInteractions(safetyValve);
        verify(rdsConfig, never()).getSslCertificateFilePath();
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("database_host", HOST),
                entry("database_port", PORT),
                entry("database_name", DB_NAME),
                entry("database_type", DB_PROVIDER),
                entry("database_user", USER_NAME),
                entry("database_password", PASSWORD),
                entry("hue_service_safety_valve", expectedSafetyValveValue));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenKnoxConfiguredWithLoadBalancerWithUpperCaseCharsInFQDN() {
        BlueprintView blueprintView = getMockBlueprintView("7.0.1");

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);

        String expectedExternalFQDN = "myaddress.cloudera.site";
        String expectedLBFQDN = "loadbalancer-GATEWAY.cloudera.site";
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setExternalFQDN(expectedExternalFQDN);
        generalClusterConfigs.setKnoxUserFacingCertConfigured(true);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.empty());
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.of(expectedLBFQDN));

        TemplatePreparationObject tpo = new Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withGateway(new Gateway(), "", new HashSet<>())
                .withBlueprintView(blueprintView)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_1), null)
                .withRdsViews(Set.of(rdsConfig))
                .build();

        when(iniFileFactory.create()).thenReturn(safetyValve);
        String proxyHostsExpected = String.join(",", expectedExternalFQDN, expectedLBFQDN, expectedLBFQDN.toLowerCase(Locale.ROOT));
        String expectedSafetyValveValue = "[desktop]\n[[knox]]\nknox_proxyhosts=".concat(proxyHostsExpected);
        when(safetyValve.print()).thenReturn(expectedSafetyValveValue);

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        verify(safetyValve).addContent(expectedSafetyValveValue);
        verifyNoMoreInteractions(safetyValve);
        verify(rdsConfig, never()).getSslCertificateFilePath();
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        assertThat(configToValue).containsOnly(
                entry("database_host", HOST),
                entry("database_port", PORT),
                entry("database_name", DB_NAME),
                entry("database_type", DB_PROVIDER),
                entry("database_user", USER_NAME),
                entry("database_password", PASSWORD),
                entry("hue_service_safety_valve", expectedSafetyValveValue));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenKnoxConfiguredWithLoadBalancerPost710() {
        BlueprintView blueprintView = getMockBlueprintView("7.1.0");

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(HUE);
        when(rdsConfig.getHost()).thenReturn(HOST);
        when(rdsConfig.getDatabaseName()).thenReturn(DB_NAME);
        when(rdsConfig.getPort()).thenReturn(PORT);
        when(rdsConfig.getSubprotocol()).thenReturn(DB_PROVIDER);
        when(rdsConfig.getConnectionUserName()).thenReturn(USER_NAME);
        when(rdsConfig.getConnectionPassword()).thenReturn(PASSWORD);

        String expectedExternalFQDN = "myaddress.cloudera.site";
        String expectedLBFQDN = "loadbalancer-gateway.cloudera.site";
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setExternalFQDN(expectedExternalFQDN);
        generalClusterConfigs.setKnoxUserFacingCertConfigured(true);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.empty());
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.of(expectedLBFQDN));

        TemplatePreparationObject tpo = new Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withGateway(new Gateway(), "", new HashSet<>())
                .withBlueprintView(blueprintView)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .withRdsViews(Set.of(rdsConfig))
                .build();

        when(iniFileFactory.create()).thenReturn(safetyValve);
        when(safetyValve.print()).thenReturn("");

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);

        verify(safetyValve, never()).addContent(anyString());
        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);
        String proxyHostsExpected = String.join(",", expectedExternalFQDN, expectedLBFQDN);
        assertThat(configToValue).containsOnly(
                entry("database_host", HOST),
                entry("database_port", PORT),
                entry("database_name", DB_NAME),
                entry("database_type", DB_PROVIDER),
                entry("database_user", USER_NAME),
                entry("database_password", PASSWORD),
                entry("knox_proxyhosts", proxyHostsExpected));

        Map<String, String> configToVariable = ConfigTestUtil.getConfigNameToVariableNameMap(result);
        assertThat(configToVariable).isEmpty();
    }

    private BlueprintView getMockBlueprintView(String tmplVersion) {
        BlueprintView blueprintView = mock(BlueprintView.class);

        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        when(templateProcessor.getVersion()).thenReturn(Optional.ofNullable(tmplVersion));
        when(blueprintView.getProcessor()).thenReturn(templateProcessor);

        when(blueprintView.getProcessor()).thenReturn(templateProcessor);
        return blueprintView;
    }

    private ClouderaManagerRepo generateCmRepo(Versioned version) {
        return new ClouderaManagerRepo()
                .withBaseUrl("baseurl")
                .withGpgKeyUrl("gpgurl")
                .withPredefined(true)
                .withVersion(version.getVersion());
    }

}
