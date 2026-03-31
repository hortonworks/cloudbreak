package com.sequenceiq.cloudbreak.cm;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;

@Component
public class ClouderaManagerBlueprintPortConfigCollector {

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    public Map<String, Integer> getServicePorts(String blueprintText, boolean tls) {
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);

        return exposedServiceCollector.getAllServicePorts(processor.getVersion(), tls);
    }

    public Map<String, String> getServiceProtocols(String blueprintText, boolean tls) {
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);

        return exposedServiceCollector.getAllServiceProtocols(processor.getVersion(), tls);
    }
}
