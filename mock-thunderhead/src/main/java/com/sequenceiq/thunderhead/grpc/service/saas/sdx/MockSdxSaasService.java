package com.sequenceiq.thunderhead.grpc.service.saas.sdx;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminGrpc;
import com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto;
import com.cloudera.thunderhead.service.sdxsvccommon.SDXSvcCommonProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;

import io.grpc.stub.StreamObserver;

@Service
public class MockSdxSaasService extends SDXSvcAdminGrpc.SDXSvcAdminImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSdxSaasService.class);

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    private final Map<Crn, Crn> sdxByEnv = new ConcurrentHashMap<>();

    @Override
    public void createInstance(SDXSvcAdminProto.CreateInstanceRequest request, StreamObserver<SDXSvcAdminProto.CreateInstanceResponse> responseObserver) {
        Crn envCrn = Crn.safeFromString(request.getEnvironment());
        Crn sdxCrn = regionAwareCrnGenerator.generateCrn(CrnResourceDescriptor.SDX_SAAS_INSTANCE, envCrn.getResource(), envCrn.getAccountId());
        sdxByEnv.putIfAbsent(envCrn, sdxCrn);
        SDXSvcCommonProto.Instance sdxInstance = getInstance(envCrn, sdxCrn);
        responseObserver.onNext(SDXSvcAdminProto.CreateInstanceResponse.newBuilder()
                .setInstance(sdxInstance)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteInstance(SDXSvcAdminProto.DeleteInstanceRequest request, StreamObserver<SDXSvcAdminProto.DeleteInstanceResponse> responseObserver) {
        Crn sdxCrn = Crn.safeFromString(request.getInstance());
        Optional<Crn> envCrn = sdxByEnv.entrySet().stream()
                .filter(entry -> sdxCrn.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
        SDXSvcAdminProto.DeleteInstanceResponse deleteInstanceResponse;
        if (envCrn.isPresent() && sdxByEnv.remove(envCrn.get()) != null) {
            deleteInstanceResponse = SDXSvcAdminProto.DeleteInstanceResponse.newBuilder()
                    .setInstance(getInstance(envCrn.get(), sdxCrn))
                    .build();
        } else {
            deleteInstanceResponse = SDXSvcAdminProto.DeleteInstanceResponse.newBuilder().build();
        }
        LOGGER.info("Delete instance response: " + deleteInstanceResponse);
        responseObserver.onNext(deleteInstanceResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void findInstances(SDXSvcAdminProto.FindInstancesRequest request, StreamObserver<SDXSvcAdminProto.FindInstancesResponse> responseObserver) {
        Crn envCrn = Crn.safeFromString(request.getSearchByEnvironment().getEnvironment());
        SDXSvcAdminProto.FindInstancesResponse instancesResponse;
        if (sdxByEnv.containsKey(envCrn)) {
            instancesResponse = SDXSvcAdminProto.FindInstancesResponse.newBuilder()
                    .addInstances(getInstance(envCrn, sdxByEnv.get(envCrn)))
                    .build();
            LOGGER.info("List instances response: " + instancesResponse);
        } else {
            instancesResponse = SDXSvcAdminProto.FindInstancesResponse.newBuilder().build();
        }
        responseObserver.onNext(instancesResponse);
        responseObserver.onCompleted();
    }

    private SDXSvcCommonProto.Instance getInstance(Crn environmentCrn, Crn sdxCrn) {
        return getBuilder()
                .addEnvironments(environmentCrn.toString())
                .setCrn(sdxCrn.toString())
                .setHostname(sdxCrn.getResource())
                .setName(sdxCrn.getResource())
                .build();
    }

    private SDXSvcCommonProto.Instance.Builder getBuilder() {
        return SDXSvcCommonProto.Instance.newBuilder()
                .setCloudPlatform(SDXSvcCommonProto.CloudPlatform.Value.AWS)
                .setCreated(System.currentTimeMillis())
                .setCloudRegion(regionAwareCrnGenerator.getRegion())
                .setPort(1234)
                .setStatus(SDXSvcCommonProto.InstanceHighLevelStatus.Value.HEALTHY);
    }
}
