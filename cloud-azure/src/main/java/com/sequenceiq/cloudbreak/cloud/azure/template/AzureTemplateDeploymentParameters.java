package com.sequenceiq.cloudbreak.cloud.azure.template;

public class AzureTemplateDeploymentParameters {

    private final String resourceGroupName;

    private final String templateName;

    private final String templateContent;

    private final String templateParameters;

    public AzureTemplateDeploymentParameters(String resourceGroupName, String templateName, String templateContent, String templateParameters) {
        this.resourceGroupName = resourceGroupName;
        this.templateName = templateName;
        this.templateContent = templateContent;
        this.templateParameters = templateParameters;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public String getTemplateParameters() {
        return templateParameters;
    }
}
