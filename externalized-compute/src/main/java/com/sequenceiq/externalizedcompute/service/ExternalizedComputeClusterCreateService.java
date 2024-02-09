package com.sequenceiq.externalizedcompute.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.api.DefaultApi;
import com.cloudera.model.CommonClusterMetadata;
import com.cloudera.model.CommonClusterOwner;
import com.cloudera.model.CommonClusterSpec;
import com.cloudera.model.CommonCreateClusterRequest;
import com.cloudera.model.CommonCreateClusterResponse;
import com.cloudera.model.CommonKubernetes;
import com.cloudera.model.CommonNetwork;
import com.cloudera.model.CommonNetworkTopology;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;
import com.sequenceiq.externalizedcompute.util.TagUtil;

@Service
public class ExternalizedComputeClusterCreateService {

    public static final String COMPUTE_CLUSTER_JSON = "compute-cluster.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreateService.class);

    @Value("${externalizedcompute.create.kubernetes.version:1.28}")
    private String kubernetesVersion;

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
        String databaseTemplateJson = FileReaderUtils.readFileFromClasspathQuietly(COMPUTE_CLUSTER_JSON);
        try {
            CommonCreateClusterRequest commonCreateClusterRequest = JsonUtil.readValue(databaseTemplateJson, CommonCreateClusterRequest.class);
            CommonCreateClusterRequest cluster = setupLiftieCluster(commonCreateClusterRequest, userCrn, externalizedComputeCluster);
            try {
                LOGGER.info("Send request to liftie: {}", cluster);
                CommonCreateClusterResponse clusterResponse = defaultApi.createCluster(cluster);
                externalizedComputeCluster.setLiftieName(clusterResponse.getClusterId());
                externalizedComputeClusterRepository.save(externalizedComputeCluster);
                LOGGER.info("Liftie create response: {}", clusterResponse);
            } catch (Exception e) {
                LOGGER.error("Externalized compute cluster creation failed", e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            LOGGER.error("Can't read cluster json to CommonCreateClusterRequest", e);
            throw new CloudbreakServiceException("Can't read cluster json to CommonCreateClusterRequest", e);
        }
    }

    private CommonCreateClusterRequest setupLiftieCluster(CommonCreateClusterRequest cluster, String userCrn,
            ExternalizedComputeCluster externalizedComputeCluster) {
        fillMetadata(cluster.getMetadata(), externalizedComputeCluster, userCrn);
        CommonClusterSpec spec = cluster.getSpec();
        if (spec == null) {
            throw new IllegalStateException("CommonClusterSpec can not be null!");
        }
        setKubernetes(spec);
        fillNetworkSettings(spec, externalizedComputeCluster);
        return cluster;
    }

    private void fillNetworkSettings(CommonClusterSpec spec, ExternalizedComputeCluster externalizedComputeCluster) {
        DetailedEnvironmentResponse environment = environmentEndpoint.getByCrn(externalizedComputeCluster.getEnvironmentCrn());
        CommonNetwork network = getNetworkFromEnvResponse(environment);
        spec.setNetwork(network);
    }

    private void setKubernetes(CommonClusterSpec spec) {
        CommonKubernetes kubernetes = new CommonKubernetes();
        kubernetes.setVersion(kubernetesVersion);
        spec.setKubernetes(kubernetes);
    }

    private void fillMetadata(CommonClusterMetadata metadata, ExternalizedComputeCluster externalizedComputeCluster, String userCrn) {
        CrnUser crnUser = crnUserDetailsService.loadUserByUsername(userCrn);
        metadata.setOwnerEmail(crnUser.getEmail());
        CommonClusterOwner clusterOwner = new CommonClusterOwner().crn(crnUser.getUserCrn()).accountId(crnUser.getTenant());
        metadata.setEnv(externalizedComputeCluster.getEnvironmentCrn());
        metadata.setClusterOwner(clusterOwner);
        Map<String, String> tags = TagUtil.getTags(externalizedComputeCluster.getTags());
        metadata.setLabels(tags);
        metadata.setName(externalizedComputeCluster.getName());
    }

    private CommonNetwork getNetworkFromEnvResponse(DetailedEnvironmentResponse environment) {
        CommonNetwork network = new CommonNetwork();
        CommonNetworkTopology topology = new CommonNetworkTopology();
        topology.setSubnets(new ArrayList<>(environment.getNetwork().getLiftieSubnets().keySet()));
        network.setTopology(topology);
        return network;
    }

}
