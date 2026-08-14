package com.sequenceiq.it.util.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.client.SdxClient;

public class CleanupWaitUtilTest {

    private CloudbreakClient cloudbreakClient;

    private SdxClient sdxClient;

    private EnvironmentClient environmentClient;

    private DistroXV1Endpoint distroXEndpoint;

    private SdxEndpoint sdxEndpoint;

    private EnvironmentEndpoint environmentEndpoint;

    private CleanupWaitUtil underTest;

    @BeforeMethod
    public void setUp() {
        cloudbreakClient = mock(CloudbreakClient.class);
        sdxClient = mock(SdxClient.class);
        environmentClient = mock(EnvironmentClient.class);
        distroXEndpoint = mock(DistroXV1Endpoint.class);
        sdxEndpoint = mock(SdxEndpoint.class);
        environmentEndpoint = mock(EnvironmentEndpoint.class);

        underTest = new CleanupWaitUtil();
        // Keep the poll loops (not exercised here) from being slow if they ever run.
        ReflectionTestUtils.setField(underTest, "pollingInterval", 1L);
        ReflectionTestUtils.setField(underTest, "maxRetry", 1);

        lenient().when(cloudbreakClient.distroXV1Endpoint()).thenReturn(distroXEndpoint);
        lenient().when(sdxClient.sdxEndpoint()).thenReturn(sdxEndpoint);
        lenient().when(environmentClient.environmentV1Endpoint()).thenReturn(environmentEndpoint);
    }

    // ---------- checkDistroxIsAvailable ----------

    @Test
    void checkDistroxIsAvailableNotFoundReturnsFalse() throws Exception {
        when(distroXEndpoint.getByName(any(), anySet())).thenThrow(new NotFoundException("gone"));
        assertThat((boolean) invoke("checkDistroxIsAvailable",
                new Class<?>[]{CloudbreakClient.class, String.class},
                cloudbreakClient, "distroA")).isFalse();
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
                new Class<?>[]{CloudbreakClient.class, String.class},
                cloudbreakClient, "distroA")).isTrue();
    }

    // ---------- checkSdxIsAvailable ----------

    @Test
    void checkSdxIsAvailableNotFoundReturnsFalse() throws Exception {
        when(sdxEndpoint.get("sdxA")).thenThrow(new NotFoundException("gone"));
        assertThat((boolean) invoke("checkSdxIsAvailable",
                new Class<?>[]{SdxClient.class, String.class},
                sdxClient, "sdxA")).isFalse();
    }

    @Test
    void checkSdxIsAvailableTransientExceptionReturnsTrue() throws Exception {
        when(sdxEndpoint.get("sdxA")).thenThrow(new ProcessingException("boom"));
        assertThat((boolean) invoke("checkSdxIsAvailable",
                new Class<?>[]{SdxClient.class, String.class},
                sdxClient, "sdxA")).isTrue();
    }

    // ---------- checkEnvironmentIsAvailable ----------

    @Test
    void checkEnvironmentIsAvailableTransientExceptionReturnsTrue() throws Exception {
        when(environmentEndpoint.list(null)).thenThrow(new ProcessingException("boom"));
        assertThat((boolean) invoke("checkEnvironmentIsAvailable",
                new Class<?>[]{EnvironmentClient.class, String.class},
                environmentClient, "envA")).isTrue();
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
                new Class<?>[]{CloudbreakClient.class, Map.class},
                cloudbreakClient, Map.of("envA-crn", "envA"));
        assertThat(result).isTrue();
    }

    // ---------- checkSdxesAreAvailable (bulk) ----------

    @Test
    void checkSdxesAreAvailableTransientExceptionReturnsTrue() throws Exception {
        when(sdxEndpoint.list(eq("envA"), anyBoolean())).thenThrow(new ProcessingException("boom"));
        boolean result = (boolean) invoke("checkSdxesAreAvailable",
                new Class<?>[]{SdxClient.class, Map.class},
                sdxClient, Map.of("envA-crn", "envA"));
        assertThat(result).isTrue();
    }

    // ---------- checkEnvironmentsAreAvailable (bulk) ----------

    @Test
    void checkEnvironmentsAreAvailableTransientExceptionReturnsTrue() throws Exception {
        when(environmentEndpoint.list(null)).thenThrow(new ProcessingException("boom"));
        boolean result = (boolean) invoke("checkEnvironmentsAreAvailable",
                new Class<?>[]{EnvironmentClient.class},
                environmentClient);
        assertThat(result).isTrue();
    }

    // ---------- helpers ----------

    private static SimpleEnvironmentResponse env(String name, EnvironmentStatus status) {
        SimpleEnvironmentResponse r = new SimpleEnvironmentResponse();
        r.setName(name);
        r.setEnvironmentStatus(status);
        return r;
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
