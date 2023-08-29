package com.sequenceiq.cloudbreak.service.metering;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.converter.StackDtoToMeteringEventConverter;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.job.metering.MeteringJobAdapter;
import com.sequenceiq.cloudbreak.job.metering.MeteringJobService;
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
    private MeteringJobService meteringJobService;

    @Inject
    private CommonMetricService metricsService;

    public void sendMeteringSyncEventForStack(long stackId) {
        sendMeteringSyncEventForStack(stackDtoService.getByIdWithoutResources(stackId));
    }

    public void sendMeteringSyncEventForStack(StackDtoDelegate stack) {
        if (StackType.WORKLOAD == stack.getType()) {
            sendPeriodicMeteringEvent(stackDtoToMeteringEventConverter.convertToSyncEvent(stack));
        }
    }

    public void sendMeteringStatusChangeEventForStack(long stackId, ClusterStatus.Value eventOperation) {
        sendMeteringStatusChangeEventForStack(stackDtoService.getByIdWithoutResources(stackId), eventOperation);
    }

    public void sendMeteringStatusChangeEventForStack(StackDtoDelegate stack, ClusterStatus.Value eventOperation) {
        if (StackType.WORKLOAD == stack.getType()) {
            sendMeteringEvent(stackDtoToMeteringEventConverter.convertToStatusChangeEvent(stack, eventOperation));
        }
    }

    public void scheduleSync(long stackId) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        if (StackType.WORKLOAD == stack.getType()) {
            meteringJobService.schedule(stackId, MeteringJobAdapter.class);
        }
    }

    public void unscheduleSync(long stackId) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        if (StackType.WORKLOAD == stack.getType()) {
            meteringJobService.unschedule(String.valueOf(stackId));
        }
    }

    private void sendPeriodicMeteringEvent(MeteringEvent meteringEvent) {
        try {
            grpcMeteringClient.sendMeteringEventWithoutRetry(meteringEvent);
            metricsService.incrementMetricCounter(MetricType.METERING_REPORT_SUCCESSFUL,
                    MeteringMetricTag.REPORT_TYPE.name(), SYNC);
        } catch (Exception e) {
            metricsService.incrementMetricCounter(MetricType.METERING_REPORT_FAILED,
                    MeteringMetricTag.REPORT_TYPE.name(), SYNC);
            LOGGER.warn("Periodic Metering event send failed.", e);
        }
    }

    private void sendMeteringEvent(MeteringEvent meteringEvent) {
        try {
            grpcMeteringClient.sendMeteringEvent(meteringEvent);
            metricsService.incrementMetricCounter(MetricType.METERING_REPORT_SUCCESSFUL,
                    MeteringMetricTag.REPORT_TYPE.name(), meteringEvent.getStatusChange().getStatus().name());
        } catch (Exception e) {
            metricsService.incrementMetricCounter(MetricType.METERING_REPORT_FAILED,
                    MeteringMetricTag.REPORT_TYPE.name(), meteringEvent.getStatusChange().getStatus().name());
            LOGGER.warn("Metering event send failed.", e);
        }
    }
}