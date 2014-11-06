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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.DiskList;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.dns.model.ResourceRecordSet;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccDiskMode;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccDiskType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccImageType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccInstanceType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

@Component
public class GccStackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccStackUtil.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL);

    private static final int MAX_POLLING_ATTEMPTS = 60;
    private static final int POLLING_INTERVAL = 5000;
    private static final int TTL = 3600;
    private static final int NOT_FOUND = 404;
    private static final long PRIORITY = 900L;
    private static final int LAST = 3;

    @Autowired
    private GccInstanceCheckerStatus gccInstanceReadyCheckerStatus;

    @Autowired
    private PollingService<GccInstanceReadyPollerObject> gccInstanceReadyPollerObjectPollingService;

    @Autowired
    private GccDiskCheckerStatus gccDiskCheckerStatus;

    @Autowired
    private PollingService<GccDiskReadyPollerObject> gccDiskReadyPollerObjectPollingService;

    @Autowired
    private GccRemoveCheckerStatus gccRemoveCheckerStatus;

    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;

    public Compute buildCompute(GccCredential gccCredential, String appName) {
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

    public ResourceRecordSet buildResourceRecordSet(Stack stack, List<String> ips) throws IOException {
        GccCredential credential = (GccCredential) stack.getCredential();
        ResourceRecordSet resourceRecordSet = new ResourceRecordSet();
        resourceRecordSet.setName(String.format("%s.%s.", credential.getProjectId(), "com"));
        resourceRecordSet.setTtl(TTL);
        resourceRecordSet.setType("A");
        resourceRecordSet.setRrdatas(ips);
        return resourceRecordSet;
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


    public Instance buildInstance(Compute compute, Stack stack,
            List<NetworkInterface> networkInterfaces, List<AttachedDisk> attachedDisks, String forName, String userData) throws IOException {
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential credential = (GccCredential) stack.getCredential();
        Instance instance = new Instance();
        instance.setMachineType(buildMachineType(credential.getProjectId(), gccTemplate.getGccZone(), gccTemplate.getGccInstanceType()));
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
        Compute.Instances.Insert ins = compute.instances().insert(credential.getProjectId(), gccTemplate.getGccZone().getValue(), instance);

        ins.setPrettyPrint(Boolean.TRUE);
        ins.execute();
        GccInstanceReadyPollerObject gccInstanceReady = new GccInstanceReadyPollerObject(compute, stack, forName);
        gccInstanceReadyPollerObjectPollingService.pollWithTimeout(gccInstanceReadyCheckerStatus, gccInstanceReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        return instance;
    }

    public void removeInstance(Compute compute, Stack stack, String name) throws IOException {
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        Operation execute = compute.instances().delete(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), name).execute();
        Compute.ZoneOperations.Get zoneOperations = createZoneOperations(compute, gccCredential, gccTemplate, execute);
        Compute.GlobalOperations.Get globalOperations = createGlobalOperations(compute, gccCredential, gccTemplate, execute);
        GccRemoveReadyPollerObject gccRemoveReady = new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack.getId(), name);
        gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerStatus, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
    }


    public void removeDisk(Compute compute, Stack stack, String name) throws IOException {
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        Operation execute = compute.disks().delete(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), name).execute();
        Compute.ZoneOperations.Get zoneOperations = createZoneOperations(compute, gccCredential, gccTemplate, execute);
        Compute.GlobalOperations.Get globalOperations = createGlobalOperations(compute, gccCredential, gccTemplate, execute);
        GccRemoveReadyPollerObject gccRemoveReady = new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack.getId(), name);
        gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerStatus, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
    }

    private Compute.ZoneOperations.Get createZoneOperations(Compute compute, GccCredential gccCredential, GccTemplate gccTemplate, Operation operation)
            throws IOException {
        return compute.zoneOperations().get(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), operation.getName());
    }

    private Compute.GlobalOperations.Get createGlobalOperations(Compute compute, GccCredential gccCredential, GccTemplate gccTemplate, Operation operation)
            throws IOException {
        return compute.globalOperations().get(gccCredential.getProjectId(), operation.getName());
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

    public String buildMachineType(String projectId, GccZone gccZone, GccInstanceType instanceType) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                projectId, gccZone.getValue(), instanceType.getValue());
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

    public List<AttachedDisk> buildAttachedDisks(String name, Disk disk, Compute compute, Stack stack) throws IOException {
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        List<AttachedDisk> listOfDisks = new ArrayList<>();

        AttachedDisk diskToInsert = new AttachedDisk();
        diskToInsert.setBoot(true);
        diskToInsert.setType(GccDiskType.PERSISTENT.getValue());
        diskToInsert.setMode(GccDiskMode.READ_WRITE.getValue());
        diskToInsert.setDeviceName(name);
        diskToInsert.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s?sourceImage=%s",
                gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), disk.getName(), gccTemplate.getGccZone()));
        listOfDisks.add(diskToInsert);

        for (int i = 0; i < gccTemplate.getVolumeCount(); i++) {
            String value = name + "-" + i;
            Disk disk1 = buildRawDisk(compute, stack, gccCredential.getProjectId(),
                    gccTemplate.getGccZone(), value, Long.parseLong(gccTemplate.getVolumeSize().toString()));
            AttachedDisk diskToInsert1 = new AttachedDisk();
            diskToInsert1.setBoot(false);
            diskToInsert1.setType(GccDiskType.PERSISTENT.getValue());
            diskToInsert1.setMode(GccDiskMode.READ_WRITE.getValue());
            diskToInsert1.setDeviceName(value);
            diskToInsert1.setSource(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                    gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), disk1.getName()));
            listOfDisks.add(diskToInsert1);
        }
        return listOfDisks;
    }

    public Disk buildDisk(Compute compute, Stack stack, String projectId, GccZone zone, String name, Long size) throws IOException {
        Disk disk = new Disk();
        disk.setSizeGb(size.longValue());
        disk.setName(name);
        disk.setKind(((GccTemplate) stack.getTemplate()).getGccRawDiskType().getUrl(projectId, zone));
        Compute.Disks.Insert insDisk = compute.disks().insert(projectId, zone.getValue(), disk);
        insDisk.setSourceImage(GccImageType.DEBIAN_HACK.getAmbariUbuntu(projectId));
        insDisk.execute();
        GccDiskReadyPollerObject gccDiskReady = new GccDiskReadyPollerObject(compute, stack, name);
        gccDiskReadyPollerObjectPollingService.pollWithTimeout(gccDiskCheckerStatus, gccDiskReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        return disk;
    }

    public Disk buildRawDisk(Compute compute, Stack stack, String projectId, GccZone zone, String name, Long size) throws IOException {
        Disk disk = new Disk();
        disk.setSizeGb(size.longValue());
        disk.setName(name);
        disk.setKind(((GccTemplate) stack.getTemplate()).getGccRawDiskType().getUrl(projectId, zone));
        Compute.Disks.Insert insDisk = compute.disks().insert(projectId, zone.getValue(), disk);
        insDisk.execute();
        GccDiskReadyPollerObject gccDiskReady = new GccDiskReadyPollerObject(compute, stack, name);
        gccDiskReadyPollerObjectPollingService.pollWithTimeout(gccDiskCheckerStatus, gccDiskReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        return disk;
    }

    public CoreInstanceMetaData getMetadata(Stack stack, Compute compute, String resource) {
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

    public String getVmName(String stackName, int i) {
        return String.format("%s-%s", stackName, i);
    }

}
