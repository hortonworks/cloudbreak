package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpEnabledInstanceTypes.GCP_ENABLED_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.TagValidator;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpDiskUtil;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
class GcpPlatformParametersTest {
    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Mock
    private GcpTagValidator gcpTagValidator;

    @Mock
    private TagSpecification tagSpecification;

    @Mock
    private GcpDiskUtil gcpDiskUtil;

    @InjectMocks
    private GcpPlatformParameters underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "defaultDiskType", GcpPlatformParameters.GcpDiskType.BALANCED.value());
        ReflectionTestUtils.setField(underTest, "defaultRootDiskType", GcpPlatformParameters.GcpDiskType.SSD.value());
    }

    @Test
    void testDiskTypes() {
        DiskTypes diskTypes = underTest.diskTypes();
        assertEquals(8, diskTypes.diskMapping().keySet().size());
    }

    @Test
    void testTagValidator() {
        TagValidator tagValidator = underTest.tagValidator();
        assertEquals(tagValidator, gcpTagValidator);
    }

    @Test
    void testScriptParams() {
        ScriptParams scriptParams = underTest.scriptParams();
        assertEquals(scriptParams.getStartLabel(), Integer.valueOf(97));
        assertEquals(scriptParams.getDiskPrefix(), "sd");
    }

    @Test
    void testTagSpecification() {
        TagSpecification tagSpecification = underTest.tagSpecification();
        assertEquals(tagSpecification, tagSpecification);
    }

    @Test
    void testPlatforName() {
        String platforName = underTest.platforName();
        assertEquals(GcpConstants.GCP_PLATFORM.value(), platforName);
    }

    @Test
    void testIsAutoTlsSupported() {
        boolean autoTlsSupported = underTest.isAutoTlsSupported();
        assertEquals(true, autoTlsSupported);
    }

    @Test
    void testAdditionalStackParameters() {
        List<StackParamValidation> stackParamValidations = underTest.additionalStackParameters();
        assertEquals(1, stackParamValidations.size());
    }

    @Test
    void testDistroxEnabledInstanceTypes() {
        List<String> expected = GCP_ENABLED_TYPES_LIST;
        Set<String> result = underTest.getDistroxEnabledInstanceTypes(Architecture.X86_64);
        assertThat(result).hasSameElementsAs(new HashSet<>(expected));
    }

    @Test
    void testOrchestratorParams() {
        PlatformOrchestrator platformOrchestrator = underTest.orchestratorParams();
        assertEquals(1, platformOrchestrator.types().size());
    }

    @Test
    void testAddDiskSupported() {
        assertFalse(underTest.specialParameters().getSpecialParameters().get(PlatformParametersConsts.ADD_VOLUMES_SUPPORTED));
    }

    @Test
    void testDefaultDiskTypes() {
        assertEquals(diskType(GcpPlatformParameters.GcpDiskType.BALANCED.value()), underTest.defaultDiskType());
        assertEquals(diskType(GcpPlatformParameters.GcpDiskType.SSD.value()), underTest.defaultRootDiskType(null));
    }
}
