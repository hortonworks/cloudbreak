package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDnsZoneDeploymentParameters;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
public class AzureNetworkDnsZoneTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureNetworkDnsZoneTemplateBuilder.class);

    @Value("${cb.arm.network.dnszone.template.path:}")
    private String armTemplatePath;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public String build(AzureDnsZoneDeploymentParameters parameters) {
        try {
            Map<String, Object> model = createModel(parameters);
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

    private Map<String, Object> createModel(AzureDnsZoneDeploymentParameters parameters) {
        List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices = parameters.getEnabledPrivateEndpointServices();
        String networkId = parameters.getNetworkId();
        String resourceGroup = parameters.getResourceGroupName();
        boolean deployOnlyNetworkLinks = parameters.getDeployOnlyNetworkLinks();
        Map<String, String> tags = parameters.getTags();

        Map<String, Object> model = new HashMap<>();
        model.put("virtualNetworkName", StringUtils.substringAfterLast(networkId, "/"));
        model.put("virtualNetworkId", networkId);
        model.put("privateEndpointServices", enabledPrivateEndpointServices);
        model.put("resourceGroupName", resourceGroup);
        model.put("serverTags", tags);
        model.put("deployOnlyNetworkLinks", deployOnlyNetworkLinks);
        return model;
    }
}
