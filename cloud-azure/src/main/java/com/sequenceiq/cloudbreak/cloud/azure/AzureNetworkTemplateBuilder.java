package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
public class AzureNetworkTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateBuilder.class);

    @Value("${cb.arm.network.template.path:}")
    private String armTemplatePath;

    @Value("${cb.arm.parameter.path:}")
    private String armTemplateParametersPath;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public String build(NetworkCreationRequest networkRequest) {
        try {
            Map<String, Object> model = createModel(networkRequest);
            String generatedTemplate = freeMarkerTemplateUtils.processTemplateIntoString(getTemplate(), model);
            LOGGER.debug("Generated Arm template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateBuilder", e);
        }
    }

    public String buildParameters() {
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(armTemplateParametersPath, "UTF-8"), new HashMap<>());
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateParameterBuilder", e);
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

    private Map<String, Object> createModel(NetworkCreationRequest networkRequest) {
        Map<String, Object> model = new HashMap<>();
        model.put("virtualNetworkName", networkRequest.getEnvName());
        model.put("region", networkRequest.getRegion().value());
        model.put("networkPrefix", networkRequest.getNetworkCidr());
        model.put("subnetPrefixList", networkRequest.getSubnetCidrs());
        model.put("resourceGroupName", networkRequest.getEnvName());
        model.put("noPublicIp", networkRequest.isNoPublicIp());
        model.put("noFirewallRules", networkRequest.isNoFirewallRules());
        model.putAll(defaultCostTaggingService.prepareNetworkTagging());
        model.put("network_resource", CloudbreakResourceType.NETWORK);
        return model;
    }
}
