package com.sequenceiq.it.cloudbreak.testcase.oldinfo;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.FreeIpaTest;
import com.sequenceiq.it.cloudbreak.mock.CBVersion;

public class FreeIpaInfoTest extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInfoTest.class);

    @Test
    private void testInfo() {
        String infoUrl = getItContext().getContextParam(FreeIpaTest.FREEIPA_SERVER_ROOT);
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(infoUrl).path("info");
        CBVersion cbVersion = target.request().get().readEntity(CBVersion.class);

        Assert.assertEquals(cbVersion.getApp().getName(), "freeipa");
        if (getTestParameter().get("target.cbd.version") != null) {
            LOGGER.warn("TARGET_CBD_VERSION is provided.");
            Assert.assertEquals(cbVersion.getApp().getVersion(), getTestParameter().get("target.cbd.version"));
        } else {
            LOGGER.warn("TARGET_CBD_VERSION is not provided!");
        }
    }
}
