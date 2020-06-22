package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;

public class JidInfoResponseTransformerTest {

    private Map<String, List<Map<String, Object>>> saltResponseData;

    @Before
    public void setUp() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_real_response.json")) {
            saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), HashMap.class);
        }
    }

    @Test
    public void testWithOrdering() {
        Map<String, List<RunnerInfo>> res = JidInfoResponseTransformer.getHighStates(saltResponseData);
        for (List<RunnerInfo> value : res.values()) {
            int current = -1;
            List<RunnerInfo> runnerInfoList = value;
            for (RunnerInfo runnerInfo : runnerInfoList) {
                int former = current;
                current = Math.max(current, runnerInfo.getRunNum());
                assertTrue("SaltStates are not properly ordered", former < current);
            }
        }
    }

    @Test
    public void testResultSummaryWithStderr() {
        Map<String, List<RunnerInfo>> res = JidInfoResponseTransformer.getHighStates(saltResponseData);
        RunnerInfo extraInfo = res.get("host-10-0-0-6.openstacklocal").get(18);
        assertEquals("Command \"/opt/ambari-server/install-mpack-1.sh\" run", extraInfo.getComment());
        assertEquals("+ ARGS= + echo yes + ambari-server install-mpack --", extraInfo.getStderr());
        assertEquals("\nComment: Command \"/opt/ambari-server/install-mpack-1.sh\" run\nStderr: + ARGS= + echo yes + ambari-server install-mpack --",
                extraInfo.getErrorResultSummary());
    }

    @Test
    public void testResultSummary() {
        Map<String, List<RunnerInfo>> res = JidInfoResponseTransformer.getHighStates(saltResponseData);
        RunnerInfo noExtraInfo = res.get("host-10-0-0-6.openstacklocal").get(0);
        assertEquals("No changes needed to be made", noExtraInfo.getComment());
        assertEquals("null", noExtraInfo.getStderr());
        assertEquals("\nName: /etc/hosts\nComment: No changes needed to be made", noExtraInfo.getErrorResultSummary());
    }

    @Test
    public void testFailedStateResponse() throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_failed_response.json")) {
            saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), HashMap.class);
        }
        Map<String, List<RunnerInfo>> result = JidInfoResponseTransformer.getSimpleStates(saltResponseData);
        assertThat(result, hasValue(allOf(hasItem(allOf(hasProperty("result", is(true)), hasProperty("runNum", is(3)))))));
        assertThat(result, hasValue(allOf(hasItem(allOf(hasProperty("result", is(false)), hasProperty("runNum", is(4)))))));
    }
}


