package com.sequenceiq.thunderhead.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceCredential;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.thunderhead.entity.ClassicCluster;
import com.sequenceiq.thunderhead.model.EntityType;
import com.sequenceiq.thunderhead.repository.ClassicClusterRepository;
import com.sequenceiq.thunderhead.util.MockResponseLoader;

@Service
public class ClassicClusterService implements LoadResourcesForAccountIdService {

    public static final String CLUSTER_PROXY_CONFIG_SERVICE_NAME = "cloudera-manager";

    @Value("${mock.classicCluster.jsonPath:}")
    private String classicClustersJsonPath;

    @Inject
    private MockResponseLoader mockResponseLoader;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private ClassicClusterRepository classicClusterRepository;

    @Inject
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @Override
    public void load(String accountId) throws Exception {
        List<ClassicCluster> classicClusters = mockResponseLoader.load(ClassicCluster.class, new TypeReference<>() { }, classicClustersJsonPath);
        classicClusters.stream()
                .map(classicCluster -> extend(classicCluster, accountId))
                .map(classicClusterRepository::save)
                .forEach(this::registerClusterProxy);
    }

    private ClassicCluster extend(ClassicCluster classicCluster, String accountId) {
        String sanitizedName = classicCluster.getName().replaceAll("[^A-Za-z0-9]", "");
        String crn = regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.CLASSIC_CLUSTER, sanitizedName, accountId);
        classicCluster.setCrn(crn);
        classicCluster.setAccountId(accountId);
        return classicCluster;
    }

    public List<ClassicCluster> findAllByAccountId(String accountId) {
        return classicClusterRepository.findAllByAccountId(accountId);
    }

    public Optional<ClassicCluster> findByCrn(String crn) {
        return classicClusterRepository.findById(crn);
    }

    private void registerClusterProxy(ClassicCluster classicCluster) {
        ConfigRegistrationRequest configRegistrationRequest = new ConfigRegistrationRequest(
                classicCluster.getCrn(),
                classicCluster.getCrn(),
                null,
                classicCluster.getAccountId(),
                false,
                null,
                null,
                getClusterServiceConfigs(classicCluster),
                null,
                false,
                null,
                false
        );
        clusterProxyRegistrationClient.registerConfig(configRegistrationRequest);
    }

    private List<ClusterServiceConfig> getClusterServiceConfigs(ClassicCluster classicCluster) {
        ClusterServiceConfig clusterServiceConfig = new ClusterServiceConfig(
                CLUSTER_PROXY_CONFIG_SERVICE_NAME,
                List.of(classicCluster.getUrl()),
                null,
                false,
                getCredentials(classicCluster),
                null,
                null
        );
        return List.of(clusterServiceConfig);
    }

    private List<ClusterServiceCredential> getCredentials(ClassicCluster classicCluster) {
        List<ClusterServiceCredential> credentials = new ArrayList<>();
        if (EntityType.REAL.equals(classicCluster.getType())) {
            SecretResponse secretKey = stringToSecretResponseConverter.convert(classicCluster.getPassword().getSecret());
            String cdpAccessKeyRef = String.format("%s:secret:text", secretKey.getSecretPath());
            credentials.add(new ClusterServiceCredential(classicCluster.getUserName(), cdpAccessKeyRef, true));
        }
        return credentials;
    }
}
