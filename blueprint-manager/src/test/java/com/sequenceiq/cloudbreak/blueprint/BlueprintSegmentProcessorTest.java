package com.sequenceiq.cloudbreak.blueprint;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.template.processor.templates.ServiceName;
import com.sequenceiq.cloudbreak.template.processor.templates.TemplateFiles;
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

import static com.sequenceiq.cloudbreak.template.processor.templates.ServiceName.serviceName;
import static com.sequenceiq.cloudbreak.template.processor.templates.TemplateFiles.templateFiles;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
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

    private TemplatePreparationObject object = TemplatePreparationObject.Builder.builder().build();

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
        when(blueprintTemplateProcessor.process(anyString(), any(TemplatePreparationObject.class), anyMap())).thenReturn(expectedBlueprint);
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