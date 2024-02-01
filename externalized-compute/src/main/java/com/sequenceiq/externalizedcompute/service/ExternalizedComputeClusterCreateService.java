package com.sequenceiq.externalizedcompute.service;

import java.util.ArrayList;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.DefaultApi;
import com.cloudera.model.CommonAPIServer;
import com.cloudera.model.CommonClusterMetadata;
import com.cloudera.model.CommonClusterOwner;
import com.cloudera.model.CommonClusterSpec;
import com.cloudera.model.CommonCreateClusterRequest;
import com.cloudera.model.CommonCreateClusterResponse;
import com.cloudera.model.CommonKubernetes;
import com.cloudera.model.CommonNetwork;
import com.cloudera.model.CommonNetworkTopology;
import com.cloudera.model.CommonSecretEncryption;
import com.cloudera.model.CommonSecurity;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;
import com.sequenceiq.externalizedcompute.util.TagUtil;

@Service
public class ExternalizedComputeClusterCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreateService.class);

    @Inject
    private LiftieService liftieService;

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private ExternalizedComputeClusterRepository externalizedComputeClusterRepository;

    @Inject
    private CrnUserDetailsService crnUserDetailsService;

    public void initiateCreation(Long id, String userCrn) {
        ExternalizedComputeCluster externalizedComputeCluster = externalizedComputeClusterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Can't find externalized compute cluster in DB"));
        if (externalizedComputeCluster.getLiftieName() == null) {
            createLiftieCluster(userCrn, externalizedComputeCluster);
        }
    }

    private void createLiftieCluster(String userCrn, ExternalizedComputeCluster externalizedComputeCluster) {
        DefaultApi defaultApi = liftieService.getDefaultApi();
        CommonCreateClusterRequest cluster = setupLiftieCluster(userCrn, externalizedComputeCluster);
        try {
            CommonCreateClusterResponse clusterResponse = defaultApi.createCluster(cluster);
            externalizedComputeCluster.setLiftieName(clusterResponse.getClusterId());
            externalizedComputeClusterRepository.save(externalizedComputeCluster);
            LOGGER.info("Liftie create response: {}", clusterResponse);
        } catch (Exception e) {
            LOGGER.error("Externalized compute cluster creation failed", e);
            throw new RuntimeException(e);
        }
    }

    private CommonCreateClusterRequest setupLiftieCluster(String userCrn, ExternalizedComputeCluster externalizedComputeCluster) {
        CommonCreateClusterRequest cluster = new CommonCreateClusterRequest();
        CommonClusterMetadata metadata = getMetadata(externalizedComputeCluster, userCrn);
        cluster.setMetadata(metadata);

        CommonClusterSpec spec = new CommonClusterSpec();
        CommonKubernetes kubernetes = new CommonKubernetes();
        kubernetes.setVersion("1.25");
        spec.kubernetes(kubernetes);

        DetailedEnvironmentResponse environment = environmentEndpoint.getByCrn(externalizedComputeCluster.getEnvironmentCrn());
        CommonNetwork network = getNetwork(environment);
        spec.setNetwork(network);
        CommonSecurity security = getSecurity();
        spec.setSecurity(security);
        cluster.setSpec(spec);
        cluster.setKind("ManagedK8sCluster");
        cluster.setApiVersion("compute.cloud.cloudera.io/v1alpha1");
        return cluster;
    }

    private CommonClusterMetadata getMetadata(ExternalizedComputeCluster externalizedComputeCluster, String userCrn) {
        CommonClusterMetadata metadata = new CommonClusterMetadata();
        CrnUser crnUser = crnUserDetailsService.loadUserByUsername(userCrn);
        metadata.setOwnerEmail(crnUser.getEmail());
        CommonClusterOwner clusterOwner = new CommonClusterOwner().crn(crnUser.getUserCrn()).accountId(crnUser.getTenant());
        metadata.setEnv(externalizedComputeCluster.getEnvironmentCrn());
        metadata.setClusterOwner(clusterOwner);
        metadata.setClusterType("Dedicated");
        Map<String, String> tags = TagUtil.getTags(externalizedComputeCluster.getTags());
        metadata.setLabels(tags);
        metadata.setName(externalizedComputeCluster.getName());
        return metadata;
    }

    private CommonNetwork getNetwork(DetailedEnvironmentResponse environment) {
        CommonNetwork network = new CommonNetwork();
        CommonNetworkTopology topology = new CommonNetworkTopology();
        topology.setSubnets(new ArrayList<>(environment.getNetwork().getLiftieSubnets().keySet()));
        network.setTopology(topology);
        return network;
    }

    private CommonSecurity getSecurity() {
        CommonSecurity security = new CommonSecurity();
        CommonAPIServer apiServer = new CommonAPIServer();
        apiServer.setEnabled(true);
        security.setApiServer(apiServer);
        security.setPrivate(false);
        CommonSecretEncryption secretEncryption = new CommonSecretEncryption();
        secretEncryption.setEnabled(false);
        security.setSecretEncryption(secretEncryption);
        return security;
    }

}
