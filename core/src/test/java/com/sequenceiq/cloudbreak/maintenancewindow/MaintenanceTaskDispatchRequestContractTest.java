package com.sequenceiq.cloudbreak.maintenancewindow;

import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest;
import static org.assertj.core.api.Assertions.assertThat;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

/**
 * Guards the dispatch wire contract between the maintenance module's producer record
 * ({@code com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskDispatchRequest}) and this module's consumer
 * class. The two are maintained by hand and neither module is on the other's compile classpath, so the shared
 * {@code dispatch-request.json} fixture is the only thing tying them together: the producer side asserts it emits
 * these keys, and this test asserts core still consumes them.
 * <p>
 * Uses strict deserialization so a key the consumer no longer recognises fails loudly instead of being dropped.
 */
class MaintenanceTaskDispatchRequestContractTest {

    private static final String FIXTURE = "maintenancewindow/dispatch-request.json";

    @Test
    void dispatcherPayloadDeserializesWithEveryFieldPopulated() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath(FIXTURE);

        MaintenanceTaskDispatchRequest request = JsonUtil.readValueStrict(json, MaintenanceTaskDispatchRequest.class);

        assertThat(request.getTaskId()).isEqualTo(7L);
        assertThat(request.getRunId()).isEqualTo(99L);
        assertThat(request.getIdempotencyKey()).isEqualTo("99:2");
        assertThat(request.getAccountId()).isEqualTo("acc-12345");
        assertThat(request.getResourceCrn()).isEqualTo("crn:cdp:datahub:us-west-1:acc-12345:cluster:my-dh");
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

    /**
     * The authorization aspects resolve the {@code @ResourceCrn} field through {@code PropertyUtils.getProperty}
     * (commons-beanutils), which discovers properties via {@link Introspector} and therefore needs a
     * {@code getResourceCrn()} accessor. A record only exposes {@code resourceCrn()}, so converting this DTO back to
     * a record would silently break internal-actor tenant resolution and make every dispatch fail with 403 — the
     * beanutils lookup throws and {@code InternalCrnModifier} swallows it. This asserts against the same
     * {@link Introspector} contract beanutils relies on.
     */
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
}
