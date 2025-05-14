package com.sequenceiq.thunderhead.controller;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudera.cdp.servicediscovery.model.ApiRemoteDataContext;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.EnvironmentSummary;
import com.cloudera.thunderhead.service.environments2api.model.ListEnvironmentsResponse;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RestController
@RequestMapping(MockPvcControlPlaneApiController.PATH)
public class MockPvcControlPlaneApiController {

    public static final String PATH = "PvcControlPlane";

    private static final Logger LOGGER = LoggerFactory.getLogger(MockPvcControlPlaneApiController.class);

    private DescribeEnvironmentResponse mockRemoteEnvironmentResponse;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @PostConstruct
    public void setup() throws IOException {
        String describeEnvironment = FileReaderUtils.readFileFromClasspathQuietly("mock-responses/clusterproxy/describe-environment.json");
        mockRemoteEnvironmentResponse = JsonUtil.readValue(describeEnvironment, DescribeEnvironmentResponse.class);
    }

    @PostMapping("/{crn}/api/v1/environments2/listEnvironments")
    public ResponseEntity<ListEnvironmentsResponse> listEnvironments(@PathVariable("crn") String crn) {
        LOGGER.info("List remote environments for crn: '{}'", crn);
        try {
            ListEnvironmentsResponse mockRemoteEnvironmentResponses = new ListEnvironmentsResponse();
            for (int i = 0; i < 10; i++) {
                Crn controlPlaneCrn = Crn.safeFromString(crn);
                String pvControlPlaneAccountId = controlPlaneCrn.getResource();
                String environmentResourceId = pvControlPlaneAccountId + "-" + i;
                String environmentName = i + pvControlPlaneAccountId.substring(pvControlPlaneAccountId.lastIndexOf("-"));
                Crn envCrn = regionAwareCrnGenerator.generateCrn(
                        CrnResourceDescriptor.ENVIRONMENT,
                        environmentName + "/" + environmentResourceId,
                        pvControlPlaneAccountId);
                EnvironmentSummary mockRemoteEnvironmentResponse = new EnvironmentSummary();
                mockRemoteEnvironmentResponse.setEnvironmentName(environmentName);
                mockRemoteEnvironmentResponse.setCrn(envCrn.toString());
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

    @PostMapping("/{crn}/api/v1/environments2/describeEnvironment")
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

    @PostMapping("/{crn}/PvcControlPlane/api/v1/servicediscovery/describeDatalakeAsApiRemoteDataContext")
    public ResponseEntity<DescribeDatalakeAsApiRemoteDataContextResponse> fetchRemoteDataContext(@PathVariable("crn") String crn,
            @RequestBody DescribeDatalakeAsApiRemoteDataContextRequest describeDatalakeAsApiRemoteDataContextRequest) {
        LOGGER.info("Describe remote environments for crn: '{}'", crn);
        try {
            String pdlResponse = FileReaderUtils.readFileFromClasspathQuietly("mock-responses/pdl/pdl-rdc.json");
            LOGGER.info("PDL Response is {}", pdlResponse);
            ApiRemoteDataContext apiRemoteDataContext = JsonUtil.readValue(pdlResponse, ApiRemoteDataContext.class);
            DescribeDatalakeAsApiRemoteDataContextResponse describeDatalakeAsApiRemoteDataContextResponse =
                    new DescribeDatalakeAsApiRemoteDataContextResponse();
            describeDatalakeAsApiRemoteDataContextResponse.setDatalake(describeDatalakeAsApiRemoteDataContextRequest.getDatalake());
            describeDatalakeAsApiRemoteDataContextResponse.setContext(apiRemoteDataContext);
            return new ResponseEntity<>(
                    describeDatalakeAsApiRemoteDataContextResponse,
                    HttpStatus.OK);
        } catch (Exception ex) {
            String msg = "UH-OH something went wrong!";
            LOGGER.warn(msg, ex);
            return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
