package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiGrpc;
import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config.GrpcWorkloadConnectivityAdminConfig;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;

@Component
public class WorkloadConnectivityAdminClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadConnectivityAdminClient.class);

    @Qualifier("workloadConnectivityAdminManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private GrpcWorkloadConnectivityAdminConfig grpcWorkloadConnectivityAdminConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse listTunnelStatus(String accountId, String environmentCrn) {
        WorkloadConnectivityAdminApiGrpc.WorkloadConnectivityAdminApiBlockingStub client = makeClient(channelWrapper.getChannel());
        WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest request =
                WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest.newBuilder()
                        .setAccountId(accountId)
                        .setEnvironmentCrn(environmentCrn)
                        .build();
        LOGGER.debug("Calling listTunnelStatus with params environmentCrn: {}", environmentCrn);

        return client.listTunnelStatus(request);
    }

    private WorkloadConnectivityAdminApiGrpc.WorkloadConnectivityAdminApiBlockingStub makeClient(ManagedChannel managedChannel) {
        String internalUserCrn = regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString();
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return WorkloadConnectivityAdminApiGrpc.newBlockingStub(managedChannel)
                .withInterceptors(GrpcUtil.getTimeoutInterceptor(grpcWorkloadConnectivityAdminConfig.getGrpcTimeoutSec()),
                        new AltusMetadataInterceptor(requestId, internalUserCrn));
    }
}
