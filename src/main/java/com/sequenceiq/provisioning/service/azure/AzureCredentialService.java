package com.sequenceiq.provisioning.service.azure;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.InternalServerException;
import com.sequenceiq.provisioning.controller.json.CredentialJson;
import com.sequenceiq.provisioning.converter.AzureCredentialConverter;
import com.sequenceiq.provisioning.domain.AzureCredential;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.AzureCredentialRepository;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.service.CredentialService;

@Service
public class AzureCredentialService implements CredentialService {

    private static final String DATADIR = "userdatas";
    private static final String ENTRY = "mydomain";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AzureCredentialRepository azureCredentialRepository;

    @Autowired
    private AzureCredentialConverter azureCredentialConverter;

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    @Override
    public void saveCredentials(User user, CredentialJson credentialRequest) {
        try {
            User managedUser = userRepository.findByEmail(user.getEmail());
            AzureCredential azureCredential = azureCredentialConverter.convert(credentialRequest);
            azureCredential.setUser(user);
            azureCredentialRepository.save(azureCredential);
            generateCertificate(azureCredential, managedUser);
        } catch (Exception e) {
            throw new InternalServerException("Failed to generate Azure certificate.", e);
        }
    }

    @Override
    public Set<CredentialJson> retrieveCredentials(User user) {
        return azureCredentialConverter.convertAllEntityToJson(user.getAzureCredentials());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    public File getCertificateFile(User user) throws Exception {
        return new File(getUserCerFileName(user.emailAsFolder()));
    }

    private void generateCertificate(AzureCredential azureCredential, User user) throws Exception {
        File sourceFolder = new File(DATADIR);
        if (!sourceFolder.exists()) {
            FileUtils.forceMkdir(new File(DATADIR));
        }
        File userFolder = new File(getUserFolder(user.emailAsFolder()));
        if (!userFolder.exists()) {
            FileUtils.forceMkdir(new File(getUserFolder(user.emailAsFolder())));
        }
        if (new File(getUserJksFileName(user.emailAsFolder())).exists()) {
            FileUtils.forceDelete(new File(getUserJksFileName(user.emailAsFolder())));
        }
        keyGeneratorService.generateKey(user, ENTRY, getUserJksFileName(user.emailAsFolder()));
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] pass = azureCredential.getJks().toCharArray();
        java.io.FileInputStream fis = null;
        try {
            fis = new java.io.FileInputStream(new File(getUserJksFileName(user.emailAsFolder())));
            ks.load(fis, pass);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        Certificate certificate = ks.getCertificate(ENTRY);
        final FileOutputStream os = new FileOutputStream(getUserCerFileName(user.emailAsFolder()));
        os.write(Base64.encodeBase64(certificate.getEncoded(), true));
        os.close();
    }

    private String getUserFolder(String user) {
        return String.format("%s/%s/", DATADIR, user);
    }

    private String getUserJksFileName(String user) {
        return String.format("%s/%s/%s.jks", DATADIR, user, user);
    }

    private String getUserCerFileName(String user) {
        return String.format("%s/%s/%s.cer", DATADIR, user, user);
    }
}
