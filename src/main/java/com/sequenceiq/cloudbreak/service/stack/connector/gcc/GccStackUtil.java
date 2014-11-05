package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
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
import com.sequenceiq.cloudbreak.logger.CbLoggerFactory;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

@Component
public class GccStackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccStackUtil.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL);

    public Compute buildCompute(GccCredential gccCredential, String appName) {
        CbLoggerFactory.buildMdcContext(gccCredential);
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            BufferedReader br = new BufferedReader(new StringReader(gccCredential.getServiceAccountPrivateKey()));
            Security.addProvider(new BouncyCastleProvider());
            KeyPair kp = (KeyPair) new PEMReader(br).readObject();
            GoogleCredential credential = new GoogleCredential.Builder().setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId(gccCredential.getServiceAccountId())
                    .setServiceAccountScopes(SCOPES)
                    .setServiceAccountPrivateKey(kp.getPrivate())
                    .build();
            Compute compute = new Compute.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(appName)
                    .setHttpRequestInitializer(credential)
                    .build();
            return compute;
        } catch (GeneralSecurityException e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
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

    public Storage buildStorage(GccCredential gccCredential, Stack stack) {
        CbLoggerFactory.buildMdcContext(stack);
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            BufferedReader br = new BufferedReader(new StringReader(gccCredential.getServiceAccountPrivateKey()));
            Security.addProvider(new BouncyCastleProvider());
            KeyPair kp = (KeyPair) new PEMReader(br).readObject();
            GoogleCredential credential = new GoogleCredential.Builder().setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId(gccCredential.getServiceAccountId())
                    .setServiceAccountScopes(SCOPES)
                    .setServiceAccountPrivateKey(kp.getPrivate())
                    .build();
            Storage storage = new Storage.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(stack.getName())
                    .setHttpRequestInitializer(credential)
                    .build();
            return storage;
        } catch (GeneralSecurityException e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
        }
        return null;
    }

    public CoreInstanceMetaData getMetadata(Stack stack, Compute compute, String resource) {
        CbLoggerFactory.buildMdcContext(stack);
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

    private String longName(String resourceName, String projectId) {
        return String.format("%s.c.%s.internal", resourceName, projectId);
    }

}
