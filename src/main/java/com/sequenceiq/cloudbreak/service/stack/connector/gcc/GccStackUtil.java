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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.Address;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Network;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component
public class GccStackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccStackUtil.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL);
    private static final String READY = "READY";
    private static final String RUNNING = "RUNNING";
    private static final int WAIT_TIME = 1000;
    private static final int FINISHED = 100;
    private static final int NOT_FOUND = 404;

    public Compute buildCompute(GccCredential gccCredential, Stack stack) {
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
                    httpTransport, JSON_FACTORY, null).setApplicationName(stack.getName())
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

    public Instance buildInstance(Compute compute, GccTemplate gccTemplate, GccCredential credential,
            List<NetworkInterface> networkInterfaces, List<AttachedDisk> attachedDisks, String forName, String userData) throws IOException {
        Instance instance = new Instance();
        instance.setMachineType(buildMachineType(gccTemplate.getProjectId(), gccTemplate.getGccZone(), gccTemplate.getGccInstanceType()));
        instance.setName(forName);
        instance.setCanIpForward(Boolean.TRUE);
        instance.setNetworkInterfaces(networkInterfaces);
        instance.setDisks(attachedDisks);
        Metadata metadata = new Metadata();
        metadata.setItems(Lists.<Metadata.Items>newArrayList());

        Metadata.Items item1 = new Metadata.Items();
        item1.setKey("sshKeys");
        item1.setValue("ubuntu:" + credential.getPublicKey());

        Metadata.Items item2 = new Metadata.Items();
        item2.setKey("startup-script");
        item2.setValue(userData);

        metadata.getItems().add(item1);
        metadata.getItems().add(item2);
        instance.setMetadata(metadata);
        Compute.Instances.Insert ins = compute.instances().insert(gccTemplate.getProjectId(), gccTemplate.getGccZone().getValue(), instance);

        ins.setPrettyPrint(Boolean.TRUE);
        ins.execute();
        try {
            Thread.sleep(WAIT_TIME);
            try {
                Compute.Instances.Get getInstances = compute.instances().get(gccTemplate.getProjectId(), gccTemplate.getGccZone().getValue(), forName);
                while (!getInstances.execute().getStatus().equals(RUNNING)) {
                    Thread.sleep(WAIT_TIME);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public void removeInstance(Compute compute, GccTemplate gccTemplate, GccCredential credential, String name) throws IOException {
        try {
            Operation execute = compute.instances().delete(gccTemplate.getProjectId(), gccTemplate.getGccZone().getValue(), name).execute();
            Integer progress = compute.globalOperations().get(gccTemplate.getProjectId(), execute.getName()).execute().getProgress();
            while (progress.intValue() != FINISHED) {
                progress = compute.globalOperations().get(gccTemplate.getProjectId(), execute.getName()).execute().getProgress();
            }
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, name);
        }
    }



    public void removeDisk(Compute compute, GccTemplate gccTemplate, GccCredential credential, String name) throws IOException {
        try {
            Operation execute = compute.disks().delete(gccTemplate.getProjectId(), gccTemplate.getGccZone().getValue(), name).execute();
            Integer progress = compute.globalOperations().get(gccTemplate.getProjectId(), execute.getName()).execute().getProgress();
            while (progress.intValue() != FINISHED) {
                progress = compute.globalOperations().get(gccTemplate.getProjectId(), execute.getName()).execute().getProgress();
            }
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, name);
        }
    }

    public void removeNetwork(Compute compute, GccTemplate gccTemplate, String name) throws IOException {
        try {
            Operation execute = compute.networks().delete(gccTemplate.getProjectId(), name).execute();
            Integer progress = compute.globalOperations().get(gccTemplate.getProjectId(), execute.getName()).execute().getProgress();
            while (progress.intValue() != FINISHED) {
                progress = compute.globalOperations().get(gccTemplate.getProjectId(), execute.getName()).execute().getProgress();
            }
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, name);
        }
    }

    public Storage buildStorage(GccCredential gccCredential, Stack stack) {
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

    private Credential authorize(GccCredential gccCredential, HttpTransport httpTransport) throws Exception {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new StringReader(
                        String.format("{\"installed\": {\"client_id\": \"%s\",\"client_secret\": \"%s\"}}\n",
                                gccCredential.getServiceAccountId(),
                                gccCredential.getServiceAccountPrivateKey())
                )
        );
        MemoryDataStoreFactory dataStoreFactory = new MemoryDataStoreFactory();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                .authorize(gccCredential.getGccCredentialOwner().getEmail());
    }

    public String buildMachineType(String projectId, GccZone gccZone, GccInstanceType instanceType) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                projectId, gccZone.getValue(), instanceType.getValue());
    }

    public List<NetworkInterface> buildNetworkInterfaces(Compute compute, String projectId, String name) throws IOException {
        Network network = new Network();
        network.setName(name);
        network.setIPv4Range("10.0.0.0/16");
        Compute.Networks.Insert networkInsert = compute.networks().insert(projectId, network);
        networkInsert.execute();
        buildFireWallOut(compute, projectId, name);
        buildFireWallIn(compute, projectId, name);
        List<NetworkInterface> networkInterfaces = new ArrayList<>();
        networkInterfaces.add(buildNetworkInterface(projectId, name));
        return networkInterfaces;
    }

    public NetworkInterface buildNetworkInterface(String projectId, String name) {
        NetworkInterface iface = new NetworkInterface();
        iface.setName(name);
        AccessConfig accessConfig = new AccessConfig();
        accessConfig.setName(name);
        accessConfig.setType("ONE_TO_ONE_NAT");
        iface.setAccessConfigs(ImmutableList.of(accessConfig));
        iface.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, name));
        return iface;
    }

    public void buildAddress(Compute compute, String projectId, GccZone gccZone, String name) throws IOException {
        Address address = new Address();
        address.setName(name);
        Compute.Addresses.Insert addressInsert = compute.addresses().insert(projectId, gccZone.getValue(), address);
        addressInsert.execute();
        Compute.Addresses.Get addressInsert1 = compute.addresses().get(projectId, gccZone.getValue(), name);
    }

    public void buildFireWallOut(Compute compute, String projectId, String name) throws IOException {
        Firewall firewall = new Firewall();
        Firewall.Allowed allowed1 = new Firewall.Allowed();
        allowed1.setIPProtocol("tcp");
        allowed1.setPorts(ImmutableList.of("1-65535"));

        Firewall.Allowed allowed2 = new Firewall.Allowed();
        allowed2.setIPProtocol("icmp");

        Firewall.Allowed allowed3 = new Firewall.Allowed();
        allowed3.setIPProtocol("udp");
        allowed3.setPorts(ImmutableList.of("1-65535"));

        firewall.setAllowed(ImmutableList.of(allowed1, allowed2, allowed3));
        firewall.setName(name + "out");
        firewall.setSourceRanges(ImmutableList.of("0.0.0.0/0"));
        firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, name));
        Compute.Firewalls.Insert firewallInsert = compute.firewalls().insert(projectId, firewall);
        firewallInsert.execute();
    }

    public void buildFireWallIn(Compute compute, String projectId, String name) throws IOException {
        Firewall firewall = new Firewall();

        Firewall.Allowed allowed1 = new Firewall.Allowed();
        allowed1.setIPProtocol("udp");
        allowed1.setPorts(ImmutableList.of("1-65535"));

        Firewall.Allowed allowed2 = new Firewall.Allowed();
        allowed2.setIPProtocol("icmp");

        Firewall.Allowed allowed3 = new Firewall.Allowed();
        allowed3.setIPProtocol("tcp");
        allowed3.setPorts(ImmutableList.of("1-65535"));

        firewall.setAllowed(ImmutableList.of(allowed1, allowed2, allowed3));
        firewall.setName(name + "in");
        firewall.setSourceRanges(ImmutableList.of("10.0.0.0/16"));
        firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, name));
        Compute.Firewalls.Insert firewallInsert = compute.firewalls().insert(projectId, firewall);
        firewallInsert.execute();
    }

    public List<AttachedDisk> buildAttachedDisks(String name, Disk disk, Compute compute, GccTemplate gccTemplate) throws IOException {
        List<AttachedDisk> listOfDisks = new ArrayList<>();

        AttachedDisk diskToInsert = new AttachedDisk();
        diskToInsert.setBoot(true);
        diskToInsert.setType(GccDiskType.PERSISTENT.getValue());
        diskToInsert.setMode(GccDiskMode.READ_WRITE.getValue());
        diskToInsert.setDeviceName(name);
        diskToInsert.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s?sourceImage=%s",
                gccTemplate.getProjectId(), gccTemplate.getGccZone().getValue(), disk.getName(), gccTemplate.getGccZone()));
        listOfDisks.add(diskToInsert);

        for (int i = 0; i < gccTemplate.getVolumeCount(); i++) {
            String value = name + i;
            Disk disk1 = buildRawDisk(compute, gccTemplate.getProjectId(), gccTemplate.getGccZone(), value, Long.parseLong(gccTemplate.getVolumeSize().toString()));
            AttachedDisk diskToInsert1 = new AttachedDisk();
            diskToInsert1.setBoot(false);
            diskToInsert1.setType(GccDiskType.PERSISTENT.getValue());
            diskToInsert1.setMode(GccDiskMode.READ_WRITE.getValue());
            diskToInsert1.setDeviceName(value);
            diskToInsert1.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                    gccTemplate.getProjectId(), gccTemplate.getGccZone().getValue(), disk1.getName()));
            listOfDisks.add(diskToInsert1);
        }
        return listOfDisks;
    }

    public Disk buildDisk(Compute compute, String projectId, GccZone zone, String name, Long size) throws IOException {
        Disk disk = new Disk();
        disk.setSizeGb(size.longValue());
        disk.setName(name);
        Compute.Disks.Insert insDisk = compute.disks().insert(projectId, zone.getValue(), disk);
        insDisk.setSourceImage(GccImageType.UBUNTU_HACK.getAmbariUbuntu(projectId));
        insDisk.execute();
        try {
            Thread.sleep(WAIT_TIME);
            try {
                Compute.Disks.Get getDisk = compute.disks().get(projectId, zone.getValue(), name);
                while (!getDisk.execute().getStatus().equals(READY)) {
                    Thread.sleep(WAIT_TIME);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return disk;
    }

    public Disk buildRawDisk(Compute compute, String projectId, GccZone zone, String name, Long size) throws IOException {
        Disk disk = new Disk();
        disk.setSizeGb(size.longValue());
        disk.setName(name);
        Compute.Disks.Insert insDisk = compute.disks().insert(projectId, zone.getValue(), disk);
        insDisk.execute();
        try {
            Thread.sleep(WAIT_TIME);
            try {
                Compute.Disks.Get getDisk = compute.disks().get(projectId, zone.getValue(), name);
                while (!getDisk.execute().getStatus().equals(READY)) {
                    Thread.sleep(WAIT_TIME);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return disk;
    }

    private void exceptionHandler(GoogleJsonResponseException ex, String name) throws GoogleJsonResponseException {
        if (ex.getDetails().get("code").equals(NOT_FOUND)) {
            LOGGER.info(String.format("Resource was delete with name: %s", name));
        } else {
            throw ex;
        }
    }

    public String getVmName(String stackName, int i) {
        return String.format("%s-%s", stackName, i);
    }

}
