package com.sequenceiq.cloudbreak.template.processor.processor;

import org.springframework.stereotype.Component;

@Component
public class TemplateProcessorFactory {

    public TemplateTextProcessor get(String templateText) {
        return new TemplateTextProcessor(templateText);
    }

}
