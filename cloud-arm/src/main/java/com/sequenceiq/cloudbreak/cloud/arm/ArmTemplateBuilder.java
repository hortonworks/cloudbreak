package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_ARM_PARAMETER_PATH;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_ARM_TEMPLATE_PATH;
import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;

import javax.inject.Inject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmGroupView;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmSecurityView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Inject
    private ArmUtils armUtils;

    public String build(String stackName, CloudCredential cloudCredential, CloudContext cloudContext, CloudStack cloudStack) {
        try {
            String imageName = cloudStack.getImage().getImageName();
            imageName = imageName.replace("https://", "");
            String[] split = imageName.split("/");
            ArmCredentialView armCredentialView = new ArmCredentialView(cloudCredential);
            Map<String, Object> model = new HashMap<>();
            if (armUtils.isPersistentStorage()) {
                model.put("storage_account_name",
                        armUtils.getPersistentStorageName(cloudCredential, cloudContext.getLocation().getRegion().value()));
                model.put("attached_disk_storage_account_name",
                        armUtils.getStorageName(cloudCredential, cloudContext, cloudContext.getLocation().getRegion().value()));
            } else {
                model.put("storage_account_name", armUtils.getStorageName(cloudCredential, cloudContext, cloudContext.getLocation().getRegion().value()));
                model.put("attached_disk_storage_account_name",
                        armUtils.getStorageName(cloudCredential, cloudContext, cloudContext.getLocation().getRegion().value()));
            }
            model.put("image_storage_container_name", ArmSetup.IMAGES);
            model.put("storage_container_name", armUtils.getDiskContainerName(cloudContext));
            model.put("storage_vhd_name", split[2]);
            model.put("admin_user_name", cloudCredential.getLoginUserName());
            model.put("stackname", stackName);
            model.put("ssh_key", armCredentialView.getPublicKey());
            model.put("region", cloudContext.getLocation().getRegion().value());
            model.put("subnet1Prefix", cloudStack.getNetwork().getSubnet().getCidr());
            model.put("groups", new ArmGroupView(cloudStack.getGroups()).getGroups());
            model.put("securities", new ArmSecurityView(cloudStack.getSecurity()));
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
            return processTemplateIntoString(freemarkerConfiguration.getTemplate(armTemplateParametersPath, "UTF-8"), new HashMap<>());
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the Arm TemplateParameterBuilder", e);
        }
    }

    private String base64EncodedUserData(String data) {
        return new String(Base64.encodeBase64(String.format("%s", data).getBytes()));
    }
}