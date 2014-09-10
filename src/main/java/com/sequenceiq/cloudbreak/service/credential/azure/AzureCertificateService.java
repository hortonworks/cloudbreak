package com.sequenceiq.cloudbreak.service.credential.azure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.repository.AzureCredentialRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.KeyGeneratorService;

@Service
public class AzureCertificateService {

    public static final Logger LOGGER = LoggerFactory.getLogger(AzureCertificateService.class);
    private static final String DATADIR = "userdatas";
    private static final String CERTIFICATE_DATADIR = "certificate";
    private static final String SSH_DATADIR = "ssh";
    private static final String ENTRY = "mydomain";

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    @Autowired
    private AzureCredentialRepository azureCredentialRepository;

    @Autowired
    private AzureStackUtil azureStackUtil;

    public static String getSshFolder(String user) {
        return String.format("%s/%s/%s", DATADIR, user, SSH_DATADIR);
    }

    public static String getCertificateFolder(String user) {
        return String.format("%s/%s/%s", DATADIR, user, CERTIFICATE_DATADIR);
    }

    public static String getSimpleUserFolder(String user) {
        return String.format("%s/%s", DATADIR, user);
    }

    public static String getSshFolderForTemplate(String user, Long credentialId) {
        return String.format("%s/%s", getSshFolder(user), credentialId);
    }

    public static String getPemFile(String user, Long credentialId) {
        return String.format("%s/%s.pem", getSshFolderForTemplate(user, credentialId), user);
    }

    public static String getCerFile(String user, Long credentialId) {
        return String.format("%s/%s.cer", getSshFolderForTemplate(user, credentialId), user);
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

    public File getCertificateFile(Long credentialId, CbUser user) {
        AzureCredential credential = azureCredentialRepository.findOne(credentialId);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found", credentialId));
        }
        return new File(getUserCerFileName(credential, azureStackUtil.emailAsFolder(user.getUsername())));
    }

    public void generateCertificate(CbUser user, AzureCredential azureCredential) {
        try {
            String emailAsFolder = azureStackUtil.emailAsFolder(user.getUsername());
            File sourceFolder = new File(DATADIR);
            if (!sourceFolder.exists()) {
                FileUtils.forceMkdir(new File(DATADIR));
            }
            File userFolder = new File(getCertificateFolder(azureCredential, emailAsFolder));
            if (!userFolder.exists()) {
                FileUtils.forceMkdir(new File(getCertificateFolder(azureCredential, emailAsFolder)));
            }
            if (new File(getUserJksFileName(azureCredential, emailAsFolder)).exists()) {
                FileUtils.forceDelete(new File(getUserJksFileName(azureCredential, emailAsFolder)));
            }
            keyGeneratorService.generateKey(user, azureCredential, ENTRY, getUserJksFileName(azureCredential, emailAsFolder));
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] pass = azureCredential.getJks().toCharArray();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(getUserJksFileName(azureCredential, emailAsFolder)));
                ks.load(fis, pass);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
            Certificate certificate = ks.getCertificate(ENTRY);
            final FileOutputStream os = new FileOutputStream(getUserCerFileName(azureCredential, emailAsFolder));
            os.write(Base64.encodeBase64(certificate.getEncoded(), true));
            os.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new InternalServerException("There was a problem with the certificate generation", e);
        }
    }

    public File getSshPublicKeyFile(CbUser user, Long azureCredentialId) {
        return new File(getPemFile(azureStackUtil.emailAsFolder(user.getUsername()), azureCredentialId));
    }

    public void generateSshCertificate(CbUser user, AzureCredential azureCredential, String sshKey) {
        try {
            String emailAsFolder = azureStackUtil.emailAsFolder(user.getUsername());
            File userFolder = new File(getSimpleUserFolder(emailAsFolder));
            if (!userFolder.exists()) {
                FileUtils.forceMkdir(new File(getSimpleUserFolder(emailAsFolder)));
            }
            File sshFolder = new File(getSshFolder(emailAsFolder));
            if (!sshFolder.exists()) {
                FileUtils.forceMkdir(new File(getSshFolder(emailAsFolder)));
            }
            File templateFolder = new File(getSshFolderForTemplate(emailAsFolder, azureCredential.getId()));
            if (!templateFolder.exists()) {
                FileUtils.forceMkdir(new File(getSshFolderForTemplate(emailAsFolder, azureCredential.getId())));
            }
            try {
                File file = new File(getPemFile(emailAsFolder, azureCredential.getId()));
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(sshKey);
                bw.close();
                keyGeneratorService.generateSshKey(getSshFolderForTemplate(emailAsFolder, azureCredential.getId()) + "/" + emailAsFolder);
            } catch (InterruptedException e) {
                LOGGER.error("An error occured under the ssh generation for {} template. The error was: {} {}", azureCredential.getId(), e.getMessage(), e);
                throw new InternalServerException(e.getMessage());
            }
        } catch (IOException ex) {
            LOGGER.error("An error occured under the ssh folder generation: {} {}", ex.getMessage(), ex);
            throw new InternalServerException("There was a problem with the ssh key generation", ex);
        }
    }
}
