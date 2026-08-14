package com.sequenceiq.it.util.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.client.SdxClient;

public class CleanupUtilTest {

    private EnvironmentClient environmentClient;

    private CloudbreakClient cloudbreakClient;

    private SdxClient sdxClient;

    private EnvironmentEndpoint environmentEndpoint;

    private CredentialEndpoint credentialEndpoint;

    private DistroXV1Endpoint distroXEndpoint;

    private SdxEndpoint sdxEndpoint;

    private CleanupWaitUtil waitUtil;

    private Path outputDir;

    private CleanupUtil underTest;

    @BeforeMethod
    public void setUp() throws IOException {
        environmentClient = mock(EnvironmentClient.class);
        cloudbreakClient = mock(CloudbreakClient.class);
        sdxClient = mock(SdxClient.class);
        environmentEndpoint = mock(EnvironmentEndpoint.class);
        credentialEndpoint = mock(CredentialEndpoint.class);
        distroXEndpoint = mock(DistroXV1Endpoint.class);
        sdxEndpoint = mock(SdxEndpoint.class);
        waitUtil = mock(CleanupWaitUtil.class);

        outputDir = Files.createTempDirectory("cleanup-util-test");

        // Spy the SUT so cleanupXxx() paths that call createEnvironmentClient()/createCloudbreakClient()/createSdxClient()
        // pick up our mocks instead of building real HTTP clients.
        underTest = spy(new CleanupUtil());
        underTest.setEnvironmentClient(environmentClient);
        underTest.setCloudbreakClient(cloudbreakClient);
        underTest.setSdxClient(sdxClient);

        ReflectionTestUtils.setField(underTest, "waitUtil", waitUtil);
        ReflectionTestUtils.setField(underTest, "outputDirectory", outputDir.toString());
        ReflectionTestUtils.setField(underTest, "cleanupAfterAbort", false);

        doReturn(environmentClient).when(underTest).createEnvironmentClient();
        lenient().doReturn(cloudbreakClient).when(underTest).createCloudbreakClient();
        lenient().doReturn(sdxClient).when(underTest).createSdxClient();

        // Endpoint wiring — lenient because not every test hits every endpoint.
        lenient().when(environmentClient.environmentV1Endpoint()).thenReturn(environmentEndpoint);
        lenient().when(environmentClient.credentialV1Endpoint()).thenReturn(credentialEndpoint);
        lenient().when(cloudbreakClient.distroXV1Endpoint()).thenReturn(distroXEndpoint);
        lenient().when(sdxClient.sdxEndpoint()).thenReturn(sdxEndpoint);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (outputDir != null && Files.exists(outputDir)) {
            try (var stream = Files.walk(outputDir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    /**
     * Happy path: one distrox listed in resource_names.json, delete succeeds, re-list returns empty.
     * Expects no exception.
     */
    @Test
    void deleteResourcesAllDeletesSucceedNoException() throws Exception {
        writeResourceFile("resource_names_1.json", "{\"distroxName\": [\"distroA\"]}");

        lenient().when(waitUtil.waitForDistroxCleanup(cloudbreakClient, "distroA")).thenReturn(WaitResult.SUCCESSFUL);
        // Re-list: no environments → getDistroxes() sees no distroxes → no leftovers.
        when(environmentEndpoint.list(null)).thenReturn(emptyEnvResponses());

        CleanupReport report = new CleanupReport();
        assertThatCode(() -> invokeDeleteResources(List.of("distroA"), "distroxName", report))
                .doesNotThrowAnyException();
        verify(distroXEndpoint, times(1)).deleteByName("distroA", true);
        assertThat(report.getDeletedByType()).containsEntry("distroxName", List.of("distroA"));
    }

    /**
     * Regression test for the original credential bug: previously the code set e2eCleanupFailed=true
     * on every credential delete and reported the accumulated deletedResources map as leftover, so
     * this scenario always threw. After the fix a clean credential delete must not throw.
     */
    @Test
    void deleteResourcesCredentialDeleteSucceedsNoException() throws Exception {
        writeResourceFile("resource_names_1.json", "{\"credentialName\": [\"credA\"]}");

        when(credentialEndpoint.list()).thenReturn(emptyCredResponses());

        assertThatCode(() -> invokeDeleteResources(List.of("credA"), "credentialName", new CleanupReport()))
                .doesNotThrowAnyException();
        verify(credentialEndpoint, times(1)).deleteByName("credA");
    }

    /**
     * Re-list still shows the resource we tried to delete → genuine leftover → exception with
     * a correct "still present: 1" count and "delete errors: 0".
     */
    @Test
    void deleteResourcesReListStillShowsResourceThrows() throws Exception {
        writeResourceFile("resource_names_1.json", "{\"credentialName\": [\"credA\"]}");

        CredentialResponses creds = new CredentialResponses();
        creds.setResponses(List.of(credentialResponse("credA")));
        when(credentialEndpoint.list()).thenReturn(creds);

        assertThatThrownBy(() -> invokeDeleteResources(List.of("credA"), "credentialName", new CleanupReport()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("still present: 1")
                .hasMessageContaining("delete errors: 0");
    }

    /**
     * A single delete failure must not abort the whole run — the second resource must still be
     * attempted, and the exception must report exactly one delete error.
     */
    @Test
    void deleteResourcesDeleteThrowsRecordsFailureAndContinues() throws Exception {
        writeResourceFile("resource_names_1.json", "{\"distroxName\": [\"distroA\", \"distroB\"]}");

        // First delete blows up → deleteDistrox rewraps as RuntimeException.
        doThrow(new RuntimeException("boom")).when(distroXEndpoint).deleteByName(eq("distroA"), anyBoolean());
        // Second delete succeeds. Wait returns SUCCESSFUL.
        lenient().when(waitUtil.waitForDistroxCleanup(cloudbreakClient, "distroB")).thenReturn(WaitResult.SUCCESSFUL);
        // Re-list: no environments so no leftovers found.
        when(environmentEndpoint.list(null)).thenReturn(emptyEnvResponses());

        assertThatThrownBy(() -> invokeDeleteResources(List.of("distroA", "distroB"), "distroxName", new CleanupReport()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("delete errors: 1");
        verify(distroXEndpoint).deleteByName("distroA", true);
        verify(distroXEndpoint).deleteByName("distroB", true);
    }

    /**
     * A resource named in the file but not present in foundResources should be silently skipped —
     * no delete call, no exception.
     */
    @Test
    void deleteResourcesResourceNotInFoundListSkipped() throws Exception {
        writeResourceFile("resource_names_1.json", "{\"credentialName\": [\"credA\", \"credB\"]}");

        lenient().when(credentialEndpoint.list()).thenReturn(emptyCredResponses());

        assertThatCode(() -> invokeDeleteResources(List.of("credA"), "credentialName", new CleanupReport()))
                .doesNotThrowAnyException();
        verify(credentialEndpoint, times(1)).deleteByName("credA");
        verify(credentialEndpoint, never()).deleteByName("credB");
    }

    /**
     * A NotFoundException while deleting means the resource is already gone — deleteCredential
     * swallows it and no exception should propagate.
     */
    @Test
    void deleteResourcesNotFoundExceptionOnDeleteTreatedAsAlreadyGone() throws Exception {
        writeResourceFile("resource_names_1.json", "{\"credentialName\": [\"credA\"]}");

        doThrow(new NotFoundException("gone")).when(credentialEndpoint).deleteByName("credA");
        when(credentialEndpoint.list()).thenReturn(emptyCredResponses());

        assertThatCode(() -> invokeDeleteResources(List.of("credA"), "credentialName", new CleanupReport()))
                .doesNotThrowAnyException();
    }

    @Test
    void getResourcesFromFileJsonArrayReturnsAllElements() throws Exception {
        Path file = writeResourceFile("resource_names_arr.json", "{\"distroxName\": [\"a\", \"b\", \"c\"]}");
        List<String> result = invokeGetResourcesFromFile("distroxName", file);
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void getResourcesFromFileJsonScalarReturnsSingleton() throws Exception {
        Path file = writeResourceFile("resource_names_scalar.json", "{\"distroxName\": \"solo\"}");
        List<String> result = invokeGetResourcesFromFile("distroxName", file);
        assertThat(result).containsExactly("solo");
    }

    @Test
    void getResourcesFromFileKeyMissingReturnsEmpty() throws Exception {
        Path file = writeResourceFile("resource_names_other.json", "{\"sdxName\": \"x\"}");
        List<String> result = invokeGetResourcesFromFile("distroxName", file);
        assertThat(result).isEmpty();
    }

    /**
     * If nothing was attempted, findLeftoverResources must not hit any API and must return an empty list.
     */
    @Test
    void findLeftoverResourcesEmptyAttemptedReturnsEmpty() throws Exception {
        List<String> result = invokeFindLeftoverResources("distroxName", List.of());
        assertThat(result).isEmpty();
        verify(environmentEndpoint, never()).list(any());
    }

    /**
     * If the re-list fails with a transient exception, we conservatively treat every attempted resource
     * as still present rather than silently declaring success on a network/auth glitch.
     */
    @Test
    void findLeftoverResourcesReListThrowsReturnsAllAttempted() throws Exception {
        when(credentialEndpoint.list()).thenThrow(new ProcessingException("connect timed out"));

        List<String> result = invokeFindLeftoverResources("credentialName", List.of("credA", "credB"));

        assertThat(result).containsExactlyInAnyOrder("credA", "credB");
    }

    /**
     * A successful deleteResources call must record the resource in the per-run "deletedByType"
     * accumulator so the end-of-run summary can list it.
     */
    @Test
    void deleteResourcesRecordsSuccessInAccumulator() throws Exception {
        writeResourceFile("resource_names_1.json", "{\"credentialName\": [\"credA\"]}");
        when(credentialEndpoint.list()).thenReturn(emptyCredResponses());

        CleanupReport report = new CleanupReport();
        invokeDeleteResources(List.of("credA"), "credentialName", report);

        assertThat(report.getDeletedByType()).containsEntry("credentialName", List.of("credA"));
    }

    /**
     * A leftover (re-list still shows the resource) must be recorded in "leftoversByType", not in
     * "deletedByType" — the summary needs to show it under "Still present after delete".
     */
    @Test
    void deleteResourcesRecordsLeftoverInAccumulator() throws Exception {
        writeResourceFile("resource_names_1.json", "{\"credentialName\": [\"credA\"]}");
        CredentialResponses creds = new CredentialResponses();
        creds.setResponses(List.of(credentialResponse("credA")));
        when(credentialEndpoint.list()).thenReturn(creds);

        CleanupReport report = new CleanupReport();
        assertThatThrownBy(() -> invokeDeleteResources(List.of("credA"), "credentialName", report))
                .isInstanceOf(RuntimeException.class);

        assertThat(report.getDeletedByType()).doesNotContainKey("credentialName");
        assertThat(report.getLeftoversByType()).containsEntry("credentialName", List.of("credA"));
    }

    /**
     * A delete error must be recorded in "deleteErrorsByType" and NOT counted as a success.
     */
    @Test
    void deleteResourcesRecordsDeleteErrorInAccumulator() throws Exception {
        writeResourceFile("resource_names_1.json", "{\"distroxName\": [\"distroA\"]}");
        doThrow(new RuntimeException("boom")).when(distroXEndpoint).deleteByName(eq("distroA"), anyBoolean());
        when(environmentEndpoint.list(null)).thenReturn(emptyEnvResponses());

        CleanupReport report = new CleanupReport();
        assertThatThrownBy(() -> invokeDeleteResources(List.of("distroA"), "distroxName", report))
                .isInstanceOf(RuntimeException.class);

        Map<String, Map<String, String>> errors = report.getDeleteErrorsByType();
        assertThat(report.getDeletedByType()).doesNotContainKey("distroxName");
        assertThat(errors).containsKey("distroxName");
        assertThat(errors.get("distroxName")).containsKey("distroA");
    }

    // ---------- helpers ----------

    private Path writeResourceFile(String name, String content) throws IOException {
        Path file = outputDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private static SimpleEnvironmentResponses emptyEnvResponses() {
        SimpleEnvironmentResponses r = new SimpleEnvironmentResponses();
        r.setResponses(Collections.emptyList());
        return r;
    }

    private static CredentialResponses emptyCredResponses() {
        CredentialResponses r = new CredentialResponses();
        r.setResponses(Collections.emptyList());
        return r;
    }

    private static CredentialResponse credentialResponse(String name) {
        CredentialResponse r = new CredentialResponse();
        r.setName(name);
        return r;
    }

    private void invokeDeleteResources(List<String> found, String type, CleanupReport report) throws Exception {
        Method m = CleanupUtil.class.getDeclaredMethod("deleteResources", List.class, String.class, CleanupReport.class);
        m.setAccessible(true);
        try {
            m.invoke(underTest, found, type, report);
        } catch (InvocationTargetException ite) {
            if (ite.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw ite;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeGetResourcesFromFile(String type, Path filePath) throws Exception {
        Method m = CleanupUtil.class.getDeclaredMethod("getResourcesFromFile", String.class, Path.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(underTest, type, filePath);
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeFindLeftoverResources(String type, List<String> attempted) throws Exception {
        Method m = CleanupUtil.class.getDeclaredMethod("findLeftoverResources", String.class, List.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(underTest, type, attempted);
    }
}
