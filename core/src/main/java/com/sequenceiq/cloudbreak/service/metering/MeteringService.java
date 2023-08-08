package com.sequenceiq.cloudbreak.service.metering;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.sequenceiq.cloudbreak.converter.StackDtoToMeteringEventConverter;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.metering.GrpcMeteringClient;

@Service
public class MeteringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringService.class);

    @Inject
    private StackDtoToMeteringEventConverter stackDtoToMeteringEventConverter;

    @Inject
    private GrpcMeteringClient grpcMeteringClient;

    public void sendMeteringSyncEventForStack(StackDto stack) {
        MeteringEvent meteringEvent = stackDtoToMeteringEventConverter.convertToSyncEvent(stack);
        grpcMeteringClient.sendMeteringEvent(meteringEvent);
    }

    public void sendMeteringStatusChangeEventForStack(StackDto stack, ClusterStatus.Value eventOperation) {
        MeteringEvent meteringEvent = stackDtoToMeteringEventConverter.convertToStatusChangeEvent(stack, eventOperation);
        grpcMeteringClient.sendMeteringEvent(meteringEvent);
    }
}