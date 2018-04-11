package com.sequenceiq.cloudbreak.blueprint;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessorFactory;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateTextProcessor;
import com.sequenceiq.cloudbreak.templateprocessor.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.templateprocessor.templates.ServiceName;
import com.sequenceiq.cloudbreak.templateprocessor.templates.TemplateFiles;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.sequenceiq.cloudbreak.templateprocessor.templates.ServiceName.serviceName;
import static com.sequenceiq.cloudbreak.templateprocessor.templates.TemplateFiles.templateFiles;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintSegmentProcessorTest {

    @Mock
    private TemplateProcessor blueprintTemplateProcessor;

    @Mock
    private BlueprintSegmentReader blueprintSegmentReader;

    @Mock
    private TemplateTextProcessor blueprintProcessor;

    @Mock
    private TemplateProcessorFactory blueprintProcessorFactory;

    @InjectMocks
    private final BlueprintSegmentProcessor underTest = new BlueprintSegmentProcessor();

    private String expectedBlueprint;

    private PreparationObject object = PreparationObject.Builder.builder().build();

    @Before
    public void before() throws IOException {
        expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        Map<ServiceName, TemplateFiles> configFiles = new HashMap<>();
        configFiles.put(serviceName("zeppelin"), templateFiles(
                Lists.newArrayList("blueprints/basics/zeppelin/shiro_ini_content.handlebars", "blueprints/basics/zeppelin/services.json")));

        Map<ServiceName, TemplateFiles> serviceFiles = new HashMap<>();
        serviceFiles.put(serviceName("atlas"), templateFiles(
                Lists.newArrayList("handlebar/blueprints/atlas/atlas-with-ldap.json", "handlebar/blueprints/atlas/atlas-without-ldap.json")));

        when(blueprintSegmentReader.collectAllConfigFile()).thenReturn(configFiles);
        when(blueprintSegmentReader.collectAllServiceFile()).thenReturn(serviceFiles);
        when(blueprintProcessor.componentsExistsInBlueprint(anySet())).thenReturn(true);
        when(blueprintTemplateProcessor.process(anyString(), any(PreparationObject.class), anyMap())).thenReturn(expectedBlueprint);
        when(blueprintProcessor.addConfigEntryStringToBlueprint(anyString(), anyBoolean())).thenReturn(blueprintProcessor);
        when(blueprintProcessor.asText()).thenReturn(expectedBlueprint);
        when(blueprintProcessorFactory.get(anyString())).thenReturn(blueprintProcessor);
    }

    @Test
    public void test() {
        String process = underTest.process(expectedBlueprint, object);

        Assert.assertEquals(expectedBlueprint, process);

    }

}