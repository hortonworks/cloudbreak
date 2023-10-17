package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@ExtendWith(MockitoExtension.class)
public class BlueprintUpgradeOptionValidatorTest {

    private static final BlueprintUpgradeOption BLUEPRINT_UPGRADE_OPTION = ENABLED;

    @InjectMocks
    private BlueprintUpgradeOptionValidator underTest;

    @Mock
    private CustomTemplateUpgradeValidator customTemplateUpgradeValidator;

    @Mock
    private BlueprintUpgradeOptionCondition blueprintUpgradeOptionCondition;

    private static Object[][] testScenariosProvider() {
        return new Object[][] {
                {DATALAKE, DEFAULT, true, false, true},
                {DATALAKE, DEFAULT, false,  false, true},
                {WORKLOAD, DEFAULT, true,  false, true},

                {WORKLOAD, DEFAULT, false,  false, true},
                {WORKLOAD, DEFAULT, false,  false, false},

                {WORKLOAD, USER_MANAGED, false, false, false},
                {WORKLOAD, USER_MANAGED, false, false, true},
                {WORKLOAD, USER_MANAGED, false, true, true},
        };
    }

    @ParameterizedTest(name = "StackType: {0}, Blueprint type: {1}, skipValidations: {2}, dataHubUpgradeEntitled: {3}, expected: {4}")
    @MethodSource("testScenariosProvider")
    public void test(StackType stackType, ResourceStatus resourceStatus, boolean skipValidations, boolean dataHubUpgradeEntitled, boolean expectedValue) {
        String errorMessage = "The cluster template is not eligible for upgrade";
        Blueprint blueprint = createBlueprint(resourceStatus);
        ImageFilterParams imageFilterParams = createImageFilterParams(stackType, blueprint, skipValidations, dataHubUpgradeEntitled);

        lenient().when(customTemplateUpgradeValidator.isValid(blueprint)).thenReturn(createValidationResult(expectedValue, errorMessage));
        lenient().when(blueprintUpgradeOptionCondition.validate(imageFilterParams, BLUEPRINT_UPGRADE_OPTION))
                .thenReturn(createValidationResult(expectedValue, errorMessage));

        BlueprintValidationResult actual = underTest.isValidBlueprint(imageFilterParams);

        assertEquals(expectedValue, actual.isValid());
        assertEquals(expectedValue ?  null : errorMessage, actual.getReason());
    }

    private BlueprintValidationResult createValidationResult(boolean expectedValue, String errorMessage) {
        return new BlueprintValidationResult(expectedValue, expectedValue ? null : errorMessage);
    }

    private Blueprint createBlueprint(ResourceStatus resourceStatus) {
        Blueprint blueprint = new Blueprint();
        blueprint.setStatus(resourceStatus);
        blueprint.setBlueprintUpgradeOption(BLUEPRINT_UPGRADE_OPTION);
        return blueprint;
    }

    private ImageFilterParams createImageFilterParams(StackType stackType, Blueprint blueprint, boolean skipValidations, boolean dataHubUpgradeEntitled) {
        return new ImageFilterParams(null, null, false, null, stackType, blueprint, null,
                new InternalUpgradeSettings(skipValidations, dataHubUpgradeEntitled, false), null, null, null, false);
    }

}