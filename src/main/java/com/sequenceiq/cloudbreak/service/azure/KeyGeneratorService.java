package com.sequenceiq.cloudbreak.service.azure;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.User;

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
