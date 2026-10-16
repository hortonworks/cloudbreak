package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

class HyperVGenerationCompatibilityUtilTest {

    private static final Map<String, String> SUPPORTED_GENERATIONS = Map.of(
            "Standard_D8s_v5", "V1,V2",
            "Standard_E8ds_v5", "V1,V2",
            "Standard_D4_v2", "V1",
            "Standard_D8s_v3", "V1",
            "Standard_Gen2Only", "V2");

    private static final Function<String, Optional<String>> LOOKUP = instanceType -> Optional.ofNullable(SUPPORTED_GENERATIONS.get(instanceType));

    @Test
    void testValidateWhenAllInstanceTypesSupportTheGenerationThenPasses() {
        StackV4Request request = request(group("master", "Standard_D8s_v5"), group("worker", "Standard_E8ds_v5"));

        assertDoesNotThrow(() -> HyperVGenerationCompatibilityUtil.validate(request, "V2", LOOKUP, false));
    }

    @Test
    void testValidateWhenIncompatibleInstanceTypesThenAllAreListedInGroupOrder() {
        StackV4Request request = request(group("master", "Standard_D8s_v3"), group("compute", "Standard_D8s_v5"), group("worker", "Standard_D4_v2"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> HyperVGenerationCompatibilityUtil.validate(request, "V2", LOOKUP, false));

        assertEquals("The selected image is Hyper-V generation V2, but the following instance types do not support it: "
                + "Standard_D8s_v3 (master, supports V1), Standard_D4_v2 (worker, supports V1). "
                + "Choose a V2 compatible instance type or an image of the matching generation.", exception.getMessage());
    }

    @Test
    void testValidateWhenGen1ImageAndGen2OnlyInstanceTypeThenRejected() {
        StackV4Request request = request(group("worker", "Standard_Gen2Only"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> HyperVGenerationCompatibilityUtil.validate(request, "V1", LOOKUP, false));

        assertEquals("The selected image is Hyper-V generation V1, but the following instance types do not support it: "
                + "Standard_Gen2Only (worker, supports V2). Choose a V1 compatible instance type or an image of the matching generation.",
                exception.getMessage());
    }

    @Test
    void testValidateWhenFallbackEnabledThenDistinctIncompatibleFallbacksOtherThanThePrimaryAreListed() {
        StackV4Request request = request(group("worker", "Standard_D4_v2", "Standard_D4_v2", "Standard_D8s_v3", "", "Standard_D8s_v3",
                "Standard_E8ds_v5"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> HyperVGenerationCompatibilityUtil.validate(request, "V2", LOOKUP, true));

        assertEquals("The selected image is Hyper-V generation V2, but the following instance types do not support it: "
                + "Standard_D4_v2 (worker, supports V1), Standard_D8s_v3 (worker fallback, supports V1). "
                + "Choose a V2 compatible instance type or an image of the matching generation.", exception.getMessage());
    }

    @Test
    void testValidateWhenFallbackDisabledThenFallbacksAreIgnored() {
        StackV4Request request = request(group("worker", "Standard_D8s_v5", "Standard_D4_v2"));

        assertDoesNotThrow(() -> HyperVGenerationCompatibilityUtil.validate(request, "V2", LOOKUP, false));
    }

    @Test
    void testValidateWhenSupportedGenerationsAreUnknownThenPasses() {
        StackV4Request request = request(group("worker", "Standard_NotReported", "Standard_NotReportedEither"));

        assertDoesNotThrow(() -> HyperVGenerationCompatibilityUtil.validate(request, "V2", LOOKUP, true));
    }

    @Test
    void testValidateWhenNoInstanceGroupsOrTemplatesOrInstanceTypesThenPasses() {
        InstanceGroupV4Request withoutTemplate = new InstanceGroupV4Request();
        withoutTemplate.setName("master");
        InstanceGroupV4Request withoutInstanceType = group("worker", null);

        assertDoesNotThrow(() -> HyperVGenerationCompatibilityUtil.validate(new StackV4Request(), "V2", LOOKUP, true));
        assertDoesNotThrow(() -> HyperVGenerationCompatibilityUtil.validate(request(withoutTemplate, withoutInstanceType), "V2", LOOKUP, true));
    }

    private StackV4Request request(InstanceGroupV4Request... groups) {
        StackV4Request request = new StackV4Request();
        request.setInstanceGroups(new ArrayList<>(List.of(groups)));
        return request;
    }

    private InstanceGroupV4Request group(String name, String instanceType, String... fallbackInstanceTypes) {
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        template.setInstanceType(instanceType);
        template.setFallbackInstanceTypes(Arrays.asList(fallbackInstanceTypes));
        InstanceGroupV4Request group = new InstanceGroupV4Request();
        group.setName(name);
        group.setTemplate(template);
        return group;
    }
}
