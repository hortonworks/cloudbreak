package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
public class VmAdvisorTest {

    private static final String VERSION_7_2_1 = "7.2.1";

    private static final String VERSION_7_3_1 = "7.3.1";

    @Mock
    private BlueprintTextProcessor blueprintTextProcessor;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private CloudParameterService cloudParameterService;

    @InjectMocks
    private VmAdvisor underTest;

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @Test
    public void testRecommendVMTypesWhenStackVersionGreaterThanEqualTo731AndArm() {
        when(blueprintTextProcessor.getVersion()).thenReturn(java.util.Optional.of(VERSION_7_3_1));
        Blueprint blueprint = createBlueprint();
        CloudVmTypes cloudVmTypes = new CloudVmTypes(Map.of("vmtypes", Set.of(vmType("vmtype"))), Map.of("default", vmType("vmtype")));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(cloudVmTypes);
        when(extendedCloudCredentialConverter.convert(any())).thenReturn(extendedCloudCredential);

        CloudVmTypes result = underTest.recommendVmTypes(blueprintTextProcessor, null, null, null, null,
                Architecture.ARM64);

        assertTrue(!result.getCloudVmResponses().isEmpty());
        assertTrue(!result.getDefaultCloudVmResponses().isEmpty());
    }

    @Test
    public void testRecommendVMTypesWhenStackVersionLesserThan731AndArm() {
        when(blueprintTextProcessor.getVersion()).thenReturn(java.util.Optional.of(VERSION_7_2_1));
        Blueprint blueprint = createBlueprint();
        CloudVmTypes cloudVmTypes = new CloudVmTypes(Map.of("vmtypes", Set.of(vmType("vmtype"))), Map.of("default", vmType("vmtype")));

        CloudVmTypes result = underTest.recommendVmTypes(blueprintTextProcessor, "", "", null, null, Architecture.ARM64);

        assertTrue(result.getCloudVmResponses().isEmpty());
        assertTrue(result.getDefaultCloudVmResponses().isEmpty());
    }

    @Test
    public void testRecommendVMTypesWhenStackVersionLesserThan731AndNotArm() {
        when(blueprintTextProcessor.getVersion()).thenReturn(java.util.Optional.of(VERSION_7_2_1));
        Blueprint blueprint = createBlueprint();
        CloudVmTypes cloudVmTypes = new CloudVmTypes(Map.of("vmtypes", Set.of(vmType("vmtype"))), Map.of("default", vmType("vmtype")));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(cloudVmTypes);

        CloudVmTypes result = underTest.recommendVmTypes(blueprintTextProcessor, "", "", null, null, Architecture.X86_64);

        assertTrue(!result.getCloudVmResponses().isEmpty());
        assertTrue(!result.getDefaultCloudVmResponses().isEmpty());
    }

    private Blueprint createBlueprint() {
        Blueprint blueprint = new Blueprint();
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        blueprint.setWorkspace(workspace);
        blueprint.setBlueprintText("{\"Blueprints\":{123:2}}");
        return blueprint;
    }

}