package com.sequenceiq.cloudbreak.service.image;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@Service
public class ImageService {

    private static final String NAME = "image";

    @Inject
    private ImageNameUtil imageNameUtil;

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private ComponentRepository componentRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public Image getImage(Long stackId) {
        Component component = componentRepository.findComponentByStackIdComponentTypeName(stackId, ComponentType.IMAGE, NAME);
        try {
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(Image.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read image", e);
        }
    }

    public void create(Stack stack, PlatformParameters params) {
        try {
            CloudPlatform cloudPlatform = stack.cloudPlatform();
            String imageName = imageNameUtil.determineImageName(cloudPlatform, stack.getRegion());
            String tmpSshKey = tlsSecurityService.readPublicSshKey(stack.getId());
            String sshUser = stack.getCredential().getLoginUserName();
            Map<InstanceGroupType, String> userData = userDataBuilder.buildUserData(cloudPlatform, tmpSshKey, sshUser, params);
            Image image = new Image(imageName, userData);
            Component component = new Component(ComponentType.IMAGE, NAME, new Json(image), stack);
            componentRepository.save(component);
        } catch (JsonProcessingException e) {
            throw new CloudbreakServiceException("Failed to create json", e);
        } catch (CloudbreakSecuritySetupException e) {
            throw new CloudbreakServiceException("Failed to read temporary ssh credentials", e);
        }
    }
}
