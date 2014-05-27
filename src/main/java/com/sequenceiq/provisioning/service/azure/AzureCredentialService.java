package com.sequenceiq.provisioning.service.azure;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.domain.AzureCredential;
import com.sequenceiq.provisioning.domain.Credential;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.AzureCredentialRepository;

@Service
public class AzureCredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialService.class);

    private static final String DATADIR = "userdatas";
    private static final String ENTRY = "mydomain";

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    @Autowired
    private AzureCredentialRepository azureCredentialRepository;

    public File getCertificateFile(Long credentialId, User user) {
        AzureCredential credential = azureCredentialRepository.findOne(credentialId);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found", credentialId));
        }
        return new File(getUserCerFileName(credential, user.emailAsFolder()));
    }

    public void generateCertificate(AzureCredential azureCredential, User user) {
        try {
            File sourceFolder = new File(DATADIR);
            if (!sourceFolder.exists()) {
                FileUtils.forceMkdir(new File(DATADIR));
            }
            File userFolder = new File(getUserFolder(azureCredential, user.emailAsFolder()));
            if (!userFolder.exists()) {
                FileUtils.forceMkdir(new File(getUserFolder(azureCredential, user.emailAsFolder())));
            }
            if (new File(getUserJksFileName(azureCredential, user.emailAsFolder())).exists()) {
                FileUtils.forceDelete(new File(getUserJksFileName(azureCredential, user.emailAsFolder())));
            }
            keyGeneratorService.generateKey(user, azureCredential, ENTRY, getUserJksFileName(azureCredential, user.emailAsFolder()));
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] pass = azureCredential.getJks().toCharArray();
            java.io.FileInputStream fis = null;
            try {
                fis = new java.io.FileInputStream(new File(getUserJksFileName(azureCredential, user.emailAsFolder())));
                ks.load(fis, pass);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
            Certificate certificate = ks.getCertificate(ENTRY);
            final FileOutputStream os = new FileOutputStream(getUserCerFileName(azureCredential, user.emailAsFolder()));
            os.write(Base64.encodeBase64(certificate.getEncoded(), true));
            os.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static String getUserFolder(Credential credential, String user) {
        return String.format("%s/%s/%s/", DATADIR, user, credential.getId());
    }

    public static String getUserJksFileName(Credential credential, String user) {
        return String.format("%s/%s/%s/%s.jks", DATADIR, user, credential.getId(), user);
    }

    public static String getUserCerFileName(Credential credential, String user) {
        return String.format("%s/%s/%s/%s.cer", DATADIR, user, credential.getId(), user);
    }
}
