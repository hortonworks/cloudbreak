package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;

@Component
public class CmTemplateService {

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public Set<String> getServiceTypes(String blueprintText) {
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return cmTemplateProcessor.getTemplate().getServices().stream()
                .map(ApiClusterTemplateService::getServiceType)
                .collect(Collectors.toSet());
    }
}
