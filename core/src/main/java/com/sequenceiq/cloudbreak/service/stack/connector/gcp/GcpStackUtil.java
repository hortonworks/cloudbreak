package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.DiskList;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

@Component
public class GcpStackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpStackUtil.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL);

    private static final String GCP_IMAGE_TYPE_PREFIX = "https://www.googleapis.com/compute/v1/projects/%s/global/images/";
    private static final String EMPTY_BUCKET = "";

    public Compute buildCompute(GcpCredential gcpCredential) {
        return buildCompute(gcpCredential, gcpCredential.getName());
    }

    public Compute buildCompute(GcpCredential gcpCredential, Stack stack) {
        return buildCompute(gcpCredential, stack.getName());
    }

    public Storage buildStorage(GcpCredential gcpCredential, Stack stack) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = buildGoogleCredential(gcpCredential, httpTransport);
            return new Storage.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(stack.getName())
                    .setHttpRequestInitializer(credential)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error occurred while building Google Storage access.", e);
        }
        return null;
    }

    public List<Disk> listDisks(Compute compute, GcpCredential gcpCredential) throws IOException {
        List<Disk> disks = new ArrayList<>();
        for (CloudRegion gcpZone : CloudRegion.gcpRegions()) {
            try {
                Compute.Disks.List list = compute.disks().list(gcpCredential.getProjectId(), gcpZone.value());
                DiskList execute = list.execute();
                disks.addAll(execute.getItems());
            } catch (NullPointerException ex) {
                disks.addAll(new ArrayList<Disk>());
            }
        }
        return disks;
    }

    public CoreInstanceMetaData getMetadata(Stack stack, Compute compute, Resource resource) {
        GcpCredential credential = (GcpCredential) stack.getCredential();
        Instance executeInstance = getInstance(stack, compute, resource);
        if (executeInstance != null) {
            CoreInstanceMetaData coreInstanceMetaData = new CoreInstanceMetaData(
                    resource.getResourceName(),
                    executeInstance.getNetworkInterfaces().get(0).getNetworkIP(),
                    executeInstance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).getNatIP(),
                    stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()).getTemplate().getVolumeCount(),
                    stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup())
            );
            return coreInstanceMetaData;
        } else {
            return null;
        }
    }

    public Instance getInstance(Stack stack, Compute compute, Resource resource) {
        try {
            GcpCredential credential = (GcpCredential) stack.getCredential();
            Compute.Instances.Get instanceGet = compute.instances().get(credential.getProjectId(),
                    CloudRegion.valueOf(stack.getRegion()).value(), resource.getResourceName());
            return instanceGet.execute();
        } catch (IOException e) {
            LOGGER.error("Instance {} is not reachable: {}", resource, e.getMessage(), e);
        }
        return null;
    }

    private Compute buildCompute(GcpCredential gcpCredential, String appName) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = buildGoogleCredential(gcpCredential, httpTransport);
            return new Compute.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(appName)
                    .setHttpRequestInitializer(credential)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error occurred while building Google Compute access.", e);
        }
        return null;
    }

    private GoogleCredential buildGoogleCredential(GcpCredential gcpCredential, HttpTransport httpTransport) throws IOException, GeneralSecurityException {
        String p12path = gcpCredential.getId() == null ? "/tmp/" + new Date().getTime() + ".p12" : "/tmp/" + gcpCredential.getId() + ".p12";
        File p12file = new File(p12path);
        if (!p12file.exists()) {
            FileOutputStream output = new FileOutputStream(p12file);
            IOUtils.write(Base64.decodeBase64(gcpCredential.getServiceAccountPrivateKey()), output);
        }
        GoogleCredential credential = new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(gcpCredential.getServiceAccountId())
                .setServiceAccountScopes(SCOPES)
                .setServiceAccountPrivateKeyFromP12File(p12file)
                .build();
        if (gcpCredential.getId() == null) {
            p12file.delete();
        }
        return credential;
    }

    public String getBucket(String image) {
        if (!StringUtils.isEmpty(image) && createParts(image).length > 1) {
            String[] parts = createParts(image);
            return StringUtils.join(ArrayUtils.remove(parts, parts.length - 1), "/");
        } else {
            LOGGER.warn("No bucket found in source image path.");
            return EMPTY_BUCKET;
        }
    }

    public String getTarName(String image) {
        if (!StringUtils.isEmpty(image)) {
            String[] parts = createParts(image);
            return parts[parts.length - 1];
        } else {
            throw new GcpResourceException("Source image path environment variable is not well formed");
        }
    }

    public String getImageName(String image) {
        return getTarName(image).replaceAll("(\\.tar|\\.zip|\\.gz|\\.gzip)", "").replaceAll("\\.", "-");
    }

    public String getAmbariUbuntu(String projectId, String image) {
        return String.format(GCP_IMAGE_TYPE_PREFIX + getImageName(image), projectId);
    }

    private String[] createParts(String splittable) {
        return splittable.split("/");
    }
}
