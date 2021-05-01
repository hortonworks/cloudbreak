package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.TagValidator;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

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
    public void testTagValidator() {
        TagValidator tagValidator = underTest.tagValidator();
        Assert.assertEquals(tagValidator, gcpTagValidator);
    }

    @Test
    public void testScriptParams() {
        ScriptParams scriptParams = underTest.scriptParams();
        Assert.assertEquals(scriptParams.getStartLabel(), Integer.valueOf(97));
        Assert.assertEquals(scriptParams.getDiskPrefix(), "sd");
    }

    @Test
    public void testTagSpecification() {
        TagSpecification tagSpecification = underTest.tagSpecification();
        Assert.assertEquals(tagSpecification, tagSpecification);
    }

    @Test
    public void testPlatforName() {
        String platforName = underTest.platforName();
        Assert.assertEquals(GcpConstants.GCP_PLATFORM.value(), platforName);
    }

    @Test
    public void testIsAutoTlsSupported() {
        boolean autoTlsSupported = underTest.isAutoTlsSupported();
        Assert.assertEquals(true, autoTlsSupported);
    }

    @Test
    public void testAdditionalStackParameters() {
        List<StackParamValidation> stackParamValidations = underTest.additionalStackParameters();
        Assert.assertEquals(1, stackParamValidations.size());
    }

    @Test
    public void testOrchestratorParams() {
        PlatformOrchestrator platformOrchestrator = underTest.orchestratorParams();
        Assert.assertEquals(1, platformOrchestrator.types().size());
    }

}