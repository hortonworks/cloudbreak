package com.sequenceiq.provisioning.service.aws;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.CloudFormationTemplate;
import com.sequenceiq.provisioning.util.FileReaderUtils;

@Component
public class TemplateReader {

    public CloudFormationTemplate readTemplateFromFile(String templateName) throws IOException {
        return new CloudFormationTemplate(FileReaderUtils.readFileFromClasspath(templateName));
    }

}
