package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureSecurityView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service("AzureTemplateBuilder")
public class AzureTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateBuilder.class);

    @Value("${cb.arm.template.path:}")
    private String armTemplatePath;

    @Value("${cb.arm.parameter.path:}")
    private String armTemplateParametersPath;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureStorage azureStorage;

    public String build(String stackName, String customImageId, AzureCredentialView armCredentialView, AzureStackView armStack, CloudContext cloudContext,
            CloudStack cloudStack) {
        try {
            String imageUrl = cloudStack.getImage().getImageName();
            String imageName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

            Network network = cloudStack.getNetwork();
            Map<String, Object> model = new HashMap<>();
            model.put("credential", armCredentialView);
            String rootDiskStorage = azureStorage.getImageStorageName(armCredentialView, cloudContext,
                    azureStorage.getPersistentStorageName(cloudStack.getParameters()),
                    azureStorage.getArmAttachedStorageOption(cloudStack.getParameters()));
            AzureSecurityView armSecurityView = new AzureSecurityView(cloudStack.getGroups());

            model.put("customImageId", customImageId);
            model.put("storage_account_name", rootDiskStorage);
            model.put("image_storage_container_name", AzureStorage.IMAGES);
            model.put("storage_container_name", azureStorage.getDiskContainerName(cloudContext));
            model.put("storage_vhd_name", imageName);
            model.put("stackname", stackName);
            model.put("region", cloudContext.getLocation().getRegion().value());
            model.put("subnet1Prefix", network.getSubnet().getCidr());
            model.put("groups", armStack.getGroups());
            model.put("igs", armStack.getInstanceGroups());
            model.put("securities", armSecurityView.getPorts());
            model.put("corecustomData", base64EncodedUserData(cloudStack.getImage().getUserData(InstanceGroupType.CORE)));
            model.put("gatewaycustomData", base64EncodedUserData(cloudStack.getImage().getUserData(InstanceGroupType.GATEWAY)));
            model.put("disablePasswordAuthentication", !armCredentialView.passwordAuthenticationRequired());
            model.put("existingVPC", azureUtils.isExistingNetwork(network));
            model.put("resourceGroupName", azureUtils.getCustomResourceGroupName(network));
            model.put("existingVNETName", azureUtils.getCustomNetworkId(network));
            model.put("existingSubnetName", azureUtils.getCustomSubnetId(network));
            model.put("noPublicIp", azureUtils.isPrivateIp(network));
            model.put("noFirewallRules", azureUtils.isNoSecurityGroups(network));
            model.put("userDefinedTags", cloudStack.getTags());
            String generatedTemplate = processTemplateIntoString(getTemplate(cloudStack), model);
            LOGGER.debug("Generated Arm template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the Arm TemplateBuilder", e);
        }
    }

    public String buildParameters(CloudCredential credential, Network network, Image image) {
        try {
            return processTemplateIntoString(freemarkerConfiguration.getTemplate(armTemplateParametersPath, "UTF-8"), new HashMap<>());
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the Arm TemplateParameterBuilder", e);
        }
    }

    public String getTemplateString() {
        return getTemplate().toString();
    }

    public Template getTemplate(CloudStack stack) {
        try {
            return new Template(armTemplatePath, stack.getTemplate(), freemarkerConfiguration);
        } catch (IOException e) {
            throw new CloudConnectorException("can't create template object", e);
        }
    }

    private Template getTemplate() {
        try {
            return freemarkerConfiguration.getTemplate(armTemplatePath, "UTF-8");
        } catch (IOException e) {
            throw new CloudConnectorException("can't get arm template", e);
        }
    }

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }

}