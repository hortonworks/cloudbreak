package com.sequenceiq.externalizedcompute.service;

import static com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CommonSecurity;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CommonClusterMetadata;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CommonClusterOwner;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CommonNetwork;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CommonNetworkTopology;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterResponse;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;
import com.sequenceiq.externalizedcompute.util.LiftieValidationResponseUtil;
import com.sequenceiq.externalizedcompute.util.TagUtil;
import com.sequenceiq.liftie.client.LiftieGrpcClient;

@Service
public class ExternalizedComputeClusterCreateService {

    public static final String COMPUTE_CLUSTER_JSON = "compute-cluster.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreateService.class);

    @Inject
    private LiftieGrpcClient liftieGrpcClient;

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private ExternalizedComputeClusterRepository externalizedComputeClusterRepository;

    @Inject
    private CrnUserDetailsService crnUserDetailsService;

    @Inject
    private LiftieValidationResponseUtil liftieValidationResponseUtil;

    public void initiateCreation(Long id, String userCrn) {
        ExternalizedComputeCluster externalizedComputeCluster = externalizedComputeClusterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Can't find externalized compute cluster in DB"));
        if (externalizedComputeCluster.getLiftieName() == null) {
            createLiftieCluster(userCrn, externalizedComputeCluster);
        }
    }

    private void createLiftieCluster(String userCrn, ExternalizedComputeCluster externalizedComputeCluster) {
        String databaseTemplateJson = FileReaderUtils.readFileFromClasspathQuietly(COMPUTE_CLUSTER_JSON);
        try {
            CreateClusterRequest.Builder createClusterBuilder = CreateClusterRequest.newBuilder();
            JsonFormat.parser().merge(databaseTemplateJson, createClusterBuilder);
            CreateClusterRequest cluster = setupLiftieCluster(createClusterBuilder, userCrn, externalizedComputeCluster);
            try {
                LOGGER.info("Send request to liftie: {}", cluster);
                CreateClusterResponse clusterResponse = liftieGrpcClient.createCluster(cluster, userCrn);
                if (clusterResponse.hasValidationResponse()) {
                    liftieValidationResponseUtil.throwException(clusterResponse.getValidationResponse());
                }
                if (StringUtils.isEmpty(clusterResponse.getClusterId())) {
                    LOGGER.warn("Liftie name cannot be empty!");
                    throw new CloudbreakServiceException("Externalized compute cluster creation failed, cluster id cannot be empty!");
                }
                externalizedComputeCluster.setLiftieName(clusterResponse.getClusterId());
                externalizedComputeClusterRepository.save(externalizedComputeCluster);
                LOGGER.info("Liftie create response: {}", clusterResponse);
            } catch (Exception e) {
                LOGGER.error("Externalized compute cluster creation failed", e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            LOGGER.error("Can't read cluster json to create cluster object", e);
            throw new CloudbreakServiceException("Can't read cluster json to create cluster object", e);
        }
    }

    private CreateClusterRequest setupLiftieCluster(CreateClusterRequest.Builder createClusterBuilder, String userCrn,
            ExternalizedComputeCluster externalizedComputeCluster) {
        DetailedEnvironmentResponse environment = environmentEndpoint.getByCrn(externalizedComputeCluster.getEnvironmentCrn());
        fillMetadata(createClusterBuilder, externalizedComputeCluster, userCrn);
        fillNetworkSettings(createClusterBuilder, environment);
        fillSecurity(createClusterBuilder, environment);
        createClusterBuilder.setIsDefault(externalizedComputeCluster.isDefaultCluster());
        return createClusterBuilder.build();
    }

    private void fillSecurity(CreateClusterRequest.Builder createClusterBuilder, DetailedEnvironmentResponse environment) {
        if (environment.getExternalizedComputeCluster() != null) {
            createClusterBuilder.getSpecBuilder()
                    .setSecurity(CommonSecurity.newBuilder()
                            .setPrivate(environment.getExternalizedComputeCluster().isPrivateCluster())
                            .build());
        }
    }

    private void fillNetworkSettings(CreateClusterRequest.Builder commonClusterBuilder, DetailedEnvironmentResponse environment) {
        CommonNetwork network = getNetworkFromEnvResponse(environment);
        commonClusterBuilder.getSpecBuilder().setNetwork(network);
    }

    private void fillMetadata(CreateClusterRequest.Builder commonClusterBuilder, ExternalizedComputeCluster externalizedComputeCluster,
            String userCrn) {
        CommonClusterMetadata.Builder metadataBuilder = commonClusterBuilder.getMetadataBuilder();
        CrnUser crnUser = crnUserDetailsService.loadUserByUsername(userCrn);
        CommonClusterOwner commonClusterOwner = CommonClusterOwner.newBuilder().setCrn(crnUser.getUserCrn())
                .setAccountId(crnUser.getTenant()).build();
        metadataBuilder.setClusterOwner(commonClusterOwner);
        metadataBuilder.setEnvironmentCrn(externalizedComputeCluster.getEnvironmentCrn());
        Map<String, String> tags = TagUtil.getTags(externalizedComputeCluster.getTags());
        metadataBuilder.putAllLabels(tags);
        metadataBuilder.setName(externalizedComputeCluster.getName());
    }

    private CommonNetwork getNetworkFromEnvResponse(DetailedEnvironmentResponse environment) {
        CommonNetworkTopology topology = CommonNetworkTopology.newBuilder()
                .addAllSubnets(environment.getNetwork().getLiftieSubnets().keySet()).build();
        return CommonNetwork.newBuilder().setTopology(topology).build();
    }

}
