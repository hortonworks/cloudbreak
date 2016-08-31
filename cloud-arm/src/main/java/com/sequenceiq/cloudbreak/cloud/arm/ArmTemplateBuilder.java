package com.sequenceiq.cloudbreak.cloud.arm;

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
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmSecurityView;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmStackView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Service("ArmTemplateBuilder")
public class ArmTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmTemplateBuilder.class);

    @Value("${cb.arm.template.path:}")
    private String armTemplatePath;

    @Value("${cb.arm.parameter.path:}")
    private String armTemplateParametersPath;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private ArmUtils armUtils;

    @Inject
    private ArmStorage armStorage;

    public String build(String stackName, ArmCredentialView armCredentialView, ArmStackView armStack, CloudContext cloudContext, CloudStack cloudStack) {
        try {
            String imageUrl = cloudStack.getImage().getImageName();
            String imageName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            Network network = cloudStack.getNetwork();
            Map<String, Object> model = new HashMap<>();
            model.put("credential", armCredentialView);
            String rootDiskStorage = armStorage.getImageStorageName(armCredentialView, cloudContext,
                    armStorage.getPersistentStorageName(cloudStack.getParameters()),
                    armStorage.getArmAttachedStorageOption(cloudStack.getParameters()));
            ArmSecurityView armSecurityView = new ArmSecurityView(cloudStack.getGroups());

            model.put("storage_account_name", rootDiskStorage);
            model.put("image_storage_container_name", ArmStorage.IMAGES);
            model.put("storage_container_name", armStorage.getDiskContainerName(cloudContext));
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
            model.put("existingVPC", armUtils.isExistingNetwork(network));
            model.put("resourceGroupName", armUtils.getCustomResourceGroupName(network));
            model.put("existingVNETName", armUtils.getCustomNetworkId(network));
            model.put("existingSubnetName", armUtils.getCustomSubnetId(network));
            model.put("userImageName", String.format("https://%s.blob.core.windows.net/%s/%s", rootDiskStorage, ArmStorage.IMAGES, imageName));
            model.put("osDiskVhdName", String.format("https://%s.blob.core.windows.net/%s/%sosDisk",
                    rootDiskStorage, armStorage.getDiskContainerName(cloudContext), stackName));
            model.put("sshKeyPath", String.format("/home/%s/.ssh/authorized_keys", armCredentialView.getLoginUserName()));
            model.put("ipConfigurationsAddress", String.format("'/frontendIPConfigurations/%sipcn", stackName));
            model.put("ilbBackendAddress", String.format("/backendAddressPools/%sbapn", stackName));
            model.put("loadBalancerAddress", String.format("Microsoft.Network/loadBalancers/%slb", stackName));
            model.put("virtualNetworkAddress", String.format("Microsoft.Network/virtualNetworks/%s", stackName));
            model.put("publicIpAddressId", String.format("Microsoft.Network/publicIPAddresses/%s", stackName));
            model.put("networkInterfaceAddress", String.format("Microsoft.Network/networkInterfaces/%s", stackName));
            model.put("osDiskName", String.format("%s-osDisk", stackName));
            model.put("dataDiskAddress", String.format("%s/%sdatadisk", armStorage.getDiskContainerName(cloudContext), stackName));

            if (armUtils.isExistingNetwork(network)) {
                model.put("vnetID", String.format("%sMicrosoft.Network/virtualNetworks%s",
                        armUtils.getCustomResourceGroupName(network), armUtils.getCustomNetworkId(network)));
                model.put("subnet1Address", String.format("/subnets/%s", armUtils.getCustomSubnetId(network)));
            } else {
                model.put("vnetID", String.format("Microsoft.Network/virtualNetworks%s", stackName));
                model.put("subnet1Address", String.format("/subnets/%ssubnet", stackName, stackName));
            }
            model.put("lbID", String.format("Microsoft.Network/loadBalancers%slb", stackName));
            String generatedTemplate = processTemplateIntoString(freemarkerConfiguration.getTemplate(armTemplatePath, "UTF-8"), model);
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

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }
}