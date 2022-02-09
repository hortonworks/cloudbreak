package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.junit.Assert.assertTrue;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HbaseServiceConfigProviderTest extends AbstractHbaseConfigProviderTest {
    private static final String MOCK_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    private static final String MOCK_HBASE_VIRTUAL_ADMIN_GROUP = "_c_hbase_admin";

    private static final String RANGER_HBASE_ADMIN_VIRTUAL_GROUPS = "ranger.hbase.default.admin.groups";

    @InjectMocks
    HbaseServiceConfigProvider underTest;

    @Mock
    VirtualGroupService virtualGroupService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testValidateServiceType() {
        assertTrue(underTest.getServiceType().equals(HbaseRoles.HBASE));
    }

    @Test
    public void testVirtualHbaseAdminGroupConfigured() {
        // Test that valid configuration is returned when an appropriate CM version is detected
        String blueprintText = getBlueprintText("input/cdp-opdb.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(blueprintText);
        cmTemplateProcessor.setCmVersion("7.6.0");
        VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(MOCK_CRN, MOCK_HBASE_VIRTUAL_ADMIN_GROUP);
        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder.builder()
            .withVirtualGroupView(virtualGroupRequest)
            .build();
        doReturn(MOCK_HBASE_VIRTUAL_ADMIN_GROUP)
            .when(virtualGroupService).getVirtualGroup(virtualGroupRequest, UmsRight.HBASE_ADMIN.getRight());
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, templatePreparationObject);
        assertEquals("Unexpected number of configurations returned: ", 2, serviceConfigs.size());
        assertEquals("Unexpected group(s) returned: ", RANGER_HBASE_ADMIN_VIRTUAL_GROUPS, serviceConfigs.get(1).getName());
        assertEquals("Unexpected virtual admin group(s) returned: ", MOCK_HBASE_VIRTUAL_ADMIN_GROUP, serviceConfigs.get(1).getValue());
    }

    @Test
    public void testVirtualHbaseAdminGroupNotConfigured() {
        // Test that no configurations are returned for an unknown CM version
        String blueprintText = getBlueprintText("input/cdp-opdb.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(blueprintText);
        VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(MOCK_CRN, MOCK_HBASE_VIRTUAL_ADMIN_GROUP);
        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder.builder()
            .withVirtualGroupView(virtualGroupRequest)
            .build();
        doReturn(MOCK_HBASE_VIRTUAL_ADMIN_GROUP)
            .when(virtualGroupService).getVirtualGroup(virtualGroupRequest, UmsRight.HBASE_ADMIN.getRight());
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, templatePreparationObject);
        assertEquals("Unexpexted number of configurations returned: ", 0, serviceConfigs.size());
    }
}
