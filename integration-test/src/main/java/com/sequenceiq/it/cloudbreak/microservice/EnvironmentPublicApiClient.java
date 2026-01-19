package com.sequenceiq.it.cloudbreak.microservice;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Set;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.environments2api.ApiClient;
import com.cloudera.thunderhead.service.environments2api.api.EnvironmentPublicApi;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sequenceiq.cloudbreak.client.ApiKeyRequestFilter;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.envpublicapi.EnvironmentPublicApiLastSyncStatusDto;
import com.sequenceiq.it.cloudbreak.dto.envpublicapi.EnvironmentPublicApiTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;

public class EnvironmentPublicApiClient<E extends Enum<E>, W extends WaitObject>
        extends MicroserviceClient<EnvironmentPublicApi, Void, E, W> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentPublicApiClient.class);

    private final EnvironmentPublicApi client;

    private EnvironmentPublicApi alternativeClient;

    public EnvironmentPublicApiClient(CloudbreakUser cloudbreakUser, String address, String alternativeAddress) {
        setActing(cloudbreakUser);
        client = new EnvironmentPublicApi(createApiClient(cloudbreakUser, address));

        if (isNotEmpty(alternativeAddress)) {
            alternativeClient = new EnvironmentPublicApi(createApiClient(cloudbreakUser, alternativeAddress));
        }
    }

    private static ApiClient createApiClient(CloudbreakUser cloudbreakUser, String address) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(address);
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .registerModule(new JavaTimeModule());
        JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider(mapper);
        ClientConfig clientConfig = apiClient.getClientConfig()
                .register(new ApiKeyRequestFilter(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey()))
                .register(jacksonJsonProvider);
        apiClient.setClientConfig(clientConfig);
        return apiClient;
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        LOGGER.info("Flow is not supported by public environment client");
        return null;
    }

    @Override
    public EnvironmentPublicApi getDefaultClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeClient;
        } else {
            return client;
        }
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(
                EnvironmentPublicApiTestDto.class.getSimpleName(),
                EnvironmentPublicApiLastSyncStatusDto.class.getSimpleName());
    }
}
