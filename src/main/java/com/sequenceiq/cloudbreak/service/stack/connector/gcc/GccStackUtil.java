package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

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
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

@Component
public class GccStackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccStackUtil.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL);

    public Compute buildCompute(GccCredential gccCredential) {
        MDCBuilder.buildMdcContext(gccCredential);
        return buildCompute(gccCredential, gccCredential.getName());
    }

    public Compute buildCompute(GccCredential gccCredential, Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        return buildCompute(gccCredential, stack.getName());
    }

    public Storage buildStorage(GccCredential gccCredential, Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = buildGoogleCredential(gccCredential, httpTransport);
            return new Storage.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(stack.getName())
                    .setHttpRequestInitializer(credential)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Something unexpected happened while building Google Storage access.", e);
        }
        return null;
    }

    public List<Disk> listDisks(Compute compute, GccCredential gccCredential) throws IOException {
        List<Disk> disks = new ArrayList<>();
        for (GccZone gccZone : GccZone.values()) {
            try {
                Compute.Disks.List list = compute.disks().list(gccCredential.getProjectId(), gccZone.getValue());
                DiskList execute = list.execute();
                disks.addAll(execute.getItems());
            } catch (NullPointerException ex) {
                disks.addAll(new ArrayList<Disk>());
            }
        }
        return disks;
    }

    public CoreInstanceMetaData getMetadata(Stack stack, Compute compute, String resource) {
        MDCBuilder.buildMdcContext(stack);
        try {
            GccCredential credential = (GccCredential) stack.getCredential();
            GccTemplate template = (GccTemplate) stack.getTemplate();
            Compute.Instances.Get instanceGet = compute.instances().get(
                    credential.getProjectId(), template.getGccZone().getValue(), resource);
            Instance executeInstance = instanceGet.execute();
            CoreInstanceMetaData coreInstanceMetaData = new CoreInstanceMetaData(
                    resource,
                    executeInstance.getNetworkInterfaces().get(0).getNetworkIP(),
                    executeInstance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).getNatIP(),
                    template.getVolumeCount(),
                    longName(resource, credential.getProjectId()),
                    template.getContainerCount()
            );
            return coreInstanceMetaData;
        } catch (IOException e) {
            LOGGER.error(String.format("Instance %s is not reachable: %s", resource, e.getMessage()), e);
        }
        return null;
    }

    private Compute buildCompute(GccCredential gccCredential, String appName) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = buildGoogleCredential(gccCredential, httpTransport);
            return new Compute.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(appName)
                    .setHttpRequestInitializer(credential)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Something unexpected happened while building Google Compute access.", e);
        }
        return null;
    }

    private GoogleCredential buildGoogleCredential(GccCredential gccCredential, HttpTransport httpTransport) throws IOException, GeneralSecurityException {
        String p12path = gccCredential.getId() == null ? "/tmp/" + new Date().getTime() + ".p12" : "/tmp/" + gccCredential.getId() + ".p12";
        File p12file = new File(p12path);
        if (!p12file.exists()) {
            FileOutputStream output = new FileOutputStream(p12file);
            IOUtils.write(Base64.decodeBase64(gccCredential.getServiceAccountPrivateKey()), output);
        }
        return new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(gccCredential.getServiceAccountId())
                .setServiceAccountScopes(SCOPES)
                .setServiceAccountPrivateKeyFromP12File(p12file)
                .build();
    }

    private String longName(String resourceName, String projectId) {
        return String.format("%s.c.%s.internal", resourceName, projectId);
    }

}
