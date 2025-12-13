package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class HbaseRestKnoxServiceConfigProviderTest extends AbstractHbaseConfigProviderTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @InjectMocks
    private final HbaseRestKnoxServiceConfigProvider underTest = new HbaseRestKnoxServiceConfigProvider();

    @Mock
    private EntitlementService entitlementService;

    @Test
    void getServiceConfigsWhenWireEncryptionEnabled() {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);

        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .withStackType(StackType.DATALAKE)
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(true);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
            assertEquals(2, serviceConfigs.size());
            assertEquals("hbase_rpc_protection", serviceConfigs.get(1).getName());
            assertEquals("privacy", serviceConfigs.get(1).getValue());
        });
    }

    @Test
    void getServiceConfigsWhenWireEncryptionNotEnabled() {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);

        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/namenode-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .withStackType(StackType.DATALAKE)
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(false);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
            assertEquals(1, serviceConfigs.size());
        });
    }
}