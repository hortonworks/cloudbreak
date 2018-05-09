package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class JidInfoResponseTransformerTest {
    private static Map saltResponseData;

    @BeforeClass
    public static void setUp() throws Exception {
        populateTestData("/jid_real_response.json", saltResponseData);
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
        assertEquals("\nComment: No changes needed to be made", noExtraInfo.getErrorResultSummary());
    }

    private static void populateTestData(String path, Map data) throws IOException {
        try (InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream(path)) {
            saltResponseData = JsonUtil.readValue(IOUtils.toString(responseStream), HashMap.class);
        }
    }
}


