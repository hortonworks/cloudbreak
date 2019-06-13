package com.sequenceiq.cloudbreak.cloud.aws;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
public class AwsNetworkCfTemplateProvider {

    @Inject
    private Configuration freemarkerConfiguration;

    @Value("${cb.aws.cf.network.template.path:}")
    private String cloudFormationNetworkTemplatePath;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public String provide(String vpcCidr, List<CreatedSubnet> subnets) {

        Map<String, Object> model = createModel(vpcCidr, subnets);
        try {
            String freeMarkerTemplate = freemarkerConfiguration.getTemplate(cloudFormationNetworkTemplatePath, "UTF-8").toString();
            Template template = new Template("aws-template", freeMarkerTemplate, freemarkerConfiguration);
            return freeMarkerTemplateUtils.processTemplateIntoString(template, model).replaceAll("\\t|\\n| [\\s]+", "");
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process CloudFormation freemarker template", e);
        }
    }

    private Map<String, Object> createModel(String vpcCidr, List<CreatedSubnet> subnets) {
        Map<String, Object> model = new HashMap<>();
        model.put("vpcCidr", vpcCidr);
        model.put("subnetDetails", subnets);
        model.put("network_resource", CloudbreakResourceType.NETWORK);
        return model;
    }
}
