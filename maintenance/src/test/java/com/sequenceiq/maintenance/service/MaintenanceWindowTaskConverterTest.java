package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;
import com.sequenceiq.maintenance.domain.MaintenanceTaskKind;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;

class MaintenanceWindowTaskConverterTest {

    private final MaintenanceWindowTaskConverter converter = new MaintenanceWindowTaskConverter();

    @Test
    void toEntityRejectsNonSerializableExecutionRef() {
        MaintenanceWindowTaskRequest request = validRequest();
        request.setExecutionRef(Map.of("bad", new ExplodingBean()));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> converter.toEntity(request));

        assertThat(exception.getMessage()).isEqualTo("executionRef must be JSON-serializable.");
    }

    @Test
    void toResponseAllowsNullOptionalAndLegacyColumns() {
        MaintenanceWindowTask task = new MaintenanceWindowTask();
        task.setId(1L);
        task.setAccountId("acc-1");
        task.setResourceCrn("crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1");
        task.setEnvironmentCrn("crn:cdp:environments:us-west-1:acc-1:environment:env-1");
        task.setTaskType("secret-rotation");
        task.setWorkItemId("secret-1");
        task.setSubmitterService("secret-rotation-service");

        MaintenanceWindowTaskResponse response = converter.toResponse(task, null);

        assertThat(response.getTaskKind()).isNull();
        assertThat(response.getStatus()).isNull();
        assertThat(response.getTaskPayload()).isNull();
        assertThat(response.getExecutionRef()).isNull();
    }

    @Test
    void toResponseMapsStatusExecutionRefAndTaskPayloadWhenPresent() {
        MaintenanceWindowTask task = new MaintenanceWindowTask();
        task.setTaskKind(MaintenanceTaskKind.EVERY_WINDOW);
        task.setStatus(MaintenanceTaskStatus.ACTIVE);
        task.setTaskPayload(new Json(Map.of("key", "value")));
        task.setExecutionRef(new Json(Map.of("type", "http")));

        MaintenanceWindowTaskResponse response = converter.toResponse(task, null);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getTaskPayload()).isEqualTo(Map.of("key", "value"));
        assertThat(response.getExecutionRef()).isEqualTo(Map.of("type", "http"));
    }

    @Test
    void toEntityRejectsNonSerializableTaskPayload() {
        MaintenanceWindowTaskRequest request = validRequest();
        request.setTaskPayload(Map.of("bad", new ExplodingBean()));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> converter.toEntity(request));

        assertThat(exception.getMessage()).isEqualTo("taskPayload must be JSON-serializable.");
    }

    private MaintenanceWindowTaskRequest validRequest() {
        MaintenanceWindowTaskRequest request = new MaintenanceWindowTaskRequest();
        request.setResourceCrn("crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1");
        request.setEnvironmentCrn("crn:cdp:environments:us-west-1:acc-1:environment:env-1");
        request.setTaskType("secret-rotation");
        request.setWorkItemId("secret-1");
        request.setTaskKind("EVERY_WINDOW");
        request.setSubmitterService("secret-rotation-service");
        request.setExecutionRef(Map.of("type", "http", "url", "http://example/execute"));
        return request;
    }

    private static final class ExplodingBean {
        public String getValue() {
            throw new IllegalStateException("not serializable");
        }
    }
}
