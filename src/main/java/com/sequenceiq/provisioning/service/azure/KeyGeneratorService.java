package com.sequenceiq.provisioning.service.azure;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.AzureCredential;
import com.sequenceiq.provisioning.domain.User;

@Component
public class KeyGeneratorService {

    public void generateKey(User user, AzureCredential azureCredential, String alias, String path) throws Exception {
        sun.security.tools.KeyTool.main(new String[]{
                "-genkeypair",
                "-alias", alias,
                "-keyalg", "RSA",
                "-keystore", path,
                "-keysize", "2048",
                "-keypass", azureCredential.getJks(),
                "-storepass", azureCredential.getJks(),
                "-dname", "cn=" + user.getLastName() + ", ou=engineering, o=company, c=US"
        });
    }

}
