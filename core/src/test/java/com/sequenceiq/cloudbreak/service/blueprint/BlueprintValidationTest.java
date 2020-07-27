package com.sequenceiq.cloudbreak.service.blueprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.MapBindingResult;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintValidationTest {
    public static final String BLUEPRINT_TEXT_EMPTY = "";

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @InjectMocks
    private BlueprintValidator blueprintValidator;

    private Blueprint blueprint = new Blueprint();

    private MapBindingResult errors = new MapBindingResult(new HashMap(), "blueprint");

    @Before
    public void setup() {
        blueprint.setBlueprintText("test");
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
    }

    @Test
    public void testValidationEmptyText() {
        blueprint.setBlueprintText(BLUEPRINT_TEXT_EMPTY);

        blueprintValidator.validate(blueprint, errors);
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getErrorCount());
        assertTrue(errors.getAllErrors().get(0).getDefaultMessage().contains("The blueprint text is empty"));
    }

    @Test
    public void testValidationSameHostgroupName() {
        when(cmTemplateProcessor.getHostTemplateNames()).thenReturn(List.of("master", "master"));
        blueprintValidator.validate(blueprint, errors);
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getErrorCount());
        assertTrue(errors.getAllErrors().get(0).getDefaultMessage().contains("Host null names must be unique"));
    }

    @Test
    public void testValidationRoleTypeMissing() {
        when(cmTemplateProcessor.getServiceComponentsByHostGroup()).thenReturn(
                Map.of("test", Set.of(ServiceComponent.of("test1", null)))
        );
        blueprintValidator.validate(blueprint, errors);
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getErrorCount());
        assertTrue(errors.getAllErrors().get(0).getDefaultMessage().contains("Role type is mandatory in role config"));
    }
}
