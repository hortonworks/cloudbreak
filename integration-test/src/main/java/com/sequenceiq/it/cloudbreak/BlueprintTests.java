package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import org.springframework.util.StreamUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class BlueprintTests extends CloudbreakTest {

    public static final String UPLOADBP = "bluuvelvet";

    @Test
    public void createBlueprint22() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(UPLOADBP)
                .withDescription("alma")
                .withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
        then(Blueprint.assertThis(
                (blueprint, t)-> {
                    Assert.assertNotNull(blueprint.getRequest());
                })
        );
    }

/*
    @AfterSuite
    public void cleanUp() {
        given(CloudbreakClient.isCreated());
        given(ObsoleteBlueprint.request().withName(UPLOADBP));
        when(ObsoleteBlueprint.delete());
    }
*/

    private String getBlueprintFile() throws IOException {
        return new String(
                StreamUtils.copyToByteArray(
                        applicationContext
                                .getResource("classpath:/blueprint/hdp-multinode-default.bp")
                                .getInputStream()));
    }
}
