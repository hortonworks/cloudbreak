package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
