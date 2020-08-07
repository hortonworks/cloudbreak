package com.sequenceiq.it.cloudbreak.dto.diagnostics;

import java.util.Arrays;
import java.util.List;

import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;

public abstract class BaseDiagnosticsTestDto<
        REQUEST extends BaseDiagnosticsCollectionRequest,
        TEST_DTO extends BaseDiagnosticsTestDto,
        CLIENT extends MicroserviceClient> extends AbstractTestDto<REQUEST, FlowIdentifier, TEST_DTO, CLIENT> {

    public BaseDiagnosticsTestDto(String newId) {
        super(newId);
    }

    public BaseDiagnosticsTestDto(REQUEST request, TestContext testContext) {
        super(request, testContext);
    }

    public TEST_DTO withDefaults() {
        withDestination(DiagnosticsDestination.CLOUD_STORAGE);
        withLabels(Arrays.asList("fluentd"));
        return (TEST_DTO) this;
    }

    public TEST_DTO withDestination(DiagnosticsDestination destination) {
        getRequest().setDestination(destination);
        return (TEST_DTO) this;
    }

    public TEST_DTO withLabels(List<String> labels) {
        getRequest().setLabels(labels);
        return (TEST_DTO) this;
    }

    public TEST_DTO withAdditionalLogs(List<VmLog> additionalLogs) {
        getRequest().setAdditionalLogs(additionalLogs);
        return (TEST_DTO) this;
    }
}
