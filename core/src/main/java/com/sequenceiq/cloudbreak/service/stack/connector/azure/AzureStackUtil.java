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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.controller.validation.StackParam;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureStackUtil {

    public static final String UNSUPPORTED_OPERATION = "Unsupported operation: %s, the only supported operation is termination";

    public static final String NAME = "name";
    public static final String SERVICENAME = "serviceName";
    public static final String IMAGE_NAME = "ambari-docker";
    public static final String DEFAULT_JKS_PASS = "azure1";
    public static final Logger LOGGER = LoggerFactory.getLogger(AzureStackUtil.class);
    public static final int ROOTFS_COUNT = 1;
    public static final int GLOBAL_STORAGE = -1;
    public static final int MAX_LENGTH_OF_ARRAY = 4096;

    @Inject
    private KeyGeneratorService keyGeneratorService;

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private ImageService imageService;

    public String getOsImageName(Stack stack, String storageName) {
        String imageName = imageService.getImage(stack.getId()).getImageName();
        String[] split = imageName.split("/");
        return format("%s-%s-%s", storageName, IMAGE_NAME, split[split.length - 1].replaceAll(".vhd", ""));
    }

    /**
     * Determines the number of required Storage Accounts to distribute the VHDs.
     * In case of global Storage Account it returns -1.
     */
    public int getNumOfStorageAccounts(Stack stack) {
        if (!isBlobCountSpecified(stack)) {
            return GLOBAL_STORAGE;
        }
        List<Resource> storages = stack.getResourcesByType(ResourceType.AZURE_STORAGE);
        if (!storages.isEmpty()) {
            return storages.size();
        }
        int vhdPerStorageAccount = getNumOfVHDPerStorageAccount(stack);
        List<InstanceGroup> instanceGroups = getOrderedInstanceGroups(stack);
        int[] accounts = new int[MAX_LENGTH_OF_ARRAY];
        Arrays.fill(accounts, vhdPerStorageAccount);
        for (InstanceGroup ig : instanceGroups) {
            int nodeCount = ig.getNodeCount();
            int volumeCount = ig.getTemplate().getVolumeCount() + ROOTFS_COUNT;
            for (int i = 0; i < nodeCount; i++) {
                for (int j = 0; j < accounts.length; j++) {
                    int space = accounts[j];
                    if (space - volumeCount >= 0) {
                        accounts[j] = space - volumeCount;
                        break;
                    }
                }
            }
        }
        int numAccounts = 0;
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i] != vhdPerStorageAccount) {
                numAccounts++;
            } else {
                break;
            }
        }
        return numAccounts;
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

    private boolean isBlobCountSpecified(Stack stack) {
        return stack.getParameters() != null && stack.getParameters().get(StackParam.DISK_PER_STORAGE.getName()) != null;
    }

    private int getNumOfVHDPerStorageAccount(Stack stack) {
        String vhdNum = stack.getParameters().get(StackParam.DISK_PER_STORAGE.getName());
        int vhdPS = Integer.valueOf(vhdNum == null ? "0" : vhdNum);
        for (InstanceGroup ig : stack.getInstanceGroups()) {
            int volumeCount = ig.getTemplate().getVolumeCount() + ROOTFS_COUNT;
            if (volumeCount > vhdPS) {
                vhdPS = volumeCount;
            }
        }
        return vhdPS;
    }

    private List<InstanceGroup> getOrderedInstanceGroups(Stack stack) {
        List<InstanceGroup> instanceGroups = stack.getInstanceGroupsAsList();
        Collections.sort(instanceGroups, new Comparator<InstanceGroup>() {
            @Override
            public int compare(InstanceGroup o1, InstanceGroup o2) {
                return o1.getNodeCount().compareTo(o2.getNodeCount());
            }
        });
        return instanceGroups;
    }

    private Certificate getCertificate(AzureCredential azureCredential, String serviceFilesPathWithoutExtension) throws Exception {
        String email = userDetailsService.getDetails(azureCredential.getOwner(), UserFilterField.USERID).getUsername();
        keyGeneratorService.generateKey(email, "mydomain", serviceFilesPathWithoutExtension + ".jks");
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
}
