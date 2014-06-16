package com.sequenceiq.cloudbreak.service.azure;

import java.io.IOException;

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

    public void generateSshKey(String path) throws IOException, InterruptedException {
        String[] commands = new String[]{
                //String.format("openssl req -x509 -nodes -days 365 -newkey rsa:1024 -keyout %s.pem -out %s.pem -batch", path, path),
                String.format("openssl x509 -inform pem -in %s.pem -outform der -out %s.cer", path, path),
                String.format("openssl pkcs12 -export -out %s.p12 -inkey %s.pem -in %s.pem -password pass:password", path, path, path),
                String.format("keytool -importkeystore -destkeystore %s.jks -srcstoretype PKCS12 -srckeystore %s.p12 "
                        + "-storepass password -srcstorepass password -noprompt", path, path)
        };

        for (String command : commands) {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        }
    }


}
