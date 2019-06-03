package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.TelemetryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.logging.LoggingV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.workload.WorkloadAnalyticsV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.telemetry.TelemetryV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.telemetry.logging.LoggingV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.telemetry.workload.WorkloadAnalyticsV1Request;

@Component
public class DistroXTelemetryToTelemetryV4Converter {

    public TelemetryV4Request convert(TelemetryV1Request source) {
        TelemetryV4Request telemetryV4Request = new TelemetryV4Request();
        if (source.getLogging() != null) {
            LoggingV1Request loggingV1Request = source.getLogging();
            LoggingV4Request loggingV4Request = new LoggingV4Request();
            loggingV4Request.setEnabled(loggingV1Request.isEnabled());
            loggingV4Request.setOutput(loggingV1Request.getOutput());
            loggingV4Request.setAttributes(loggingV1Request.getAttributes());
            telemetryV4Request.setLogging(loggingV4Request);
        }
        if (source.getWorkloadAnalytics() != null) {
            WorkloadAnalyticsV1Request waV1Request = source.getWorkloadAnalytics();
            WorkloadAnalyticsV4Request waV4Request = new WorkloadAnalyticsV4Request();
            waV4Request.setEnabled(waV1Request.isEnabled());
            waV4Request.setAttributes(waV1Request.getAttributes());
            waV4Request.setDatabusEndpoint(waV1Request.getDatabusEndpoint());
            waV4Request.setAccessKey(waV1Request.getAccessKey());
            waV4Request.setPrivateKey(waV1Request.getPrivateKey());
            telemetryV4Request.setWorkloadAnalytics(waV4Request);
        }
        return telemetryV4Request;
    }
}
