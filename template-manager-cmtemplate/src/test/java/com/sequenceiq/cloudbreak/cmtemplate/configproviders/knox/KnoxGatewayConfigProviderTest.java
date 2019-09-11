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
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class KnoxGatewayConfigProviderTest {

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
        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key")
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .build();

        assertEquals(
                List.of(
                        config("idbroker_master_secret", gateway.getKnoxMasterSecret())
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gateway.getKnoxMasterSecret()),
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
    public void roleConfigsWithGatewayAndUserFacingCertificationGeneratedAndAutoTlsIsDisabled() {
        final String gatewaySiteCustomUserFacingCertConfigValue = "<property><name>gateway.tls.keystore.path</name>"
                + "<value>/var/lib/knox/cloudbreak_resources/security/keystores/userfacing.jks</value>"
                + "</property><property><name>gateway.tls.key.alias</name><value>userfacing-identity</value></property>";
        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setKnoxUserFacingCertConfigured(Boolean.TRUE);
        generalClusterConfigs.setAutoTlsEnabled(Boolean.FALSE);
        TemplatePreparationObject source = Builder.builder()
                .withGateway(gateway, "key")
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        assertEquals(
                List.of(
                        config("gateway_master_secret", gateway.getKnoxMasterSecret()),
                        config("gateway_path", gateway.getPath()),
                        config("gateway_signing_keystore_name", "signing.jks"),
                        config("gateway_signing_keystore_type", "JKS"),
                        config("gateway_signing_key_alias", "signing-identity"),
                        config("gateway_dispatch_whitelist", "^*.*$"),
                        config("conf/gateway-site.xml_role_safety_valve", gatewaySiteCustomUserFacingCertConfigValue)
                ),
                underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, source)
        );
    }

    @Test
    public void roleConfigsWithoutGateway() {
        GeneralClusterConfigs gcc = new GeneralClusterConfigs();
        gcc.setPassword("secret");
        TemplatePreparationObject source = Builder.builder().withGeneralClusterConfigs(gcc).build();

        assertEquals(
                List.of(
                        config("idbroker_master_secret", gcc.getPassword())
                ),
                underTest.getRoleConfigs(KnoxRoles.IDBROKER, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gcc.getPassword())
                ),
                underTest.getRoleConfigs(KnoxRoles.KNOX_GATEWAY, source)
        );
        assertEquals(List.of(), underTest.getRoleConfigs("NAMENODE", source));
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
