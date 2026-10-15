package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiKnoxProxyHostRoleConfigProvider.NIFI_WEB_PROXY_HOST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;

@ExtendWith(MockitoExtension.class)
class NifiKnoxProxyHostRoleConfigProviderTest {

    private static final String PRIMARY_GATEWAY = "cluster-gateway0.env.cloudera.site";

    private static final String OTHER_GATEWAY = "cluster-gateway1.env.cloudera.site";

    private static final String EXTERNAL_FQDN = "cluster.env.cloudera.site";

    private final NifiKnoxProxyHostRoleConfigProvider underTest = new NifiKnoxProxyHostRoleConfigProvider();

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Test
    void testRoleConfigsContainGatewaysExternalFqdnAndLoadBalancerWithLowerCaseVariant() {
        GeneralClusterConfigs generalClusterConfigs = generalClusterConfigs();
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.of("Cluster-GWAY.elb.amazonaws.com"));

        assertEquals(List.of(config(NIFI_WEB_PROXY_HOST, String.join(",", PRIMARY_GATEWAY, OTHER_GATEWAY, EXTERNAL_FQDN,
                        "Cluster-GWAY.elb.amazonaws.com", "cluster-gway.elb.amazonaws.com"))),
                underTest.getRoleConfigs(NifiRoles.NIFI_NODE, cmTemplateProcessor, tpo(generalClusterConfigs, "4.12.0.20000-231")));
    }

    @Test
    void testRoleConfigsWithoutLoadBalancer() {
        GeneralClusterConfigs generalClusterConfigs = generalClusterConfigs();
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.empty());

        assertEquals(List.of(config(NIFI_WEB_PROXY_HOST, String.join(",", PRIMARY_GATEWAY, OTHER_GATEWAY, EXTERNAL_FQDN))),
                underTest.getRoleConfigs(NifiRoles.NIFI_NODE, cmTemplateProcessor, tpo(generalClusterConfigs, "4.12.0.20000-231")));
    }

    @Test
    void testRoleConfigsAddLowerCaseVariantForEveryMixedCaseHost() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("Cluster-Gateway0.env.cloudera.site"));
        generalClusterConfigs.setOtherGatewayInstancesDiscoveryFQDN(new LinkedHashSet<>(List.of("Cluster-Gateway1.env.cloudera.site")));
        generalClusterConfigs.setExternalFQDN("Cluster.env.cloudera.site");
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.empty());

        assertEquals(List.of(config(NIFI_WEB_PROXY_HOST, String.join(",",
                        "Cluster-Gateway0.env.cloudera.site", PRIMARY_GATEWAY,
                        "Cluster-Gateway1.env.cloudera.site", OTHER_GATEWAY,
                        "Cluster.env.cloudera.site", EXTERNAL_FQDN))),
                underTest.getRoleConfigs(NifiRoles.NIFI_NODE, cmTemplateProcessor, tpo(generalClusterConfigs, "4.12.0.20000-231")));
    }

    @Test
    void testRoleConfigsWhenOtherGatewaysNotInitializedLikeInBlueprintPreview() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("pending..."));
        generalClusterConfigs.setOtherGatewayInstancesDiscoveryFQDN(null);
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.empty());

        assertEquals(List.of(config(NIFI_WEB_PROXY_HOST, "pending...")),
                underTest.getRoleConfigs(NifiRoles.NIFI_NODE, cmTemplateProcessor, tpo(generalClusterConfigs, "4.12.0.20000-231")));
    }

    @Test
    void testRoleConfigsEmptyWhenNoHostKnown() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.empty());
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.empty());

        assertEquals(List.of(),
                underTest.getRoleConfigs(NifiRoles.NIFI_NODE, cmTemplateProcessor, tpo(generalClusterConfigs, "4.12.0.20000-231")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.12.0.0", "4.12.0.20000-231", "4.13.0.0-1"})
    void testConfigurationNeededFromCfm4120(String cfmVersion) {
        when(cmTemplateProcessor.isRoleTypePresentInService("NIFI", List.of(NifiRoles.NIFI_NODE))).thenReturn(true);

        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo(generalClusterConfigs(), cfmVersion)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.2.9.0-1", "4.0.0.0-1", "4.11.0.0-100"})
    void testConfigurationNotNeededBeforeCfm4120(String cfmVersion) {
        when(cmTemplateProcessor.isRoleTypePresentInService("NIFI", List.of(NifiRoles.NIFI_NODE))).thenReturn(true);

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo(generalClusterConfigs(), cfmVersion)));
    }

    @Test
    void testConfigurationNotNeededWithoutGateway() {
        when(cmTemplateProcessor.isRoleTypePresentInService("NIFI", List.of(NifiRoles.NIFI_NODE))).thenReturn(true);
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .withGeneralClusterConfigs(generalClusterConfigs())
                .withProductDetails(null, List.of(new ClouderaManagerProduct().withName("CFM").withVersion("4.12.0.20000-231")))
                .build();

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    void testConfigurationNotNeededWithoutNifiNode() {
        when(cmTemplateProcessor.isRoleTypePresentInService("NIFI", List.of(NifiRoles.NIFI_NODE))).thenReturn(false);

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo(generalClusterConfigs(), "4.12.0.20000-231")));
    }

    private GeneralClusterConfigs generalClusterConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(PRIMARY_GATEWAY));
        generalClusterConfigs.setOtherGatewayInstancesDiscoveryFQDN(new LinkedHashSet<>(List.of(OTHER_GATEWAY)));
        generalClusterConfigs.setExternalFQDN(EXTERNAL_FQDN);
        return generalClusterConfigs;
    }

    private TemplatePreparationObject tpo(GeneralClusterConfigs generalClusterConfigs, String cfmVersion) {
        return TemplatePreparationObject.Builder.builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withGateway(new Gateway(), "", new HashSet<>())
                .withProductDetails(null, List.of(new ClouderaManagerProduct().withName("CFM").withVersion(cfmVersion)))
                .build();
    }
}
