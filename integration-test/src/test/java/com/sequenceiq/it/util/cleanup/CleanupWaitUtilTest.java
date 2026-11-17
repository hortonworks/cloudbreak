package com.sequenceiq.it.util.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.client.SdxClient;

@ExtendWith(MockitoExtension.class)
class CleanupWaitUtilTest {

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private SdxClient sdxClient;

    @Mock
    private EnvironmentClient environmentClient;

    @Mock
    private DistroXV1Endpoint distroXEndpoint;

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @InjectMocks
    private CleanupWaitUtil underTest;

    @BeforeEach
    void setUp() {
        // Keep the poll loops (not exercised here) from being slow if they ever run.
        ReflectionTestUtils.setField(underTest, "pollingInterval", 1L);
        ReflectionTestUtils.setField(underTest, "maxRetry", 1);
        ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", 10);
        ReflectionTestUtils.setField(underTest, "maxWaitMinutes", 120L);
        ReflectionTestUtils.setField(underTest, "deleteSleep", 0L);

        lenient().when(cloudbreakClient.distroXV1Endpoint()).thenReturn(distroXEndpoint);
        lenient().when(sdxClient.sdxEndpoint()).thenReturn(sdxEndpoint);
        lenient().when(environmentClient.environmentV1Endpoint()).thenReturn(environmentEndpoint);
    }

    // ---------- checkDistroxIsAvailable ----------

    @Test
    void checkDistroxIsAvailableNotFoundReturnsFalse() throws Exception {
        when(distroXEndpoint.getByName(any(), anySet())).thenThrow(new NotFoundException("gone"));
        assertThat((boolean) invoke("checkDistroxIsAvailable",
                new Class<?>[]{CloudbreakClient.class, String.class, CleanupWaitUtil.ConsecutiveErrors.class},
                cloudbreakClient, "distroA", new CleanupWaitUtil.ConsecutiveErrors(10))).isFalse();
    }

    /**
     * Regression: previously any Exception (including transient network errors) returned false,
     * which lied about the resource being gone. After the fix, transient errors return true so
     * the poll keeps going.
     */
    @Test
    void checkDistroxIsAvailableTransientExceptionReturnsTrue() throws Exception {
        when(distroXEndpoint.getByName(any(), anySet())).thenThrow(new ProcessingException("connect timed out"));
        assertThat((boolean) invoke("checkDistroxIsAvailable",
                new Class<?>[]{CloudbreakClient.class, String.class, CleanupWaitUtil.ConsecutiveErrors.class},
                cloudbreakClient, "distroA", new CleanupWaitUtil.ConsecutiveErrors(10))).isTrue();
    }

    // ---------- checkSdxIsAvailable ----------

    @Test
    void checkSdxIsAvailableNotFoundReturnsFalse() throws Exception {
        when(sdxEndpoint.get("sdxA")).thenThrow(new NotFoundException("gone"));
        assertThat((boolean) invoke("checkSdxIsAvailable",
                new Class<?>[]{SdxClient.class, String.class, CleanupWaitUtil.ConsecutiveErrors.class},
                sdxClient, "sdxA", new CleanupWaitUtil.ConsecutiveErrors(10))).isFalse();
    }

    @Test
    void checkSdxIsAvailableTransientExceptionReturnsTrue() throws Exception {
        when(sdxEndpoint.get("sdxA")).thenThrow(new ProcessingException("boom"));
        assertThat((boolean) invoke("checkSdxIsAvailable",
                new Class<?>[]{SdxClient.class, String.class, CleanupWaitUtil.ConsecutiveErrors.class},
                sdxClient, "sdxA", new CleanupWaitUtil.ConsecutiveErrors(10))).isTrue();
    }

    // ---------- checkEnvironmentIsAvailable ----------

    @Test
    void checkEnvironmentIsAvailableTransientExceptionReturnsTrue() throws Exception {
        when(environmentEndpoint.list(null)).thenThrow(new ProcessingException("boom"));
        assertThat((boolean) invoke("checkEnvironmentIsAvailable",
                new Class<?>[]{EnvironmentClient.class, String.class, CleanupWaitUtil.ConsecutiveErrors.class},
                environmentClient, "envA", new CleanupWaitUtil.ConsecutiveErrors(10))).isTrue();
    }

    // ---------- checkEnvironmentDeleteFailedStatus ----------

    @Test
    void checkEnvironmentDeleteFailedStatusEnvironmentNotInListReturnsFalse() throws Exception {
        // list returns other envs but not envA → previously fell through to ARCHIVED via .orElse(); now must be false.
        SimpleEnvironmentResponses envs = new SimpleEnvironmentResponses();
        envs.setResponses(List.of(env("envB", EnvironmentStatus.AVAILABLE)));
        when(environmentEndpoint.list(null)).thenReturn(envs);

        assertThat((boolean) invoke("checkEnvironmentDeleteFailedStatus",
                new Class<?>[]{EnvironmentClient.class, String.class},
                environmentClient, "envA")).isFalse();
    }

