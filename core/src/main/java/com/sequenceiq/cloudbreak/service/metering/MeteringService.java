package com.sequenceiq.cloudbreak.service.metering;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.converter.StackDtoToMeteringEventConverter;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.metering.GrpcMeteringClient;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Service
public class MeteringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringService.class);

    @Inject
    private StackDtoToMeteringEventConverter stackDtoToMeteringEventConverter;

    @Inject
    private GrpcMeteringClient grpcMeteringClient;

    @Inject
    private StackDtoService stackDtoService;

    public void sendMeteringSyncEventForStack(StackDtoDelegate stack) {
        if (StackType.WORKLOAD == stack.getType()) {
            sendMeteringEvent(stackDtoToMeteringEventConverter.convertToSyncEvent(stack));
        }
    }

    public void sendMeteringStatusChangeEventForStack(long stackId, ClusterStatus.Value eventOperation) {
        sendMeteringStatusChangeEventForStack(stackDtoService.getById(stackId), eventOperation);
    }

    public void sendMeteringStatusChangeEventForStack(StackDtoDelegate stack, ClusterStatus.Value eventOperation) {
        if (StackType.WORKLOAD == stack.getType()) {
            grpcMeteringClient.sendMeteringEvent(stackDtoToMeteringEventConverter.convertToStatusChangeEvent(stack, eventOperation));
        }
    }

    private void sendMeteringEvent(MeteringEvent meteringEvent) {
        try {
            grpcMeteringClient.sendMeteringEvent(meteringEvent);
        } catch (Exception e) {
            LOGGER.warn("Metering event send failed.", e);
        }
    }
}