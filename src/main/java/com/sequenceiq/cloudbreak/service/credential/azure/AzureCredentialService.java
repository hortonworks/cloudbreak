package com.sequenceiq.cloudbreak.service.credential.azure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.AzureCredentialRepository;
import com.sequenceiq.cloudbreak.service.stack.azure.KeyGeneratorService;

@Service
public class AzureCredentialService {

    public static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DATADIR = "userdatas";
    private static final String CERTIFICATE_DATADIR = "certificate";
    private static final String SSH_DATADIR = "ssh";
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
            File userFolder = new File(getCertificateFolder(azureCredential, user.emailAsFolder()));
            if (!userFolder.exists()) {
                FileUtils.forceMkdir(new File(getCertificateFolder(azureCredential, user.emailAsFolder())));
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

    public File getSshPublicKeyFile(User user, Long templateId) {
        return new File(getPemFile(user.emailAsFolder(), templateId));
    }

    public void generateSshCertificate(User user, AzureTemplate azureTemplate, String sshKey) {
        try {
            File userFolder = new File(getSimpleUserFolder(user.emailAsFolder()));
            if (!userFolder.exists()) {
                FileUtils.forceMkdir(new File(getSimpleUserFolder(user.emailAsFolder())));
            }
            File sshFolder = new File(getSshFolder(user.emailAsFolder()));
            if (!sshFolder.exists()) {
                FileUtils.forceMkdir(new File(getSshFolder(user.emailAsFolder())));
            }
            File templateFolder = new File(getSshFolderForTemplate(user.emailAsFolder(), azureTemplate.getId()));
            if (!templateFolder.exists()) {
                FileUtils.forceMkdir(new File(getSshFolderForTemplate(user.emailAsFolder(), azureTemplate.getId())));
            }
            try {
                File file = new File(getPemFile(user.emailAsFolder(), azureTemplate.getId()));
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(sshKey);
                bw.close();
                keyGeneratorService.generateSshKey(getSshFolderForTemplate(user.emailAsFolder(), azureTemplate.getId()) + "/" + user.emailAsFolder());
            } catch (InterruptedException e) {
                LOGGER.error("An error occured under the ssh generation for {} template. The error was: {} {}", azureTemplate.getId(), e.getMessage(), e);
            }
        } catch (IOException ex) {
            LOGGER.error("An error occured under the ssh folder generation: {} {}", ex.getMessage(), ex);
        }
    }

    public static String getSshFolder(String user) {
        return String.format("%s/%s/%s", DATADIR, user, SSH_DATADIR);
    }

    public static String getCertificateFolder(String user) {
        return String.format("%s/%s/%s", DATADIR, user, CERTIFICATE_DATADIR);
    }

    public static String getSimpleUserFolder(String user) {
        return String.format("%s/%s", DATADIR, user);
    }

    public static String getSshFolderForTemplate(String user, Long templateId) {
        return String.format("%s/%s", getSshFolder(user), templateId);
    }

    public static String getPemFile(String user, Long templateId) {
        return String.format("%s/%s.pem", getSshFolderForTemplate(user, templateId), user);
    }

    public static String getCerFile(String user, Long templateId) {
        return String.format("%s/%s.cer", getSshFolderForTemplate(user, templateId), user);
    }

    public static String getCertificateFolder(Credential credential, String user) {
        return String.format("%s/%s/", getCertificateFolder(user), credential.getId());
    }

    public static String getUserJksFileName(Credential credential, String user) {
        return String.format("%s/%s/%s.jks", getCertificateFolder(user), credential.getId(), user);
    }

    public static String getUserCerFileName(Credential credential, String user) {
        return String.format("%s/%s/%s.cer", getCertificateFolder(user), credential.getId(), user);
    }
}
