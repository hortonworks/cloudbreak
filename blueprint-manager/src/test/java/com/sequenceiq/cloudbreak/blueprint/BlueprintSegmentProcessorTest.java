package com.sequenceiq.cloudbreak.blueprint;

import static com.sequenceiq.cloudbreak.blueprint.templates.ServiceName.serviceName;
import static com.sequenceiq.cloudbreak.blueprint.templates.TemplateFiles.templateFiles;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.blueprint.template.BlueprintTemplateProcessor;
import com.sequenceiq.cloudbreak.blueprint.templates.ServiceName;
import com.sequenceiq.cloudbreak.blueprint.templates.TemplateFiles;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintSegmentProcessorTest {

    @Mock
    private BlueprintTemplateProcessor blueprintTemplateProcessor;

    @Mock
    private BlueprintSegmentReader blueprintSegmentReader;

    @Mock
    private BlueprintTextProcessor blueprintProcessor;

    @Mock
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @InjectMocks
    private final BlueprintSegmentProcessor underTest = new BlueprintSegmentProcessor();

    private String expectedBlueprint;

    private BlueprintPreparationObject object = BlueprintPreparationObject.Builder.builder().build();

    @Before
    public void before() throws IOException {
        expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        Map<ServiceName, TemplateFiles> configFiles = new HashMap<>();
        configFiles.put(serviceName("zeppelin"), templateFiles(
                Lists.newArrayList("blueprints/basics/zeppelin/shiro_ini_content.handlebars", "blueprints/basics/zeppelin/services.json")));

        Map<ServiceName, TemplateFiles> serviceFiles = new HashMap<>();
        serviceFiles.put(serviceName("atlas"), templateFiles(
                Lists.newArrayList("handlebar/configurations/atlas/atlas-with-ldap.json", "handlebar/configurations/atlas/atlas-without-ldap.json")));

        when(blueprintSegmentReader.collectAllConfigFile()).thenReturn(configFiles);
        when(blueprintSegmentReader.collectAllServiceFile()).thenReturn(serviceFiles);
        when(blueprintProcessor.componentsExistsInBlueprint(anySet())).thenReturn(true);
        when(blueprintTemplateProcessor.process(anyString(), any(BlueprintPreparationObject.class), anyMap())).thenReturn(expectedBlueprint);
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