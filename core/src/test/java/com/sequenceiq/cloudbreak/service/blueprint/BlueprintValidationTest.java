package com.sequenceiq.cloudbreak.service.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.MapBindingResult;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@ExtendWith(MockitoExtension.class)
class BlueprintValidationTest {
    public static final String BLUEPRINT_TEXT_EMPTY = "";

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @InjectMocks
    private BlueprintValidator blueprintValidator;

    private Blueprint blueprint = new Blueprint();

    private MapBindingResult errors = new MapBindingResult(new HashMap(), "blueprint");

    @BeforeEach
    void setup() {
        blueprint.setBlueprintText("test");
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
    }

    @Test
    void testValidationEmptyText() {
        blueprint.setBlueprintText(BLUEPRINT_TEXT_EMPTY);

        blueprintValidator.validate(blueprint, errors);
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getErrorCount());
        assertTrue(errors.getAllErrors().get(0).getDefaultMessage().contains("The blueprint text is empty"));
    }

    @Test
    void testValidationSameHostgroupName() {
        when(cmTemplateProcessor.getHostTemplateNames()).thenReturn(List.of("master", "master"));
        blueprintValidator.validate(blueprint, errors);
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getErrorCount());
        assertTrue(errors.getAllErrors().get(0).getDefaultMessage().contains("Host null names must be unique"));
    }

    @Test
    void testValidationRoleTypeMissing() {
        when(cmTemplateProcessor.getServiceComponentsByHostGroup()).thenReturn(
                Map.of("test", Set.of(ServiceComponent.of("test1", null)))
        );
        blueprintValidator.validate(blueprint, errors);
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getErrorCount());
        assertTrue(errors.getAllErrors().get(0).getDefaultMessage().contains("Role type is mandatory in role config"));
    }
}
