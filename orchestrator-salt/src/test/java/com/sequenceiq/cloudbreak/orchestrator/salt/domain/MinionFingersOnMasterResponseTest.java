package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

class MinionFingersOnMasterResponseTest {

    public static final String FULL_INPUT = "{\n"
            + "  \"return\": [\n"
            + "    {\n"
            + "      \"tag\": \"salt/wheel/20200624130125986280\",\n"
            + "      \"data\": {\n"
            + "        \"fun\": \"wheel.key.finger\",\n"
            + "        \"jid\": \"20200624130125986280\",\n"
            + "        \"tag\": \"salt/wheel/20200624130125986280\",\n"
            + "        \"user\": \"saltuser\",\n"
            + "        \"_stamp\": \"2020-06-24T13:01:26.203524\",\n"
            + "        \"return\": {\n"
            + "          \"minions_pre\": {\n"
            + "            \"test-master3.test.xcu2-8y8x.wl.cloudera.site\": "
            + "                 \"dd:3b:bb:de:6b:02:22:73:91:80:bc:35:52:a1:d1:dd:0d:72:e7:9a:fa:c1:61:ac:6e:10:8a:04:1a:cc:54:10\",\n"
            + "            \"test-worker0.test.xcu2-8y8x.wl.cloudera.site\": "
            + "                 \"c1:8d:ec:7f:e6:f6:51:70:5b:34:04:a2:71:17:52:82:1a:e6:b2:d7:37:55:90:6c:36:2e:91:37:de:dc:4a:c2\",\n"
            + "            \"test-worker1.test.xcu2-8y8x.wl.cloudera.site\": "
            + "                 \"c0:7c:c7:dc:0b:22:48:e8:19:08:55:59:b0:e0:f7:44:a6:84:d5:d3:a4:71:7a:f3:26:f6:12:d8:fe:3c:26:31\",\n"
            + "            \"test-worker2.test.xcu2-8y8x.wl.cloudera.site\": "
            + "                 \"e9:0d:31:ac:95:f8:37:2b:74:35:4e:46:f7:40:b3:9f:3c:8f:12:ad:71:0b:ca:e8:2d:af:d4:65:83:db:ce:97\"\n"
            + "          }\n"
            + "        },\n"
            + "        \"success\": true\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static final String NO_MINIONS = "{\n"
            + "  \"return\": [\n"
            + "    {\n"
            + "      \"tag\": \"salt/wheel/20200624130125986280\",\n"
            + "      \"data\": {\n"
            + "        \"fun\": \"wheel.key.finger\",\n"
            + "        \"jid\": \"20200624130125986280\",\n"
            + "        \"tag\": \"salt/wheel/20200624130125986280\",\n"
            + "        \"user\": \"saltuser\",\n"
            + "        \"_stamp\": \"2020-06-24T13:01:26.203524\",\n"
            + "        \"return\": {\n"
            + "          \"minions_pre\": {\n"
            + "          }\n"
            + "        },\n"
            + "        \"success\": true\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static final String NO_MINIONS_PRE = "{\n"
            + "  \"return\": [\n"
            + "    {\n"
            + "      \"tag\": \"salt/wheel/20200624130125986280\",\n"
            + "      \"data\": {\n"
            + "        \"fun\": \"wheel.key.finger\",\n"
            + "        \"jid\": \"20200624130125986280\",\n"
            + "        \"tag\": \"salt/wheel/20200624130125986280\",\n"
            + "        \"user\": \"saltuser\",\n"
            + "        \"_stamp\": \"2020-06-24T13:01:26.203524\",\n"
            + "        \"return\": {\n"
            + "        },\n"
            + "        \"success\": true\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static final String NO_RETURN = "{\n"
            + "  \"return\": [\n"
            + "    {\n"
            + "      \"tag\": \"salt/wheel/20200624130125986280\",\n"
            + "      \"data\": {\n"
            + "        \"fun\": \"wheel.key.finger\",\n"
            + "        \"jid\": \"20200624130125986280\",\n"
            + "        \"tag\": \"salt/wheel/20200624130125986280\",\n"
            + "        \"user\": \"saltuser\",\n"
            + "        \"_stamp\": \"2020-06-24T13:01:26.203524\",\n"
            + "        \"success\": true\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static final String NO_DATA = "{\n"
            + "  \"return\": [\n"
            + "    {\n"
            + "      \"tag\": \"salt/wheel/20200624130125986280\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    @Test
    public void testAllUnAcceptedMinion() throws IOException {
        MinionFingersOnMasterResponse response = JsonUtil.readValue(FULL_INPUT, MinionFingersOnMasterResponse.class);
        Map<String, String> unacceptedMinions = response.getUnacceptedMinions();
        assertEquals(4, unacceptedMinions.size());
        assertMinionFingerprint(unacceptedMinions, "dd:3b:bb:de:6b:02:22:73:91:80:bc:35:52:a1:d1:dd:0d:72:e7:9a:fa:c1:61:ac:6e:10:8a:04:1a:cc:54:10",
                "test-master3.test.xcu2-8y8x.wl.cloudera.site");
        assertMinionFingerprint(unacceptedMinions, "c1:8d:ec:7f:e6:f6:51:70:5b:34:04:a2:71:17:52:82:1a:e6:b2:d7:37:55:90:6c:36:2e:91:37:de:dc:4a:c2",
                "test-worker0.test.xcu2-8y8x.wl.cloudera.site");
        assertMinionFingerprint(unacceptedMinions, "c0:7c:c7:dc:0b:22:48:e8:19:08:55:59:b0:e0:f7:44:a6:84:d5:d3:a4:71:7a:f3:26:f6:12:d8:fe:3c:26:31",
                "test-worker1.test.xcu2-8y8x.wl.cloudera.site");
        assertMinionFingerprint(unacceptedMinions, "e9:0d:31:ac:95:f8:37:2b:74:35:4e:46:f7:40:b3:9f:3c:8f:12:ad:71:0b:ca:e8:2d:af:d4:65:83:db:ce:97",
                "test-worker2.test.xcu2-8y8x.wl.cloudera.site");
    }

    public void assertMinionFingerprint(Map<String, String> unacceptedMinions, String fingerprint, String minionId) {
        assertEquals(fingerprint, unacceptedMinions.get(minionId));
    }

    @Test
    public void testNoMinions() throws IOException {
        MinionFingersOnMasterResponse response = JsonUtil.readValue(NO_MINIONS, MinionFingersOnMasterResponse.class);
        assertTrue(response.getUnacceptedMinions().isEmpty());
    }

    @Test
    public void testNoMinionsPre() throws IOException {
        MinionFingersOnMasterResponse response = JsonUtil.readValue(NO_MINIONS_PRE, MinionFingersOnMasterResponse.class);
        assertTrue(response.getUnacceptedMinions().isEmpty());
    }

    @Test
    public void testNoReturn() throws IOException {
        MinionFingersOnMasterResponse response = JsonUtil.readValue(NO_RETURN, MinionFingersOnMasterResponse.class);
        assertTrue(response.getUnacceptedMinions().isEmpty());
    }

    @Test
    public void testNoData() throws IOException {
        MinionFingersOnMasterResponse response = JsonUtil.readValue(NO_DATA, MinionFingersOnMasterResponse.class);
        assertTrue(response.getUnacceptedMinions().isEmpty());
    }
}