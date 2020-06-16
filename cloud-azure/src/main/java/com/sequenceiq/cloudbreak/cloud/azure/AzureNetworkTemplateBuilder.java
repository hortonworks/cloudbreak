package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
public class AzureNetworkTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateBuilder.class);

    @Value("${cb.arm.network.template.path:}")
    private String armTemplatePath;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public String build(NetworkCreationRequest networkRequest, List<SubnetRequest> subnets, String resourceGroupName) {
        try {
            Map<String, Object> model = createModel(networkRequest, subnets, resourceGroupName);
            String generatedTemplate = freeMarkerTemplateUtils.processTemplateIntoString(getTemplate(), model);
            LOGGER.debug("Generated Arm template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateBuilder", e);
        }
    }

    private Template getTemplate() {
        try {
            String freeMarkerTemplate = freemarkerConfiguration.getTemplate(armTemplatePath, "UTF-8").toString();
            return new Template("azure-template", freeMarkerTemplate, freemarkerConfiguration);
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't create template object", e);
        }
    }

    private Map<String, Object> createModel(NetworkCreationRequest networkRequest, List<SubnetRequest> subnets, String resourceGroupName) {
        Map<String, Object> model = new HashMap<>();
        model.put("virtualNetworkName", networkRequest.getEnvName());
        model.put("region", networkRequest.getRegion().value());
        model.put("networkPrefix", networkRequest.getNetworkCidr());
        model.put("subnetDetails", subnets);
        model.put("resourceGroupName", resourceGroupName);
        model.put("noPublicIp", networkRequest.isNoPublicIp());
        model.put("noFirewallRules", false);
        return model;
    }
}
