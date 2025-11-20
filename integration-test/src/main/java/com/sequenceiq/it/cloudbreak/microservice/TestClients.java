package com.sequenceiq.it.cloudbreak.microservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.collections.Maps;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.config.server.ServerProperties;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

@Prototype
public class TestClients {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestClients.class);

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private ServerProperties serverProperties;

    private final Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> clients = new HashMap<>();

    public Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> getClients() {
        return clients;
    }

    public SdxClient getSdxClient(String who) {
        SdxClient sdxClient = (SdxClient) clients.getOrDefault(who, Map.of()).get(SdxClient.class);
        if (sdxClient == null) {
            throw new IllegalStateException("Should create an SDX client for this user: " + who);
        }
        return sdxClient;
    }

    public CloudbreakClient getCloudbreakClient(String who) {
        CloudbreakClient cloudbreakClient = (CloudbreakClient) clients.getOrDefault(who, Map.of()).get(CloudbreakClient.class);
        if (cloudbreakClient == null) {
            throw new IllegalStateException("Should create a Cloudbreak client for this user: " + who);
        }
        return cloudbreakClient;
    }

    public <U extends MicroserviceClient> U getMicroserviceClientByType(Class<U> msClientClass, String who) {
        U microserviceClient = (U) clients.getOrDefault(who, Map.of()).get(msClientClass);
        if (microserviceClient == null) {
            throw new IllegalStateException("Should create a client for this user: " + who);
        }
        return microserviceClient;
    }

    public <U extends MicroserviceClient> U getMicroserviceClient(Class<? extends CloudbreakTestDto> testDtoClass, String who) {

        if (clients.get(who) == null || clients.get(who).isEmpty()) {
            throw new IllegalStateException("Should create a client for this user: " + who);
        }

        List<MicroserviceClient> microserviceClients = clients.get(who).values()
                .stream()
                .filter(client -> client.supportedTestDtos().contains(testDtoClass.getSimpleName()))
                .collect(Collectors.toList());

        if (microserviceClients.isEmpty()) {
            throw new IllegalStateException("This Dto is not supported by any clients: " + testDtoClass.getSimpleName());
        }

        if (microserviceClients.size() > 1) {
            throw new IllegalStateException("This Dto is supported by more than one clients: " + testDtoClass.getSimpleName() + ", clients" +
                    microserviceClients);
        }

        return (U) microserviceClients.get(0);
    }

    public void createTestClients(CloudbreakUser cloudbreakUser) {
        if (clients.get(cloudbreakUser.getAccessKey()) == null) {
            CloudbreakClient cloudbreakClient = createCloudbreakClient(cloudbreakUser);
            FreeIpaClient freeIpaClient = createFreeipaClient(cloudbreakUser);
            EnvironmentClient environmentClient = createEnvironmentClient(cloudbreakUser);
            SdxClient sdxClient = createSdxClient(cloudbreakUser);
            ExternalizedComputeClusterClient externalizedComputeClusterClient = createExternalizedComputeClusterClient(cloudbreakUser);
            UmsClient umsClient = createUmsClient();
            AuthDistributorClient authDistributorClient = createAuthDistributorClient();
            RedbeamsClient redbeamsClient = createRedbeamsClient(cloudbreakUser);
            PeriscopeClient periscopeClient = createPeriscopeClient(cloudbreakUser);
            RemoteEnvironmentClient remoteEnvironmentClient = createRemoteEnvironmentClient(cloudbreakUser);
            EnvironmentPublicApiClient environmentPublicApiClient = createEnvironmentPublicApiClient(cloudbreakUser);

            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Maps.newHashMap();
            clientMap.put(CloudbreakClient.class, cloudbreakClient);
            clientMap.put(FreeIpaClient.class, freeIpaClient);
            clientMap.put(EnvironmentClient.class, environmentClient);
            clientMap.put(SdxClient.class, sdxClient);
            clientMap.put(RedbeamsClient.class, redbeamsClient);
            clientMap.put(ExternalizedComputeClusterClient.class, externalizedComputeClusterClient);
            clientMap.put(UmsClient.class, umsClient);
            clientMap.put(AuthDistributorClient.class, authDistributorClient);
            clientMap.put(PeriscopeClient.class, periscopeClient);
            clientMap.put(RemoteEnvironmentClient.class, remoteEnvironmentClient);
            clientMap.put(EnvironmentPublicApiClient.class, environmentPublicApiClient);
            clients.put(cloudbreakUser.getAccessKey(), clientMap);
            LOGGER.info(" Microservice clients have been initialized successfully for account: \nDisplay name: {} \nAccess key: {} \nSecret key: {} " +
                            "\nCrn: {} \nAdmin: {}", cloudbreakUser.getDisplayName(), cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey(),
                    cloudbreakUser.getCrn(), cloudbreakUser.getAdmin());
        }
    }

    private CloudbreakClient createCloudbreakClient(CloudbreakUser cloudbreakUser) {
        return new CloudbreakClient(cloudbreakUser, regionAwareInternalCrnGeneratorFactory.iam(), serverProperties.getCloudbreakAddress(),
                serverProperties.getCloudbreakInternalAddress());
    }

    private EnvironmentClient createEnvironmentClient(CloudbreakUser cloudbreakUser) {
        return new EnvironmentClient(cloudbreakUser, regionAwareInternalCrnGeneratorFactory.iam(), serverProperties.getEnvironmentAddress(),
                serverProperties.getEnvironmentInternalAddress());
    }

    private FreeIpaClient createFreeipaClient(CloudbreakUser cloudbreakUser) {
        return new FreeIpaClient(cloudbreakUser, regionAwareInternalCrnGeneratorFactory.iam(), serverProperties.getFreeipaAddress(),
                serverProperties.getFreeipaInternalAddress());
    }

    private RedbeamsClient createRedbeamsClient(CloudbreakUser cloudbreakUser) {
        return new RedbeamsClient(cloudbreakUser, serverProperties.getRedbeamsAddress());
    }

    private PeriscopeClient createPeriscopeClient(CloudbreakUser cloudbreakUser) {
        return new PeriscopeClient(cloudbreakUser, serverProperties.getPeriscopeAddress());
    }

    private SdxClient createSdxClient(CloudbreakUser cloudbreakUser) {
        return new SdxClient(cloudbreakUser, serverProperties.getSdxAddress(), serverProperties.getSdxInternalAddress(),
                regionAwareInternalCrnGeneratorFactory.iam());
    }

    private RemoteEnvironmentClient createRemoteEnvironmentClient(CloudbreakUser cloudbreakUser) {
        return new RemoteEnvironmentClient(cloudbreakUser, serverProperties.getRemoteEnvironmentAddress(), regionAwareInternalCrnGeneratorFactory.iam());
    }

    private ExternalizedComputeClusterClient createExternalizedComputeClusterClient(CloudbreakUser cloudbreakUser) {
        return new ExternalizedComputeClusterClient(cloudbreakUser, regionAwareInternalCrnGeneratorFactory.iam(),
                serverProperties.getExternalizedComputeAddress(), serverProperties.getExternalizedComputeInternalAddress());
    }

    private UmsClient createUmsClient() {
        return new UmsClient(serverProperties.getUmsHost(), serverProperties.getUmsPort(), regionAwareInternalCrnGeneratorFactory);
    }

    private synchronized AuthDistributorClient createAuthDistributorClient() {
        return new AuthDistributorClient(regionAwareInternalCrnGeneratorFactory, serverProperties.getAuthDistributorHost());
    }

    private EnvironmentPublicApiClient createEnvironmentPublicApiClient(CloudbreakUser cloudbreakUser) {
        return new EnvironmentPublicApiClient(cloudbreakUser, serverProperties.getEnvironmentPublicApiAddress());
    }
}
