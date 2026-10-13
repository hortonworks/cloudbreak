package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@ExtendWith(MockitoExtension.class)
class SmartRouterRdsConfigProviderTest {

    private static final String SMART_ROUTER = "smartrouter";

    private static final String PORT = "5432";

    private static final String BLUEPRINT_TEXT = "{\"cdhVersion\": \"7.3.2\"}";

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @InjectMocks
    private SmartRouterRdsConfigProvider underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "smartRouterDbUser", SMART_ROUTER);
        ReflectionTestUtils.setField(underTest, "smartRouterDb", SMART_ROUTER);
        ReflectionTestUtils.setField(underTest, "smartRouterDbPort", PORT);
    }

    @Test
    void testSmartRouterRdsConfigProvider() {
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.SMART_ROUTER);
        assertThat(underTest.getDbUser()).isEqualTo(SMART_ROUTER);
        assertThat(underTest.getDb()).isEqualTo(SMART_ROUTER);
        assertThat(underTest.getDbPort()).isEqualTo(PORT);
        assertThat(underTest.getPillarKey()).isEqualTo(SMART_ROUTER);
    }

    @Test
    void testIsRdsConfigNeededWhenTheSmartRouterServerIsPresentInTheBlueprint() {
        StackDtoDelegate stack = createStack();
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.doesCMComponentExistsInBlueprint("SMARTROUTER_SERVER")).thenReturn(true);

        assertThat(underTest.isRdsConfigNeeded(stack)).isTrue();
    }

    @Test
    void testIsRdsConfigNeededWhenTheSmartRouterServerIsNotPresentInTheBlueprint() {
        StackDtoDelegate stack = createStack();
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.doesCMComponentExistsInBlueprint("SMARTROUTER_SERVER")).thenReturn(false);

        assertThat(underTest.isRdsConfigNeeded(stack)).isFalse();
    }

    @Test
    void testIsRdsConfigNeededWhenTheBlueprintIsNull() {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        when(stack.getBlueprint()).thenReturn(null);

        assertThat(underTest.isRdsConfigNeeded(stack)).isFalse();
        verifyNoInteractions(cmTemplateProcessorFactory);
    }

    private StackDtoDelegate createStack() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        when(stack.getBlueprint()).thenReturn(blueprint);
        return stack;
    }
}