    @Test
    void checkEnvironmentDeleteFailedStatusEnvironmentInDeleteFailedReturnsTrue() throws Exception {
        SimpleEnvironmentResponses envs = new SimpleEnvironmentResponses();
        envs.setResponses(List.of(env("envA", EnvironmentStatus.DELETE_FAILED)));
        when(environmentEndpoint.list(null)).thenReturn(envs);

        assertThat((boolean) invoke("checkEnvironmentDeleteFailedStatus",
                new Class<?>[]{EnvironmentClient.class, String.class},
                environmentClient, "envA")).isTrue();
    }

    @Test
    void checkEnvironmentDeleteFailedStatusEnvironmentInOtherStateReturnsFalse() throws Exception {
        SimpleEnvironmentResponses envs = new SimpleEnvironmentResponses();
        envs.setResponses(List.of(env("envA", EnvironmentStatus.DELETE_INITIATED)));
        when(environmentEndpoint.list(null)).thenReturn(envs);

        assertThat((boolean) invoke("checkEnvironmentDeleteFailedStatus",
                new Class<?>[]{EnvironmentClient.class, String.class},
                environmentClient, "envA")).isFalse();
    }

    // ---------- checkDistroxDeleteFailedStatus ----------

    @Test
    void checkDistroxDeleteFailedStatusNotFoundReturnsFalse() throws Exception {
        when(distroXEndpoint.getByName(any(), anySet())).thenThrow(new NotFoundException("gone"));
        assertThat((boolean) invoke("checkDistroxDeleteFailedStatus",
                new Class<?>[]{CloudbreakClient.class, String.class},
                cloudbreakClient, "distroA")).isFalse();
    }

    @Test
    void checkDistroxDeleteFailedStatusDeleteFailedReturnsTrue() throws Exception {
        StackV4Response resp = new StackV4Response();
        resp.setStatus(Status.DELETE_FAILED);
        when(distroXEndpoint.getByName(any(), anySet())).thenReturn(resp);
        assertThat((boolean) invoke("checkDistroxDeleteFailedStatus",
                new Class<?>[]{CloudbreakClient.class, String.class},
                cloudbreakClient, "distroA")).isTrue();
    }

    // ---------- checkSdxDeleteFailedStatus ----------

    @Test
    void checkSdxDeleteFailedStatusNotFoundReturnsFalse() throws Exception {
        when(sdxEndpoint.get("sdxA")).thenThrow(new NotFoundException("gone"));
        assertThat((boolean) invoke("checkSdxDeleteFailedStatus",
                new Class<?>[]{SdxClient.class, String.class},
                sdxClient, "sdxA")).isFalse();
    }

    @Test
    void checkSdxDeleteFailedStatusDeleteFailedReturnsTrue() throws Exception {
        SdxClusterResponse resp = new SdxClusterResponse();
        resp.setStatus(SdxClusterStatusResponse.DELETE_FAILED);
        when(sdxEndpoint.get("sdxA")).thenReturn(resp);
        assertThat((boolean) invoke("checkSdxDeleteFailedStatus",
                new Class<?>[]{SdxClient.class, String.class},
                sdxClient, "sdxA")).isTrue();
    }

    // ---------- checkDistroxesAreAvailable (bulk) ----------

    /**
     * Regression for finding 1 (bulk variants): previously any Exception (including transient) → false.
     * Now transient errors bias toward "still present" so the poll keeps going.
     */
    @Test
    void checkDistroxesAreAvailableTransientExceptionReturnsTrue() throws Exception {
        when(distroXEndpoint.list(eq("envA"), eq("envA-crn"))).thenThrow(new ProcessingException("boom"));
        boolean result = (boolean) invoke("checkDistroxesAreAvailable",
                new Class<?>[]{CloudbreakClient.class, Map.class, CleanupWaitUtil.ConsecutiveErrors.class},
                cloudbreakClient, Map.of("envA-crn", "envA"), new CleanupWaitUtil.ConsecutiveErrors(10));
        assertThat(result).isTrue();
    }

    // ---------- checkSdxesAreAvailable (bulk) ----------

    @Test
    void checkSdxesAreAvailableTransientExceptionReturnsTrue() throws Exception {
        when(sdxEndpoint.list(eq("envA"), anyBoolean())).thenThrow(new ProcessingException("boom"));
        boolean result = (boolean) invoke("checkSdxesAreAvailable",
                new Class<?>[]{SdxClient.class, Map.class, CleanupWaitUtil.ConsecutiveErrors.class},
                sdxClient, Map.of("envA-crn", "envA"), new CleanupWaitUtil.ConsecutiveErrors(10));
        assertThat(result).isTrue();
    }

    // ---------- checkEnvironmentsAreAvailable (bulk) ----------

    @Test
    void checkEnvironmentsAreAvailableTransientExceptionReturnsTrue() throws Exception {
        when(environmentEndpoint.list(null)).thenThrow(new ProcessingException("boom"));
        boolean result = (boolean) invoke("checkEnvironmentsAreAvailable",
                new Class<?>[]{EnvironmentClient.class, CleanupWaitUtil.ConsecutiveErrors.class},
                environmentClient, new CleanupWaitUtil.ConsecutiveErrors(10));
        assertThat(result).isTrue();
    }

    // ---------- waitForEnvironmentCleanup: consecutive error limit and deadline ----------

