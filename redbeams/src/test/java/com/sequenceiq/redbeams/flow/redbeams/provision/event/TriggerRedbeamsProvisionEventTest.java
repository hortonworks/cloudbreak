package com.sequenceiq.redbeams.flow.redbeams.provision.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsNetworkV4Parameters;

class TriggerRedbeamsProvisionEventTest {

    private static final String SERIALIZED = """
            {"@type":"com.sequenceiq.redbeams.flow.redbeams.provision.event.TriggerRedbeamsProvisionEvent","selector":"REDBEAMS_PROVISION_EVENT",""" + """
            "resourceId":14710,"networkParameters":{""" + """
            "@type":"com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest",""" + """
            "aws":{"subnetId":"subnet-05298c25c080b5035,subnet-09e1883e8da76e990,subnet-06b65e94536fb84ca"}},""" + """
            "forced":false,"exception":null}""";

    @Test
    void testDeserialization() {
        JsonUtil.readValueUnchecked(SERIALIZED, Payload.class);
    }

    @Test
    void testSerialization() throws JsonProcessingException {
        NetworkV4StackRequest networkParameters = new NetworkV4StackRequest();
        AwsNetworkV4Parameters aws = new AwsNetworkV4Parameters();
        aws.setSubnetId("subnet-05298c25c080b5035,subnet-09e1883e8da76e990,subnet-06b65e94536fb84ca");
        networkParameters.setAws(aws);
        TriggerRedbeamsProvisionEvent event = new TriggerRedbeamsProvisionEvent("REDBEAMS_PROVISION_EVENT", 14710L, networkParameters);

        String result = JsonUtil.writeValueAsString(event);

        assertEquals(SERIALIZED, result);
    }

}