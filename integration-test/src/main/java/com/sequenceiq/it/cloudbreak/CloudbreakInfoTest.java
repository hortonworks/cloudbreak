package com.sequenceiq.it.cloudbreak;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.mock.json.CBVersion;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;

public class CloudbreakInfoTest extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakInfoTest.class);

    @Test
    private void testInfo() {
        String infoUrl = getItContext().getContextParam(CLOUDBREAK_SERVER_ROOT);
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(infoUrl).path("info");
        CBVersion cbVersion = target.request().get().readEntity(CBVersion.class);

        Assert.assertEquals(cbVersion.getApp().getName(), "cloudbreak");
        if (getTestParameter().get("target.cbd.version") != null) {
            LOGGER.warn("TARGET_CBD_VERSION is provided.");
            Assert.assertEquals(cbVersion.getApp().getVersion(), getTestParameter().get("target.cbd.version"));
        } else {
            LOGGER.warn("TARGET_CBD_VERSION is not provided!");
        }
    }
}
