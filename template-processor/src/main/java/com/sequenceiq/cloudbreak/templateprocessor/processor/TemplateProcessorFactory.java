package com.sequenceiq.cloudbreak.templateprocessor.processor;

import org.springframework.stereotype.Component;

@Component
public class TemplateProcessorFactory {

    public TemplateTextProcessor get(String templateText) {
        return new TemplateTextProcessor(templateText);
    }

}
