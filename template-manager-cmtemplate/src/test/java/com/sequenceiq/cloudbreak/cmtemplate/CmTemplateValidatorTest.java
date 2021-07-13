package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidationException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RunWith(MockitoJUnitRunner.class)
public class CmTemplateValidatorTest {

    private static final String ACCOUNT_ID = "1";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CmTemplateValidator subject = new CmTemplateValidator();

    @Spy
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Injected by Mockito")
    private CmTemplateProcessorFactory templateProcessorFactory = new CmTemplateProcessorFactory();

    @Test
    public void validWithZeroComputeNodesWhenCardinalityUnspecified() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/cdp-data-mart-no-cardinality.bp"));
        Set<HostGroup> hostGroups = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 3),
                hostGroup("compute", 0)
        );
        Collection<InstanceGroup> instanceGroups = hostGroups.stream()
                .map(HostGroup::getInstanceGroup)
                .collect(toSet());
        subject.validate(blueprint, hostGroups, instanceGroups, true);
    }

    @Test
    public void validWithZeroComputeNodes() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/cdp-data-mart.bp"));
        Set<HostGroup> hostGroups = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 3),
                hostGroup("compute", 0)
        );
        Collection<InstanceGroup> instanceGroups = hostGroups.stream()
                .map(HostGroup::getInstanceGroup)
                .collect(toSet());
        subject.validate(blueprint, hostGroups, instanceGroups, true);
    }

    @Test
    public void invalidWithoutComputeHostGroup() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/cdp-data-mart.bp"));
        Set<HostGroup> hostGroups = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 3)
        );
        Collection<InstanceGroup> instanceGroups = hostGroups.stream()
                .map(HostGroup::getInstanceGroup)
                .collect(toSet());

        assertThrows(BlueprintValidationException.class, () -> subject.validate(blueprint, hostGroups, instanceGroups, true));
    }

    @Test
    public void testDownscaleValidationIfKafkaPresentedThenShouldThrowBadRequest() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/kafka.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("broker");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, -1));
    }

    @Test
    public void testDownscaleValidationIfKafkaPresentedAndEntitledForScalingThenValidationShouldReturnTrue() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/kafka.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("broker");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, -1));
    }

    @Test
    public void testUpscaleValidationIfKafkaPresentedThenValidationShouldThrowBadRequest() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/kafka.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("broker");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, 2));
    }

    @Test
    public void testUpscaleValidationIfKafkaPresentedAndEntitledForScalingThenValidationShouldReturnTrue() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/kafka.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("broker");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, 2));
    }

    @Test
    public void testValidationIfNifiPresentedAndDownScaleThenValidationShouldThrowException() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, -2));
    }

    @Test
    public void testValidationIfNifi728PresentedAndUpScaleThenValidationShouldNotThrowBecauseTheBPVersionIsHigher() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi_7_2_8.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertDoesNotThrow(() -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, 2));
    }

    @Test
    public void testValidationIfNifi727PresentedAndUpScaleThenValidationShouldThrowBecauseTheBPVersionIsLower() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi_7_2_7.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, 2));
    }

    @Test
    public void testValidationIfNifi726PresentedAndUpScaleThenValidationShouldNotThrowBecauseTheBPVersionIsHigher() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi_7_2_6.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, 2));
    }

    @Test
    public void testValidationIfNifi728PresentedAndDownScaleThenValidationShouldThrowException() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi_7_2_8.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, -2));
    }

    @Test
    public void testValidationIfNifiPresentedAndDownScaleAndEntitledForScalingThenValidationShouldReturnTrue() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, -2));
    }

    @Test
    public void testValidationIfNifiPresentedAndUpScaleThenValidationShouldThrowException() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, 2));
    }

    @Test
    public void testValidationIfNifiPresentedAndUpScaleAndEntitledForScalingThenValidationShouldReturnTrue() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, 2));
    }

    @Test
    public void testValidationIfKafka7212PresentedAndDownScaleThenValidationShouldNOTThrowError() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/kafka-cc_7_2_12.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("broker");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, -2));
    }

    @Test
    public void testValidationIfKafka7212WithoutCruiseControlPresentedAndDownScaleThenValidationShouldThrowError() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/kafka-no-cc_7_2_12.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("broker");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, hostGroup, -2));
    }

}
