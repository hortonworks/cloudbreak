package com.sequenceiq.thunderhead.grpc.service.metering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionGrpc.MeteringIngestionImplBase;
import com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest;
import com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse;

import io.grpc.stub.StreamObserver;

@Component
public class MockMeteringService extends MeteringIngestionImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockMeteringService.class);

    @Override
    public void submitEvent(SubmitEventRequest request, StreamObserver<SubmitEventResponse> responseObserver) {
        LOGGER.info("Submit metering event for environment: {}", request.getEvent().getEnvironmentCrn());
        responseObserver.onNext(SubmitEventResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
