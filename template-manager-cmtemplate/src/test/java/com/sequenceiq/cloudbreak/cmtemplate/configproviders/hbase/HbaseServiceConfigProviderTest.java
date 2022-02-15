package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static org.mockito.Mockito.doReturn;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HbaseServiceConfigProviderTest extends AbstractHbaseConfigProviderTest {
    private static final String MOCK_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    private static final String MOCK_HBASE_VIRTUAL_ADMIN_GROUP = "_c_hbase_admin";

    private static final String RANGER_HBASE_ADMIN_VIRTUAL_GROUPS = "ranger.hbase.default.admin.groups";

    private static final String SPNEGO_ADMIN_GROUP = "hbase_security_authentication_spnego_admin_groups";

    @InjectMocks
    private HbaseServiceConfigProvider underTest;

    @Mock
    private VirtualGroupService virtualGroupService;

    @Test
    @DisplayName("Test appropriate service type is initialized")
    public void testValidateServiceType() {
        Assertions.assertTrue(underTest.getServiceType().equals(HbaseRoles.HBASE), "Incorrect service type");
    }

    @Test
    @DisplayName("Test proper configuration properties are returned for a given version")
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
        Assertions.assertEquals(2, serviceConfigs.size(), "Unexpected number of configurations returned");
        Assertions.assertEquals(SPNEGO_ADMIN_GROUP, serviceConfigs.get(0).getName(), "Unexpected configuration returned");
        Assertions.assertEquals(MOCK_HBASE_VIRTUAL_ADMIN_GROUP, serviceConfigs.get(0).getValue(), "Unexpected group returned");
        Assertions.assertEquals(RANGER_HBASE_ADMIN_VIRTUAL_GROUPS, serviceConfigs.get(1).getName(), "Unexpected configuration returned");
        Assertions.assertEquals(MOCK_HBASE_VIRTUAL_ADMIN_GROUP, serviceConfigs.get(1).getValue(), "Unexpected group returned");
    }

    @Test
    @DisplayName("Test no configuration properties are returned if no CM version is given")
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
        Assertions.assertEquals(0, serviceConfigs.size(), "Unexpected number of configurations returned");
    }
}
