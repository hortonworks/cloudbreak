package com.sequenceiq.cloudbreak.component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DnsManagementServiceTest.CreateCertificationTestConfig.class)
@TestPropertySource(properties = {
        "clusterdns.host=localhost",
        "clusterdns.port=8982",
        "cert.polling.attempt=50",
        "cert.base.domain.name=workload-dev.cloudera.com",
        "altus.ums.host=ums.thunderhead-dev.cloudera.com",
        "actor.crn=<ypur-actor-crn>",
        "account.id=<your-account-id>"
})
public class DnsManagementServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsManagementServiceTest.class);

    @Inject
    private DnsManagementService dnsManagementService;

    @Value("${actor.crn}")
    private String actorCrn;

    @Value("${account.id}")
    private String accountId;

    @Test
    public void createAndDeleteDnsEntryWithIp() throws IOException {
        String endpoint = "gknox";
        String environment = "gtopolyai-without-freeipa";
        boolean wildcard = false;
        List<String> ips = List.of("10.65.65.152");
        dnsManagementService.createOrUpdateDnsEntryWithIp(actorCrn, accountId, endpoint, environment, wildcard, ips);
        LOGGER.info("dns is registered");
        dnsManagementService.deleteDnsEntryWithIp(actorCrn, accountId, endpoint, environment, wildcard, ips);
        LOGGER.info("dns is deleted");

    }

    @Test
    public void createDnsEntryWithIp() throws IOException {
//        String endpoint = "gasd";
//        String endpoint = "gasd1";
        String endpoint = "gknox";
        String environment = "gtopolyai-without-freeipa";
        boolean wildcard = false;
        List<String> ips = List.of("10.65.65.212");
        dnsManagementService.createOrUpdateDnsEntryWithIp(actorCrn, accountId, endpoint, environment, wildcard, ips);
        LOGGER.info("dns is registered");
    }

    @Test
    public void deleteDnsEntryWithIp() throws IOException {
        //gtopolyai-cluster.gtopolya.xcu2-8y8x.workload-dev.cloudera.com
        String endpoint = "gtopolyai-cluster";
        String environment = "gtopolyai-without-freeipa";
        boolean wildcard = false;
        List<String> ips = List.of("10.65.65.66");
        dnsManagementService.deleteDnsEntryWithIp(actorCrn, accountId, endpoint, environment, wildcard, ips);
        verifyHost(endpoint);
        LOGGER.info("dns is deleted");
    }

    @Test
    public void verifyHost() throws UnknownHostException {
        verifyHost("gtopolyai-cluster");
    }

    private void verifyHost(String endpoint) throws UnknownHostException {
        InetAddress inetHost = InetAddress.getByName(endpoint + ".gtopolya.xcu2-8y8x.workload-dev.cloudera.com");
        String hostName = inetHost.getHostName();
        LOGGER.info("The host name was: " + hostName);
        LOGGER.info("The hosts IP address is: " + inetHost.getHostAddress());
    }

    @Configuration
    @ComponentScan(
            basePackages = {"com.sequenceiq.cloudbreak.certificate",
                    "com.sequenceiq.cloudbreak.client",
                    "com.sequenceiq.cloudbreak.auth.altus",
                    "com.sequenceiq.cloudbreak.auth"}
    )
    public static class CreateCertificationTestConfig {

    }
}
