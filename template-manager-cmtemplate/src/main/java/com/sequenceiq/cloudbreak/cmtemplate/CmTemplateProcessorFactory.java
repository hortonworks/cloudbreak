package com.sequenceiq.cloudbreak.cmtemplate;

import org.springframework.stereotype.Component;

@Component
public class CmTemplateProcessorFactory {
    public CmTemplateProcessor get(String cmTemplateText) {
        return new CmTemplateProcessor(cmTemplateText);
    }
}
