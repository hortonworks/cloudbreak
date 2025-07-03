package com.sequenceiq.remoteenvironment.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto;
import com.sequenceiq.remotecluster.client.GrpcRemoteClusterClient;

@Service
public class PrivateEnvironmentBaseClusterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateEnvironmentBaseClusterService.class);

    private static final String CM_HOST_PATTERN = "^(https?)://([-a-zA-Z0-9.]+):(7180|7183).*";

    private static final String DATA_CENTER_NAME_PATTERN = "%s_%s_datacenter";

    private final GrpcRemoteClusterClient grpcRemoteClusterClient;

    private final Pattern compiledCmHostPattern;

    public PrivateEnvironmentBaseClusterService(GrpcRemoteClusterClient grpcRemoteClusterClient) {
        this.grpcRemoteClusterClient = grpcRemoteClusterClient;
        this.compiledCmHostPattern = Pattern.compile(CM_HOST_PATTERN);
    }

    public String registerBaseCluster(DescribeEnvironmentResponse envDetails, String controlPlaneCrn, String controlPlaneName) {
        String result = null;
        Optional<PvcEnvironmentDetails> pvcEnvironmentDetailsOpt = Optional.of(envDetails.getEnvironment())
                .map(Environment::getPvcEnvironmentDetails);
        if (pvcEnvironmentDetailsOpt.isPresent()) {
            PvcEnvironmentDetails pvcEnvironmentDetails = pvcEnvironmentDetailsOpt.get();
            String cmHost = pvcEnvironmentDetails.getCmHost();
            String environmentName = envDetails.getEnvironment().getEnvironmentName();
            String knoxGatewayUrl = pvcEnvironmentDetails.getKnoxGatewayUrl();
            LOGGER.info("Register base cluster for CM host({}) of environment({}) in control plane ({})/({})", cmHost, environmentName, controlPlaneName,
                    controlPlaneCrn);
            if (StringUtils.isNotEmpty(cmHost)) {
                Matcher cmHostMatcher = compiledCmHostPattern.matcher(cmHost);
                if (cmHostMatcher.matches()) {
                    result = createRequestAndRegister(controlPlaneCrn, controlPlaneName, cmHost, knoxGatewayUrl, envDetails.getEnvironment());
                } else {
                    LOGGER.warn("Could not register because environment's CM host({}) doesn't match with our pattern for control plane({}) and environment({})",
                            cmHost, controlPlaneCrn, envDetails.getEnvironment().getEnvironmentName());
                }
            } else {
                LOGGER.warn("Could not register because environment details doesn't contain CM host for control plane({}) and environment({})", controlPlaneCrn,
                        envDetails.getEnvironment().getEnvironmentName());
            }
        } else {
            LOGGER.warn("Could not register because environment details is empty for control plane({}) and environment({})", controlPlaneCrn,
                    envDetails.getEnvironment().getEnvironmentName());
        }
        return result;
    }

    private String createRequestAndRegister(String controlPlaneCrn, String controlPlaneName, String cmUrl, String knoxGatewayUrl, Environment environment) {
        String dcName = String.format(DATA_CENTER_NAME_PATTERN, controlPlaneName, environment.getEnvironmentName());
        LOGGER.info("Sending base cluster registration request with dcName({}), CM URL({}) and Knox gateway URL({}), control plane CRN({})", dcName,
                cmUrl, knoxGatewayUrl, controlPlaneName);
        RemoteClusterInternalProto.RegisterPvcBaseClusterRequest.Builder requestBuilder = RemoteClusterInternalProto.RegisterPvcBaseClusterRequest.newBuilder()
                .setCmUrl(cmUrl)
                .setPvcCrn(controlPlaneCrn)
                .setDcName(dcName);
        if (StringUtils.isNotEmpty(knoxGatewayUrl)) {
            requestBuilder = requestBuilder.setKnoxGatewayUrl(knoxGatewayUrl);
        }
        if (StringUtils.isNotEmpty(environment.getCrn())) {
            requestBuilder = requestBuilder.setEnvironmentCrn(environment.getCrn());
        }
        return grpcRemoteClusterClient.registerPrivateEnvironmentBaseCluster(requestBuilder.build());
    }
}
