package com.sequenceiq.thunderhead.controller.remotecluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.thunderhead.controller.remotecluster.domain.MockRemoteEnvironmentResponse;
import com.sequenceiq.thunderhead.controller.remotecluster.domain.MockRemoteEnvironmentResponses;

@RestController
public class MockThunderheadRemoteClusterApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockThunderheadRemoteClusterApiController.class);

    @PostMapping("/cluster-proxy/proxy/{crn}/PvcControlPlane/api/v1/environments2/listEnvironments")
    public ResponseEntity<MockRemoteEnvironmentResponses> describeCustomConfig(@PathVariable("crn") String crn) {
        LOGGER.info("Describe remote cluster for crn: '{}'", crn);
        try {
            MockRemoteEnvironmentResponses mockRemoteEnvironmentResponses = new MockRemoteEnvironmentResponses();
            for (int i = 0; i < 10; i++) {
                Crn crnObject = Crn.fromString(crn);
                MockRemoteEnvironmentResponse mockRemoteEnvironmentResponse = new MockRemoteEnvironmentResponse();
                mockRemoteEnvironmentResponse.setEnvironmentName(crnObject.getResource() + "-" + i);
                mockRemoteEnvironmentResponse.setCrn(crn);
                mockRemoteEnvironmentResponse.setCloudPlatform("OPENSHIFT");
                mockRemoteEnvironmentResponse.setStatus("AVAILABLE");
                mockRemoteEnvironmentResponses.getEnvironments().add(mockRemoteEnvironmentResponse);
            }
            return new ResponseEntity<>(
                    mockRemoteEnvironmentResponses,
                    HttpStatus.OK);
        } catch (Exception ex) {
            String msg = "UH-OH something went wrong!";
            LOGGER.warn(msg, ex);
            return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
