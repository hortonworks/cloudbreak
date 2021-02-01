package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation.UPSCALE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureAcceleratedNetworkValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureSecurityView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

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
    private CustomVMImageNameProvider customVMImageNameProvider;

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private AzureAcceleratedNetworkValidator azureAcceleratedNetworkValidator;

    public String build(String stackName, String customImageId, AzureCredentialView armCredentialView, AzureStackView armStack, CloudContext cloudContext,
            CloudStack cloudStack, AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        try {
            String imageUrl = cloudStack.getImage().getImageName();
            String imageName = customVMImageNameProvider.getImageNameFromConnectionString(imageUrl);

            Network network = cloudStack.getNetwork();
            Map<String, Object> model = new HashMap<>();
            AzureInstanceCredentialView azureInstanceCredentialView = new AzureInstanceCredentialView(cloudStack);
            model.put("credential", azureInstanceCredentialView);
            String rootDiskStorage = azureStorage.getImageStorageName(armCredentialView, cloudContext, cloudStack);
            AzureSecurityView armSecurityView = new AzureSecurityView(cloudStack.getGroups());

            AzureLoadBalancerModelBuilder loadBalancerModelBuilder = new AzureLoadBalancerModelBuilder(cloudStack, stackName);

            // needed for pre 1.16.5 templates and Load balancer setup on Medium duty datalakes.
            model.put("existingSubnetName", azureUtils.getCustomSubnetIds(network).stream().findFirst().orElse(""));

            model.put("customImageId", customImageId);
            model.put("storage_account_name", rootDiskStorage);
            model.put("image_storage_container_name", AzureStorage.IMAGES_CONTAINER);
            model.put("storage_container_name", azureStorage.getDiskContainerName(cloudContext));
            model.put("storage_vhd_name", imageName);
            model.put("stackname", stackName);
            model.put("region", cloudContext.getLocation().getRegion().value());
            model.put("subnet1Prefix", network.getSubnet().getCidr());
            model.put("groups", armStack.getGroups());
            model.put("igs", armStack.getInstanceGroups());
            model.put("securities", armSecurityView.getPorts());
            model.put("securityGroups", armSecurityView.getSecurityGroupIds());
            model.put("corecustomData", base64EncodedUserData(cloudStack.getImage().getUserDataByType(InstanceGroupType.CORE)));
            model.put("gatewaycustomData", base64EncodedUserData(cloudStack.getImage().getUserDataByType(InstanceGroupType.GATEWAY)));
            model.put("disablePasswordAuthentication", !azureInstanceCredentialView.passwordAuthenticationRequired());
            model.put("existingVPC", azureUtils.isExistingNetwork(network));
            model.put("resourceGroupName", azureUtils.getCustomResourceGroupName(network));
            model.put("existingVNETName", azureUtils.getCustomNetworkId(network));
            model.put("noPublicIp", azureUtils.isPrivateIp(network));
            model.put("noFirewallRules", false);
            model.put("userDefinedTags", cloudStack.getTags());
            model.put("acceleratedNetworkEnabled", azureAcceleratedNetworkValidator.validate(armStack));
            model.put("isUpscale", UPSCALE.equals(azureInstanceTemplateOperation));
            model.putAll(loadBalancerModelBuilder.buildModel());
            String generatedTemplate = freeMarkerTemplateUtils.processTemplateIntoString(getTemplate(cloudStack), model);
            LOGGER.info("Generated Arm template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateBuilder", e);
        }
    }

    public String buildParameters(CloudCredential credential, Network network, Image image) {
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(armTemplateParametersPath, "UTF-8"), new HashMap<>());
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateParameterBuilder", e);
        }
    }

    public String getTemplateString() {
        return getTemplate().toString();
    }

    public Template getTemplate(CloudStack stack) {
        try {
            return new Template(armTemplatePath, stack.getTemplate(), freemarkerConfiguration);
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't create template object", e);
        }
    }

    private Template getTemplate() {
        try {
            return freemarkerConfiguration.getTemplate(armTemplatePath, "UTF-8");
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't get ARM template", e);
        }
    }

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }
}
