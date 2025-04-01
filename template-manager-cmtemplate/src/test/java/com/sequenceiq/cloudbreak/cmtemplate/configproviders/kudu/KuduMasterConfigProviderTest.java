package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu.KuduConfigs.GENERATED_RANGER_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu.KuduConfigs.RANGER_KUDU_PLUGIN_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspathQuietly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@ExtendWith(MockitoExtension.class)
class KuduMasterConfigProviderTest {
    private final KuduMasterConfigProvider subject = new KuduMasterConfigProvider();

    @Test
    public void testRangerPluginServiceConfig7210() {
        CmTemplateProcessor templateProcessor = new CmTemplateProcessor(readFileFromClasspathQuietly("input/cdp-data-mart.bp"));
        templateProcessor.setCdhVersion("7.2.10");
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(templateProcessor, "7.2.10");

        List<ApiClusterTemplateConfig> roleConfigs = subject.getRoleConfigs("KUDU_MASTER", templateProcessor, preparationObject);

        assertEquals(0, roleConfigs.size());
        assertTrue(subject.isConfigurationNeeded(templateProcessor, preparationObject));
    }

    @Test
    public void testRangerPluginServiceConfig7211() {
        CmTemplateProcessor templateProcessor = new CmTemplateProcessor(readFileFromClasspathQuietly("input/cdp-data-mart.bp"));
        templateProcessor.setCdhVersion("7.2.11");
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(templateProcessor, "7.2.11");

        List<ApiClusterTemplateConfig> roleConfigs = subject.getRoleConfigs("KUDU_MASTER", templateProcessor, preparationObject);

        assertEquals(1, roleConfigs.size());
        assertTrue(roleConfigs.contains(config(RANGER_KUDU_PLUGIN_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME)));
        assertTrue(subject.isConfigurationNeeded(templateProcessor, preparationObject));
    }

    @Test
    public void testCongigNotNeeded() {
        CmTemplateProcessor templateProcessor = new CmTemplateProcessor(readFileFromClasspathQuietly("input/cdp-streaming.bp"));
        templateProcessor.setCdhVersion("7.2.11");
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(templateProcessor, "7.2.11");

        assertFalse(subject.isConfigurationNeeded(templateProcessor, preparationObject));
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor processor, String version) {
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
            .withBlueprintView(new BlueprintView("text", version, "CDH", null, processor))
            .build();
        return preparationObject;
    }
}
