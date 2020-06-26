package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

class MinionKeysOnMasterResponseTest {

    public static final String FULL_INPUT = "{\"return\":"
            + "[{\"tag\":\"salt/wheel/20200618143117443966\", "
            + "\"data\":{"
            + "\"fun\":\"wheel.key.list_all\","
            + "\"jid\":\"20200618143117443966\","
            + "\"tag\":\"salt/wheel/20200618143117443966\","
            + "\"user\":\"saltuser\","
            + "\"_stamp\":\"2020-06-18T14:31:17.670425\","
            + "\"return\":"
            + "{\"minions\":[\"test-compute1.test.xcu2-8y8x.wl.cloudera.site\"],"
            + "\"minions_pre\":[\"test-master0.test.xcu2-8y8x.wl.cloudera.site\",\"test-worker1.test.xcu2-8y8x.wl.cloudera.site\","
            + "\"test-worker2.test.xcu2-8y8x.wl.cloudera.site\",\"test-worker3.test.xcu2-8y8x.wl.cloudera.site\"],"
            + "\"minions_rejected\":[\"test-compute0.test.xcu2-8y8x.wl.cloudera.site\"],"
            + "\"minions_denied\":[\"test-compute2.test.xcu2-8y8x.wl.cloudera.site\"],"
            + "\"local\":[\"master.pem\",\"master.pub\",\"master_sign.pem\"]},"
            + "\"success\":true}}]"
            + '}';

    public static final String EMPTY_ARRAY = "{\"return\":"
            + "[{\"tag\":\"salt/wheel/20200618143117443966\", "
            + "\"data\":{"
            + "\"fun\":\"wheel.key.list_all\","
            + "\"jid\":\"20200618143117443966\","
            + "\"tag\":\"salt/wheel/20200618143117443966\","
            + "\"user\":\"saltuser\","
            + "\"_stamp\":\"2020-06-18T14:31:17.670425\","
            + "\"return\":"
            + "{\"minions\":[],"
            + "\"local\":[\"master.pem\",\"master.pub\",\"master_sign.pem\"]},"
            + "\"success\":true}}]"
            + '}';

    public static final String MISSING_STATE = "{\"return\":"
            + "[{\"tag\":\"salt/wheel/20200618143117443966\", "
            + "\"data\":{"
            + "\"fun\":\"wheel.key.list_all\","
            + "\"jid\":\"20200618143117443966\","
            + "\"tag\":\"salt/wheel/20200618143117443966\","
            + "\"user\":\"saltuser\","
            + "\"_stamp\":\"2020-06-18T14:31:17.670425\","
            + "\"return\":"
            + "{\"minions\":[],"
            + "\"local\":[\"master.pem\",\"master.pub\",\"master_sign.pem\"]},"
            + "\"success\":true}}]"
            + '}';

    public static final String MISSING_RETURN = "{\"return\":"
            + "[{\"tag\":\"salt/wheel/20200618143117443966\", "
            + "\"data\":{"
            + "\"fun\":\"wheel.key.list_all\","
            + "\"jid\":\"20200618143117443966\","
            + "\"tag\":\"salt/wheel/20200618143117443966\","
            + "\"user\":\"saltuser\","
            + "\"_stamp\":\"2020-06-18T14:31:17.670425\","
            + "\"success\":true}}]"
            + '}';

    public static final String MISSING_DATA = "{\"return\":"
            + "[{\"tag\":\"salt/wheel/20200618143117443966\""
            + "}]"
            + '}';

    @Test
    public void testAllMinions() throws IOException {
        MinionKeysOnMasterResponse response = JsonUtil.readValue(FULL_INPUT, MinionKeysOnMasterResponse.class);
        List<String> unacceptedMinions = response.getUnacceptedMinions();
        assertTrue(Iterables.elementsEqual(List.of("test-master0.test.xcu2-8y8x.wl.cloudera.site", "test-worker1.test.xcu2-8y8x.wl.cloudera.site",
                "test-worker2.test.xcu2-8y8x.wl.cloudera.site", "test-worker3.test.xcu2-8y8x.wl.cloudera.site"), unacceptedMinions));
        List<String> acceptedMinions = response.getAcceptedMinions();
        assertTrue(Iterables.elementsEqual(List.of("test-compute1.test.xcu2-8y8x.wl.cloudera.site"), acceptedMinions));
        List<String> rejectedMinions = response.getRejectedMinions();
        assertTrue(Iterables.elementsEqual(List.of("test-compute0.test.xcu2-8y8x.wl.cloudera.site"), rejectedMinions));
        List<String> deniedMinions = response.getDeniedMinions();
        assertTrue(Iterables.elementsEqual(List.of("test-compute2.test.xcu2-8y8x.wl.cloudera.site"), deniedMinions));
    }

    @Test
    public void testEmptyList() throws IOException {
        MinionKeysOnMasterResponse response = JsonUtil.readValue(EMPTY_ARRAY, MinionKeysOnMasterResponse.class);
        List<String> acceptedMinions = response.getAcceptedMinions();
        assertTrue(acceptedMinions.isEmpty());
    }

    @Test
    public void testMissingState() throws IOException {
        MinionKeysOnMasterResponse response = JsonUtil.readValue(MISSING_STATE, MinionKeysOnMasterResponse.class);
        List<String> rejectedMinions = response.getRejectedMinions();
        assertNotNull(rejectedMinions);
        assertTrue(rejectedMinions.isEmpty());
    }

    @Test
    public void testMissingReturn() throws IOException {
        MinionKeysOnMasterResponse response = JsonUtil.readValue(MISSING_RETURN, MinionKeysOnMasterResponse.class);
        List<String> rejectedMinions = response.getRejectedMinions();
        assertNotNull(rejectedMinions);
        assertTrue(rejectedMinions.isEmpty());
    }

    @Test
    public void testMissingData() throws IOException {
        MinionKeysOnMasterResponse response = JsonUtil.readValue(MISSING_DATA, MinionKeysOnMasterResponse.class);
        List<String> rejectedMinions = response.getRejectedMinions();
        assertNotNull(rejectedMinions);
        assertTrue(rejectedMinions.isEmpty());
    }
}