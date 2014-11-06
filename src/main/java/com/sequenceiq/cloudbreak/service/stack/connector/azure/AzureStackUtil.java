package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Component
public class AzureStackUtil {

    public static final int NOT_FOUND = 404;
    public static final String NAME = "name";
    public static final String SERVICENAME = "serviceName";
    public static final String ERROR = "\"error\":\"Could not fetch data from azure\"";
    public static final String CREDENTIAL = "credential";
    public static final String EMAILASFOLDER = "emailAsFolder";
    public static final String IMAGE_NAME = "ambari-docker-v1";

    @Autowired
    private UserDetailsService userDetailsService;

    public String getOsImageName(Credential credential) {
        return String.format("%s-%s", ((AzureCredential) credential).getCommonName(), IMAGE_NAME);
    }

    public AzureClient createAzureClient(Credential credential, String filePath) {
        File file = new File(filePath);
        return new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(), file.getAbsolutePath(), ((AzureCredential) credential).getJks());
    }

    public static Map<String, String> createVMContext(String vmName) {
        Map<String, String> context = new HashMap<>();
        context.put(SERVICENAME, vmName);
        context.put(NAME, vmName);
        return context;
    }

    public String emailAsFolder(String userId) {
        String email = userDetailsService.getDetails(userId, UserFilterField.USERID).getUsername();
        return email.replaceAll("@", "_").replace(".", "_");
    }

    public String emailAsFolder(CbUser user) {
        return user.getUsername().replaceAll("@", "_").replace(".", "_");
    }
}
