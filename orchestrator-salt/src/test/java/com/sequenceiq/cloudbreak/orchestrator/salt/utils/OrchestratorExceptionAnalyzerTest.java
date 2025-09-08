package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;

@ExtendWith(MockitoExtension.class)
class OrchestratorExceptionAnalyzerTest {

    @InjectMocks
    private OrchestratorExceptionAnalyzer underTest;

    @Test
    void testValidateWhenDnsValidationFailureWithParams() throws Exception {
        Multimap<String, String> nodeWithErrors = HashMultimap.create();
        nodeWithErrors.put("ipaserver0.fs-aws2.xcu2-8y8x.wl.cloudera.site", "Error(s): Failed to execute: " +
                "{Comment=Command \"/opt/salt/scripts/ad_dns_validation.sh ec2amaz-uvredmu.hybrid.cloudera.org 10.84.202.113 10.84.202.14\" run, " +
                "Stderr=Failure: Expected IP did not match. \n(Expected ip: 10.84.202.14, Resolved ips: 10.84.202.113), " +
                "Name=/opt/salt/scripts/ad_dns_validation.sh ec2amaz-uvredmu.hybrid.cloudera.org 10.84.202.113 10.84.202.14, " +
                "Stdout=Resolved IPs for ec2amaz-uvredmu.hybrid.cloudera.org using DNS server 10.84.202.113: 10.84.202.113}");
        nodeWithErrors.put("ipaserver1.fs-aws2.xcu2-8y8x.wl.cloudera.site", "param1Key=param1Value, param2Key=param2Value");
        nodeWithErrors.put("ipaserver1.fs-aws2.xcu2-8y8x.wl.cloudera.site", "param1Key=param1Value2, param3Key=param2Value");
        CloudbreakOrchestratorFailedException orchestratorException = new CloudbreakOrchestratorFailedException("Dns validation error", nodeWithErrors);

        Map<String, String> additionalParams = underTest.getNodeErrorParameters(orchestratorException);

        assertEquals(additionalParams.get("ipaserver0.fs-aws2.xcu2-8y8x.wl.cloudera.site.Comment"),
                "Command \"/opt/salt/scripts/ad_dns_validation.sh ec2amaz-uvredmu.hybrid.cloudera.org 10.84.202.113 10.84.202.14\" run");
        assertEquals(additionalParams.get("ipaserver0.fs-aws2.xcu2-8y8x.wl.cloudera.site.Stderr"),
                "Failure: Expected IP did not match. \n(Expected ip: 10.84.202.14, Resolved ips: 10.84.202.113)");
        assertEquals(additionalParams.get("ipaserver0.fs-aws2.xcu2-8y8x.wl.cloudera.site.Stdout"),
                "Resolved IPs for ec2amaz-uvredmu.hybrid.cloudera.org using DNS server 10.84.202.113: 10.84.202.113");
        assertEquals(additionalParams.get("ipaserver0.fs-aws2.xcu2-8y8x.wl.cloudera.site.Name"),
                "/opt/salt/scripts/ad_dns_validation.sh ec2amaz-uvredmu.hybrid.cloudera.org 10.84.202.113 10.84.202.14");
        assertTrue(additionalParams.get("ipaserver1.fs-aws2.xcu2-8y8x.wl.cloudera.site.param1Key").contains("param1Value"));
        assertTrue(additionalParams.get("ipaserver1.fs-aws2.xcu2-8y8x.wl.cloudera.site.param1Key").contains("param1Value2"));
        assertEquals(additionalParams.get("ipaserver1.fs-aws2.xcu2-8y8x.wl.cloudera.site.param2Key"), "param2Value");
    }

}