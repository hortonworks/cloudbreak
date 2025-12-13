package com.sequenceiq.cloudbreak.service.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.utils.StackInfoService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@ExtendWith(MockitoExtension.class)
class BlueprintViewProviderTest {

    private static final String TEST_BLUEPRINT_TEXT = "{}";

    private static final String TEST_STACK_TYPE = "CDP";

    private static final String TEST_STACK_VERSION = "1.0";

    @InjectMocks
    private BlueprintViewProvider subject;

    @Mock
    private BlueprintTextProcessorFactory blueprintTextProcessorFactory;

    @Mock
    private StackInfoService stackInfoService;

    @Mock
    private Blueprint blueprint;

    @Mock
    private BlueprintStackInfo blueprintStackInfo;

    @BeforeEach
    void setup() {
        when(blueprint.getBlueprintJsonText()).thenReturn(TEST_BLUEPRINT_TEXT);
        when(stackInfoService.blueprintStackInfo(TEST_BLUEPRINT_TEXT)).thenReturn(blueprintStackInfo);
        when(blueprintStackInfo.getType()).thenReturn(TEST_STACK_TYPE);
        when(blueprintStackInfo.getVersion()).thenReturn(TEST_STACK_VERSION);
    }

    @Test
    void usesAmbariBlueprintTextProcessor() {
        testUsesProcessorFromFactory(new CmTemplateProcessor(TEST_BLUEPRINT_TEXT));
    }

    @Test
    void usesCmTemplateProcessor() {
        testUsesProcessorFromFactory(new CmTemplateProcessor(TEST_BLUEPRINT_TEXT));
    }

    private void testUsesProcessorFromFactory(BlueprintTextProcessor processor) {
        when(blueprintTextProcessorFactory.createBlueprintTextProcessor(TEST_BLUEPRINT_TEXT)).thenReturn(processor);

        BlueprintView blueprintView = subject.getBlueprintView(blueprint);

        assertEquals(TEST_BLUEPRINT_TEXT, blueprintView.getBlueprintText());
        assertEquals(TEST_STACK_TYPE, blueprintView.getType());
        assertEquals(TEST_STACK_VERSION, blueprintView.getVersion());
        assertSame(processor, blueprintView.getProcessor());
    }
}
