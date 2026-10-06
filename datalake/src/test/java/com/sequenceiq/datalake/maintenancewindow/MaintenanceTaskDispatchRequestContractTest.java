package com.sequenceiq.datalake.maintenancewindow;

import static com.sequenceiq.datalake.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.RESOURCE_CRN;
import static com.sequenceiq.datalake.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest;
import static org.assertj.core.api.Assertions.assertThat;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.sdx.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;

/**
 * Guards the dispatch wire contract between the maintenance module's producer record
 * ({@code com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskDispatchRequest}) and Data Lake's consumer
 * class. The two are maintained by hand and neither module is on the other's compile classpath, so the shared
 * {@code dispatch-request.json} fixture is the only thing tying them together.
 */
class MaintenanceTaskDispatchRequestContractTest {

    private static final String FIXTURE = "maintenancewindow/dispatch-request.json";

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void dispatcherPayloadDeserializesWithEveryFieldPopulated() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath(FIXTURE);

        MaintenanceTaskDispatchRequest request = JsonUtil.readValueStrict(json, MaintenanceTaskDispatchRequest.class);

        assertThat(request.getTaskId()).isEqualTo(7L);
        assertThat(request.getRunId()).isEqualTo(99L);
        assertThat(request.getIdempotencyKey()).isEqualTo("99:2");
        assertThat(request.getAccountId()).isEqualTo("acc-12345");
        assertThat(request.getResourceCrn()).isEqualTo("crn:cdp:datalake:us-west-1:acc-12345:datalake:my-dl");
        assertThat(request.getTaskType()).isEqualTo(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        assertThat(request.getWorkItemId()).isEqualTo("SALT_PASSWORD");
        assertThat(request.getTaskKind()).isEqualTo("ONE_SHOT");
        assertThat(request.getTaskPayload())
                .containsEntry(MaintenanceWindowSecretRotationSupport.PAYLOAD_SECRET_NAMES, List.of("SALT_PASSWORD"));
        assertThat(request.getMaintenanceScheduleId()).isEqualTo(42L);
        assertThat(request.getPolicyRevision()).isEqualTo("42:v1");
        assertThat(request.getWindowStart()).isEqualTo(1000L);
        assertThat(request.getWindowEnd()).isEqualTo(2000L);
    }

    @Test
    void resourceCrnIsDiscoverableAsAJavaBeanProperty() throws Exception {
        PropertyDescriptor[] descriptors =
                Introspector.getBeanInfo(MaintenanceTaskDispatchRequest.class).getPropertyDescriptors();

        PropertyDescriptor resourceCrn = Arrays.stream(descriptors)
                .filter(descriptor -> "resourceCrn".equals(descriptor.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "resourceCrn is not a discoverable JavaBean property; the authorization aspects cannot "
                                + "resolve the target tenant and every dispatch will fail with 403"));

        assertThat(resourceCrn.getReadMethod()).isNotNull();
        assertThat(resourceCrn.getReadMethod().getName()).isEqualTo("getResourceCrn");
        assertThat(resourceCrn.getReadMethod().invoke(aDispatchRequest().build())).isEqualTo(RESOURCE_CRN);
    }

    @Test
    void roundTripPreservesSnakeCaseWireFormat() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath(FIXTURE);
        MaintenanceTaskDispatchRequest request = JsonUtil.readValueStrict(json, MaintenanceTaskDispatchRequest.class);

        Map<String, Object> reserialized = JsonUtil.readValue(JsonUtil.writeValueAsString(request), Map.class);

        assertThat(reserialized).containsOnlyKeys(
                "task_id", "run_id", "idempotency_key", "account_id", "resource_crn", "task_type",
                "work_item_id", "task_kind", "task_payload", "maintenance_schedule_id", "policy_revision",
                "window_start", "window_end");
    }

    /**
     * {@code @AccountIdMatchesResourceCrn} only runs when its group is declared on the annotation <em>and</em> this
     * class redefines {@code Default} via {@code @GroupSequence}. Dropping either leaves the cross-tenant check
     * silently disabled, so assert the wiring here rather than trusting it; the constraint's own behaviour is covered
     * by {@code AccountIdMatchesResourceCrnValidatorTest} in {@code common}.
     */
    @Test
    void accountIdMismatchWithResourceCrnIsRejected() {
        MaintenanceTaskDispatchRequest request = aDispatchRequest().withAccountId("other-account").build();

        Set<ConstraintViolation<MaintenanceTaskDispatchRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("accountId");
        assertThat(violations.iterator().next().getMessage()).contains("account_id");
    }
}
