package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

public class HueConfigProviderTest {

    private static final String HUE = "HUE";

    private static final String DB_PROVIDER = "dbProvider";

    private static final String HOST = "host";

    private static final String PORT = "12345";

    private static final String DB_NAME = "dbName";

    private static final String USER_NAME = "userName";

    private static final String PASSWORD = "password";

    private HueConfigProvider underTest;

    @Before
    public void setUp() {
        underTest = new HueConfigProvider();
    }

    @Test
    public void getServiceConfigs() {
        BlueprintView blueprintView = getMockBlueprintView("7.0.2", "7.0.2");

        TemplatePreparationObject tpo = new Builder().withBlueprintView(blueprintView).build();
        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);
        Map<String, String> paramToVariable =
                result.stream().collect(Collectors.toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getVariable));
        assertThat(paramToVariable).containsOnly(
                new SimpleEntry<>("database_host", "hue-hue_database_host"),
                new SimpleEntry<>("database_port", "hue-hue_database_port"),
                new SimpleEntry<>("database_name", "hue-hue_database_name"),
                new SimpleEntry<>("database_type", "hue-hue_database_type"),
                new SimpleEntry<>("database_user", "hue-hue_database_user"),
                new SimpleEntry<>("database_password", "hue-hue_database_password")
        );
    }

    @Test
    public void getServiceConfigsWhenKnoxConfiguredToExternalDomain() {
        BlueprintView blueprintView = getMockBlueprintView("7.0.2", "7.0.2");

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setExternalFQDN("myaddress.cloudera.site");
        generalClusterConfigs.setKnoxUserFacingCertConfigured(true);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("private-gateway.cloudera.site"));
        TemplatePreparationObject tpo = new Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withBlueprintView(blueprintView)
                .withGateway(new Gateway(), "", new HashSet<>())
                .build();
        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(null, tpo);
        Map<String, String> paramToVariable =
                result.stream().collect(Collectors.toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getVariable));
        assertThat(paramToVariable).containsOnly(
                new SimpleEntry<>("database_host", "hue-hue_database_host"),
                new SimpleEntry<>("database_port", "hue-hue_database_port"),
                new SimpleEntry<>("database_name", "hue-hue_database_name"),
                new SimpleEntry<>("database_type", "hue-hue_database_type"),
                new SimpleEntry<>("database_user", "hue-hue_database_user"),
                new SimpleEntry<>("database_password", "hue-hue_database_password"),
                new SimpleEntry<>("hue_service_safety_valve", "hue-hue_service_safety_valve")
        );
    }

    @Test
    public void getServiceConfigVariables() {
        BlueprintView blueprintView = getMockBlueprintView("7.1.1", "7.1.0");

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(HUE);
        rdsConfig.setConnectionURL(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        rdsConfig.setConnectionUserName(USER_NAME);
        rdsConfig.setConnectionPassword(PASSWORD);
        TemplatePreparationObject tpo = new Builder()
                .withRdsConfigs(Set.of(rdsConfig))
                .withBlueprintView(blueprintView)
                .build();

        List<ApiClusterTemplateVariable> result = underTest.getServiceConfigVariables(tpo);
        Map<String, String> paramToVariable =
                result.stream().collect(Collectors.toMap(ApiClusterTemplateVariable::getName, ApiClusterTemplateVariable::getValue));
        assertThat(paramToVariable).containsOnly(
                new SimpleEntry<>("hue-hue_database_host", HOST),
                new SimpleEntry<>("hue-hue_database_port", PORT),
                new SimpleEntry<>("hue-hue_database_name", DB_NAME),
                new SimpleEntry<>("hue-hue_database_type", DB_PROVIDER),
                new SimpleEntry<>("hue-hue_database_user", USER_NAME),
                new SimpleEntry<>("hue-hue_database_password", PASSWORD));
    }

    @Test
    public void getServiceConfigVariablesWhenKnoxConfiguredToExternalDomain() {
        BlueprintView blueprintView = getMockBlueprintView("7.0.1", "7.0.1");

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(HUE);
        rdsConfig.setConnectionURL(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        rdsConfig.setConnectionUserName(USER_NAME);
        rdsConfig.setConnectionPassword(PASSWORD);

        String expectedExternalFQDN = "myaddress.cloudera.site";
        String expectedInternalFQDN = "private-gateway.cloudera.site";
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setExternalFQDN(expectedExternalFQDN);
        generalClusterConfigs.setKnoxUserFacingCertConfigured(true);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(expectedInternalFQDN));

        TemplatePreparationObject tpo = new Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withGateway(new Gateway(), "", new HashSet<>())
                .withBlueprintView(blueprintView)
                .withRdsConfigs(Set.of(rdsConfig))
                .build();

        List<ApiClusterTemplateVariable> result = underTest.getServiceConfigVariables(tpo);
        Map<String, String> paramToVariable =
                result.stream().collect(Collectors.toMap(ApiClusterTemplateVariable::getName, ApiClusterTemplateVariable::getValue));
        String proxyHostsExpected = String.join(",", expectedInternalFQDN, expectedExternalFQDN);
        String expectedSafetyValveValue = "[desktop]\n[[knox]]\nknox_proxyhosts=".concat(proxyHostsExpected);
        assertThat(paramToVariable).containsOnly(
                new SimpleEntry<>("hue-hue_database_host", HOST),
                new SimpleEntry<>("hue-hue_database_port", PORT),
                new SimpleEntry<>("hue-hue_database_name", DB_NAME),
                new SimpleEntry<>("hue-hue_database_type", DB_PROVIDER),
                new SimpleEntry<>("hue-hue_database_user", USER_NAME),
                new SimpleEntry<>("hue-hue_database_password", PASSWORD),
                new SimpleEntry<>("hue-hue_service_safety_valve", expectedSafetyValveValue));
    }

    @Test
    public void getServiceConfigVariablesWhenKnoxConfiguredToExternalDomainWhenNoSafetyValve() {
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getVersion()).thenReturn("7.1.0");

        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        when(templateProcessor.getVersion()).thenReturn(Optional.ofNullable("7.1.0"));

        when(blueprintView.getProcessor()).thenReturn(templateProcessor);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(HUE);
        rdsConfig.setConnectionURL(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        rdsConfig.setConnectionUserName(USER_NAME);
        rdsConfig.setConnectionPassword(PASSWORD);

        String expectedExternalFQDN = "myaddress.cloudera.site";
        String expectedInternalFQDN = "private-gateway.cloudera.site";
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setExternalFQDN(expectedExternalFQDN);
        generalClusterConfigs.setKnoxUserFacingCertConfigured(true);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(expectedInternalFQDN));

        TemplatePreparationObject tpo = new Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withGateway(new Gateway(), "", new HashSet<>())
                .withBlueprintView(blueprintView)
                .withRdsConfigs(Set.of(rdsConfig))
                .build();

        List<ApiClusterTemplateVariable> result = underTest.getServiceConfigVariables(tpo);
        Map<String, String> paramToVariable =
                result.stream().collect(Collectors.toMap(ApiClusterTemplateVariable::getName, ApiClusterTemplateVariable::getValue));
        String proxyHostsExpected = String.join(",", expectedInternalFQDN, expectedExternalFQDN);
        assertThat(paramToVariable).containsOnly(
                new SimpleEntry<>("hue-hue_database_host", HOST),
                new SimpleEntry<>("hue-hue_database_port", PORT),
                new SimpleEntry<>("hue-hue_database_name", DB_NAME),
                new SimpleEntry<>("hue-hue_database_type", DB_PROVIDER),
                new SimpleEntry<>("hue-hue_database_user", USER_NAME),
                new SimpleEntry<>("hue-hue_database_password", PASSWORD),
                new SimpleEntry<>("knox_proxyhosts", proxyHostsExpected));
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

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(HUE);
        rdsConfig.setConnectionURL(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        rdsConfig.setConnectionUserName(USER_NAME);
        rdsConfig.setConnectionPassword(PASSWORD);
        TemplatePreparationObject tpo = new Builder().withRdsConfigs(Set.of(rdsConfig)).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoHueOnClusterr() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(false);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(HUE);
        rdsConfig.setConnectionURL(String.format("jdbc:%s://%s:%s/%s", DB_PROVIDER, HOST, PORT, DB_NAME));
        rdsConfig.setConnectionUserName(USER_NAME);
        rdsConfig.setConnectionPassword(PASSWORD);
        TemplatePreparationObject tpo = new Builder().withRdsConfigs(Set.of(rdsConfig)).build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoDBRegistered() {
        CmTemplateProcessor mockTemplateProcessor = mock(CmTemplateProcessor.class);
        when(mockTemplateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(true);

        TemplatePreparationObject tpo = new Builder().build();

        boolean result = underTest.isConfigurationNeeded(mockTemplateProcessor, tpo);
        assertThat(result).isFalse();
    }

    private BlueprintView getMockBlueprintView(String bpVersion, String tmplVersion) {
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getVersion()).thenReturn(bpVersion);

        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        when(templateProcessor.getVersion()).thenReturn(Optional.ofNullable(tmplVersion));
        when(blueprintView.getProcessor()).thenReturn(templateProcessor);

        when(blueprintView.getProcessor()).thenReturn(templateProcessor);
        return blueprintView;
    }
}
