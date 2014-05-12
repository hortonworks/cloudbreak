package com.sequenceiq.provisioning.service.aws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.CloudFormationTemplate;

@Component
public class TemplateReader {

    public CloudFormationTemplate readTemplateFromFile(String templateName) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(new ClassPathResource(templateName).getInputStream(), "UTF-8"));
        for (int c = br.read(); c != -1; c = br.read()) {
            sb.append((char) c);
        }
        return new CloudFormationTemplate(sb.toString());
    }

}
