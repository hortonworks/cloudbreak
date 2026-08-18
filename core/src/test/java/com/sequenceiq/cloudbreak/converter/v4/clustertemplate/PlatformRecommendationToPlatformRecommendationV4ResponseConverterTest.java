package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.DiskV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AutoscaleRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.GatewayRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.ResizeRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.converter.v4.connectors.VmTypeToVmTypeV4ResponseConverter;

@ExtendWith(MockitoExtension.class)
class PlatformRecommendationToPlatformRecommendationV4ResponseConverterTest {

    private static final String MASTER = "master";

    @Mock
    private VmTypeToVmTypeV4ResponseConverter vmTypeToVmTypeV4ResponseConverter;

    @InjectMocks
    private PlatformRecommendationToPlatformRecommendationV4ResponseConverter underTest;

    @Test
    void testConvertIncludesDeprecatedVmTypes() {
        VmType recommendationVmType = VmType.vmType("r5.2xlarge");
        VmType activeVmType = VmType.vmType("r5.4xlarge");
        VmType deprecatedVmType = VmType.vmType("r4.4xlarge");

        VmTypeV4Response recommendationResponse = vmTypeResponse("r5.2xlarge");
        VmTypeV4Response activeResponse = vmTypeResponse("r5.4xlarge");
        VmTypeV4Response deprecatedResponse = vmTypeResponse("r4.4xlarge");

        when(vmTypeToVmTypeV4ResponseConverter.convert(recommendationVmType)).thenReturn(recommendationResponse);
        when(vmTypeToVmTypeV4ResponseConverter.convert(activeVmType)).thenReturn(activeResponse);
        when(vmTypeToVmTypeV4ResponseConverter.convert(deprecatedVmType)).thenReturn(deprecatedResponse);

        PlatformRecommendation source = createRecommendation(recommendationVmType, Set.of(activeVmType), Set.of(deprecatedVmType),
                new DiskTypes(Set.of(), null, Map.of(), Map.of()));

        RecommendationV4Response result = underTest.convert(source);

        assertEquals(Set.of(activeResponse), result.getVirtualMachines());
        assertEquals(Set.of(deprecatedResponse), result.getDeprecatedVirtualMachines());
        assertEquals(recommendationResponse, result.getRecommendations().get(MASTER));
        assertEquals(1, result.getInstanceCounts().get(MASTER).getMinimumCount());
        assertEquals(1, result.getInstanceCounts().get(MASTER).getMaximumCount());
        assertEquals(Set.of(MASTER), result.getGatewayRecommendation().getHostGroups());
        assertEquals(Set.of(MASTER), result.getAutoscaleRecommendation().getTimeBasedHostGroups());
        assertEquals(Set.of(MASTER), result.getAutoscaleRecommendation().getLoadBasedHostGroups());
        assertEquals(Set.of(MASTER), result.getResizeRecommendation().getScaleUpHostGroups());
        assertEquals(Set.of(MASTER), result.getResizeRecommendation().getScaleDownHostGroups());
        assertTrue(result.getDiskResponses().isEmpty());
        assertNotNull(result.getDeprecatedVirtualMachines());

        verify(vmTypeToVmTypeV4ResponseConverter).convert(deprecatedVmType);
        verify(vmTypeToVmTypeV4ResponseConverter, times(3)).convert(any(VmType.class));
    }

    @Test
    void testConvertHandlesEmptyDeprecatedVmTypes() {
        VmType recommendationVmType = VmType.vmType("m6i.xlarge");
        VmType activeVmType = VmType.vmType("m6i.2xlarge");
        VmTypeV4Response recommendationResponse = vmTypeResponse("m6i.xlarge");
        VmTypeV4Response activeResponse = vmTypeResponse("m6i.2xlarge");

        when(vmTypeToVmTypeV4ResponseConverter.convert(recommendationVmType)).thenReturn(recommendationResponse);
        when(vmTypeToVmTypeV4ResponseConverter.convert(activeVmType)).thenReturn(activeResponse);

        PlatformRecommendation source = createRecommendation(recommendationVmType, Set.of(activeVmType), Set.of(),
                new DiskTypes(Set.of(), null, Map.of(), Map.of()));

        RecommendationV4Response result = underTest.convert(source);

        assertEquals(Set.of(activeResponse), result.getVirtualMachines());
        assertTrue(result.getDeprecatedVirtualMachines().isEmpty());
        verify(vmTypeToVmTypeV4ResponseConverter, times(2)).convert(any(VmType.class));
    }

    @Test
    void testConvertAddsDiskResponseForMatchingMapping() {
        VmType recommendationVmType = VmType.vmType("c6i.2xlarge");
        VmTypeV4Response recommendationResponse = vmTypeResponse("c6i.2xlarge");
        when(vmTypeToVmTypeV4ResponseConverter.convert(recommendationVmType)).thenReturn(recommendationResponse);

        DiskType diskType = DiskType.diskType("gp3");
        DiskTypes diskTypes = new DiskTypes(
                Set.of(diskType),
                diskType,
                Map.of("gp3", VolumeParameterType.SSD),
                Map.of(diskType, DisplayName.displayName("General Purpose SSD")));

        PlatformRecommendation source = createRecommendation(recommendationVmType, Set.of(), Set.of(), diskTypes);

        RecommendationV4Response result = underTest.convert(source);

        assertEquals(1, result.getDiskResponses().size());
        DiskV4Response diskResponse = result.getDiskResponses().iterator().next();
        assertEquals("gp3", diskResponse.getName());
        assertEquals("SSD", diskResponse.getType());
        assertEquals("General Purpose SSD", diskResponse.getDisplayName());
    }

    private PlatformRecommendation createRecommendation(VmType recommendationVmType, Set<VmType> activeVmTypes, Set<VmType> deprecatedVmTypes,
            DiskTypes diskTypes) {
        return new PlatformRecommendation(
                Map.of(MASTER, recommendationVmType),
                activeVmTypes,
                deprecatedVmTypes,
                diskTypes,
                Map.of(MASTER, InstanceCount.EXACTLY_ONE),
                new GatewayRecommendation(Set.of(MASTER)),
                new AutoscaleRecommendation(Set.of(MASTER), Set.of(MASTER)),
                new ResizeRecommendation(Set.of(MASTER), Set.of(MASTER)));
    }

    private VmTypeV4Response vmTypeResponse(String value) {
        VmTypeV4Response response = new VmTypeV4Response();
        response.setValue(value);
        return response;
    }
}
