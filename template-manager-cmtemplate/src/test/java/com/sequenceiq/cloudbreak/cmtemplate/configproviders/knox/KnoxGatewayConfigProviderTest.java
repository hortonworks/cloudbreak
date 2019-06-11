package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
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
        HostgroupView any = mock(HostgroupView.class);
        Gateway gateway = new Gateway();
        gateway.setKnoxMasterSecret("admin");
        gateway.setPath("/a/b/c");
        TemplatePreparationObject source = Builder.builder().withGateway(gateway, "key").build();

        assertEquals(
                List.of(
                        config("idbroker_master_secret", gateway.getKnoxMasterSecret())
                ),
                underTest.getRoleConfig(KnoxRoles.IDBROKER, any, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gateway.getKnoxMasterSecret()),
                        config("gateway_path", gateway.getPath())
                ),
                underTest.getRoleConfig(KnoxRoles.KNOX_GATEWAY, any, source)
        );
        assertEquals(List.of(), underTest.getRoleConfig("NAMENODE", any, source));
    }

    @Test
    public void roleConfigsWithoutGateway() {
        HostgroupView any = mock(HostgroupView.class);
        GeneralClusterConfigs gcc = new GeneralClusterConfigs();
        gcc.setPassword("secret");
        TemplatePreparationObject source = Builder.builder().withGeneralClusterConfigs(gcc).build();

        assertEquals(
                List.of(
                        config("idbroker_master_secret", gcc.getPassword())
                ),
                underTest.getRoleConfig(KnoxRoles.IDBROKER, any, source)
        );
        assertEquals(
                List.of(
                        config("gateway_master_secret", gcc.getPassword())
                ),
                underTest.getRoleConfig(KnoxRoles.KNOX_GATEWAY, any, source)
        );
        assertEquals(List.of(), underTest.getRoleConfig("NAMENODE", any, source));
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
