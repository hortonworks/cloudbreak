package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidationException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(MockitoExtension.class)
public class CmTemplateValidatorTest {

    private static final String ACCOUNT_ID = "1";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CmTemplateValidator subject = new CmTemplateValidator();

    @Spy
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Injected by Mockito")
    private CmTemplateProcessorFactory templateProcessorFactory = new CmTemplateProcessorFactory();

    private static Stream<Arguments> blackListValueTest() {
        return Stream.of(
                Arguments.of("", false),
                Arguments.of("7.2.11", false),
                Arguments.of("7.1.12", false),
                Arguments.of("7.2.12", true),
                Arguments.of("7.2.13", true),
                Arguments.of("7.2.16", true)
        );
    }

    @Test
    public void validWithZeroComputeNodesWhenCardinalityUnspecified() {
        Blueprint blueprint = readBlueprint("input/cdp-data-mart-no-cardinality.bp");
        Set<HostGroup> hostGroups = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 3),
                hostGroup("compute", 0)
        );
        Collection<InstanceGroupView> instanceGroups = hostGroups.stream()
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
        Collection<InstanceGroupView> instanceGroups = hostGroups.stream()
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
        Collection<InstanceGroupView> instanceGroups = hostGroups.stream()
                .map(HostGroup::getInstanceGroup)
                .collect(toSet());

        assertThrows(BlueprintValidationException.class, () -> subject
                .validate(blueprint, hostGroups, instanceGroups, true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testDownscaleValidationIfKafkaPresentedThenShouldThrowBadRequest(boolean forced) {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        if (forced) {
            assertDoesNotThrow(() -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -1, List.of(), true));
        } else {
            assertThrows(BadRequestException.class, () -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -1, List.of(), false));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testDownscaleValidationIfInstanceGroupAdjustmentsAndKafkaPresentedThenShouldThrowBadRequest(boolean forced) {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        if (forced) {
            assertDoesNotThrow(() -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Map.of(hostGroup, -1), Optional.of(clouderaManagerRepo), List.of(), true));
        } else {
            assertThrows(BadRequestException.class, () -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Map.of(hostGroup, -1), Optional.of(clouderaManagerRepo), List.of(), false));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testUpscaleValidationIfKafkaPresentedThenValidationShouldThrowBadRequest(boolean forced) {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        if (forced) {
            assertDoesNotThrow(() -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of(), true));
        } else {
            assertThrows(BadRequestException.class, () -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of(), false));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testValidationIfNifiPresentedAndDownScaleThenValidationShouldThrowException(boolean forced) {
        Blueprint blueprint = readBlueprint("input/nifi.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        if (forced) {
            assertDoesNotThrow(() -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of(), true));
        } else {
            assertThrows(BadRequestException.class, () -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of(), false));
        }
    }

    @Test
    public void testValidationIfNifi728PresentedAndUpScaleThenValidationShouldNotThrowBecauseTheBPVersionIsHigher() {
        Blueprint blueprint = readBlueprint("input/nifi_7_2_8.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.8");

        assertDoesNotThrow(() -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of(), false));
    }

    @Test
    public void testValidationIfNifi727PresentedAndUpScaleThenValidationShouldThrowBecauseTheBPVersionIsLower() {
        Blueprint blueprint = readBlueprint("input/nifi_7_2_7.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.7");
        assertThrows(BadRequestException.class, () -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of(), false));
    }

    @Test
    public void testValidationIfNifi726PresentedAndUpScaleThenValidationShouldThrowBecauseTheBPVersionIsLower() {
        Blueprint blueprint = readBlueprint("input/nifi_7_2_6.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.6");

        assertThrows(BadRequestException.class, () -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of(), false));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testValidationIfNifiPresentedAndUpScaleThenValidationShouldThrowException(boolean forced) {
        Blueprint blueprint = readBlueprint("input/nifi.bp");

        String hostGroup = "master";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.0.0");

        if (forced) {
            assertDoesNotThrow(() -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of(), true));
        } else {
            assertThrows(BadRequestException.class, () -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, 2, List.of(), false));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testValidationIfKafkaUpgradedTo7211PresentedAndUpScaleThenValidationShouldThrowError(boolean forced) {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.11");

        if (forced) {
            assertDoesNotThrow(() -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, +1, List.of(), true));
        } else {
            assertThrows(BadRequestException.class, () -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, +1, List.of(), false));
        }
    }

    @Test
    public void testValidationIfKafkaUpgradedTo7212PresentedAndUpScaleThenValidationShouldNOTThrowError() {
        Blueprint blueprint = readBlueprint("input/kafka.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.12");

        assertDoesNotThrow(() -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, +1, List.of(), false));
    }

    @Test
    public void testValidationIfKafka7212PresentedAndDownScaleThenValidationShouldNOTThrowError() {
        Blueprint blueprint = readBlueprint("input/kafka-cc_7_2_12.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.12");
        assertDoesNotThrow(() -> subject
                .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of(), false));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testValidationIfKafka7212WithoutCruiseControlPresentedAndDownScaleThenValidationShouldThrowError(boolean forced) {
        Blueprint blueprint = readBlueprint("input/kafka-no-cc_7_2_12.bp");

        String hostGroup = "broker";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion("7.2.12");

        if (forced) {
            assertDoesNotThrow(() -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of(), true));
        } else {
            assertThrows(BadRequestException.class, () -> subject
                    .validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -2, List.of(), false));
        }
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
                Optional.of(clouderaManagerRepo), hostGroup, -2, Set.of(compute, worker), false);
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
                Optional.of(clouderaManagerRepo), hostGroup, -2, Set.of(compute, worker), false);
    }

    @ParameterizedTest
    @MethodSource("blackListValueTest")
    public void checkKafkaBrokerBlackListedUpscaleShouldDetermined(String version, boolean allowUpscale) {
        String hostGroup = "compute";
        ClouderaManagerProduct clouderaManagerRepo = new ClouderaManagerProduct();
        clouderaManagerRepo.setVersion(version);
        CmTemplateProcessor processor = mock(CmTemplateProcessor.class);
        Map<String, Set<String>> componentsByHostGroup = new HashMap<>();
        Set<String> components = new HashSet<>();
        components.add("KAFKA_BROKER");
        componentsByHostGroup.put("compute", components);
        Mockito.lenient().when(processor.getComponentsByHostGroup()).thenReturn(componentsByHostGroup);
        Optional<ClouderaManagerProduct> product = Optional.of(clouderaManagerRepo);
        if (!allowUpscale) {
            Throwable exception = assertThrows(BadRequestException.class,
                    () -> subject.validateBlackListedScalingRoles(ACCOUNT_ID, processor, hostGroup, 1, product));
            assertEquals("'KAFKA_BROKER' service is not enabled to scaling up until CDP 7.2.12", exception.getMessage());
        } else {
            subject.validateBlackListedScalingRoles(ACCOUNT_ID, processor, hostGroup, 1, product);
        }
    }

    @ParameterizedTest
    @MethodSource("blackListValueTest")
    public void isVersionEnablesScaling(String versionParam, boolean allowedUpscale) {
        assertEquals(allowedUpscale, subject.isVersionEnablesScaling(() -> versionParam, BlackListedUpScaleRole.KAFKA_BROKER));
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
                subject.validateHostGroupScalingRequest(ACCOUNT_ID, blueprint, Optional.of(clouderaManagerRepo), hostGroup, -3, List.of(compute, worker),
                        false));
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