    @Test
    void waitForEnvironmentCleanupStopsAfterConsecutiveErrorLimit() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", 3);
        when(environmentEndpoint.list(null)).thenThrow(new NotFoundException("nginx 404"));

        assertThat(underTest.waitForEnvironmentCleanup(environmentClient, "envA", RunDeadline.none())).isEqualTo(WaitResult.TIMEOUT);
        // Exactly 5 list calls: iterations 1 and 2 each do the availability check and the DELETE_FAILED check (4 calls);
        // in iteration 3 the availability check fails, the limit of 3 is reached and the DELETE_FAILED check is skipped (1 call);
        // the post-loop error-limit check returns before any further call.
        verify(environmentEndpoint, times(5)).list(null);
    }

    @Test
    void waitForEnvironmentCleanupIntermittentErrorsResetTheCounter() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", 2);
        SimpleEnvironmentResponses present = new SimpleEnvironmentResponses();
        present.setResponses(List.of(env("envA", EnvironmentStatus.DELETE_INITIATED)));
        SimpleEnvironmentResponses gone = new SimpleEnvironmentResponses();
        gone.setResponses(List.of());
        // Each loop iteration lists twice (availability, then DELETE_FAILED); the failure is only seen by the availability check.
        when(environmentEndpoint.list(null))
                .thenThrow(new NotFoundException("nginx 404"))
                .thenReturn(present).thenReturn(present)
                .thenThrow(new NotFoundException("nginx 404"))
                .thenReturn(present).thenReturn(present)
                .thenThrow(new NotFoundException("nginx 404"))
                .thenReturn(gone);

        assertThat(underTest.waitForEnvironmentCleanup(environmentClient, "envA", RunDeadline.none())).isEqualTo(WaitResult.SUCCESSFUL);
    }

    @Test
    void waitForEnvironmentCleanupReturnsTimeoutWhenDeadlineExceeded() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        ReflectionTestUtils.setField(underTest, "maxWaitMinutes", 0L);
        SimpleEnvironmentResponses present = new SimpleEnvironmentResponses();
        present.setResponses(List.of(env("envA", EnvironmentStatus.DELETE_INITIATED)));
        when(environmentEndpoint.list(null)).thenReturn(present);

        assertThat(underTest.waitForEnvironmentCleanup(environmentClient, "envA", RunDeadline.none())).isEqualTo(WaitResult.TIMEOUT);
        // only the post-loop DELETE_FAILED and availability checks ran
        verify(environmentEndpoint, times(2)).list(null);
    }

    @Test
    void waitForEnvironmentCleanupReturnsTimeoutWhenRunDeadlineExceeded() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        SimpleEnvironmentResponses present = new SimpleEnvironmentResponses();
        present.setResponses(List.of(env("envA", EnvironmentStatus.DELETE_INITIATED)));
        when(environmentEndpoint.list(null)).thenReturn(present);

        // maxWaitMinutes (120) still allows waiting, but the run deadline has already passed
        RunDeadline runDeadline = expiredRunDeadline();
        assertThat(underTest.waitForEnvironmentCleanup(environmentClient, "envA", runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isTrue();
        // only the post-loop DELETE_FAILED and availability checks ran
        verify(environmentEndpoint, times(2)).list(null);
    }

    @Test
    void waitForSdxCleanupReturnsTimeoutWhenRunDeadlineExceeded() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        SdxClusterResponse sdx = new SdxClusterResponse();
        sdx.setStatus(SdxClusterStatusResponse.STACK_DELETION_IN_PROGRESS);
        when(sdxEndpoint.get("sdxA")).thenReturn(sdx);

        RunDeadline runDeadline = expiredRunDeadline();
        assertThat(underTest.waitForSdxCleanup(sdxClient, "sdxA", runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isTrue();
        verify(sdxEndpoint, times(2)).get("sdxA");
    }

    @Test
    void waitForEnvironmentsCleanupReturnsTimeoutWhenRunDeadlineExceeded() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        SimpleEnvironmentResponses present = new SimpleEnvironmentResponses();
        present.setResponses(List.of(env("envA", EnvironmentStatus.DELETE_INITIATED)));
        when(environmentEndpoint.list(null)).thenReturn(present);

        RunDeadline runDeadline = expiredRunDeadline();
        assertThat(underTest.waitForEnvironmentsCleanup(environmentClient, runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isTrue();
        verify(environmentEndpoint, times(2)).list(null);
    }

    // ---------- wrapper wiring: call counts ----------
    // Call counts follow the poll order. Per loop iteration: availability, then (if the error limit is not reached) DELETE_FAILED.
    // After the loop: error-limit check (no call), DELETE_FAILED, availability.
    //   gone:            availability (loop ends), DELETE_FAILED, availability                      -> 3 calls
    //   DELETE_FAILED:   availability, DELETE_FAILED (loop ends), DELETE_FAILED                     -> 3 calls
    //   error limit 3:   2 x (availability + DELETE_FAILED), availability (limit reached), return   -> 5 calls
    //   run deadline:    loop skipped, DELETE_FAILED, availability                                  -> 2 calls
    // The plural wrappers additionally list the environments once before polling.

    // ---------- waitForDistroxCleanup ----------

    @Test
    void waitForDistroxCleanupIsSuccessfulWhenGone() {
        setLimits(1000, 3);
        when(distroXEndpoint.getByName("distroA", Collections.emptySet())).thenThrow(new NotFoundException("gone"));

        assertThat(underTest.waitForDistroxCleanup(cloudbreakClient, "distroA", RunDeadline.none())).isEqualTo(WaitResult.SUCCESSFUL);
        verify(distroXEndpoint, times(3)).getByName("distroA", Collections.emptySet());
    }

    @Test
    void waitForDistroxCleanupFailsWhenDeleteFailed() {
        setLimits(1000, 3);
        StackV4Response response = new StackV4Response();
        response.setStatus(Status.DELETE_FAILED);
        when(distroXEndpoint.getByName("distroA", Collections.emptySet())).thenReturn(response);

        assertThat(underTest.waitForDistroxCleanup(cloudbreakClient, "distroA", RunDeadline.none())).isEqualTo(WaitResult.FAILED);
        verify(distroXEndpoint, times(3)).getByName("distroA", Collections.emptySet());
    }

    @Test
    void waitForDistroxCleanupStopsAfterConsecutiveErrorLimit() {
        setLimits(1000, 3);
        // a 404 means "gone" for a distrox, so use another failure
        when(distroXEndpoint.getByName("distroA", Collections.emptySet())).thenThrow(new ProcessingException("boom"));

        RunDeadline runDeadline = RunDeadline.none();
        assertThat(underTest.waitForDistroxCleanup(cloudbreakClient, "distroA", runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isFalse();
        verify(distroXEndpoint, times(5)).getByName("distroA", Collections.emptySet());
    }

    @Test
    void waitForDistroxCleanupReturnsTimeoutWhenRunDeadlineExceeded() {
        setLimits(1000, 3);
        StackV4Response response = new StackV4Response();
        response.setStatus(Status.DELETE_IN_PROGRESS);
        when(distroXEndpoint.getByName("distroA", Collections.emptySet())).thenReturn(response);

        RunDeadline runDeadline = expiredRunDeadline();
        assertThat(underTest.waitForDistroxCleanup(cloudbreakClient, "distroA", runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isTrue();
        verify(distroXEndpoint, times(2)).getByName("distroA", Collections.emptySet());
    }

    // ---------- waitForDistroxesCleanup ----------

    @Test
    void waitForDistroxesCleanupIsSuccessfulWhenGone() {
        setLimits(1000, 3);
        stubSingleEnvironment();
        when(distroXEndpoint.list("envA", "envA-crn")).thenReturn(new StackViewV4Responses());

        assertThat(underTest.waitForDistroxesCleanup(cloudbreakClient, environmentClient, RunDeadline.none())).isEqualTo(WaitResult.SUCCESSFUL);
        verify(environmentEndpoint, times(1)).list(null);
        verify(distroXEndpoint, times(3)).list("envA", "envA-crn");
    }

    @Test
    void waitForDistroxesCleanupFailsWhenDeleteFailed() {
        setLimits(1000, 3);
        stubSingleEnvironment();
        when(distroXEndpoint.list("envA", "envA-crn")).thenReturn(stackViews(Status.DELETE_FAILED));

        assertThat(underTest.waitForDistroxesCleanup(cloudbreakClient, environmentClient, RunDeadline.none())).isEqualTo(WaitResult.FAILED);
        verify(environmentEndpoint, times(1)).list(null);
        verify(distroXEndpoint, times(3)).list("envA", "envA-crn");
    }

    @Test
    void waitForDistroxesCleanupStopsAfterConsecutiveErrorLimit() {
        setLimits(1000, 3);
        stubSingleEnvironment();
        when(distroXEndpoint.list("envA", "envA-crn")).thenThrow(new ProcessingException("boom"));

        RunDeadline runDeadline = RunDeadline.none();
        assertThat(underTest.waitForDistroxesCleanup(cloudbreakClient, environmentClient, runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isFalse();
        verify(environmentEndpoint, times(1)).list(null);
        verify(distroXEndpoint, times(5)).list("envA", "envA-crn");
    }

    @Test
    void waitForDistroxesCleanupReturnsTimeoutWhenRunDeadlineExceeded() {
        setLimits(1000, 3);
        stubSingleEnvironment();
        when(distroXEndpoint.list("envA", "envA-crn")).thenReturn(stackViews(Status.DELETE_IN_PROGRESS));

        RunDeadline runDeadline = expiredRunDeadline();
        assertThat(underTest.waitForDistroxesCleanup(cloudbreakClient, environmentClient, runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isTrue();
        verify(environmentEndpoint, times(1)).list(null);
        verify(distroXEndpoint, times(2)).list("envA", "envA-crn");
    }

    // ---------- waitForSdxCleanup ----------

    @Test
    void waitForSdxCleanupIsSuccessfulWhenGone() {
        setLimits(1000, 3);
        when(sdxEndpoint.get("sdxA")).thenThrow(new NotFoundException("gone"));

        assertThat(underTest.waitForSdxCleanup(sdxClient, "sdxA", RunDeadline.none())).isEqualTo(WaitResult.SUCCESSFUL);
        verify(sdxEndpoint, times(3)).get("sdxA");
    }

    @Test
    void waitForSdxCleanupFailsWhenDeleteFailed() {
        setLimits(1000, 3);
        when(sdxEndpoint.get("sdxA")).thenReturn(sdxResponse(SdxClusterStatusResponse.DELETE_FAILED));

        assertThat(underTest.waitForSdxCleanup(sdxClient, "sdxA", RunDeadline.none())).isEqualTo(WaitResult.FAILED);
        verify(sdxEndpoint, times(3)).get("sdxA");
    }

    @Test
    void waitForSdxCleanupStopsAfterConsecutiveErrorLimit() {
        setLimits(1000, 3);
        // a 404 means "gone" for a data lake, so use another failure
        when(sdxEndpoint.get("sdxA")).thenThrow(new ProcessingException("boom"));

        RunDeadline runDeadline = RunDeadline.none();
        assertThat(underTest.waitForSdxCleanup(sdxClient, "sdxA", runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isFalse();
        verify(sdxEndpoint, times(5)).get("sdxA");
    }

    // ---------- waitForSdxesCleanup ----------

    @Test
    void waitForSdxesCleanupIsSuccessfulWhenGone() {
        setLimits(1000, 3);
        stubSingleEnvironment();
        when(sdxEndpoint.list("envA", false)).thenReturn(List.of());

        assertThat(underTest.waitForSdxesCleanup(sdxClient, environmentClient, RunDeadline.none())).isEqualTo(WaitResult.SUCCESSFUL);
        verify(environmentEndpoint, times(1)).list(null);
        verify(sdxEndpoint, times(3)).list("envA", false);
    }

    @Test
    void waitForSdxesCleanupFailsWhenDeleteFailed() {
        setLimits(1000, 3);
        stubSingleEnvironment();
        when(sdxEndpoint.list("envA", false)).thenReturn(List.of(sdxResponse(SdxClusterStatusResponse.DELETE_FAILED)));

        assertThat(underTest.waitForSdxesCleanup(sdxClient, environmentClient, RunDeadline.none())).isEqualTo(WaitResult.FAILED);
        verify(environmentEndpoint, times(1)).list(null);
        verify(sdxEndpoint, times(3)).list("envA", false);
    }

    @Test
    void waitForSdxesCleanupStopsAfterConsecutiveErrorLimit() {
        setLimits(1000, 3);
        stubSingleEnvironment();
        when(sdxEndpoint.list("envA", false)).thenThrow(new ProcessingException("boom"));

        RunDeadline runDeadline = RunDeadline.none();
        assertThat(underTest.waitForSdxesCleanup(sdxClient, environmentClient, runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isFalse();
        verify(environmentEndpoint, times(1)).list(null);
        verify(sdxEndpoint, times(5)).list("envA", false);
    }

    @Test
    void waitForSdxesCleanupReturnsTimeoutWhenRunDeadlineExceeded() {
        setLimits(1000, 3);
        stubSingleEnvironment();
        when(sdxEndpoint.list("envA", false)).thenReturn(List.of(sdxResponse(SdxClusterStatusResponse.STACK_DELETION_IN_PROGRESS)));

        RunDeadline runDeadline = expiredRunDeadline();
        assertThat(underTest.waitForSdxesCleanup(sdxClient, environmentClient, runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isTrue();
        verify(environmentEndpoint, times(1)).list(null);
        verify(sdxEndpoint, times(2)).list("envA", false);
    }

    // ---------- waitForEnvironmentsCleanup ----------

    @Test
    void waitForEnvironmentsCleanupIsSuccessfulWhenGone() {
        setLimits(1000, 3);
        SimpleEnvironmentResponses gone = new SimpleEnvironmentResponses();
        gone.setResponses(List.of());
        when(environmentEndpoint.list(null)).thenReturn(gone);

        assertThat(underTest.waitForEnvironmentsCleanup(environmentClient, RunDeadline.none())).isEqualTo(WaitResult.SUCCESSFUL);
        verify(environmentEndpoint, times(3)).list(null);
    }

    @Test
    void waitForEnvironmentsCleanupFailsWhenDeleteFailed() {
        setLimits(1000, 3);
        SimpleEnvironmentResponses failed = new SimpleEnvironmentResponses();
        failed.setResponses(List.of(env("envA", EnvironmentStatus.DELETE_FAILED)));
        when(environmentEndpoint.list(null)).thenReturn(failed);

        assertThat(underTest.waitForEnvironmentsCleanup(environmentClient, RunDeadline.none())).isEqualTo(WaitResult.FAILED);
        verify(environmentEndpoint, times(3)).list(null);
    }

    @Test
    void waitForEnvironmentsCleanupStopsAfterConsecutiveErrorLimit() {
        setLimits(1000, 3);
        when(environmentEndpoint.list(null)).thenThrow(new ProcessingException("boom"));

        RunDeadline runDeadline = RunDeadline.none();
        assertThat(underTest.waitForEnvironmentsCleanup(environmentClient, runDeadline)).isEqualTo(WaitResult.TIMEOUT);
        assertThat(runDeadline.wasCutShort()).isFalse();
        verify(environmentEndpoint, times(5)).list(null);
    }

    // ---------- waitForEnvironmentCleanup ----------

    @Test
    void waitForEnvironmentCleanupIsSuccessfulWhenGone() {
        setLimits(1000, 3);
        SimpleEnvironmentResponses gone = new SimpleEnvironmentResponses();
        gone.setResponses(List.of());
        when(environmentEndpoint.list(null)).thenReturn(gone);

        assertThat(underTest.waitForEnvironmentCleanup(environmentClient, "envA", RunDeadline.none())).isEqualTo(WaitResult.SUCCESSFUL);
        verify(environmentEndpoint, times(3)).list(null);
    }

    @Test
    void waitForEnvironmentCleanupFailsWhenDeleteFailed() {
        setLimits(1000, 3);
        SimpleEnvironmentResponses failed = new SimpleEnvironmentResponses();
        failed.setResponses(List.of(env("envA", EnvironmentStatus.DELETE_FAILED)));
        when(environmentEndpoint.list(null)).thenReturn(failed);

        assertThat(underTest.waitForEnvironmentCleanup(environmentClient, "envA", RunDeadline.none())).isEqualTo(WaitResult.FAILED);
        verify(environmentEndpoint, times(3)).list(null);
    }

    // ---------- poll ----------

    @Test
    void pollStopsOnMaxRetry() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 3);
        AtomicInteger availableCalls = new AtomicInteger();
        AtomicInteger deleteFailedCalls = new AtomicInteger();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing",
                errors -> availableCalls.incrementAndGet() > 0, () -> deleteFailedCalls.incrementAndGet() < 0, RunDeadline.none());

        assertThat(outcome.result()).isEqualTo(WaitResult.TIMEOUT);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.MAX_RETRY);
        // 3 loop iterations plus the final checks
        assertThat(availableCalls).hasValue(4);
        assertThat(deleteFailedCalls).hasValue(4);
    }

    @Test
    void pollStopsOnWaitDeadline() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        ReflectionTestUtils.setField(underTest, "maxWaitMinutes", 0L);
        AtomicInteger availableCalls = new AtomicInteger();
        AtomicInteger deleteFailedCalls = new AtomicInteger();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing",
                errors -> availableCalls.incrementAndGet() > 0, () -> deleteFailedCalls.incrementAndGet() < 0, RunDeadline.none());

        assertThat(outcome.result()).isEqualTo(WaitResult.TIMEOUT);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.WAIT_DEADLINE);
        // loop skipped, only the final checks ran
        assertThat(availableCalls).hasValue(1);
        assertThat(deleteFailedCalls).hasValue(1);
    }

    @Test
    void pollStopsOnRunDeadline() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        AtomicInteger availableCalls = new AtomicInteger();
        AtomicInteger deleteFailedCalls = new AtomicInteger();

        RunDeadline runDeadline = expiredRunDeadline();
        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing",
                errors -> availableCalls.incrementAndGet() > 0, () -> deleteFailedCalls.incrementAndGet() < 0, runDeadline);

        assertThat(outcome.result()).isEqualTo(WaitResult.TIMEOUT);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.RUN_DEADLINE);
        assertThat(runDeadline.wasCutShort()).isTrue();
        assertThat(availableCalls).hasValue(1);
        assertThat(deleteFailedCalls).hasValue(1);
    }

    @Test
    void pollStopsOnErrorLimit() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", 2);
        AtomicInteger availableCalls = new AtomicInteger();
        AtomicInteger deleteFailedCalls = new AtomicInteger();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing", errors -> {
            availableCalls.incrementAndGet();
            errors.increment();
            return true;
        }, () -> deleteFailedCalls.incrementAndGet() < 0, RunDeadline.none());

        assertThat(outcome.result()).isEqualTo(WaitResult.TIMEOUT);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.ERROR_LIMIT);
        // the limit is hit on the second availability check, before the DELETE_FAILED check of that iteration;
        // the error-limit exit returns without any final check
        assertThat(availableCalls).hasValue(2);
        assertThat(deleteFailedCalls).hasValue(1);
    }

    @Test
    void consecutiveErrorsLimitBelowOneIsTreatedAsOne() {
        for (int limit : new int[]{0, -5}) {
            CleanupWaitUtil.ConsecutiveErrors errors = new CleanupWaitUtil.ConsecutiveErrors(limit);

            assertThat(errors.limitReached()).as("limit %s before any error", limit).isFalse();
            errors.increment();
            assertThat(errors.limitReached()).as("limit %s after one error", limit).isTrue();
            errors.reset();
            assertThat(errors.limitReached()).as("limit %s after reset", limit).isFalse();
        }
    }

    @Test
    void pollIsSuccessfulWhenResourceIsGoneAndMaxConsecutiveErrorsIsZero() {
        ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", 0);
        AtomicInteger availableCalls = new AtomicInteger();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing",
                errors -> availableCalls.incrementAndGet() < 0, () -> false, RunDeadline.none());

        assertThat(outcome.result()).isEqualTo(WaitResult.SUCCESSFUL);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.GONE);
        assertThat(availableCalls).hasValue(2);
    }

    @Test
    void pollStopsOnFirstErrorWhenMaxConsecutiveErrorsIsZero() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", 0);
        AtomicInteger availableCalls = new AtomicInteger();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing", errors -> {
            availableCalls.incrementAndGet();
            errors.increment();
            return true;
        }, () -> false, RunDeadline.none());

        assertThat(outcome.result()).isEqualTo(WaitResult.TIMEOUT);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.ERROR_LIMIT);
        assertThat(availableCalls).hasValue(1);
    }

    @Test
    void validateConfigurationRaisesMaxConsecutiveErrorsBelowOneToOne() {
        for (int configured : new int[]{0, -5}) {
            ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", configured);

            ReflectionTestUtils.invokeMethod(underTest, "validateConfiguration");

            assertThat(ReflectionTestUtils.getField(underTest, "maxConsecutiveErrors")).as("configured %s", configured).isEqualTo(1);
        }
    }

    @Test
    void validateConfigurationKeepsValidMaxConsecutiveErrors() {
        ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", 7);

        ReflectionTestUtils.invokeMethod(underTest, "validateConfiguration");

        assertThat(ReflectionTestUtils.getField(underTest, "maxConsecutiveErrors")).isEqualTo(7);
    }

    @Test
    void validateConfigurationRaisesNegativeDeleteSleepToZero() {
        ReflectionTestUtils.setField(underTest, "deleteSleep", -1L);

        ReflectionTestUtils.invokeMethod(underTest, "validateConfiguration");

        assertThat(ReflectionTestUtils.getField(underTest, "deleteSleep")).isEqualTo(0L);
    }

    @Test
    void validateConfigurationKeepsNonNegativeDeleteSleep() {
        for (long configured : new long[]{0L, 1234L}) {
            ReflectionTestUtils.setField(underTest, "deleteSleep", configured);

            ReflectionTestUtils.invokeMethod(underTest, "validateConfiguration");

            assertThat(ReflectionTestUtils.getField(underTest, "deleteSleep")).as("configured %s", configured).isEqualTo(configured);
        }
    }

    @Test
    void deleteSleepIsBoundFromProperty() {
        try (AnnotationConfigApplicationContext context = contextWith(Map.of("integrationtest.cleanup.deleteSleep", "1234"))) {
            assertThat(ReflectionTestUtils.getField(context.getBean(CleanupWaitUtil.class), "deleteSleep")).isEqualTo(1234L);
        }
    }

    @Test
    void deleteSleepDefaultsToThirtySecondsWithoutProperty() {
        try (AnnotationConfigApplicationContext context = contextWith(Map.of())) {
            assertThat(ReflectionTestUtils.getField(context.getBean(CleanupWaitUtil.class), "deleteSleep")).isEqualTo(30000L);
        }
    }

    // Binds only the @Value properties onto a CleanupWaitUtil bean; no clients are created and nothing goes over the network.
    private AnnotationConfigApplicationContext contextWith(Map<String, Object> properties) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.register(PropertySourcesPlaceholderConfigurer.class, CleanupWaitUtil.class);
        context.refresh();
        return context;
    }

    @Test
    void pollReturnsFailedWhenDeleteFailed() {
        AtomicInteger availableCalls = new AtomicInteger();
        AtomicInteger deleteFailedCalls = new AtomicInteger();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing",
                errors -> availableCalls.incrementAndGet() > 0, () -> deleteFailedCalls.incrementAndGet() > 0, RunDeadline.none());

        assertThat(outcome.result()).isEqualTo(WaitResult.FAILED);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.DELETE_FAILED);
        assertThat(availableCalls).hasValue(1);
        // once in the loop and once in the final check
        assertThat(deleteFailedCalls).hasValue(2);
    }

    @Test
    void pollReturnsSuccessfulWhenResourceIsGone() {
        AtomicInteger availableCalls = new AtomicInteger();
        AtomicInteger deleteFailedCalls = new AtomicInteger();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing",
                errors -> availableCalls.incrementAndGet() < 0, () -> deleteFailedCalls.incrementAndGet() < 0, RunDeadline.none());

        assertThat(outcome.result()).isEqualTo(WaitResult.SUCCESSFUL);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.GONE);
        // once in the loop and once in the final check
        assertThat(availableCalls).hasValue(2);
        assertThat(deleteFailedCalls).hasValue(1);
    }

    @Test
    void pollReturnsSuccessfulAfterPollingWhenResourceDisappears() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        AtomicInteger availableCalls = new AtomicInteger();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing",
                errors -> availableCalls.incrementAndGet() <= 2, () -> false, RunDeadline.none());

        assertThat(outcome.result()).isEqualTo(WaitResult.SUCCESSFUL);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.GONE);
        assertThat(availableCalls).hasValue(4);
    }

    @Test
    void pollDeadlinePassedButResourceGoneOnFinalCheckIsSuccessfulNotDeadlineCut() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        AtomicInteger availableCalls = new AtomicInteger();

        RunDeadline runDeadline = expiredRunDeadline();
        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing",
                errors -> availableCalls.incrementAndGet() < 0, () -> false, runDeadline);

        assertThat(outcome.result()).isEqualTo(WaitResult.SUCCESSFUL);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.GONE);
        assertThat(runDeadline.wasCutShort()).isFalse();
        // loop skipped, only the final checks ran
        assertThat(availableCalls).hasValue(1);
    }

    @Test
    void pollWaitDeadlineDoesNotMarkTheRunDeadlineCutShort() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        ReflectionTestUtils.setField(underTest, "maxWaitMinutes", 0L);
        RunDeadline runDeadline = RunDeadline.none();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing", errors -> true, () -> false, runDeadline);

        assertThat(outcome.result()).isEqualTo(WaitResult.TIMEOUT);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.WAIT_DEADLINE);
        assertThat(runDeadline.wasCutShort()).isFalse();
    }

    @Test
    void pollMaxRetryDoesNotMarkTheRunDeadlineCutShort() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 2);
        RunDeadline runDeadline = RunDeadline.none();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing", errors -> true, () -> false, runDeadline);

        assertThat(outcome.result()).isEqualTo(WaitResult.TIMEOUT);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.MAX_RETRY);
        assertThat(runDeadline.wasCutShort()).isFalse();
    }

    @Test
    void pollErrorLimitDoesNotMarkTheRunDeadlineCutShort() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", 2);
        RunDeadline runDeadline = RunDeadline.none();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing", errors -> {
            errors.increment();
            return true;
        }, () -> false, runDeadline);

        assertThat(outcome.result()).isEqualTo(WaitResult.TIMEOUT);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.ERROR_LIMIT);
        assertThat(runDeadline.wasCutShort()).isFalse();
    }

    @Test
    void pollDeleteFailedAfterTheRunDeadlinePassedDoesNotMarkItCutShort() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        RunDeadline runDeadline = expiredRunDeadline();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing", errors -> true, () -> true, runDeadline);

        assertThat(outcome.result()).isEqualTo(WaitResult.FAILED);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.DELETE_FAILED);
        assertThat(runDeadline.wasCutShort()).isFalse();
    }

    @Test
    void pollSuccessBeforeTheRunDeadlineDoesNotMarkItCutShort() {
        RunDeadline runDeadline = RunDeadline.none();

        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing", errors -> false, () -> false, runDeadline);

        assertThat(outcome.result()).isEqualTo(WaitResult.SUCCESSFUL);
        assertThat(runDeadline.wasCutShort()).isFalse();
    }

    @Test
    void pollTimeoutAfterLoopSawResourceGoneIsNotReportedAsDeadline() {
        ReflectionTestUtils.setField(underTest, "maxRetry", 1000);
        AtomicInteger availableCalls = new AtomicInteger();

        // gone in the loop, but present again in the final check
        CleanupWaitUtil.PollOutcome outcome = underTest.poll("thing",
                errors -> availableCalls.incrementAndGet() > 1, () -> false, RunDeadline.none());

        assertThat(outcome.result()).isEqualTo(WaitResult.TIMEOUT);
        assertThat(outcome.stopReason()).isEqualTo(CleanupWaitUtil.StopReason.GONE);
    }

    // ---------- helpers ----------

    private static RunDeadline expiredRunDeadline() {
        return new RunDeadline(System.currentTimeMillis() - 1);
    }

    private static SimpleEnvironmentResponse env(String name, EnvironmentStatus status) {
        SimpleEnvironmentResponse r = new SimpleEnvironmentResponse();
        r.setName(name);
        r.setEnvironmentStatus(status);
        return r;
    }

    private static SimpleEnvironmentResponse env(String name, String crn, EnvironmentStatus status) {
        SimpleEnvironmentResponse r = env(name, status);
        r.setCrn(crn);
        return r;
    }

    // The plural wrappers list the environments once, before polling, to build the CRN -> name map.
    private void stubSingleEnvironment() {
        SimpleEnvironmentResponses envs = new SimpleEnvironmentResponses();
        envs.setResponses(List.of(env("envA", "envA-crn", EnvironmentStatus.AVAILABLE)));
        when(environmentEndpoint.list(null)).thenReturn(envs);
    }

    private void setLimits(int maxRetry, int maxConsecutiveErrors) {
        ReflectionTestUtils.setField(underTest, "maxRetry", maxRetry);
        ReflectionTestUtils.setField(underTest, "maxConsecutiveErrors", maxConsecutiveErrors);
    }

    private static StackViewV4Responses stackViews(Status status) {
        StackViewV4Response view = new StackViewV4Response();
        view.setName("distroA");
        view.setStatus(status);
        return new StackViewV4Responses(Set.of(view));
    }

    private static SdxClusterResponse sdxResponse(SdxClusterStatusResponse status) {
        SdxClusterResponse response = new SdxClusterResponse();
        response.setName("sdxA");
        response.setStatus(status);
        return response;
    }

    private Object invoke(String method, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = CleanupWaitUtil.class.getDeclaredMethod(method, paramTypes);
        m.setAccessible(true);
        try {
            return m.invoke(underTest, args);
        } catch (InvocationTargetException ite) {
            if (ite.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw ite;
        }
    }
}
