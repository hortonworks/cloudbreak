package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;

@Component
public class AzureStackUtil {

    public static final int NOT_FOUND = 404;
    public static final String NAME = "name";
    public static final String SERVICENAME = "serviceName";
    public static final String ERROR = "\"error\":\"Could not fetch data from azure\"";
    public static final String CREDENTIAL = "credential";
    public static final String EMAILASFOLDER = "emailAsFolder";
    public static final String IMAGE_NAME = "ambari-docker-v1";

    public String getVmName(String azureTemplate, int i) {
        return String.format("%s-%s", azureTemplate, i);
    }

    public String getOsImageName(Credential credential) {
        return String.format("%s-%s", ((AzureCredential) credential).getName().replaceAll("\\s+", ""), IMAGE_NAME);
    }

    public AzureClient createAzureClient(Credential credential, String filePath) {
        File file = new File(filePath);
        return new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(), file.getAbsolutePath(), ((AzureCredential) credential).getJks()
        );
    }

    public X509Certificate createX509Certificate(AzureCredential azureCredential, String emailAsFolder) throws FileNotFoundException, CertificateException {
        return new X509Certificate(AzureCertificateService.getCerFile(emailAsFolder, azureCredential.getId()));
    }
}
