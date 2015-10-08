package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class KeyGeneratorService {

    public void generateKey(String user, String alias, String path) throws Exception {
        String command = StringUtils.join(new String[]{
                "keytool",
                "-genkeypair",
                "-alias", alias,
                "-keyalg", "RSA",
                "-keystore", path,
                "-keysize", "2048",
                "-keypass", AzureStackUtil.DEFAULT_JKS_PASS,
                "-storepass", AzureStackUtil.DEFAULT_JKS_PASS,
                "-dname", "cn=" + user + ",ou=engineering,o=company,c=US"
        }, " ");
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
    }

}
