package com.sequenceiq.it.cloudbreak.v2.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.it.util.ServerAddressGenerator;
import com.sequenceiq.it.verification.Call;
import com.sequenceiq.it.verification.Verification;

import spark.Response;
import spark.Service;

public class MockServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockServer.class);

    private static final Gson GSON = new Gson();

    @Inject
    private ResourceLoader resourceLoader;

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    private final int mockPort;

    private final int sshPort;

    private final Service sparkService;

    private final Map<Call, Response> requestResponseMap = new HashMap<>();

    private Map<String, CloudVmMetaDataStatus> instanceMap;

    private int numberOfServers;

    private MockServer(int mockPort, int sshPort) {
        this.mockPort = mockPort;
        this.sshPort = sshPort;
        sparkService = Service.ignite();
        sparkService.port(mockPort);
        File keystoreFile = createTempFileFromClasspath("/keystore_server");
        sparkService.secure(keystoreFile.getPath(), "secret", null, null);
        sparkService.before((req, res) -> res.type("application/json"));
        sparkService.after((request, response) -> requestResponseMap.put(Call.fromRequest(request), response));
    }

    public MockServer(int mockPort, int sshPort, Map<String, CloudVmMetaDataStatus> instanceMap) {
        this(mockPort, sshPort);
        this.instanceMap = instanceMap;
        instanceMap.forEach((key, value) -> {
            if (value.getCloudVmInstanceStatus().getStatus() != InstanceStatus.TERMINATED) {
                numberOfServers++;
            }
        });
    }

    public MockServer(int mockPort, int sshPort, int numberOfServers) {
        this(mockPort, sshPort);
        instanceMap = new HashMap<>();
        this.numberOfServers = numberOfServers;
    }

    @PostConstruct
    public void initInstanceMap() {
        if (instanceMap.isEmpty()) {
            addInstance(numberOfServers);
        }
    }

    public void addInstance(int numberOfAddedInstances) {
        ServerAddressGenerator serverAddressGenerator = new ServerAddressGenerator(numberOfAddedInstances);
        serverAddressGenerator.setFrom(instanceMap.size());

        serverAddressGenerator.iterateOver((address, number) -> {
            String instanceId = "instance-" + address;
            InstanceTemplate instanceTemplate = new InstanceTemplate("medium", "group", Integer.toUnsignedLong(number),
                    new ArrayList<>(), InstanceStatus.CREATED, null, 0L, "imageId");
            InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
            CloudInstance cloudInstanceWithId = new CloudInstance(instanceId, instanceTemplate, instanceAuthentication);
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.STARTED);
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(address, mockServerAddress, sshPort, "MOCK");
            CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            instanceMap.put(instanceId, cloudVmMetaDataStatus);
        });
    }

    public static void terminateInstance(Map<String, CloudVmMetaDataStatus> instanceMap, String instanceId) {
        CloudVmMetaDataStatus vmMetaDataStatus = instanceMap.get(instanceId);
        InstanceTemplate oldTemplate = vmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getTemplate();
        InstanceTemplate newTemplate = new InstanceTemplate("medium", "group", oldTemplate.getPrivateId(),
                new ArrayList<>(), InstanceStatus.TERMINATED, null, 0L, "imageId");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstanceWithId = new CloudInstance(instanceId, newTemplate, instanceAuthentication);
        CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.TERMINATED);
        CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, vmMetaDataStatus.getMetaData());
        instanceMap.put(instanceId, cloudVmMetaDataStatus);
    }

    public Map<String, CloudVmMetaDataStatus> getInstanceMap() {
        return instanceMap;
    }

    public void stop() {
        sparkService.stop();
    }

    protected Verification verify(String path, String httpMethod) {
        return new Verification(path, httpMethod, requestResponseMap, false);
    }

    protected Verification verifyRegexpPath(String regexpPath, String httpMethod) {
        return new Verification(regexpPath, httpMethod, requestResponseMap, true);
    }

    protected Gson gson() {
        return GSON;
    }

    protected Service getSparkService() {
        return sparkService;
    }

    protected String getMockServerAddress() {
        return mockServerAddress;
    }

    public void setMockServerAddress(String address) {
        mockServerAddress = address;
    }

    protected int getMockPort() {
        return mockPort;
    }

    public int getNumberOfServers() {
        return numberOfServers;
    }

    protected String responseFromJsonFile(String path) {
//        try (InputStream inputStream = resourceLoader.getResource("/mockresponse/" + path).getInputStream()) {
//            return IOUtils.toString(inputStream);
//        } catch (IOException e) {
//            LOGGER.error("can't read file from path", e);
//            return "";
//        }
        try (InputStream inputStream = MockServer.class.getResourceAsStream("/mockresponse/" + path)) {
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            LOGGER.error("can't read file from path", e);
            return "";
        }
    }

    private File createTempFileFromClasspath(String file) {
        try {
            InputStream sshPemInputStream = new ClassPathResource(file).getInputStream();
            File tempKeystoreFile = File.createTempFile(file, ".tmp");
            try (OutputStream outputStream = new FileOutputStream(tempKeystoreFile)) {
                IOUtils.copy(sshPemInputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("can't write " + file, e);
            }
            return tempKeystoreFile;
        } catch (IOException e) {
            throw new RuntimeException(file + " not found", e);
        }
    }
}
