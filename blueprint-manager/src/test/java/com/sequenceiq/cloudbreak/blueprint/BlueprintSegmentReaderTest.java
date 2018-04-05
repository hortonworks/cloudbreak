package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.blueprint.templates.ServiceName;
import com.sequenceiq.cloudbreak.blueprint.templates.TemplateFiles;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintSegmentReaderTest {

    @InjectMocks
    private final BlueprintSegmentReader underTest = new BlueprintSegmentReader();

    private AnnotationConfigEmbeddedWebApplicationContext annotationConfigEmbeddedWebApplicationContext = new AnnotationConfigEmbeddedWebApplicationContext();

    @Before
    public void setup() throws IOException {
        ReflectionTestUtils.setField(underTest, "resourceLoader", annotationConfigEmbeddedWebApplicationContext);
        ReflectionTestUtils.setField(underTest, "blueprintTemplatePath", "templates/blueprint_templates");
        ReflectionTestUtils.setField(underTest, "basicTemplatePath", "templates/basic_templates");
        ReflectionTestUtils.setField(underTest, "settingsTemplatePath", "templates/settings_templates");
    }

    @Test
    public void testThatAllFileIsReadableShouldVerifyThatFileCountMatch() {
        Map<ServiceName, TemplateFiles> configFiles = underTest.collectAllConfigFile();
        Map<ServiceName, TemplateFiles> serviceFiles = underTest.collectAllServiceFile();
        Map<ServiceName, TemplateFiles> settingsFiles = underTest.collectAllSettingsFile();

        Assert.assertEquals(configFiles.size(), 1);
        Assert.assertEquals(serviceFiles.size(), 9);
        Assert.assertEquals(settingsFiles.size(), 1);
    }
}