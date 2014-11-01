package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Security;
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
import com.google.api.services.dns.Dns;
import com.google.api.services.dns.model.ManagedZone;
import com.google.api.services.dns.model.ManagedZonesListResponse;
import com.google.api.services.storage.StorageScopes;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;

@Component
public class GccResourceBuilderInit implements
        ResourceBuilderInit<GccProvisionContextObject, GccDeleteContextObject, GccDescribeContextObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccResourceBuilderInit.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL);

    @Override
    public GccProvisionContextObject provisionInit(Stack stack, String userData) throws Exception {
        GccCredential credential = (GccCredential) stack.getCredential();
        GccProvisionContextObject gccProvisionContextObject = new GccProvisionContextObject(stack.getId(), credential.getProjectId(),
                buildCompute(credential, stack.getName()));
        gccProvisionContextObject.setUserData(userData);
        return gccProvisionContextObject;
    }

    @Override
    public GccDeleteContextObject deleteInit(Stack stack) throws Exception {
        GccCredential credential = (GccCredential) stack.getCredential();
        GccDeleteContextObject gccDeleteContextObject = new GccDeleteContextObject(stack.getId(), credential.getProjectId(),
                buildCompute(credential, stack.getName()));
        return gccDeleteContextObject;
    }

    @Override
    public GccDescribeContextObject describeInit(Stack stack) throws Exception {
        GccCredential credential = (GccCredential) stack.getCredential();
        GccDescribeContextObject gccDescribeContextObject = new GccDescribeContextObject(stack.getId(), credential.getProjectId(),
                buildCompute(credential, stack.getName()));
        return gccDescribeContextObject;
    }

    private Compute buildCompute(GccCredential gccCredential, String appName) {
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

    private Dns buildDns(GccCredential gccCredential, Stack stack) {
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

            Dns dns = new Dns.Builder(httpTransport, JSON_FACTORY, null).setApplicationName(stack.getName())
                    .setHttpRequestInitializer(credential)
                    .build();

            return dns;
        } catch (GeneralSecurityException e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
        }
        return null;
    }

    private ManagedZone buildManagedZone(Dns dns, Stack stack) throws IOException {
        GccCredential credential = (GccCredential) stack.getCredential();
        ManagedZonesListResponse execute1 = dns.managedZones().list(credential.getProjectId()).execute();
        ManagedZone original = null;
        for (ManagedZone managedZone : execute1.getManagedZones()) {
            if (managedZone.getName().equals(credential.getProjectId())) {
                original = managedZone;
                break;
            }
        }
        if (original == null) {
            ManagedZone managedZone = new ManagedZone();
            managedZone.setName(credential.getProjectId());
            managedZone.setDnsName(String.format("%s.%s", credential.getProjectId(), "com"));
            ManagedZone execute = dns.managedZones().create(credential.getProjectId(), managedZone).execute();
            return execute;
        }
        return original;
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.RESOURCE_BUILDER_INIT;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCC;
    }
}
