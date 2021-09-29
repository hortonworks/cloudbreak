package com.sequenceiq.it.cloudbreak.dto.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;

@Prototype
public class FreeIpaOperationStatusTestDto extends AbstractFreeIpaTestDto<String, OperationStatus, FreeIpaOperationStatusTestDto> {
    protected FreeIpaOperationStatusTestDto(String newId) {
        super(newId);
    }

    protected FreeIpaOperationStatusTestDto(String request, TestContext testContext) {
        super(request, testContext);
    }

    protected FreeIpaOperationStatusTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public FreeIpaOperationStatusTestDto valid() {
        if (StringUtils.isBlank(getOperationId()) && getTestContext().get(FreeIpaTestDto.class) != null) {
            String operationId = getTestContext().get(FreeIpaTestDto.class).getOperationId();
            setRequest(operationId);
            setOperationId(operationId);
        }
        return this;
    }

    public FreeIpaOperationStatusTestDto withOperationId(String operationId) {
        setRequest(operationId);
        setOperationId(operationId);
        return this;
    }

    public FreeIpaOperationStatusTestDto await(OperationState operationState) {
        return getTestContext().await(this, Map.of("status", operationState), waitForFlow().withWaitForFlow(Boolean.FALSE));
    }
}
