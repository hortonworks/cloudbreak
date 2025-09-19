package com.sequenceiq.remoteenvironment.scheduled;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;
import com.sequenceiq.remoteenvironment.service.PrivateControlPlaneService;
import com.sequenceiq.remoteenvironment.service.PrivateEnvironmentBaseClusterService;
import com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane.PrivateControlPlaneRemoteEnvironmentConnector;

@Component
public class PrivateEnvironmentBaseClusterRegistrarJob extends MdcQuartzJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateEnvironmentBaseClusterRegistrarJob.class);

    @Inject
    private PrivateControlPlaneRemoteEnvironmentConnector remoteEnvironmentService;

    @Inject
    private PrivateControlPlaneService privateControlPlaneService;

    @Inject
    private PrivateEnvironmentBaseClusterService privateEnvironmentBaseClusterService;

    @Nullable
    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            LOGGER.debug("query all private control plane and their environments with details and register base cluster with the collected information");
            List<PrivateControlPlane> controlPlanes = privateControlPlaneService.findAll();

            controlPlanes.stream().parallel().forEach(controlPlane -> {
                String cpCrn = controlPlane.getResourceCrn();
                String cpName = controlPlane.getName();
                LOGGER.info("Listing the available environments for control plane: '{}/{}'", cpName, cpCrn);
                List<SimpleRemoteEnvironmentResponse> envListResponses = remoteEnvironmentService.listRemoteEnvironmentsInternal(controlPlane);

                envListResponses.stream().parallel().forEach(envListResponse -> {
                    describeEnvironmentAndRegisterBaseCluster(controlPlane, envListResponse, cpCrn, cpName);
                });
            });
        } catch (Exception e) {
            LOGGER.error("Private environments base clusters registration failed.", e);
            throw new JobExecutionException("Could not query and update private control planes.", e);
        }
    }

    private void describeEnvironmentAndRegisterBaseCluster(PrivateControlPlane controlPlane, SimpleRemoteEnvironmentResponse envListResponse, String cpCrn,
            String cpName) {
        LOGGER.info("Describing environment with CRN: '{}', from control plane: '{}'", envListResponse.getCrn(), cpCrn);
        DescribeEnvironmentResponse envDetails = remoteEnvironmentService.describeRemoteEnvironmentInternal(controlPlane, envListResponse.getCrn());
        String registeredBaseClusterCrn = privateEnvironmentBaseClusterService.registerBaseCluster(envDetails, cpCrn, cpName);
        if (StringUtils.isNotEmpty(registeredBaseClusterCrn)) {
            LOGGER.info("Base cluster with CRN({}) has been registered successfully for environment({}) and control plane({})",
                    registeredBaseClusterCrn, envListResponse.getCrn(), cpCrn);
        } else {
            LOGGER.info("Base cluster could not be registered for environment({}) and control plane({})", envListResponse.getCrn(), cpCrn);
        }
    }
}
