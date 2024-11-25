package com.sequenceiq.thunderhead.grpc.service.remotecluster;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.REMOTE_CLUSTER;

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

import io.grpc.stub.StreamObserver;

@Component
public class MockRemoteClusterService extends RemoteClusterInternalGrpc.RemoteClusterInternalImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockRemoteClusterService.class);

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Override
    public void listAllPvcControlPlanes(ListAllPvcControlPlanesRequest request, io.grpc.stub.StreamObserver<ListAllPvcControlPlanesResponse> responseObserver) {
        Crn crn1 = regionAwareCrnGenerator.generateCrn(REMOTE_CLUSTER, "69159fae-78cd-4427-b942-aec2676a4dd5", "cloudera");
        Crn crn2 = regionAwareCrnGenerator.generateCrn(REMOTE_CLUSTER, "69159fae-78cd-4427-b942-aec2676a4dd6", "hortonworks");
        Crn crn3 = regionAwareCrnGenerator.generateCrn(REMOTE_CLUSTER, "69159fae-78cd-4427-b942-aec2676a4dd7", "cloudera");


        PvcControlPlaneConfiguration pvcControlPlaneConfiguration1 = PvcControlPlaneConfiguration.newBuilder()
                .setBaseUrl("http://localhost")
                .setPvcCrn(crn1.toString())
                .setName("control_plane_1")
                .setDescription("This is a mock data")
                .build();

        PvcControlPlaneConfiguration pvcControlPlaneConfiguration2 = PvcControlPlaneConfiguration.newBuilder()
                .setBaseUrl("http://localhost")
                .setPvcCrn(crn2.toString())
                .setName("control_plane_2")
                .setDescription("This is a mock data")
                .build();

        PvcControlPlaneConfiguration pvcControlPlaneConfiguration3 = PvcControlPlaneConfiguration.newBuilder()
                .setBaseUrl("http://localhost")
                .setPvcCrn(crn3.toString())
                .setName("control_plane_3")
                .setDescription("This is a mock data")
                .build();

        ListAllPvcControlPlanesResponse response = ListAllPvcControlPlanesResponse.newBuilder()
                .addControlPlaneConfigurations(pvcControlPlaneConfiguration1)
                .addControlPlaneConfigurations(pvcControlPlaneConfiguration2)
                .addControlPlaneConfigurations(pvcControlPlaneConfiguration3)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
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
