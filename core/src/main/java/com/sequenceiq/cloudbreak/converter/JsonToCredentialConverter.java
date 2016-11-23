package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class JsonToCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, Credential> {
    private static final String SSH_USER_CENT = "centos";

    private static final String SSH_USER_CB = "cloudbreak";

    private static final String SSH_USER_EC2 = "ec2-user";

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    @Inject
    private TopologyService topologyService;

    @Override
    public Credential convert(CredentialRequest source) {
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credential.setPublicKey(source.getPublicKey());
        String cloudPlatform = source.getCloudPlatform();
        credential.setCloudPlatform(cloudPlatform);
        Map<String, Object> parameters = credentialDefinitionService.processProperties(platform(cloudPlatform), source.getParameters());
        if (parameters != null && !parameters.isEmpty()) {
            try {
                credential.setAttributes(new Json(parameters));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        if (source.getLoginUserName() != null) {
            throw new BadRequestException("You can not modify the default user!");
        }
        setUserName(credential, source.getParameters());
        if (source.getTopologyId() != null) {
            credential.setTopology(topologyService.get(source.getTopologyId()));
        }
        return credential;
    }

    private void setUserName(Credential credential, Map<String, Object> parameters) {
        if (parameters.containsKey("keystoneVersion")) {
            credential.setLoginUserName(SSH_USER_CENT);
        } else if (parameters.containsKey("roleArn")
                || (parameters.containsKey("accessKey") && parameters.containsKey("secretKey") && !parameters.containsKey("subscriptionId"))) {
            credential.setLoginUserName(SSH_USER_EC2);
        } else {
            credential.setLoginUserName(SSH_USER_CB);
        }
    }

}
