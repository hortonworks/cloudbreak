package com.sequenceiq.thunderhead.service;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.clusterproxy.CdpAccessKey;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.thunderhead.controller.MockPvcControlPlaneApiController;
import com.sequenceiq.thunderhead.entity.PrivateControlPlane;
import com.sequenceiq.thunderhead.model.EntityType;
import com.sequenceiq.thunderhead.repository.PrivateControlPlaneRepository;
import com.sequenceiq.thunderhead.util.MockResponseLoader;

@Service
public class PrivateControlPlaneService implements LoadResourcesForAccountIdService {

    private static final String MOCK_API_PATH_PATTERN = "http://%s:%s/%s/%s";

    private static final String CLUSTER_PROXY_CONFIG_SERVICE_NAME = "PvcControlPlane";

    @Value("${mock.privateControlPlane.jsonPath:}")
    private String privateControlPlanesJsonPath;

    @Value("${mock.host:thunderhead-mock}")
    private String host;

    @Value("${server.port:8080}")
    private String port;

    @Inject
    private MockResponseLoader mockResponseLoader;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private PrivateControlPlaneRepository privateControlPlaneRepository;

    @Inject
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    public List<PrivateControlPlane> findAll() {
        return privateControlPlaneRepository.findAll();
    }

    @Override
    public void load(String accountId) throws Exception {
        List<PrivateControlPlane> privateControlPlanes =
                mockResponseLoader.load(PrivateControlPlane.class, new TypeReference<>() { }, privateControlPlanesJsonPath);
        privateControlPlanes.stream()
                .map(privateControlPlane -> extend(privateControlPlane, accountId))
                .map(privateControlPlaneRepository::save)
                .forEach(this::registerClusterProxy);
    }

    private PrivateControlPlane extend(PrivateControlPlane privateControlPlane, String accountId) {
        if (EntityType.MOCK.equals(privateControlPlane.getType())) {
            privateControlPlane.setPvcTenantId(privateControlPlane.getName());
        }
        String crn = regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.HYBRID, privateControlPlane.getPvcTenantId(), accountId);
        privateControlPlane.setCrn(crn);
        if (EntityType.MOCK.equals(privateControlPlane.getType())) {
            String url = String.format(MOCK_API_PATH_PATTERN, host, port, MockPvcControlPlaneApiController.PATH, privateControlPlane.getCrn());
            privateControlPlane.setUrl(url);
        }
        return privateControlPlane;
    }

    private void registerClusterProxy(PrivateControlPlane privateControlPlane) {
        ConfigRegistrationRequest configRegistrationRequest = new ConfigRegistrationRequest(
                privateControlPlane.getCrn(),
                privateControlPlane.getCrn(),
                null,
                privateControlPlane.getAccountId(),
                false,
                null,
                null,
                getClusterServiceConfigs(privateControlPlane),
                null,
                false,
                null,
                false
        );
        clusterProxyRegistrationClient.registerConfig(configRegistrationRequest);
    }

    private List<ClusterServiceConfig> getClusterServiceConfigs(PrivateControlPlane privateControlPlane) {
        ClusterServiceConfig clusterServiceConfig = new ClusterServiceConfig(
                CLUSTER_PROXY_CONFIG_SERVICE_NAME,
                List.of(privateControlPlane.getUrl()),
                getCdpAccessKey(privateControlPlane)
        );
        return List.of(clusterServiceConfig);
    }

    private CdpAccessKey getCdpAccessKey(PrivateControlPlane privateControlPlane) {
        CdpAccessKey cdpAccessKey = null;
        if (EntityType.REAL.equals(privateControlPlane.getType())) {
            SecretResponse secretKey = stringToSecretResponseConverter.convert(privateControlPlane.getSecretKey().getSecret());
            String cdpAccessKeyRef = String.format("%s:secret:text", secretKey.getSecretPath());
            cdpAccessKey = new CdpAccessKey(privateControlPlane.getAccessKey(), cdpAccessKeyRef);
        }
        return cdpAccessKey;
    }

}
