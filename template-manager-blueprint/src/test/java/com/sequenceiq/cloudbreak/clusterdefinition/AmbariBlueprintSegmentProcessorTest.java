package com.sequenceiq.cloudbreak.clusterdefinition;

import static com.sequenceiq.cloudbreak.template.model.ServiceName.serviceName;
import static com.sequenceiq.cloudbreak.template.model.TemplateFiles.templateFiles;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.template.model.ServiceName;
import com.sequenceiq.cloudbreak.template.model.TemplateFiles;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class AmbariBlueprintSegmentProcessorTest {

    @Mock
    private TemplateProcessor templateProcessor;

    @Mock
    private AmbariBlueprintSegmentReader ambariBlueprintSegmentReader;

    @Mock
    private AmbariBlueprintTextProcessor blueprintProcessor;

    @Mock
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @InjectMocks
    private final AmbariBlueprintSegmentProcessor underTest = new AmbariBlueprintSegmentProcessor();

    private String expectedBlueprint;

    private final TemplatePreparationObject object = Builder.builder().build();

    @Before
    public void before() throws IOException {
        expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        Map<ServiceName, TemplateFiles> configFiles = new HashMap<>();
        configFiles.put(serviceName("zeppelin"), templateFiles(
                Lists.newArrayList("blueprints/basics/zeppelin/shiro_ini_content.handlebars", "blueprints/basics/zeppelin/services.json")));

        Map<ServiceName, TemplateFiles> serviceFiles = new HashMap<>();
        serviceFiles.put(serviceName("atlas"), templateFiles(
                Lists.newArrayList("handlebar/configurations/atlas/atlas-with-ldap.json", "handlebar/configurations/atlas/atlas-without-ldap.json")));

        when(ambariBlueprintSegmentReader.collectAllConfigFile()).thenReturn(configFiles);
        when(ambariBlueprintSegmentReader.collectAllServiceFile()).thenReturn(serviceFiles);
        when(blueprintProcessor.isComponentsExistsInBlueprint(anySet())).thenReturn(true);
        when(templateProcessor.process(anyString(), any(TemplatePreparationObject.class), anyMap())).thenReturn(expectedBlueprint);
        when(blueprintProcessor.addConfigEntryStringToBlueprint(anyString(), anyBoolean())).thenReturn(blueprintProcessor);
        when(blueprintProcessor.asText()).thenReturn(expectedBlueprint);
        when(ambariBlueprintProcessorFactory.get(anyString())).thenReturn(blueprintProcessor);
    }

    @Test
    public void test() {
        String process = underTest.process(expectedBlueprint, object);

        Assert.assertEquals(expectedBlueprint, process);

    }

}