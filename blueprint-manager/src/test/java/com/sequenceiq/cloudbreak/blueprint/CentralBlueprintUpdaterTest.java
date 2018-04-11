package com.sequenceiq.cloudbreak.blueprint;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessingException;
import com.sequenceiq.cloudbreak.templateprocessor.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.templateprocessor.template.views.BlueprintView;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CentralBlueprintUpdaterTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private TemplateProcessor blueprintTemplateProcessor;

    @Mock
    private BlueprintSegmentProcessor blueprintSegmentProcessor;

    @Mock
    private BlueprintComponentProviderProcessor blueprintComponentProviderProcessor;

    @InjectMocks
    private CentralBlueprintUpdater underTest;

    private PreparationObject object;

    private String testBlueprint;

    @Before
    public void before() throws IOException {
        testBlueprint = TestUtil.blueprint().getBlueprintText();

        Cluster cluster = TestUtil.cluster();
        cluster.getBlueprint().setBlueprintText(testBlueprint);

        object = PreparationObject.Builder.builder()
                .withBlueprintView(new BlueprintView(TestUtil.blueprint().getBlueprintText(), Maps.newHashMap(), "HDP", "2.6"))
                .build();
    }

    @Test
    public void getBlueprintTextWhenEveryThingWorksFineThenShouldReturnWithAnUpdatedBlueprint() throws IOException {
        when(blueprintTemplateProcessor.process(testBlueprint, object, Maps.newHashMap())).thenReturn(testBlueprint);
        when(blueprintSegmentProcessor.process(testBlueprint, object)).thenReturn(testBlueprint);
        when(blueprintComponentProviderProcessor.process(object, testBlueprint)).thenReturn(testBlueprint);

        String result = underTest.getBlueprintText(object);

        Assert.assertEquals(testBlueprint, result);

        verify(blueprintTemplateProcessor, times(1)).process(testBlueprint, object, Maps.newHashMap());
        verify(blueprintSegmentProcessor, times(1)).process(testBlueprint, object);
        verify(blueprintComponentProviderProcessor, times(1)).process(object, testBlueprint);
    }

    @Test
    public void getBlueprintTextWhenBlueprintTemplateProcessorThrowExceptionThenShouldReturnThrowException() throws IOException {
        when(blueprintTemplateProcessor.process(testBlueprint, object, Maps.newHashMap())).thenThrow(new IOException("failed to read bp"));
        when(blueprintSegmentProcessor.process(testBlueprint, object)).thenReturn(testBlueprint);
        when(blueprintComponentProviderProcessor.process(object, testBlueprint)).thenReturn(testBlueprint);

        String message = String.format("Unable to update blueprint with default  properties which was: %s", testBlueprint);

        thrown.expect(TemplateProcessingException.class);
        thrown.expectMessage(message);

        String result = underTest.getBlueprintText(object);

        Assert.assertEquals(testBlueprint, result);

        verify(blueprintTemplateProcessor, times(1)).process(testBlueprint, object, Maps.newHashMap());
        verify(blueprintSegmentProcessor, times(0)).process(testBlueprint, object);
        verify(blueprintComponentProviderProcessor, times(0)).process(object, testBlueprint);
    }
}