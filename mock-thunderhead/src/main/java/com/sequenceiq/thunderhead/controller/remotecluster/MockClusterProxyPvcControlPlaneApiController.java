package com.sequenceiq.thunderhead.controller.remotecluster;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.EnvironmentSummary;
import com.cloudera.thunderhead.service.environments2api.model.ListEnvironmentsResponse;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RestController
public class MockClusterProxyPvcControlPlaneApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockClusterProxyPvcControlPlaneApiController.class);

    private DescribeEnvironmentResponse mockRemoteEnvironmentResponse;

    @PostConstruct
    public void setup() throws IOException {
        String describeEnvironment = FileReaderUtils.readFileFromClasspathQuietly("mock-responses/clusterproxy/describe-environment.json");
        mockRemoteEnvironmentResponse = JsonUtil.readValue(describeEnvironment, DescribeEnvironmentResponse.class);
    }

    @PostMapping("/cluster-proxy/proxy/{crn}/PvcControlPlane/api/v1/environments2/listEnvironments")
    public ResponseEntity<ListEnvironmentsResponse> listEnvironments(@PathVariable("crn") String crn) {
        LOGGER.info("List remote environments for crn: '{}'", crn);
        try {
            ListEnvironmentsResponse mockRemoteEnvironmentResponses = new ListEnvironmentsResponse();
            for (int i = 0; i < 10; i++) {
                Crn crnObject = Crn.fromString(crn);
                EnvironmentSummary mockRemoteEnvironmentResponse = new EnvironmentSummary();
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

    @PostMapping("/cluster-proxy/proxy/{crn}/PvcControlPlane/api/v1/environments2/describeEnvironment")
    public ResponseEntity<DescribeEnvironmentResponse> describeEnvironments(@PathVariable("crn") String crn,
        @RequestBody DescribeEnvironmentRequest environmentRequest) {
        LOGGER.info("Describe remote environments for crn: '{}'", crn);
        try {
            mockRemoteEnvironmentResponse.getEnvironment().setCrn(environmentRequest.getEnvironmentName());
            return new ResponseEntity<>(
                    mockRemoteEnvironmentResponse,
                    HttpStatus.OK);
        } catch (Exception ex) {
            String msg = "UH-OH something went wrong!";
            LOGGER.warn(msg, ex);
            return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
