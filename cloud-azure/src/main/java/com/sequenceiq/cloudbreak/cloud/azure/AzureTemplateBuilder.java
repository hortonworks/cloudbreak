package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation.UPSCALE;
import static com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.SupportedSourceMarketplaceImage.CLOUDERA;
import static com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.SupportedSourceMarketplaceImage.REDHAT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureAcceleratedNetworkValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureSecurityView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service("AzureTemplateBuilder")
public class AzureTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateBuilder.class);

    @Value("${cb.arm.template.path:}")
    private String armTemplatePath;

    @Value("${cb.arm.template.lb.path:}")
    private String armTemplateLbPath;

    @Value("${cb.arm.template.remove.publicip.path:}")
    private String armTemplateRemovePublicIpPath;

    @Value("${cb.arm.template.attach.publicip.path:}")
    private String armTemplateAttachPublicIpPath;

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

    @Inject
    private AzurePlatformResources platformResources;

    //CHECKSTYLE:OFF
    /**
     * Build an Azure Resource Manager template from a freemarker template, using the provided arguments.
     *
     * The ARM template is a JSON string that conforms with the ARM template syntax.
     *
     * @param stackName name of the stack, used as a prefix for a number of fields in the ARM template
     * @param customImageId the id of a custom image to use for storage on an azure Virtual Machine
     * @param armCredentialView provides information about the Azure subscription we're interacting with
     * @param armStack provides group and instance group information when provisioning virtual machines
     * @param cloudContext provides information about the Azure environment we're deploying to, used to lookup region and environment information
     * @param cloudStack provides user tags, storage image name, network information, and Azure environment information
     * @param azureInstanceTemplateOperation provides information about whether this template is part of an upscale operation
     * @param azureMarketplaceImage provides a partner center image for creating VMs
     * @return an ARM template, formatted as JSON
     */
    public String build(String stackName, String customImageId, AzureCredentialView armCredentialView, AzureStackView armStack, CloudContext cloudContext,
            CloudStack cloudStack, AzureInstanceTemplateOperation azureInstanceTemplateOperation, AzureMarketplaceImage azureMarketplaceImage) {
        try {
            String imageUrl = cloudStack.getImage().getImageName();
            String imageName = customVMImageNameProvider.getImageNameFromConnectionString(imageUrl);

            Network network = cloudStack.getNetwork();
            Map<String, Object> model = new HashMap<>();
            AzureInstanceCredentialView azureInstanceCredentialView = new AzureInstanceCredentialView(cloudStack);
            model.put("credential", azureInstanceCredentialView);
            String rootDiskStorage = azureStorage.getImageStorageName(armCredentialView, cloudContext, cloudStack);
            AzureSecurityView armSecurityView = new AzureSecurityView(cloudStack.getGroups());
            boolean containsMarketplaceImageDetails = cloudStack.getTemplate().contains("marketplaceImageDetails");

            AzureLoadBalancerModelBuilder loadBalancerModelBuilder = new AzureLoadBalancerModelBuilder(cloudStack, stackName);
            Region region = cloudContext.getLocation().getRegion();
            CloudVmTypes cloudVmTypes = platformResources.virtualMachinesNonExtended(armCredentialView.getCloudCredential(), region, null);

            LOGGER.debug("MultiAz is {}",cloudStack.isMultiAz());

            // needed for pre 1.16.5 templates and Load balancer setup on Medium duty datalakes.
            model.put("existingSubnetName", azureUtils.getCustomSubnetIds(network).stream().findFirst().orElse(""));
            model.put("endpointGwSubnet", azureUtils.getCustomEndpointGatewaySubnetIds(network).stream().findFirst().orElse(""));
            model.put("usePartnerCenter", Objects.nonNull(azureMarketplaceImage)
                    && !azureMarketplaceImage.isUsedAsSourceImage());
            model.put("marketplaceImageDetails", azureMarketplaceImage);
            model.put("useSourceImagePlan", Objects.nonNull(azureMarketplaceImage)
                    && (isRedHatByos(azureMarketplaceImage) || isClouderaByos(azureMarketplaceImage))
                    && azureMarketplaceImage.isUsedAsSourceImage());
            model.put("customImageId", customImageId);
            model.put("storage_account_name", rootDiskStorage);
            model.put("image_storage_container_name", AzureStorage.IMAGES_CONTAINER);
            model.put("storage_container_name", azureStorage.getDiskContainerName(cloudContext));
            model.put("storage_vhd_name", imageName);
            model.put("stackname", stackName);
            model.put("region", region.value());
            model.put("subnet1Prefix", network.getSubnet().getCidr());
            model.put("groups", armStack.getInstancesByGroupType());
            model.put("igs", armStack.getInstanceGroups());
            model.put("securities", armSecurityView.getPorts());
            model.put("securityGroups", armSecurityView.getSecurityGroupIds());
            model.put("corecustomData", base64EncodedUserData(cloudStack.getCoreUserData()));
            model.put("gatewaycustomData", base64EncodedUserData(cloudStack.getGatewayUserData()));
            model.put("disablePasswordAuthentication", !azureInstanceCredentialView.passwordAuthenticationRequired());
            model.put("existingVPC", azureUtils.isExistingNetwork(network));
            model.put("resourceGroupName", azureUtils.getCustomResourceGroupName(network));
            model.put("existingVNETName", azureUtils.getCustomNetworkId(network));
            model.put("noPublicIp", azureUtils.isPrivateIp(network));
            model.put("noFirewallRules", false);
            model.put("userDefinedTags", cloudStack.getTags());
            model.put("acceleratedNetworkEnabled", azureAcceleratedNetworkValidator
                    .validate(armStack, cloudVmTypes.getCloudVmResponses().getOrDefault(region.value(), Set.of())));
            model.put("isUpscale", UPSCALE.equals(azureInstanceTemplateOperation));
            model.putAll(loadBalancerModelBuilder.buildModel());
            model.put("multiAz", cloudStack.isMultiAz());
            String generatedTemplate = generateTemplate(cloudStack, model, containsMarketplaceImageDetails);
            LOGGER.info("Generated Arm template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateBuilder", e);
        }
    }

    public String buildPublicIpDetachForVMs(String stackName, CloudContext cloudContext, AzureStackView armStack, CloudStack cloudStack) {
        Network network = cloudStack.getNetwork();
        Map<String, Object> model = new HashMap<>();
        model.put("groups", armStack.getInstancesByGroupType());
        model.put("igs", armStack.getInstanceGroups());
        model.put("stackname", stackName);
        model.put("existingVPC", azureUtils.isExistingNetwork(network));
        model.put("resourceGroupName", azureUtils.getCustomResourceGroupName(network));
        model.put("existingVNETName", azureUtils.getCustomNetworkId(network));
        model.put("region", cloudContext.getLocation().getRegion().value());
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(getTemplate(armTemplateRemovePublicIpPath), model);
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateBuilder for public IP detachment", e);
        }
    }

    public String buildAttachPublicIpsForVMsAndAddLB(String stackName, CloudContext cloudContext, AzureCredentialView armCredentialView, AzureStackView armStack,
            CloudStack cloudStack) {
        Network network = cloudStack.getNetwork();
        Region region = cloudContext.getLocation().getRegion();
        AzureLoadBalancerModelBuilder loadBalancerModelBuilder = new AzureLoadBalancerModelBuilder(cloudStack, stackName);
        AzureSecurityView armSecurityView = new AzureSecurityView(cloudStack.getGroups());
        CloudVmTypes cloudVmTypes = platformResources.virtualMachinesNonExtended(armCredentialView.getCloudCredential(), region, null);
        Map<String, Object> model = new HashMap<>();
        model.put("groups", armStack.getInstancesByGroupType());
        model.put("igs", armStack.getInstanceGroups());
        model.put("stackname", stackName);
        model.put("existingVPC", azureUtils.isExistingNetwork(network));
        model.put("resourceGroupName", azureUtils.getCustomResourceGroupName(network));
        model.put("existingVNETName", azureUtils.getCustomNetworkId(network));
        model.put("region", region.value());
        model.put("noPublicIp", azureUtils.isPrivateIp(network));
        model.put("multiAz", cloudStack.isMultiAz());
        model.put("userDefinedTags", cloudStack.getTags());
        model.put("existingSubnetName", azureUtils.getCustomSubnetIds(network).stream().findFirst().orElse(""));
        model.put("endpointGwSubnet", azureUtils.getCustomEndpointGatewaySubnetIds(network).stream().findFirst().orElse(""));
        model.put("securityGroups", armSecurityView.getSecurityGroupIds());
        model.put("acceleratedNetworkEnabled", azureAcceleratedNetworkValidator
                .validate(armStack, cloudVmTypes.getCloudVmResponses().getOrDefault(region.value(), Set.of())));
        model.putAll(loadBalancerModelBuilder.buildModel());
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(getTemplate(armTemplateAttachPublicIpPath), model);
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateBuilder for public IP and LB attachment", e);
        }
    }

    public String buildLoadBalancer(String stackName, AzureCredentialView armCredentialView, AzureStackView armStack, CloudContext cloudContext,
            CloudStack cloudStack, AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        if (!StringUtils.hasText(armTemplateLbPath)) {
            LOGGER.debug("No ARM template for Load Balancer. Skipping template generation");
            return null;
        } else {
            try {
                Network network = cloudStack.getNetwork();
                AzureSecurityView armSecurityView = new AzureSecurityView(cloudStack.getGroups());

                AzureLoadBalancerModelBuilder loadBalancerModelBuilder = new AzureLoadBalancerModelBuilder(cloudStack, stackName);
                Region region = cloudContext.getLocation().getRegion();
                CloudVmTypes cloudVmTypes = platformResources.virtualMachinesNonExtended(armCredentialView.getCloudCredential(), region, null);

                Map<String, Object> model = buildLbModelForTemplate(stackName, armStack, cloudStack, region, armSecurityView, network, cloudVmTypes,
                        loadBalancerModelBuilder);
                String generatedTemplate = freeMarkerTemplateUtils
                        .processTemplateIntoString(getTemplate(armTemplateLbPath), model);
                LOGGER.info("Generated Arm template for Load Balancer: {}", generatedTemplate);
                return generatedTemplate;
            } catch (IOException | TemplateException e) {
                throw new CloudConnectorException("Failed to process the ARM TemplateBuilder", e);
            }
        }
    }

    private @NotNull Map<String, Object> buildLbModelForTemplate(String stackName, AzureStackView armStack, CloudStack cloudStack, Region region,
            AzureSecurityView armSecurityView, Network network, CloudVmTypes cloudVmTypes, AzureLoadBalancerModelBuilder loadBalancerModelBuilder) {
        Map<String, Object> model = new HashMap<>();
        model.put("stackname", stackName);
        model.put("region", region.value());
        model.put("groups", armStack.getInstancesByGroupType());
        model.put("igs", armStack.getInstanceGroups());
        model.put("securityGroups", armSecurityView.getSecurityGroupIds());
        model.put("existingVPC", azureUtils.isExistingNetwork(network));
        model.put("resourceGroupName", azureUtils.getCustomResourceGroupName(network));
        model.put("existingVNETName", azureUtils.getCustomNetworkId(network));
        model.put("userDefinedTags", cloudStack.getTags());
        model.put("acceleratedNetworkEnabled", azureAcceleratedNetworkValidator
                .validate(armStack, cloudVmTypes.getCloudVmResponses().getOrDefault(region.value(), Set.of())));
        model.putAll(loadBalancerModelBuilder.buildModel());
        model.put("multiAz", cloudStack.isMultiAz());
        return model;
    }

    private boolean isRedHatByos(AzureMarketplaceImage azureMarketplaceImage) {
        return Optional.ofNullable(azureMarketplaceImage).map(AzureMarketplaceImage::getPublisherId).orElse("").equalsIgnoreCase(REDHAT.getPublisher())
                && Optional.ofNullable(azureMarketplaceImage).map(AzureMarketplaceImage::getOfferId).orElse("").matches(REDHAT.getOffer());
    }

    private boolean isClouderaByos(AzureMarketplaceImage azureMarketplaceImage) {
        return Optional.ofNullable(azureMarketplaceImage).map(AzureMarketplaceImage::getPublisherId).orElse("").equalsIgnoreCase(CLOUDERA.getPublisher())
                && Optional.ofNullable(azureMarketplaceImage).map(AzureMarketplaceImage::getOfferId).orElse("").matches(CLOUDERA.getOffer());
    }

    public String buildParameters() {
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(armTemplateParametersPath, "UTF-8"), new HashMap<>());
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateParameterBuilder", e);
        }
    }

    public String getTemplateString() {
        return getTemplate(armTemplatePath).toString();
    }

    // Quickfix for https://cloudera.atlassian.net/browse/CB-24844
    // If the stored arm template for the stack doesn't contain marketplace image information, we have to use the latest version of the arm template.
    // In this case after template generation we have to replace the LoadBalancers' sku type to "Basic", because without this the deployment creation will be
    // failed on Azure.
    private String generateTemplate(CloudStack stack, Map<String, Object> model, boolean containsMarketplaceImageDetails)
            throws IOException, TemplateException {
        return freeMarkerTemplateUtils.processTemplateIntoString(getTemplate(stack.getTemplate(), containsMarketplaceImageDetails), model);
    }

    private Template getTemplate(String storedTemplate, boolean containsMarketplaceImageDetails) {
        try {
            if (containsMarketplaceImageDetails) {
                LOGGER.debug("Stored arm template contains marketplace image details, let's use it.");
                return new Template(armTemplatePath, storedTemplate, freemarkerConfiguration);
            } else {
                LOGGER.debug("Stored arm template doesn't contains marketplace image details, we will use the latest one.");
                return getTemplate(armTemplatePath);
            }
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't create template object", e);
        }
    }

    private Template getTemplate(String templatePath) {
        try {
            return freemarkerConfiguration.getTemplate(templatePath, "UTF-8");
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't get ARM template", e);
        }
    }

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }
}