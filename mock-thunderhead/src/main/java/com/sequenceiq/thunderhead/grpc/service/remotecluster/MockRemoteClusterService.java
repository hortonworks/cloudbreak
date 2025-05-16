package com.sequenceiq.thunderhead.grpc.service.remotecluster;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.REMOTE_CLUSTER;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalGrpc;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.PvcControlPlaneConfiguration;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.thunderhead.entity.PrivateControlPlane;
import com.sequenceiq.thunderhead.service.PrivateControlPlaneService;

import io.grpc.stub.StreamObserver;

@Component
public class MockRemoteClusterService extends RemoteClusterInternalGrpc.RemoteClusterInternalImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockRemoteClusterService.class);

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private PrivateControlPlaneService privateControlPlaneService;

    @Override
    public void listAllPvcControlPlanes(ListAllPvcControlPlanesRequest request, io.grpc.stub.StreamObserver<ListAllPvcControlPlanesResponse> responseObserver) {
        List<PvcControlPlaneConfiguration> pvcControlPlaneConfigurations = privateControlPlaneService.findAll().stream()
                .map(this::convert)
                .toList();
        ListAllPvcControlPlanesResponse response = ListAllPvcControlPlanesResponse.newBuilder()
                .addAllControlPlaneConfigurations(pvcControlPlaneConfigurations)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private PvcControlPlaneConfiguration convert(PrivateControlPlane privateControlPlane) {
        return PvcControlPlaneConfiguration.newBuilder()
                .setBaseUrl(privateControlPlane.getUrl())
                .setPvcId(privateControlPlane.getPvcTenantId())
                .setPvcCrn(privateControlPlane.getCrn())
                .setName(privateControlPlane.getName())
                .setDescription(privateControlPlane.getName())
                .build();
    }

    @Override
    public void registerPvcBaseCluster(RemoteClusterInternalProto.RegisterPvcBaseClusterRequest request,
            StreamObserver<RegisterPvcBaseClusterResponse> responseObserver) {
        String pvcAccountId = Crn.safeFromString(request.getPvcCrn()).getAccountId();
        String baseClusterCrn = regionAwareCrnGenerator.generateCrnStringWithUuid(REMOTE_CLUSTER, pvcAccountId);
        LOGGER.info("Registering private base cluster with control plane crn '{}', DC name: '{}' and CM URL: '{}', Knox gateway URL: '{}', generated base " +
                        "cluster CRN: '{}'", request.getPvcCrn(), request.getDcName(), request.getCmUrl(), request.getKnoxGatewayUrl(), baseClusterCrn);
        RegisterPvcBaseClusterResponse registerPvcBaseClusterResponse = RegisterPvcBaseClusterResponse.newBuilder()
                .setClusterCrn(baseClusterCrn)
                .build();
        responseObserver.onNext(registerPvcBaseClusterResponse);
        responseObserver.onCompleted();
    }
}
