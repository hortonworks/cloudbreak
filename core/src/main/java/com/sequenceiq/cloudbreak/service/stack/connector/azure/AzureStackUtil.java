package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class AzureStackUtil {

    public static final int NOT_FOUND = 404;
    public static final String NAME = "name";
    public static final String PORTS = "ports";
    public static final String RESERVEDIPNAME = "reservedIpName";
    public static final String SERVICENAME = "serviceName";
    public static final String VIRTUAL_NETWORK_IP_ADDRESS = "virtualNetworkIPAddress";
    public static final String CREDENTIAL = "credential";
    public static final String IMAGE_NAME = "ambari-docker";
    public static final String DEFAULT_JKS_PASS = "azure1";
    public static final String CB_STORAGE_BASE = "cb";
    public static final Logger LOGGER = LoggerFactory.getLogger(AzureStackUtil.class);

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    public String getOsImageName(Credential credential, AzureLocation location, String image) {
        String[] split = image.split("/");
        AzureCredential azureCredential = (AzureCredential) credential;
        return format("%s-%s-%s", azureCredential.getAffinityGroupName(location), IMAGE_NAME,
                split[split.length - 1].replaceAll(".vhd", ""));
    }

    public String getOSStorageName(AzureLocation location) {
        return CB_STORAGE_BASE + location.region().toLowerCase().replaceAll(" ", "");
    }

    public synchronized AzureClient createAzureClient(AzureCredential credential) {
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
            throw new AzureResourceException(e.getMessage());
        }
    }

    public File buildAzureSshCerFile(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String sshCerPath = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() + "ssh.cer" : "/tmp/" + azureCredential.getId() + "ssh.cer";
        File sshCerFile = new File(sshCerPath);
        if (!sshCerFile.exists()) {
            FileOutputStream output = new FileOutputStream(sshCerFile);
            IOUtils.write(Base64.decodeBase64(azureCredential.getSshCerFile()), output);
        }
        return sshCerFile;
    }

    public File buildAzureCerFile(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String sshCerPath = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() + ".cer" : "/tmp/" + azureCredential.getId() + ".cer";
        File sshCerFile = new File(sshCerPath);
        if (!sshCerFile.exists()) {
            FileOutputStream output = new FileOutputStream(sshCerFile);
            IOUtils.write(Base64.decodeBase64(azureCredential.getCerFile()), output);
        }
        return sshCerFile;
    }

    public AzureCredential generateAzureServiceFiles(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String serviceFilesPathWithoutExtension = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() : "/tmp/" + azureCredential.getId();
        try {
            Certificate certificate = getCertificate(azureCredential, serviceFilesPathWithoutExtension);
            final FileOutputStream os = new FileOutputStream(serviceFilesPathWithoutExtension + ".cer");
            os.write(Base64.encodeBase64(certificate.getEncoded(), true));
            os.close();
            azureCredential.setCerFile(FileReaderUtils.readFileFromPath(serviceFilesPathWithoutExtension + ".cer"));
            azureCredential.setJksFile(FileReaderUtils.readBinaryFileFromPath(serviceFilesPathWithoutExtension + ".jks"));
        } catch (Exception e) {
            LOGGER.error("Failure during azure certificate generation. Error message: {}", e.getMessage(), e);
            throw new AzureResourceException(e);
        }
        return azureCredential;
    }

    private Certificate getCertificate(AzureCredential azureCredential, String serviceFilesPathWithoutExtension) throws Exception {
        String email = userDetailsService.getDetails(azureCredential.getOwner(), UserFilterField.USERID).getUsername();
        keyGeneratorService.generateKey(email, azureCredential, "mydomain", serviceFilesPathWithoutExtension + ".jks");
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
        return ks.getCertificate("mydomain");
    }

    public AzureCredential generateAzureSshCerFile(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String sshPemPathWithoutExtension = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() + "ssh" : "/tmp/" + azureCredential.getId() + "ssh";
        String sshPemPath = sshPemPathWithoutExtension + ".pem";
        File sshPemFile = new File(sshPemPath);
        if (!sshPemFile.exists()) {
            FileOutputStream output = new FileOutputStream(sshPemFile);
            IOUtils.write(azureCredential.getPublicKey(), output);
        }
        try {
            keyGeneratorService.generateSshKey(sshPemPathWithoutExtension);
            azureCredential.setSshCerFile(FileReaderUtils.readBinaryFileFromPath(sshPemPathWithoutExtension + ".cer"));
        } catch (InterruptedException e) {
            LOGGER.error(format("An error occurred during ssh generation for %s template. The error was: %s", azureCredential.getId(), e.getMessage()), e);
            throw new AzureResourceException(e);
        }

        return azureCredential;
    }

    public AzureCredential refreshCerfile(AzureCredential azureCredential) throws Exception {
        String serviceFilesPathWithoutExtension = azureCredential.getId() == null
                ? "/tmp/refresh/" + new Date().getTime() : "/tmp/refresh/" + azureCredential.getId();
        String oldFile = "/tmp/" + azureCredential.getId();
        File files = new File("/tmp/refresh/");
        if (!files.exists()) {
            files.mkdirs();
        }
        Certificate certificate = getCertificate(azureCredential, serviceFilesPathWithoutExtension);
        final FileOutputStream os = new FileOutputStream(serviceFilesPathWithoutExtension + ".cer");
        os.write(Base64.encodeBase64(certificate.getEncoded(), true));
        os.close();
        Files.copy(Paths.get(serviceFilesPathWithoutExtension + ".cer"), Paths.get(oldFile + ".cer"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(serviceFilesPathWithoutExtension + ".jks"), Paths.get(oldFile + ".jks"), StandardCopyOption.REPLACE_EXISTING);
        azureCredential.setCerFile(FileReaderUtils.readFileFromPath(oldFile + ".cer"));
        azureCredential.setJksFile(FileReaderUtils.readBinaryFileFromPath(oldFile + ".jks"));
        return azureCredential;
    }

    public X509Certificate createX509Certificate(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        return new X509Certificate(buildAzureSshCerFile(azureCredential).getAbsolutePath());
    }

    public void migrateFilesIfNeeded(AzureCredential credential) {
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
            LOGGER.error("Unsuccessful credential migration.", ex);
        }
    }

    public String getFirstAssignableIPOfSubnet(String cIDRNotation) {
        SubnetUtils subnetUtils = new SubnetUtils(cIDRNotation);
        String startIP = subnetUtils.getInfo().getLowAddress();
        //the first 4 IPs could not be assigned in Azure subnets
        startIP = getNextIPAddress(startIP);
        startIP = getNextIPAddress(startIP);
        startIP = getNextIPAddress(startIP);
        return startIP;
    }

    public String getLastAssignableIPOfSubnet(String cIDRNotation) {
        SubnetUtils subnetUtils = new SubnetUtils(cIDRNotation);
        return subnetUtils.getInfo().getHighAddress();
    }

    public String getNextIPAddress(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException();
        }
        int value = (Integer.parseInt(octets[0], 10) << (8 * 3)) & 0xFF000000
                | (Integer.parseInt(octets[1], 10) << (8 * 2)) & 0x00FF0000
                | (Integer.parseInt(octets[2], 10) << (8 * 1)) & 0x0000FF00
                | (Integer.parseInt(octets[3], 10) << (8 * 0)) & 0x000000FF;
        return ipToString(value + 1);
    }

    private String ipToString(int value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i >= 0; --i) {
            sb.append(getOctet(i, value));
            if (i != 0) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    private int getOctet(int octet, int value) {
        return (value >> (octet * 8)) & 0x000000FF;
    }
}
