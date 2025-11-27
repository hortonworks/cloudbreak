package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.COMMENT;
import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.NAME;
import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.STDERR;
import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.STDOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;

class OrchestratorExceptionAnalyzerTest {
    @Test
    void testGetNodeErrorParameters() {
        Multimap<String, String> nodeWithErrors = HashMultimap.create();
        nodeWithErrors.put("ipaserver0", "Error(s): Failed to execute: " +
                "{Comment=Command \"/opt/salt/scripts/ad_dns_validation.sh hybrid.cloudera.org 10.84.202.113 10.84.202.14\" run, " +
                "Stderr=Failure: Expected IP did not match. \n(Expected ip: 10.84.202.14, Resolved ips: 10.84.202.113), " +
                "Name=/opt/salt/scripts/ad_dns_validation.sh hybrid.cloudera.org 10.84.202.113 10.84.202.14, " +
                "Stdout=Resolved IPs for hybrid.cloudera.org using DNS server 10.84.202.113: 10.84.202.113}");
        nodeWithErrors.put("ipaserver1", "param1Key=param1Value, param2Key=param2Value");
        nodeWithErrors.put("ipaserver1", "param1Key=param1Value2, param3Key=param2Value");
        CloudbreakOrchestratorFailedException orchestratorException = new CloudbreakOrchestratorFailedException("Dns validation error", nodeWithErrors);

        Map<String, String> additionalParams = OrchestratorExceptionAnalyzer.getNodeErrorParameters(orchestratorException);

        assertEquals(additionalParams.get("ipaserver0.comment"),
                "Command \"/opt/salt/scripts/ad_dns_validation.sh hybrid.cloudera.org 10.84.202.113 10.84.202.14\" run");
        assertEquals(additionalParams.get("ipaserver0.stderr"),
                "Failure: Expected IP did not match. \n(Expected ip: 10.84.202.14, Resolved ips: 10.84.202.113)");
        assertEquals(additionalParams.get("ipaserver0.stdout"),
                "Resolved IPs for hybrid.cloudera.org using DNS server 10.84.202.113: 10.84.202.113");
        assertEquals(additionalParams.get("ipaserver0.name"),
                "/opt/salt/scripts/ad_dns_validation.sh hybrid.cloudera.org 10.84.202.113 10.84.202.14");
        assertTrue(additionalParams.get("ipaserver1.param1key").contains("param1Value"));
        assertTrue(additionalParams.get("ipaserver1.param1key").contains("param1Value2"));
        assertEquals(additionalParams.get("ipaserver1.param2key"), "param2Value");
    }

    @Test
    void testGetHostSaltCommands() {
        Multimap<String, String> nodeWithErrors = HashMultimap.create();
        nodeWithErrors.put("ipaserver0", "Error(s): Failed to execute: " +
                "{Comment=Command \"/opt/salt/scripts/ad_dns_validation.sh hybrid.cloudera.org 10.84.202.113 10.84.202.14\" run, " +
                "Stderr=Failure: Expected IP did not match. \n(Expected ip: 10.84.202.14, Resolved ips: 10.84.202.113), " +
                "Name=/opt/salt/scripts/ad_dns_validation.sh hybrid.cloudera.org 10.84.202.113 10.84.202.14, " +
                "Stdout=Resolved IPs for hybrid.cloudera.org using DNS server 10.84.202.113: 10.84.202.113}");
        nodeWithErrors.put("ipaserver1", "param1Key=param1Value, param2Key=param2Value");
        nodeWithErrors.put("ipaserver1", "param1Key=param1Value2, param3Key=param2Value");
        CloudbreakOrchestratorFailedException orchestratorException = new CloudbreakOrchestratorFailedException("Dns validation error", nodeWithErrors);

        Set<OrchestratorExceptionAnalyzer.HostSaltCommands> hostSaltCommands = OrchestratorExceptionAnalyzer.getHostSaltCommands(orchestratorException);

        assertTrue(hostSaltCommands.contains(new OrchestratorExceptionAnalyzer.HostSaltCommands("ipaserver0",
                List.of(new OrchestratorExceptionAnalyzer.SaltCommand(
                        "Command \"/opt/salt/scripts/ad_dns_validation.sh hybrid.cloudera.org 10.84.202.113 10.84.202.14\" run",
                        Map.ofEntries(Map.entry(COMMENT,
                                        "Command \"/opt/salt/scripts/ad_dns_validation.sh hybrid.cloudera.org 10.84.202.113 10.84.202.14\" run"),
                                Map.entry(STDERR, "Failure: Expected IP did not match. \n(Expected ip: 10.84.202.14, Resolved ips: 10.84.202.113)"),
                                Map.entry(STDOUT, "Resolved IPs for hybrid.cloudera.org using DNS server 10.84.202.113: 10.84.202.113"),
                                Map.entry(NAME, "/opt/salt/scripts/ad_dns_validation.sh hybrid.cloudera.org 10.84.202.113 10.84.202.14")))))));
        OrchestratorExceptionAnalyzer.HostSaltCommands host1Command = hostSaltCommands.stream()
                .filter(hsc -> hsc.host().equals("ipaserver1"))
                .findFirst()
                .get();
        Assertions.assertThat(host1Command.saltCommands()).containsAll(List.of(
                new OrchestratorExceptionAnalyzer.SaltCommand("param1Key=param1Value, param2Key=param2Value",
                        Map.of("param1key", "param1Value", "param2key", "param2Value")),
                new OrchestratorExceptionAnalyzer.SaltCommand("param1Key=param1Value2, param3Key=param2Value",
                        Map.of("param1key", "param1Value2", "param3key", "param2Value"))));
    }
}
