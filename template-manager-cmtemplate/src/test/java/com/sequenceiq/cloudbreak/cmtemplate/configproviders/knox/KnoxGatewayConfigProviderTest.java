package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.LdapView.LdapViewBuilder;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.CdhPatchVersionProvider;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class KnoxGatewayConfigProviderTest {
    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String GATEWAY_SECURITY_DIR = "/mnt/cdp-luks/var/lib/knox/gateway/data/security";

    private static final String IDBROKER_SECURITY_DIR = "/mnt/cdp-luks/var/lib/knox/idbroker/data/security";

    @Spy
    private CdhPatchVersionProvider cdhPatchVersionProvider;

    @Mock
    private VirtualGroupService virtualGroupService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

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
        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withGateway(gateway, "key", new HashSet<>())
                .build();
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
        assertTrue(roleConfigGroup.isBase());
    }

    @Test
    public void testGetAdditionalServicesWhenKnoxRequestedAndBlueprintDoesNoContainKnoxWithMultiGateway() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master2 = new HostgroupView("master2", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        Gateway gateway = new Gateway();
        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master, master2, worker))
                .withGateway(gateway, "key", new HashSet<>())
                .build();
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
        ApiClusterTemplateRoleConfigGroup roleConfigGroup2 = knox2.getRoleConfigGroups().get(0);
        assertEquals("KNOX_GATEWAY", roleConfigGroup1.getRoleType());
        assertTrue(roleConfigGroup1.isBase());
        assertEquals("KNOX_GATEWAY", roleConfigGroup2.getRoleType());
        assertTrue(roleConfigGroup2.isBase());
    }

    @Test
    public void testGetAdditionalServicesWhenKnoxRequestedAndBlueprintContainsIt() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        Gateway gateway = new Gateway();
        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withGateway(gateway, "key", new HashSet<>())
                .build();
        String inputJson = getBlueprintText("input/clouderamanager-knox.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertTrue(additionalServices.isEmpty());
    }

    @Test
    public void roleConfigsWithGateway() {
        ReflectionTestUtils.setField(underTest, "knoxGatewaySecurityDir", GATEWAY_SECURITY_DIR);
        ReflectionTestUtils.setField(underTest, "knoxIdBrokerSecurityDir", IDBROKER_SECURITY_DIR);

        GatewayTopology topology = new GatewayTopology();
        topology.setTopologyName("my-topology");
        topology.setExposedServices(Json.silent(new ExposedServices()));

        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        gateway.setTopologies(Set.of(topology));

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAccountId(Optional.of("1234"));

        IdBroker idBroker = new IdBroker();
        idBroker.setMasterSecret("supersecret");
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.16"));
        BlueprintView blueprintView = new BlueprintView("text", "7.2.11", "CDH", null, blueprintTextProcessor);
        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key", new HashSet<>())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withBlueprintView(blueprintView)
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, ""))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.4.2"), List.of(new ClouderaManagerProduct()
                        .withVersion("7.2.10")
                        .withName("CDH")))
                .withIdBroker(idBroker)
                .withEnableSecretEncryption(true)
                .build();
        when(virtualGroupService.createOrGetVirtualGroup(source.getVirtualGroupRequest(), UmsVirtualGroupRight.KNOX_ADMIN)).thenReturn("");
        when(entitlementService.isOjdbcTokenDhOneHour(anyString())).thenReturn(true);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessor.getServiceByType(anyString())).thenReturn(Optional.of(new ApiClusterTemplateService()));
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        assertEquals(
                List.of(
                        config("idbroker_master_secret", "supersecret"),
                        config("idbroker_gateway_knox_admin_groups", ""),
                        config("idbroker_gateway_signing_keystore_name", "signing.jks"),
                        config("idbroker_gateway_signing_keystore_type", "JKS"),
                        config("idbroker_gateway_signing_key_alias", "signing-identity"),
                        config("idbroker_security_dir", IDBROKER_SECURITY_DIR)
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, cmTemplateProcessor, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gateway.getKnoxMaster()),
                        config("gateway_default_topology_name",
                            gateway.getTopologies().iterator().next().getTopologyName()),
                        config("gateway_knox_admin_groups", ""),
                        config("gateway_auto_discovery_enabled", "false"),
                        config("gateway_path", gateway.getPath()),
                        config("gateway_signing_keystore_name", "signing.jks"),
                        config("gateway_signing_keystore_type", "JKS"),
                        config("gateway_signing_key_alias", "signing-identity"),
                        config("gateway_dispatch_whitelist", "^*.*$"),
                        config("gateway_service_tokenstate_impl", "org.apache.knox.gateway.services.token.impl.JDBCTokenStateService"),
                        config("gateway_security_dir", GATEWAY_SECURITY_DIR)
                ),
                ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, cmTemplateProcessor, source))
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", cmTemplateProcessor, source));
    }

    @Test
    public void roleConfigsWithGatewayOn731() {
        ReflectionTestUtils.setField(underTest, "knoxGatewaySecurityDir", GATEWAY_SECURITY_DIR);
        ReflectionTestUtils.setField(underTest, "knoxIdBrokerSecurityDir", IDBROKER_SECURITY_DIR);

        GatewayTopology topology = new GatewayTopology();
        topology.setTopologyName("my-topology");
        topology.setExposedServices(Json.silent(new ExposedServices()));

        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        gateway.setTopologies(Set.of(topology));

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAccountId(Optional.of("1234"));

        IdBroker idBroker = new IdBroker();
        idBroker.setMasterSecret("supersecret");
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.3.1"));
        BlueprintView blueprintView = new BlueprintView("text", "7.3.1", "CDH", null, blueprintTextProcessor);
        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key", new HashSet<>())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withBlueprintView(blueprintView)
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, ""))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.4.2"), List.of(new ClouderaManagerProduct()
                        .withVersion("7.3.1")
                        .withName("CDH")))
                .withIdBroker(idBroker)
                .withEnableSecretEncryption(true)
                .build();
        when(virtualGroupService.createOrGetVirtualGroup(source.getVirtualGroupRequest(), UmsVirtualGroupRight.KNOX_ADMIN)).thenReturn("");
        when(entitlementService.isOjdbcTokenDhOneHour(anyString())).thenReturn(true);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessor.getServiceByType(anyString())).thenReturn(Optional.of(new ApiClusterTemplateService()));
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);

        assertEquals(
                List.of(
                        config("idbroker_master_secret", "supersecret"),
                        config("idbroker_gateway_knox_admin_groups", ""),
                        config("idbroker_gateway_signing_keystore_name", "signing.jks"),
                        config("idbroker_gateway_signing_keystore_type", "JKS"),
                        config("idbroker_gateway_signing_key_alias", "signing-identity"),
                        config("idbroker_security_dir", IDBROKER_SECURITY_DIR)
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, cmTemplateProcessor, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gateway.getKnoxMaster()),
                        config("gateway_default_topology_name",
                                gateway.getTopologies().iterator().next().getTopologyName()),
                        config("gateway_knox_admin_groups", ""),
                        config("gateway_auto_discovery_enabled", "false"),
                        config("gateway_path", gateway.getPath()),
                        config("gateway_signing_keystore_name", "signing.jks"),
                        config("gateway_signing_keystore_type", "JKS"),
                        config("gateway_signing_key_alias", "signing-identity"),
                        config("gateway_dispatch_whitelist", "^*.*$"),
                        config("gateway_service_tokenstate_impl", "org.apache.knox.gateway.services.token.impl.JDBCTokenStateService"),
                        config("gateway_security_dir", GATEWAY_SECURITY_DIR),
                        config("conf/gateway-site.xml_role_safety_valve",
                                "<property><name>gateway.servlet.async.supported</name><value>true</value></property>")
                ),
                ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, cmTemplateProcessor, source))
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", cmTemplateProcessor, source));
    }

    @Test
    public void roleConfigsWithGatewayWhenOdbcEntitlementFalse() {
        GatewayTopology topology = new GatewayTopology();
        topology.setTopologyName("my-topology");
        topology.setExposedServices(Json.silent(new ExposedServices()));

        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        gateway.setTopologies(Set.of(topology));

        IdBroker idBroker = new IdBroker();
        idBroker.setMasterSecret("supersecret");
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.15"));
        BlueprintView blueprintView = new BlueprintView("text", "7.2.11", "CDH", null, blueprintTextProcessor);

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAccountId(Optional.of("1234"));

        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key", new HashSet<>())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withBlueprintView(blueprintView)
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, ""))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.4.2"), List.of(new ClouderaManagerProduct()
                        .withVersion("7.2.10")
                        .withName("CDH")))
                .withIdBroker(idBroker)
                .withEnableSecretEncryption(true)
                .build();
        when(virtualGroupService.createOrGetVirtualGroup(source.getVirtualGroupRequest(), UmsVirtualGroupRight.KNOX_ADMIN)).thenReturn("");
        when(entitlementService.isOjdbcTokenDhOneHour(anyString())).thenReturn(false);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessor.getServiceByType(anyString())).thenReturn(Optional.of(new ApiClusterTemplateService()));
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        assertEquals(
                List.of(
                        config("idbroker_master_secret", "supersecret"),
                        config("idbroker_gateway_knox_admin_groups", ""),
                        config("idbroker_gateway_signing_keystore_name", "signing.jks"),
                        config("idbroker_gateway_signing_keystore_type", "JKS"),
                        config("idbroker_gateway_signing_key_alias", "signing-identity")
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, cmTemplateProcessor, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gateway.getKnoxMaster()),
                        config("gateway_default_topology_name",
                                gateway.getTopologies().iterator().next().getTopologyName()),
                        config("gateway_knox_admin_groups", ""),
                        config("gateway_auto_discovery_enabled", "false"),
                        config("gateway_path", gateway.getPath()),
                        config("gateway_signing_keystore_name", "signing.jks"),
                        config("gateway_signing_keystore_type", "JKS"),
                        config("gateway_signing_key_alias", "signing-identity"),
                        config("gateway_dispatch_whitelist", "^*.*$"),
                        config("gateway_token_generation_enable_lifespan_input", "true"),
                        config("gateway_token_generation_knox_token_ttl", "86400000"),
                        config("gateway_service_tokenstate_impl", "org.apache.knox.gateway.services.token.impl.JDBCTokenStateService")
                ),
                ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, cmTemplateProcessor, source))
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", cmTemplateProcessor, source));
    }

    @Test
    public void roleConfigsWithoutGateway() {
        GeneralClusterConfigs gcc = new GeneralClusterConfigs();
        gcc.setPassword("secret");
        gcc.setAccountId(Optional.of("1234"));
        IdBroker idBroker = new IdBroker();
        idBroker.setMasterSecret("supersecret");
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessor.getServiceByType(anyString())).thenReturn(Optional.of(new ApiClusterTemplateService()));
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        BlueprintView blueprintView = new BlueprintView("text", "7.2.11", "CDH", null, blueprintTextProcessor);
        TemplatePreparationObject source = Builder.builder()
                .withGeneralClusterConfigs(gcc)
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, ""))
                .withIdBroker(idBroker)
                .withBlueprintView(blueprintView)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.4.2"), List.of(new ClouderaManagerProduct()
                        .withVersion("7.2.10")
                        .withName("CDH")))
                .build();
        when(virtualGroupService.createOrGetVirtualGroup(source.getVirtualGroupRequest(), UmsVirtualGroupRight.KNOX_ADMIN)).thenReturn("");
        assertEquals(
                List.of(
                        config("idbroker_master_secret", "supersecret"),
                        config("idbroker_gateway_knox_admin_groups", ""),
                        config("idbroker_gateway_signing_keystore_name", "signing.jks"),
                        config("idbroker_gateway_signing_keystore_type", "JKS"),
                        config("idbroker_gateway_signing_key_alias", "signing-identity")
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, cmTemplateProcessor, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gcc.getPassword()),
                        config("gateway_default_topology_name", "cdp-proxy"),
                        config("gateway_knox_admin_groups", ""),
                        config("gateway_auto_discovery_enabled", "false"),
                        config("gateway_service_tokenstate_impl", "org.apache.knox.gateway.services.token.impl.JDBCTokenStateService")
                ),
                underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, cmTemplateProcessor, source)
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", cmTemplateProcessor, source));
    }

    @Test
    public void roleConfigsWithGatewayWithLdapConfig() {
        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        IdBroker idBroker = new IdBroker();
        idBroker.setMasterSecret("supersecret");
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);

        LdapView ldapConfig = LdapViewBuilder.aLdapView().build();
        BlueprintView blueprintView = new BlueprintView("text", "7.2.11", "CDH", null, blueprintTextProcessor);

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAccountId(Optional.of("1234"));

        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key", new HashSet<>())
                .withLdapConfig(ldapConfig)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withBlueprintView(blueprintView)
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, ""))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.4.2"), List.of(new ClouderaManagerProduct()
                        .withVersion("7.2.10")
                        .withName("CDH")))
                .withIdBroker(idBroker)
                .build();
        when(virtualGroupService.createOrGetVirtualGroup(source.getVirtualGroupRequest(), UmsVirtualGroupRight.KNOX_ADMIN)).thenReturn("knox_admins");
        when(entitlementService.isOjdbcTokenDhOneHour(anyString())).thenReturn(true);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessor.getServiceByType(anyString())).thenReturn(Optional.of(new ApiClusterTemplateService()));
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        assertEquals(
            List.of(
                config("idbroker_master_secret", "supersecret"),
                config("idbroker_gateway_knox_admin_groups", "knox_admins"),
                    config("idbroker_gateway_signing_keystore_name", "signing.jks"),
                    config("idbroker_gateway_signing_keystore_type", "JKS"),
                    config("idbroker_gateway_signing_key_alias", "signing-identity")
            ),
            underTest.getRoleConfigs(KnoxRoles.IDBROKER, cmTemplateProcessor, source)
        );
        assertEquals(
            List.of(
                config("gateway_master_secret", gateway.getKnoxMaster()),
                config("gateway_default_topology_name", "cdp-proxy"),
                config("gateway_knox_admin_groups", "knox_admins"),
                config("gateway_auto_discovery_enabled", "false"),
                config("gateway_path", gateway.getPath()),
                config("gateway_signing_keystore_name", "signing.jks"),
                config("gateway_signing_keystore_type", "JKS"),
                config("gateway_signing_key_alias", "signing-identity"),
                config("gateway_dispatch_whitelist", "^*.*$"),
                config("gateway_service_tokenstate_impl", "org.apache.knox.gateway.services.token.impl.JDBCTokenStateService")
            ),
            ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, cmTemplateProcessor, source))
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", cmTemplateProcessor, source));
    }

    @Test
    public void roleConfigsWithGatewayWithLdapConfigWhenOdbcEntitlementFalse() {
        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        IdBroker idBroker = new IdBroker();
        idBroker.setMasterSecret("supersecret");
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);

        LdapView ldapConfig = LdapViewBuilder.aLdapView().build();
        BlueprintView blueprintView = new BlueprintView("text", "7.2.11", "CDH", null, blueprintTextProcessor);

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAccountId(Optional.of("1234"));

        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key", new HashSet<>())
                .withLdapConfig(ldapConfig)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withBlueprintView(blueprintView)
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, ""))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.4.2"), List.of(new ClouderaManagerProduct()
                        .withVersion("7.2.10")
                        .withName("CDH")))
                .withIdBroker(idBroker)
                .build();
        when(virtualGroupService.createOrGetVirtualGroup(source.getVirtualGroupRequest(), UmsVirtualGroupRight.KNOX_ADMIN)).thenReturn("knox_admins");
        when(entitlementService.isOjdbcTokenDhOneHour(anyString())).thenReturn(false);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessor.getServiceByType(anyString())).thenReturn(Optional.of(new ApiClusterTemplateService()));
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        assertEquals(
                List.of(
                        config("idbroker_master_secret", "supersecret"),
                        config("idbroker_gateway_knox_admin_groups", "knox_admins"),
                        config("idbroker_gateway_signing_keystore_name", "signing.jks"),
                        config("idbroker_gateway_signing_keystore_type", "JKS"),
                        config("idbroker_gateway_signing_key_alias", "signing-identity")
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, cmTemplateProcessor, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gateway.getKnoxMaster()),
                        config("gateway_default_topology_name", "cdp-proxy"),
                        config("gateway_knox_admin_groups", "knox_admins"),
                        config("gateway_auto_discovery_enabled", "false"),
                        config("gateway_path", gateway.getPath()),
                        config("gateway_signing_keystore_name", "signing.jks"),
                        config("gateway_signing_keystore_type", "JKS"),
                        config("gateway_signing_key_alias", "signing-identity"),
                        config("gateway_dispatch_whitelist", "^*.*$"),
                        config("gateway_token_generation_enable_lifespan_input", "true"),
                        config("gateway_token_generation_knox_token_ttl", "86400000"),
                        config("gateway_service_tokenstate_impl", "org.apache.knox.gateway.services.token.impl.JDBCTokenStateService")
                ),
                ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, cmTemplateProcessor, source))
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", cmTemplateProcessor, source));
    }

    @Test
    public void roleConfigsWithGatewayOnGovCloud() {
        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        IdBroker idBroker = new IdBroker();
        idBroker.setMasterSecret("supersecret");
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);

        LdapView ldapConfig = LdapViewBuilder.aLdapView().build();
        BlueprintView blueprintView = new BlueprintView("text", "7.2.11", "CDH", null, blueprintTextProcessor);

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setGovCloud(true);
        generalClusterConfigs.setAccountId(Optional.of("1234"));

        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key", new HashSet<>())
                .withLdapConfig(ldapConfig)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withBlueprintView(blueprintView)
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, ""))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.4.2"), List.of(new ClouderaManagerProduct()
                        .withVersion("7.2.10")
                        .withName("CDH")))
                .withIdBroker(idBroker)
                .build();
        when(virtualGroupService.createOrGetVirtualGroup(source.getVirtualGroupRequest(), UmsVirtualGroupRight.KNOX_ADMIN)).thenReturn("knox_admins");
        when(entitlementService.isOjdbcTokenDhOneHour(anyString())).thenReturn(false);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessor.getServiceByType(anyString())).thenReturn(Optional.of(new ApiClusterTemplateService()));
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        assertEquals(
                List.of(
                        config("idbroker_master_secret", "supersecret"),
                        config("idbroker_gateway_knox_admin_groups", "knox_admins"),
                        config("idbroker_gateway_signing_keystore_name", "signing.bcfks"),
                        config("idbroker_gateway_signing_keystore_type", "BCFKS"),
                        config("idbroker_gateway_signing_key_alias", "signing-identity")
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, cmTemplateProcessor, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gateway.getKnoxMaster()),
                        config("gateway_default_topology_name", "cdp-proxy"),
                        config("gateway_knox_admin_groups", "knox_admins"),
                        config("gateway_auto_discovery_enabled", "false"),
                        config("gateway_path", gateway.getPath()),
                        config("gateway_signing_keystore_name", "signing.bcfks"),
                        config("gateway_signing_keystore_type", "BCFKS"),
                        config("gateway_signing_key_alias", "signing-identity"),
                        config("gateway_dispatch_whitelist", "^*.*$"),
                        config("gateway_token_generation_enable_lifespan_input", "true"),
                        config("gateway_token_generation_knox_token_ttl", "86400000"),
                        config("gateway_service_tokenstate_impl", "org.apache.knox.gateway.services.token.impl.JDBCTokenStateService")
                ),
                ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, cmTemplateProcessor, source))
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", cmTemplateProcessor, source));
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
        assertEquals(config("gateway_dispatch_whitelist", "^/.*$;^https?://([^/]+\\.example.com):[0-9]+/?.*$"),
                underTest.getGatewayWhitelistConfig(kerberosNoAutoTlsTPO));

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAutoTlsEnabled(true);
        TemplatePreparationObject kerberosAutoTlsTPO = Builder.builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withKerberosConfig(kerberosConfig)
                .build();
        assertEquals(config("gateway_dispatch_whitelist", "^/.*$;^https://([^/]+\\.example.com):[0-9]+/?.*$"),
                underTest.getGatewayWhitelistConfig(kerberosAutoTlsTPO));
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
