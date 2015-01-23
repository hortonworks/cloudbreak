package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class AzureStackUtil {

    public static final int NOT_FOUND = 404;
    public static final String NAME = "name";
    public static final String SERVICENAME = "serviceName";
    public static final String ERROR = "\"error\":\"Could not fetch data from azure\"";
    public static final String CREDENTIAL = "credential";
    public static final String IMAGE_NAME = "ambari-docker-v1";
    public static final String DEFAULT_JKS_PASS = "azure1";
    public static final Logger LOGGER = LoggerFactory.getLogger(AzureStackUtil.class);

    @Value("${cb.azure.image.uri}")
    private String baseImageUri;

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    public String getOsImageName(Credential credential, AzureLocation location) {
        String[] split = baseImageUri.split("/");
        AzureCredential azureCredential = (AzureCredential) credential;
        return String.format("%s-%s-%s", azureCredential.getCommonName(location), IMAGE_NAME,
                split[split.length - 1].replaceAll(".vhd", ""));
    }

    public synchronized AzureClient createAzureClient(AzureCredential credential) {
        MDCBuilder.buildMdcContext(credential);
        try {
            String jksPath = credential.getId() == null ? "/tmp/" + new Date().getTime() + ".jks" : "/tmp/" + credential.getId() + ".jks";
            File jksFile = new File(jksPath);
            if (!jksFile.exists()) {
                FileOutputStream output = new FileOutputStream(jksFile);
                IOUtils.write(Base64.decodeBase64(credential.getJksFile()), output);
            }
            return new AzureClient(credential.getSubscriptionId(), jksFile.getAbsolutePath(), DEFAULT_JKS_PASS);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new InternalServerException(e.getMessage());
        }
    }

    public File buildAzureSshCerFile(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String sshCerPath = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() + "ssh.cer" : "/tmp/" + azureCredential.getId() + "ssh.cer";
        File sshCerfile = new File(sshCerPath);
        if (!sshCerfile.exists()) {
            FileOutputStream output = new FileOutputStream(sshCerfile);
            IOUtils.write(Base64.decodeBase64(azureCredential.getSshCerFile()), output);
        }
        return sshCerfile;
    }

    public File buildAzureCerFile(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String sshCerPath = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() + ".cer" : "/tmp/" + azureCredential.getId() + ".cer";
        File sshCerfile = new File(sshCerPath);
        if (!sshCerfile.exists()) {
            FileOutputStream output = new FileOutputStream(sshCerfile);
            IOUtils.write(Base64.decodeBase64(azureCredential.getCerFile()), output);
        }
        return sshCerfile;
    }

    public AzureCredential generateAzureServiceFiles(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String serviceFilesPathWithoutExtension = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() : "/tmp/" + azureCredential.getId();
        try {
            keyGeneratorService.generateKey("cloudbreak", azureCredential, "mydomain", serviceFilesPathWithoutExtension + ".jks");
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] pass = azureCredential.getJks().toCharArray();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(serviceFilesPathWithoutExtension + ".jks"));
                ks.load(fis, pass);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
            Certificate certificate = ks.getCertificate("mydomain");
            final FileOutputStream os = new FileOutputStream(serviceFilesPathWithoutExtension + ".cer");
            os.write(Base64.encodeBase64(certificate.getEncoded(), true));
            os.close();
            azureCredential.setCerFile(FileReaderUtils.readFileFromPath(serviceFilesPathWithoutExtension + ".cer"));
            azureCredential.setJksFile(FileReaderUtils.readBinaryFileFromPath(serviceFilesPathWithoutExtension + ".jks"));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new InternalServerException("There was a problem with the certificate generation", e);
        }
        return azureCredential;
    }

    public AzureCredential generateAzureSshCerFile(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String sshPemPathWithoutExtension = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() + "ssh" : "/tmp/" + azureCredential.getId() + "ssh";
        String sshPemPath = sshPemPathWithoutExtension + ".pem";
        File sshPemfile = new File(sshPemPath);
        if (!sshPemfile.exists()) {
            FileOutputStream output = new FileOutputStream(sshPemfile);
            IOUtils.write(azureCredential.getPublicKey(), output);
        }
        try {
            keyGeneratorService.generateSshKey(sshPemPathWithoutExtension);
            azureCredential.setSshCerFile(FileReaderUtils.readBinaryFileFromPath(sshPemPathWithoutExtension + ".cer"));
        } catch (Exception e) {
            LOGGER.error("An error occured under the ssh generation for {} template. The error was: {} {}", azureCredential.getId(), e.getMessage(), e);
            throw new InternalServerException(e.getMessage());
        }

        return azureCredential;
    }

    public CbX509Certificate createX509Certificate(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        return new CbX509Certificate(buildAzureSshCerFile(azureCredential).getAbsolutePath());
    }

    public void migrateFilesIfNeeded(AzureCredential credential) {
        MDCBuilder.buildMdcContext(credential);
        String email = userDetailsService.getDetails(credential.getOwner(), UserFilterField.USERID).getUsername();
        String replace = email.replaceAll("@", "_").replace(".", "_");
        String sshFile = "userdatas/" + replace + "/ssh/" + credential.getId() + "/" + replace + ".cer";
        String cerFile = "userdatas/" + replace + "/certificate/" + credential.getId() + "/" + replace + ".cer";
        String jksFile = "userdatas/" + replace + "/certificate/" + credential.getId() + "/" + replace + ".jks";

        try {
            if (credential.getSshCerFile() == null) {
                if (new File(sshFile).exists()) {
                    credential.setSshCerFile(FileReaderUtils.readBinaryFileFromPath(sshFile));
                    credentialRepository.save(credential);
                }
            }
            if (credential.getCerFile() == null && credential.getJksFile() == null) {
                if (new File(cerFile).exists()) {
                    credential.setCerFile(FileReaderUtils.readFileFromPath(cerFile));
                    credentialRepository.save(credential);
                }
                if (new File(jksFile).exists()) {
                    credential.setJksFile(FileReaderUtils.readBinaryFileFromPath(jksFile));
                    credentialRepository.save(credential);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Credential migration unsuccess.", ex.getMessage());
        }
    }
}
