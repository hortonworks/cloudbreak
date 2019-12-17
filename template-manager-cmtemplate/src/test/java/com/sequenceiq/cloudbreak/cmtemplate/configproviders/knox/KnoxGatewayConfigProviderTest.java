package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.LdapView.LdapViewBuilder;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.util.TestConstants;

@RunWith(MockitoJUnitRunner.class)
public class KnoxGatewayConfigProviderTest {
    @Mock
    private VirtualGroupService virtualGroupService;

    @InjectMocks
    private final KnoxGatewayConfigProvider underTest = new KnoxGatewayConfigProvider();

    @Test
    public void testGetAdditionalServicesWhenNoKnoxRequested() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertTrue(additionalServices.isEmpty());
    }

    @Test
    public void testGetAdditionalServicesWhenKnoxRequestedAndBlueprintDoesNoContainKnox() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        Gateway gateway = new Gateway();
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).withGateway(gateway, "key").build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(cmTemplateProcessor, preparationObject);

        ApiClusterTemplateService knox = additionalServices.get("master");
        assertEquals(1, additionalServices.size());
        assertNotNull(knox);
        assertEquals("KNOX", knox.getServiceType());
        assertEquals("knox", knox.getRefName());
        ApiClusterTemplateRoleConfigGroup roleConfigGroup = knox.getRoleConfigGroups().get(0);
        assertEquals("KNOX_GATEWAY", roleConfigGroup.getRoleType());
        assertTrue(roleConfigGroup.getBase());
    }

    @Test
    public void testGetAdditionalServicesWhenKnoxRequestedAndBlueprintDoesNoContainKnoxWithMultiGateway() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master2 = new HostgroupView("master2", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        Gateway gateway = new Gateway();
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, master2, worker)).withGateway(gateway, "key").build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(cmTemplateProcessor, preparationObject);

        ApiClusterTemplateService knox1 = additionalServices.get("master");
        ApiClusterTemplateService knox2 = additionalServices.get("master2");
        assertEquals(2, additionalServices.size());
        assertNotNull(knox1);
        assertNotNull(knox2);
        assertEquals("KNOX", knox1.getServiceType());
        assertEquals("KNOX", knox2.getServiceType());
        assertEquals("knox", knox1.getRefName());
        assertEquals("knox", knox2.getRefName());
        ApiClusterTemplateRoleConfigGroup roleConfigGroup1 = knox1.getRoleConfigGroups().get(0);
        ApiClusterTemplateRoleConfigGroup roleConfigGroup2 = knox1.getRoleConfigGroups().get(0);
        assertEquals("KNOX_GATEWAY", roleConfigGroup1.getRoleType());
        assertTrue(roleConfigGroup1.getBase());
        assertEquals("KNOX_GATEWAY", roleConfigGroup2.getRoleType());
        assertTrue(roleConfigGroup2.getBase());
    }

    @Test
    public void testGetAdditionalServicesWhenKnoxRequestedAndBlueprintContainsIt() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        Gateway gateway = new Gateway();
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).withGateway(gateway, "key").build();
        String inputJson = getBlueprintText("input/clouderamanager-knox.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertTrue(additionalServices.isEmpty());
    }

    @Test
    public void roleConfigsWithGateway() {
        GatewayTopology topology = new GatewayTopology();
        topology.setTopologyName("my-topology");
        topology.setExposedServices(Json.silent(new ExposedServices()));

        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        gateway.setTopologies(Set.of(topology));
        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key")
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, "")).build();
        Mockito.when(virtualGroupService.getVirtualGroup(source.getVirtualGroupRequest(), UmsRight.KNOX_ADMIN.getRight())).thenReturn("");

        assertEquals(
                List.of(
                        config("idbroker_master_secret", gateway.getKnoxMasterSecret()),
                        config("idbroker_gateway_knox_admin_groups", "")
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gateway.getKnoxMasterSecret()),
                        config("gateway_default_topology_name",
                            gateway.getTopologies().iterator().next().getTopologyName()),
                        config("gateway_knox_admin_groups", ""),
                        config("gateway_path", gateway.getPath()),
                        config("gateway_signing_keystore_name", "signing.jks"),
                        config("gateway_signing_keystore_type", "JKS"),
                        config("gateway_signing_key_alias", "signing-identity"),
                        config("gateway_dispatch_whitelist", "^*.*$")
                ),
                underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, source)
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", source));
    }

    @Test
    public void roleConfigsWithoutGateway() {
        GeneralClusterConfigs gcc = new GeneralClusterConfigs();
        gcc.setPassword("secret");
        TemplatePreparationObject source = Builder.builder()
                .withGeneralClusterConfigs(gcc)
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, "")).build();
        Mockito.when(virtualGroupService.getVirtualGroup(source.getVirtualGroupRequest(), UmsRight.KNOX_ADMIN.getRight())).thenReturn("");
        assertEquals(
                List.of(
                        config("idbroker_master_secret", gcc.getPassword()),
                        config("idbroker_gateway_knox_admin_groups", "")
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gcc.getPassword()),
                        config("gateway_default_topology_name", "cdp-proxy"),
                        config("gateway_knox_admin_groups", "")
                ),
                underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, source)
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", source));
    }

    @Test
    public void roleConfigsWithGatewayWithLdapConfig() {
        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        LdapView ldapConfig = LdapViewBuilder.aLdapView().build();

        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key")
                .withLdapConfig(ldapConfig)
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, "")).build();
        Mockito.when(virtualGroupService.getVirtualGroup(source.getVirtualGroupRequest(), UmsRight.KNOX_ADMIN.getRight())).thenReturn("knox_admins");

        assertEquals(
            List.of(
                config("idbroker_master_secret", gateway.getKnoxMasterSecret()),
                config("idbroker_gateway_knox_admin_groups", "knox_admins")
            ),
            underTest.getRoleConfigs(KnoxRoles.IDBROKER, source)
        );
        assertEquals(
            List.of(
                config("gateway_master_secret", gateway.getKnoxMasterSecret()),
                config("gateway_default_topology_name", "cdp-proxy"),
                config("gateway_knox_admin_groups", "knox_admins"),
                config("gateway_path", gateway.getPath()),
                config("gateway_signing_keystore_name", "signing.jks"),
                config("gateway_signing_keystore_type", "JKS"),
                config("gateway_signing_key_alias", "signing-identity"),
                config("gateway_dispatch_whitelist", "^*.*$")
            ),
            underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, source)
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", source));
    }

    @Test
    public void testGatewayWhitelistConfig() {
        TemplatePreparationObject noKerberosTPO = Builder.builder()
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .build();
        assertEquals(config("gateway_dispatch_whitelist", "^*.*$"),
                underTest.getGatewayWhitelistConfig(noKerberosTPO));

        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain("example.com").build();
        TemplatePreparationObject kerberosNoAutoTlsTPO = Builder.builder()
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withKerberosConfig(kerberosConfig)
                .build();
        assertEquals(config("gateway_dispatch_whitelist", "^/.*$;^https?://(.+.example.com):[0-9]+/?.*$"),
                underTest.getGatewayWhitelistConfig(kerberosNoAutoTlsTPO));

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAutoTlsEnabled(true);
        TemplatePreparationObject kerberosAutoTlsTPO = Builder.builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withKerberosConfig(kerberosConfig)
                .build();
        assertEquals(config("gateway_dispatch_whitelist", "^/.*$;^https://(.+.example.com):[0-9]+/?.*$"),
                underTest.getGatewayWhitelistConfig(kerberosAutoTlsTPO));
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
