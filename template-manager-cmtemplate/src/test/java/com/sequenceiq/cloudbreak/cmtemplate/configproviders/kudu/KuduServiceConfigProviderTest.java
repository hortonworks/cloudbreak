package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu.KuduConfigs.GENERATED_RANGER_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu.KuduConfigs.RANGER_KUDU_PLUGIN_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspathQuietly;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
class KuduServiceConfigProviderTest {
    private final KuduServiceConfigProvider subject = new KuduServiceConfigProvider();

    @Test
    public void testRangerPluginServiceConfig7210() {
        CmTemplateProcessor templateProcessor = new CmTemplateProcessor(readFileFromClasspathQuietly("input/cdp-data-mart.bp"));
        templateProcessor.setCdhVersion("7.2.10");
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(templateProcessor, "7.2.10");

        List<ApiClusterTemplateConfig> serviceConfigs = subject.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(0, serviceConfigs.size());
    }

    @Test
    public void testRangerPluginServiceConfig7211() {
        CmTemplateProcessor templateProcessor = new CmTemplateProcessor(readFileFromClasspathQuietly("input/cdp-data-mart.bp"));
        templateProcessor.setCdhVersion("7.2.11");
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(templateProcessor, "7.2.11");

        List<ApiClusterTemplateConfig> serviceConfigs = subject.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertTrue(serviceConfigs.contains(config(RANGER_KUDU_PLUGIN_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME)));
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor processor, String version) {
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
            .withBlueprintView(new BlueprintView("text", version, "CDH", processor))
            .build();
        return preparationObject;
    }
}
