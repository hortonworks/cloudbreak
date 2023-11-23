package com.sequenceiq.it.cloudbreak.microservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            throw new IllegalStateException("Should create a client for this user: " + who);
        }
        return sdxClient;
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
            UmsClient umsClient = createUmsClient();
            SdxSaasItClient sdxSaasItClient = createSdxSaasClient();
            AuthDistributorClient authDistributorClient = createAuthDistributorClient();
            RedbeamsClient redbeamsClient = createRedbeamsClient(cloudbreakUser);
            PeriscopeClient periscopeClient = createPeriscopeClient(cloudbreakUser);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(
                    CloudbreakClient.class, cloudbreakClient,
                    FreeIpaClient.class, freeIpaClient,
                    EnvironmentClient.class, environmentClient,
                    SdxClient.class, sdxClient,
                    RedbeamsClient.class, redbeamsClient,
                    UmsClient.class, umsClient,
                    SdxSaasItClient.class, sdxSaasItClient,
                    AuthDistributorClient.class, authDistributorClient,
                    PeriscopeClient.class, periscopeClient);
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
        return new SdxClient(cloudbreakUser, serverProperties.getSdxAddress());
    }

    private UmsClient createUmsClient() {
        return new UmsClient(serverProperties.getUmsHost(), serverProperties.getUmsPort(), regionAwareInternalCrnGeneratorFactory);
    }

    private SdxSaasItClient createSdxSaasClient() {
        return new SdxSaasItClient(serverProperties.getUmsHost(), regionAwareInternalCrnGeneratorFactory);
    }

    private synchronized AuthDistributorClient createAuthDistributorClient() {
        return new AuthDistributorClient(regionAwareInternalCrnGeneratorFactory, serverProperties.getAuthDistributorHost());
    }
}
