package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.DistroxEnabledInstanceTypes.GCP_ENABLED_TYPES_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.TagValidator;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
public class GcpPlatformParametersTest {

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Mock
    private GcpTagValidator gcpTagValidator;

    @Mock
    private TagSpecification tagSpecification;

    @InjectMocks
    private GcpPlatformParameters underTest;

    @Test
    public void testDiskTypes() {
        DiskTypes diskTypes = underTest.diskTypes();
        assertEquals(5, diskTypes.diskMapping().keySet().size());
    }

    @Test
    public void testTagValidator() {
        TagValidator tagValidator = underTest.tagValidator();
        assertEquals(tagValidator, gcpTagValidator);
    }

    @Test
    public void testScriptParams() {
        ScriptParams scriptParams = underTest.scriptParams();
        assertEquals(scriptParams.getStartLabel(), Integer.valueOf(97));
        assertEquals(scriptParams.getDiskPrefix(), "sd");
    }

    @Test
    public void testTagSpecification() {
        TagSpecification tagSpecification = underTest.tagSpecification();
        assertEquals(tagSpecification, tagSpecification);
    }

    @Test
    public void testPlatforName() {
        String platforName = underTest.platforName();
        assertEquals(GcpConstants.GCP_PLATFORM.value(), platforName);
    }

    @Test
    public void testIsAutoTlsSupported() {
        boolean autoTlsSupported = underTest.isAutoTlsSupported();
        assertEquals(true, autoTlsSupported);
    }

    @Test
    public void testAdditionalStackParameters() {
        List<StackParamValidation> stackParamValidations = underTest.additionalStackParameters();
        assertEquals(1, stackParamValidations.size());
    }

    @Test
    void testDistroxEnabledInstanceTypes() {
        List<String> expected = GCP_ENABLED_TYPES_LIST;
        Set<String> result = underTest.getDistroxEnabledInstanceTypes(Architecture.X86_64);
        Assertions.assertThat(result).hasSameElementsAs(new HashSet<>(expected));
    }

    @Test
    public void testOrchestratorParams() {
        PlatformOrchestrator platformOrchestrator = underTest.orchestratorParams();
        assertEquals(1, platformOrchestrator.types().size());
    }

    @Test
    public void testAddDiskSupported() {
        assertFalse(underTest.specialParameters().getSpecialParameters().get(PlatformParametersConsts.ADD_VOLUMES_SUPPORTED));
    }

}