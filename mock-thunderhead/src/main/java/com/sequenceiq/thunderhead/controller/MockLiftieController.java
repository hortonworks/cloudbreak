package com.sequenceiq.thunderhead.controller;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.cloudera.api.LiftieApi;
import com.cloudera.model.CommonClusterView;
import com.cloudera.model.CommonCreateClusterRequest;
import com.cloudera.model.CommonCreateClusterResponse;
import com.cloudera.model.CommonDeleteClusterResponse;
import com.cloudera.model.GithubInfraClouderaComLiftieLiftiePkgCommonStatusMessage;

@RestController
public class MockLiftieController implements LiftieApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockLiftieController.class);

    private final Set<String> clusterIds = new ConcurrentSkipListSet<>();

    @Override
    public ResponseEntity<CommonCreateClusterResponse> createCluster(CommonCreateClusterRequest cluster) {
        String clusterId = UUID.randomUUID().toString();
        clusterIds.add(clusterId);
        LOGGER.info("Created cluster with id {} for request {}", clusterId, cluster);
        return new ResponseEntity<>(new CommonCreateClusterResponse().clusterId(clusterId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<CommonClusterView> getClusterStatus(String clusterId) {
        LOGGER.info("Get cluster status by id {}", clusterId);
        if (clusterIds.contains(clusterId)) {
            return new ResponseEntity<>(new CommonClusterView().clusterStatus(new GithubInfraClouderaComLiftieLiftiePkgCommonStatusMessage().status("created")),
                    HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new CommonClusterView().clusterStatus(new GithubInfraClouderaComLiftieLiftiePkgCommonStatusMessage().status("deleted")),
                    HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<CommonDeleteClusterResponse> deleteCluster(String clusterId) {
        LOGGER.info("Delete cluster by id {}", clusterId);
        if (clusterIds.contains(clusterId)) {
            return new ResponseEntity<>(new CommonDeleteClusterResponse(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
}
