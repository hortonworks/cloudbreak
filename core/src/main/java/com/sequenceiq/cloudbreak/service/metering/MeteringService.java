package com.sequenceiq.cloudbreak.service.metering;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.converter.StackDtoToMeteringEventConverter;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.job.instancechecker.InstanceCheckerJobService;
import com.sequenceiq.cloudbreak.job.metering.sync.MeteringSyncJobService;
import com.sequenceiq.cloudbreak.metering.GrpcMeteringClient;
import com.sequenceiq.cloudbreak.service.metrics.MeteringMetricTag;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class MeteringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringService.class);

    private static final String SYNC = "SYNC";

    @Inject
    private StackDtoToMeteringEventConverter stackDtoToMeteringEventConverter;

    @Inject
    private GrpcMeteringClient grpcMeteringClient;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private MeteringSyncJobService meteringSyncJobService;

    @Inject
    private InstanceCheckerJobService instanceCheckerJobService;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    public void sendMeteringSyncEventForStack(long stackId) {
        sendMeteringSyncEventForStack(stackDtoService.getByIdWithoutResources(stackId));
    }

    public void sendMeteringSyncEventForStack(StackDtoDelegate stack) {
        if (shouldSendMeteringEventForStack(stack.getStack())) {
            sendPeriodicMeteringEvent(stackDtoToMeteringEventConverter.convertToSyncEvent(stack));
        }
    }

    public void sendMeteringStatusChangeEventForStack(long stackId, ClusterStatus.Value eventOperation) {
        sendMeteringStatusChangeEventForStack(stackDtoService.getByIdWithoutResources(stackId), eventOperation);
    }

    public void sendMeteringStatusChangeEventForStack(StackDtoDelegate stack, ClusterStatus.Value eventOperation) {
        if (shouldSendMeteringEventForStack(stack.getStack())) {
            sendMeteringEvent(stackDtoToMeteringEventConverter.convertToStatusChangeEvent(stack, eventOperation));
        }
    }

    public void scheduleSync(long stackId) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        if (shouldSendMeteringEventForStack(stack)) {
            meteringSyncJobService.schedule(stackId);
        }
        if (shouldScheduleInstanceCheckerJobForStack(stack)) {
            instanceCheckerJobService.schedule(stackId);
        }
    }

    public void scheduleSyncIfNotScheduled(long stackId) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        if (shouldSendMeteringEventForStack(stack)) {
            meteringSyncJobService.scheduleIfNotScheduled(stackId);
        }
        if (shouldScheduleInstanceCheckerJobForStack(stack)) {
            instanceCheckerJobService.scheduleIfNotScheduled(stackId);
        }
    }

    public void unscheduleSync(long stackId) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        if (shouldSendMeteringEventForStack(stack)) {
            meteringSyncJobService.unschedule(String.valueOf(stackId));
        }
        if (shouldScheduleInstanceCheckerJobForStack(stack)) {
            instanceCheckerJobService.unschedule(String.valueOf(stackId));
        }
    }

    private void sendPeriodicMeteringEvent(MeteringEvent meteringEvent) {
        try {
            grpcMeteringClient.sendMeteringEventWithShortRetry(meteringEvent);
            metricService.incrementMetricCounter(MetricType.METERING_REPORT_SUCCESSFUL,
                    MeteringMetricTag.REPORT_TYPE.name(), SYNC);
        } catch (Exception e) {
            metricService.incrementMetricCounter(MetricType.METERING_REPORT_FAILED,
                    MeteringMetricTag.REPORT_TYPE.name(), SYNC);
            LOGGER.warn("Periodic Metering event send failed.", e);
        }
    }

    private void sendMeteringEvent(MeteringEvent meteringEvent) {
        try {
            grpcMeteringClient.sendMeteringEvent(meteringEvent);
            metricService.incrementMetricCounter(MetricType.METERING_REPORT_SUCCESSFUL,
                    MeteringMetricTag.REPORT_TYPE.name(), meteringEvent.getStatusChange().getStatus().name());
        } catch (Exception e) {
            metricService.incrementMetricCounter(MetricType.METERING_REPORT_FAILED,
                    MeteringMetricTag.REPORT_TYPE.name(), meteringEvent.getStatusChange().getStatus().name());
            LOGGER.warn("Metering event send failed.", e);
        }
    }

    private boolean shouldSendMeteringEventForStack(StackView stack) {
        return StackType.WORKLOAD == stack.getType() && !CloudPlatform.YARN.equalsIgnoreCase(stack.getCloudPlatform());
    }

    private boolean shouldScheduleInstanceCheckerJobForStack(StackView stack) {
        return (StackType.WORKLOAD == stack.getType() || StackType.DATALAKE == stack.getType())
                && !CloudPlatform.YARN.equalsIgnoreCase(stack.getCloudPlatform());
    }
}
