package com.sequenceiq.thunderhead.grpc.service.saas.sdx;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminGrpc;
import com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;

import io.grpc.stub.StreamObserver;

@Service
public class MockSdxSaasService extends SDXSvcAdminGrpc.SDXSvcAdminImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSdxSaasService.class);

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Override
    public void createInstance(SDXSvcAdminProto.CreateInstanceRequest request, StreamObserver<SDXSvcAdminProto.CreateInstanceResponse> responseObserver) {
        Crn environmentCrn = Crn.safeFromString(request.getEnvironment());
        SDXSvcAdminProto.Instance sdxInstance = getInstance(environmentCrn.getResource(), environmentCrn.getAccountId(), "sdx_instance");
        responseObserver.onNext(SDXSvcAdminProto.CreateInstanceResponse.newBuilder()
                .setInstance(sdxInstance)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteInstance(SDXSvcAdminProto.DeleteInstanceRequest request, StreamObserver<SDXSvcAdminProto.DeleteInstanceResponse> responseObserver) {
        String sdxCrn = request.getInstance();
        SDXSvcAdminProto.DeleteInstanceResponse deleteInstanceResponse = SDXSvcAdminProto.DeleteInstanceResponse.newBuilder()
                .setInstance(getInstance(sdxCrn))
                .build();
        LOGGER.info("Delete instance response: " + deleteInstanceResponse);
        responseObserver.onNext(deleteInstanceResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void listInstances(SDXSvcAdminProto.ListInstancesRequest request, StreamObserver<SDXSvcAdminProto.ListInstancesResponse> responseObserver) {
        SDXSvcAdminProto.ListInstancesResponse.Builder instancesResponse = SDXSvcAdminProto.ListInstancesResponse.newBuilder();
        LOGGER.info("List instances response: " + instancesResponse.build());
        responseObserver.onNext(instancesResponse.build());
        responseObserver.onCompleted();
    }

    private SDXSvcAdminProto.Instance getInstance(String environmentName, String accountId, String name) {
        String environmentCrn = regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.ENVIRONMENT, environmentName, accountId);
        return getBuilder()
                .addEnvironments(environmentCrn)
                .setCrn(regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.SDX_SAAS_INSTANCE, UUID.randomUUID().toString(),
                        Crn.safeFromString(environmentCrn).getAccountId()))
                .setHostname(name)
                .setName(name)
                .build();
    }

    private SDXSvcAdminProto.Instance getInstance(String sdxCrn) {
        return getBuilder()
                .setCrn(sdxCrn)
                .setHostname(Crn.safeFromString(sdxCrn).getResource())
                .setName(Crn.safeFromString(sdxCrn).getResource())
                .build();
    }

    private SDXSvcAdminProto.Instance.Builder getBuilder() {
        return SDXSvcAdminProto.Instance.newBuilder()
                .setCloudPlatform(SDXSvcAdminProto.CloudPlatform.Value.AWS)
                .setCreated(System.currentTimeMillis())
                .setCloudRegion(regionAwareCrnGenerator.getRegion())
                .setPort(1234)
                .setStatus(SDXSvcAdminProto.InstanceHighLevelStatus.Value.HEALTHY);
    }
}
