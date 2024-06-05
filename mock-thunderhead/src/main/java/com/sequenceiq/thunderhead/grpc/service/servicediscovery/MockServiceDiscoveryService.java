package com.sequenceiq.thunderhead.grpc.service.servicediscovery;

import org.springframework.stereotype.Service;

import com.cloudera.cdp.servicediscovery.ServiceDiscoveryGrpc;
import com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto;

import io.grpc.stub.StreamObserver;

@Service
public class MockServiceDiscoveryService extends ServiceDiscoveryGrpc.ServiceDiscoveryImplBase {

    @Override
    public void describeDatalakeAsApiRemoteDataContext(ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest request,
            StreamObserver<ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse> responseObserver) {
        responseObserver.onNext(ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse.newBuilder()
                .build());
        responseObserver.onCompleted();
    }
}
