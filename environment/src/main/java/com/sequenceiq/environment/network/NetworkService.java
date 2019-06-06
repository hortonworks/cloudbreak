package com.sequenceiq.environment.network;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.repository.BaseNetworkRepository;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@Service
public class NetworkService {

    private final BaseNetworkRepository networkRepository;

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    public NetworkService(BaseNetworkRepository baseNetworkRepository,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap) {
        this.networkRepository = baseNetworkRepository;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
    }

    public BaseNetwork createNetworkIfPossible(Environment environment, NetworkDto networkDto, CloudPlatform cloudPlatform, String accountId) {
        BaseNetwork network = null;
        if (networkDto != null) {
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(cloudPlatform);
            if (environmentNetworkConverter != null) {
                BaseNetwork baseNetwork = environmentNetworkConverter.convert(networkDto, environment);
                baseNetwork.setResourceCrn(createCRN(accountId));
                baseNetwork.setAccountId(accountId);
                network = save(baseNetwork);
            }
        }
        return network;
    }

    @SuppressWarnings("unchecked")
    public BaseNetwork save(BaseNetwork network) {
        Object saved = networkRepository.save(network);
        return (BaseNetwork) saved;
    }

    private String createCRN(@Nonnull String accountId) {
        return Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.NETWORK)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

}
