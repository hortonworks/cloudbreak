package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
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
        Blueprint blueprint = readBlueprint("input/cdp-data-mart-no-cardinality.bp");
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
        Blueprint blueprint = readBlueprint("input/cdp-data-mart.bp");
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
        Blueprint blueprint = readBlueprint("input/cdp-data-mart.bp");
        Set<HostGroup> hostGroups = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 3)
        );
        Collection<InstanceGroup> instanceGroups = hostGroups.stream()
                .map(HostGroup::getInstanceGroup)
                .collect(toSet());

        assertThrows(BlueprintValidationException.class, () -> subject
                .validate(blueprint, hostGroups, instanceGroups, true));
    }

    @Test
    public void testDownscaleValidationIfKafkaPresentedThenShouldThrowBadRequest() {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -1, List.of()));
    }

    @Test
    public void testDownscaleValidationIfKafkaPresentedAndEntitledForScalingThenValidationShouldReturnTrue() {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -1, List.of()));
    }

    @Test
    public void testUpscaleValidationIfKafkaPresentedThenValidationShouldThrowBadRequest() {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of()));
    }

    @Test
    public void testUpscaleValidationIfKafkaPresentedAndEntitledForScalingThenValidationShouldReturnTrue() {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of()));
    }

    @Test
    public void testValidationIfNifiPresentedAndDownScaleThenValidationShouldThrowException() {
        Blueprint blueprint = readBlueprint("input/nifi.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of()));
    }

    @Test
    public void testValidationIfNifi728PresentedAndUpScaleThenValidationShouldNotThrowBecauseTheBPVersionIsHigher() {
        Blueprint blueprint = readBlueprint("input/nifi_7_2_8.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.8");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertDoesNotThrow(() -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of()));
    }

    @Test
    public void testValidationIfNifi727PresentedAndUpScaleThenValidationShouldThrowBecauseTheBPVersionIsLower() {
        Blueprint blueprint = readBlueprint("input/nifi_7_2_7.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.7");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of()));
    }

    @Test
    public void testValidationIfNifi726PresentedAndUpScaleThenValidationShouldNotThrowBecauseTheBPVersionIsHigher() {
        Blueprint blueprint = readBlueprint("input/nifi_7_2_6.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.6");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of()));
    }

    @Test
    public void testValidationIfNifi728PresentedAndDownScaleThenValidationShouldThrowException() {
        Blueprint blueprint = readBlueprint("input/nifi_7_2_8.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.8");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of()));
    }

    @Test
    public void testValidationIfNifiPresentedAndDownScaleAndEntitledForScalingThenValidationShouldReturnTrue() {
        Blueprint blueprint = readBlueprint("input/nifi.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of()));
    }

    @Test
    public void testValidationIfNifiPresentedAndUpScaleThenValidationShouldThrowException() {
        Blueprint blueprint = readBlueprint("input/nifi.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of()));
    }

    @Test
    public void testValidationIfNifiPresentedAndUpScaleAndEntitledForScalingThenValidationShouldReturnTrue() {
        Blueprint blueprint = readBlueprint("input/nifi.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of()));
    }

    @Test
    public void testValidationIfKafkaUpgradedTo7211PresentedAndUpScaleThenValidationShouldThrowError() {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.11");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject
            .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, +1, List.of()));
    }

    @Test
    public void testValidationIfKafkaUpgradedTo7212PresentedAndUpScaleThenValidationShouldNOTThrowError() {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.12");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertDoesNotThrow(() -> subject
            .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, +1, List.of()));
    }

    @Test
    public void testValidationIfKafka7212PresentedAndDownScaleThenValidationShouldNOTThrowError() {
        Blueprint blueprint = readBlueprint("input/kafka-cc_7_2_12.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.12");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> subject
            .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of()));
    }

    @Test
    public void testValidationIfKafka7212WithoutCruiseControlPresentedAndDownScaleThenValidationShouldThrowError() {
        Blueprint blueprint = readBlueprint("input/kafka-no-cc_7_2_12.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.12");

        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of()));
    }

    @Test
    public void testValidationIfNodeManagerCountWillBeHigherThanZeroInOtherGroup() {
        Blueprint blueprint = readBlueprint("input/cdp-data-mart.bp");

        String hostGroup = "compute";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        InstanceGroup compute = new InstanceGroup();
        compute.setGroupName("compute");
        compute.setInstanceMetaData(Set.of(new InstanceMetaData(), new InstanceMetaData(), new InstanceMetaData()));
        InstanceGroup worker = new InstanceGroup();
        worker.setGroupName("worker");
        worker.setInstanceMetaData(Set.of(new InstanceMetaData()));

        subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint,
                Optional.of(clouderaManagerRepo), hostGroup, -2, Set.of(compute, worker));
    }

    @Test
    public void testValidationIfNodeManagerCountWillBeHigherThanZeroInTheSameGroup() {
        Blueprint blueprint = readBlueprint("input/cdp-data-mart.bp");

        String hostGroup = "compute";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        InstanceGroup compute = new InstanceGroup();
        compute.setGroupName("compute");
        compute.setInstanceMetaData(Set.of(new InstanceMetaData(), new InstanceMetaData(), new InstanceMetaData()));
        InstanceGroup worker = new InstanceGroup();
        worker.setGroupName("worker");
        worker.setInstanceMetaData(Set.of());

        subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint,
                Optional.of(clouderaManagerRepo), hostGroup, -2, Set.of(compute, worker));
    }

    @Test
    public void testValidationIfNodeManagerCountWillBeZero() {
        Blueprint blueprint = readBlueprint("input/cdp-data-mart.bp");

        String hostGroup = "compute";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        InstanceGroup compute = new InstanceGroup();
        compute.setGroupName("compute");
        compute.setInstanceMetaData(Set.of(new InstanceMetaData(), new InstanceMetaData(), new InstanceMetaData()));
        InstanceGroup worker = new InstanceGroup();
        worker.setGroupName("worker");
        worker.setInstanceMetaData(Set.of());

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
                subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -3, List.of(compute, worker)));
        assertEquals("Scaling adjustment is not allowed. NODEMANAGER role must be present on 1 host(s) but after the scaling operation 0 host(s) " +
                        "would have this role. Based on the template this role is present on the compute, worker host group(s).",
                badRequestException.getMessage());
    }

    private Blueprint readBlueprint(String file) {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly(file));
        return blueprint;
    }
}
