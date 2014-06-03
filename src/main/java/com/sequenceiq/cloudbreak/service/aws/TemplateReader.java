package com.sequenceiq.cloudbreak.service.aws;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudFormationTemplate;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class TemplateReader {

    public CloudFormationTemplate readTemplateFromFile(String templateName) throws IOException {
        return new CloudFormationTemplate(FileReaderUtils.readFileFromClasspath(templateName));
    }

}
