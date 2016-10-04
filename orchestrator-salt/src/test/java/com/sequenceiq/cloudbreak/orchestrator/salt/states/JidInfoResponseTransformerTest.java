package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class JidInfoResponseTransformerTest {

    @Test
    public void testWithOrdering() throws IOException {
        InputStream responseStream = JidInfoResponseTransformerTest.class.getResourceAsStream("/jid_real_response.json");
        Map map = JsonUtil.readValue(IOUtils.toString(responseStream), HashMap.class);
        Map<String, List<RunnerInfo>> res = JidInfoResponseTransformer.getHighStates(map);
        for (String key : res.keySet()) {
            int current = -1;
            List<RunnerInfo> runnerInfoList = res.get(key);
            for (RunnerInfo runnerInfo : runnerInfoList) {
                int former = current;
                current = Math.max(current, runnerInfo.getRunNum());
                assertTrue("SaltStates are not properly ordered", former < current);
            }
        }

    }
}


