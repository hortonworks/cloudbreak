package com.sequenceiq.cloudbreak.blueprint;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@RunWith(MockitoJUnitRunner.class)
public class CentralBlueprintUpdaterTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private TemplateProcessor templateProcessor;

    @Mock
    private BlueprintSegmentProcessor blueprintSegmentProcessor;

    @Mock
    private BlueprintComponentProviderProcessor blueprintComponentProviderProcessor;

    @InjectMocks
    private CentralBlueprintUpdater underTest;

    private TemplatePreparationObject object;

    private String testBlueprint;

    @Before
    public void before() {
        testBlueprint = TestUtil.blueprint().getBlueprintText();

        object = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(new BlueprintView(TestUtil.blueprint().getBlueprintText(), "HDP", "2.6"))
                .build();
    }

    @Test
    public void getBlueprintTextWhenEveryThingWorksFineThenShouldReturnWithAnUpdatedBlueprint() throws IOException {
        when(templateProcessor.process(testBlueprint, object, Maps.newHashMap())).thenReturn(testBlueprint);
        when(blueprintSegmentProcessor.process(testBlueprint, object)).thenReturn(testBlueprint);
        when(blueprintComponentProviderProcessor.process(object, testBlueprint)).thenReturn(testBlueprint);

        String result = underTest.getBlueprintText(object);

        Assert.assertEquals(testBlueprint, result);

        verify(templateProcessor, times(1)).process(testBlueprint, object, Maps.newHashMap());
        verify(blueprintSegmentProcessor, times(1)).process(testBlueprint, object);
        verify(blueprintComponentProviderProcessor, times(1)).process(object, testBlueprint);
    }

    @Test
    public void getBlueprintTextWhenBlueprintTemplateProcessorThrowExceptionThenShouldReturnThrowException() throws IOException {
        when(templateProcessor.process(testBlueprint, object, Maps.newHashMap())).thenThrow(new IOException("failed to read bp"));

        String message = String.format("Unable to update blueprint with default properties which was: %s", testBlueprint);

        thrown.expect(BlueprintProcessingException.class);
        thrown.expectMessage(message);

        String result = underTest.getBlueprintText(object);

        Assert.assertEquals(testBlueprint, result);

        verify(templateProcessor, times(1)).process(testBlueprint, object, Maps.newHashMap());
        verify(blueprintSegmentProcessor, times(0)).process(testBlueprint, object);
        verify(blueprintComponentProviderProcessor, times(0)).process(object, testBlueprint);
    }
}