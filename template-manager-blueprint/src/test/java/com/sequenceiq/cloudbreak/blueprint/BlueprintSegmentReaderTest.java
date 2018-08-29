package com.sequenceiq.cloudbreak.blueprint;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.template.model.ServiceName;
import com.sequenceiq.cloudbreak.template.model.TemplateFiles;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintSegmentReaderTest {

    @InjectMocks
    private final BlueprintSegmentReader underTest = new BlueprintSegmentReader();

    private AnnotationConfigApplicationContext annotationConfigEmbeddedWebApplicationContext = new AnnotationConfigApplicationContext();

    @Before
    public void setup() {
        ReflectionTestUtils.setField(underTest, "resourceLoader", annotationConfigEmbeddedWebApplicationContext);
        ReflectionTestUtils.setField(underTest, "blueprintTemplatePath", "blueprints/configurations");
        ReflectionTestUtils.setField(underTest, "basicTemplatePath", "blueprints/basics");
        ReflectionTestUtils.setField(underTest, "settingsTemplatePath", "blueprints/settings");
    }

    @Test
    public void testThatAllFileIsReadableShouldVerifyThatFileCountMatch() {
        Map<ServiceName, TemplateFiles> configFiles = underTest.collectAllConfigFile();
        Map<ServiceName, TemplateFiles> serviceFiles = underTest.collectAllServiceFile();
        Map<ServiceName, TemplateFiles> settingsFiles = underTest.collectAllSettingsFile();

        Assert.assertEquals(3L, configFiles.size());
        Assert.assertEquals(23L, serviceFiles.size());
        Assert.assertEquals(1L, settingsFiles.size());
    }
}