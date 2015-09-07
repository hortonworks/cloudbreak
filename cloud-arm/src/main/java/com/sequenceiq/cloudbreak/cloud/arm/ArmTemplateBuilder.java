package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_ARM_PARAMETER_PATH;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_ARM_TEMPLATE_PATH;
import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Service("ArmTemplateBuilder")
public class ArmTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmTemplateBuilder.class);

    @Value("${cb.arm.template.path:" + CB_ARM_TEMPLATE_PATH + "}")
    private String armTemplatePath;

    @Value("${cb.arm.parameter.path:" + CB_ARM_PARAMETER_PATH + "}")
    private String armTemplateParametersPath;

    @Inject
    private Configuration freemarkerConfiguration;
    @Inject
    private ArmClient armClient;

    public String build(String stackName, CloudCredential cloudCredential, CloudStack cloudStack) {
        try {
            String imageName = cloudStack.getImage().getImageName();
            imageName = imageName.replace("https://", "");
            String[] split = imageName.split("/");
            ArmCredentialView armCredentialView = new ArmCredentialView(cloudCredential);
            Map<String, Object> model = new HashMap<>();
            model.put("storage_account_name", armClient.getStorageName(cloudCredential, cloudStack.getRegion()));
            model.put("image_storage_container_name", ArmSetup.IMAGES);
            model.put("storage_container_name", ArmSetup.VHDS);
            model.put("storage_vhd_name", split[2]);
            model.put("admin_user_name", cloudCredential.getLoginUserName());
            model.put("stackname", stackName);
            model.put("ssh_key", armCredentialView.getPublicKey());
            model.put("region", CloudRegion.valueOf(cloudStack.getRegion()).value());
            model.put("subnet1Prefix", cloudStack.getNetwork().getSubnet().getCidr());
            model.put("addressPrefix", cloudStack.getNetwork().getParameter("subnetCIDR", String.class));
            model.put("groups", cloudStack.getGroups());
            model.put("ports", cloudStack.getSecurity().getRules().get(0).getPorts());
            model.put("port_protocol", cloudStack.getSecurity().getRules().get(0).getProtocol());
            model.put("corecustomData", base64EncodedUserData(cloudStack.getImage().getUserData(InstanceGroupType.CORE)));
            model.put("gatewaycustomData", base64EncodedUserData(cloudStack.getImage().getUserData(InstanceGroupType.GATEWAY)));
            model.put("disablePasswordAuthentication", !armCredentialView.passwordAuthenticationRequired());
            model.put("adminPassword", armCredentialView.getPassword());
            String generatedTemplate = processTemplateIntoString(freemarkerConfiguration.getTemplate(armTemplatePath, "UTF-8"), model);
            LOGGER.debug("Generated Arm template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the Arm TemplateBuilder", e);
        }
    }

    public String buildParameters(CloudCredential credential, Network network, Image image) {
        try {
            return processTemplateIntoString(freemarkerConfiguration.getTemplate(armTemplateParametersPath, "UTF-8"),  new HashMap<>());
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the Arm TemplateParameterBuilder", e);
        }
    }

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }
}