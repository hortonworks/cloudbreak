package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService.RUNNING_HIGHSTATE_JID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JidInfoResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;

class JidInfoResponseTransformerTest {

    private JidInfoResponse saltResponseData;

    @BeforeEach
    public void setUp() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_real_response.json")) {
            saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
        }
    }

    @Test
    void testWithOrdering() {
        Map<String, List<RunnerInfo>> res = JidInfoResponseTransformer.getHighStates(saltResponseData);
        for (List<RunnerInfo> value : res.values()) {
            int current = -1;
            List<RunnerInfo> runnerInfoList = value;
            for (RunnerInfo runnerInfo : runnerInfoList) {
                int former = current;
                current = Math.max(current, runnerInfo.getRunNum());
                assertTrue(former < current, "SaltStates are not properly ordered");
            }
        }
    }

    @Test
    void testResultSummaryWithStderr() {
        Map<String, List<RunnerInfo>> res = JidInfoResponseTransformer.getHighStates(saltResponseData);
        RunnerInfo extraInfo = res.get("host-10-0-0-6.aws").get(18);
        Map<String, String> expectedErrorResultSummary = Map.of(
            "Comment", "Command \"/opt/ambari-server/install-mpack-1.sh\" run",
            "Stderr", "+ ARGS= + echo yes + ambari-server install-mpack --"
        );

        assertEquals("Command \"/opt/ambari-server/install-mpack-1.sh\" run", extraInfo.getComment());
        assertEquals("+ ARGS= + echo yes + ambari-server install-mpack --", extraInfo.getStderr());
        assertEquals(expectedErrorResultSummary, extraInfo.getErrorResultSummary());
    }

    @Test
    void testResultSummary() {
        Map<String, List<RunnerInfo>> res = JidInfoResponseTransformer.getHighStates(saltResponseData);
        RunnerInfo noExtraInfo = res.get("host-10-0-0-6.aws").get(0);
        Map<String, String> expectedErrorResultSummary = Map.of(
            "Name", "/etc/hosts",
            "Comment", "No changes needed to be made"
        );

        assertEquals("No changes needed to be made", noExtraInfo.getComment());
        assertEquals("null", noExtraInfo.getStderr());
        assertEquals(expectedErrorResultSummary, noExtraInfo.getErrorResultSummary());
    }

    @Test
    void testFailedStateResponse() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_failed_response.json")) {
            saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
        }
        Map<String, List<RunnerInfo>> result = JidInfoResponseTransformer.getSimpleStates(saltResponseData);
        assertThat(result, hasValue(allOf(hasItem(allOf(hasProperty("result", is(true)), hasProperty("runNum", is(3)))))));
        assertThat(result, hasValue(allOf(hasItem(allOf(hasProperty("result", is(false)), hasProperty("runNum", is(4)))))));
    }

    @Test
    void testSaltHighstateAlreadyRunning() throws IOException {
        SaltExecutionWentWrongException execEx = assertThrows(SaltExecutionWentWrongException.class, () -> {
            try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_highstate_already_running_response.json")) {
                JidInfoResponse saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
                JidInfoResponseTransformer.getHighStates(saltResponseData);
            }
        });
        Matcher matcher = RUNNING_HIGHSTATE_JID.matcher(execEx.getMessage());
        assertTrue(matcher.matches());
        assertEquals(matcher.group(1), "123456");
    }

    @Test
    void testInvalidStateResponse() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_invalid_response1.json")) {
            saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
        }
        Map<String, List<RunnerInfo>> result = JidInfoResponseTransformer.getSimpleStates(saltResponseData);
        assertEquals(1, result.size());
    }

    @Test
    void testEmptyStateResponse() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_empty_response.json")) {
            saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
        }
        Map<String, List<RunnerInfo>> result = JidInfoResponseTransformer.getSimpleStates(saltResponseData);
        assertEquals(0, result.size());
    }

    @Test
    void testHighStateEmptyStateResponse() throws IOException {
        JidInfoResponse saltResponseData = null;
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_empty_response.json")) {
            saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
        }
        Map<String, List<RunnerInfo>> result = JidInfoResponseTransformer.getHighStates(saltResponseData);
        assertEquals(0, result.size());
    }

    @Test
    void testInvalidHighStateJidResponseWithWrongObjects() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_invalid_response1.json")) {
            JidInfoResponse saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
            assertThrows(SaltExecutionWentWrongException.class, () -> JidInfoResponseTransformer.getHighStates(saltResponseData));
        }
    }

    @Test
    void testInvalidHighStateJidResponseWithUnresponsiveMinions() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_minion_no_return.json")) {
            JidInfoResponse saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
            assertThrows(SaltExecutionWentWrongException.class, () -> JidInfoResponseTransformer.getHighStates(saltResponseData));
        }
    }

    @Test
    void testInvalidHighStateJidResponseWithNonEmptyList() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_invalid_response2.json")) {
            JidInfoResponse saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
            assertThrows(SaltExecutionWentWrongException.class, () -> JidInfoResponseTransformer.getHighStates(saltResponseData));
        }
    }

    @Test
    void testInvalidHighStateJidResponseWithNullObject() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_invalid_response3.json")) {
            JidInfoResponse saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
            assertThrows(UnsupportedOperationException.class, () -> JidInfoResponseTransformer.getHighStates(saltResponseData));
        }
    }

    @Test
    void testInvalidHighStateJidResponseWithStringObject() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_invalid_response4.json")) {
            JidInfoResponse saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), JidInfoResponse.class);
            assertThrows(UnsupportedOperationException.class, () -> JidInfoResponseTransformer.getHighStates(saltResponseData));
        }
    }
}


