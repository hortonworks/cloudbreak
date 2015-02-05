package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class KeyGeneratorService {

    public void generateKey(String user, AzureCredential azureCredential, String alias, String path) throws Exception {
        String command = StringUtils.join(new String[]{
                "keytool",
                "-genkeypair",
                "-alias", alias,
                "-keyalg", "RSA",
                "-keystore", path,
                "-keysize", "2048",
                "-keypass", AzureStackUtil.DEFAULT_JKS_PASS,
                "-storepass", AzureStackUtil.DEFAULT_JKS_PASS,
                "-dname", "cn=" + user + azureCredential.getPostFix() + ",ou=engineering,o=company,c=US"
        }, " ");
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
    }

    public void generateSshKey(String path) throws IOException, InterruptedException {
        String pemData = FileReaderUtils.readFileFromPathToString(path + ".pem");
        int headerEndOffset = pemData.indexOf('\n');
        int footerStartOffset = pemData.indexOf("-----END");
        String strippedPemData = pemData.substring(headerEndOffset + 1, footerStartOffset - 1);
        byte[] derBytes = Base64.decodeBase64(strippedPemData.getBytes());
        File file = new File(path + ".cer");
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(derBytes);
        fos.close();
    }

}
