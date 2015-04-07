package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

import java.util.Map;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.openstack4j.api.OSClient;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component
@Lazy
public class OpenStackUtil {

    private static final String CB_KEYPAIR_NAME = "cb-keypair-";

    @Autowired
    @Lazy
    private PBEStringCleanablePasswordEncryptor encryptor;

    public OSClient createOSClient(Stack stack) {
        return createOSClient((OpenStackCredential) stack.getCredential());
    }

    public OSClient createOSClient(OpenStackCredential credential) {
        return OSFactory.builder().endpoint(credential.getEndpoint())
                .credentials(encryptor.decrypt(credential.getUserName()), encryptor.decrypt(credential.getPassword()))
                .tenantName(credential.getTenantName())
                .authenticate();
    }

    public String getKeyPairName(OpenStackCredential credential) {
        return CB_KEYPAIR_NAME + deleteWhitespace(credential.getName().toLowerCase());
    }

    public String getInstanceId(String uuid, Map<String, String> metadata) {
        return uuid + "_" + getNormalizedGroupName(metadata.get(HeatTemplateBuilder.CB_INSTANCE_GROUP_NAME)) + "_"
                + metadata.get(HeatTemplateBuilder.CB_INSTANCE_PRIVATE_ID);
    }

    public String getNormalizedGroupName(String groupName) {
        return groupName.replaceAll("_", "");
    }

}
