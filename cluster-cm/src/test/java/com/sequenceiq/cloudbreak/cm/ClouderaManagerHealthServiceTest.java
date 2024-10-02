package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.FULL_VIEW;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.SUMMARY;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService.FULL_WITH_EXPLANATION_VIEW;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService.HOST_AGENT_CERTIFICATE_EXPIRY;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService.HOST_SCM_HEALTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerHealthServiceTest {

    @InjectMocks
    private ClouderaManagerHealthService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient client;

    @Mock
    private HostsResourceApi hostsResourceApi;

    @BeforeEach
    void setUp() {
        lenient().when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
    }

    @Test
    public void collectsHostHealthIfDuplicated() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host1").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host2").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        Optional<String> reason = extendedHostStatuses.getHostsHealth().get(hostName("host1")).iterator().next().getReason();
        assertTrue(reason.isPresent());
        assertTrue(reason.get().contains("This host is duplicated"));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
    }

    @Test
    public void collectsHostHealthIfAvailable() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host2").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING)),
                new ApiHost().hostname("host3").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD)),
                new ApiHost().hostname("host4").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.NOT_AVAILABLE)),
                new ApiHost().hostname("host5").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.HISTORY_NOT_AVAILABLE)),
                new ApiHost().hostname("host6").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.DISABLED))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host3")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
    }

    @Test
    public void collectsExtendedHostHealthIfAvailable() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.CONCERNING)
                                .explanation("in 30 days")),
                new ApiHost().hostname("host3")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD).explanation("explanation."))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.BAD).explanation("in 2 days")),
                new ApiHost().hostname("host4").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.NOT_AVAILABLE)),
                new ApiHost().hostname("host5").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.HISTORY_NOT_AVAILABLE)),
                new ApiHost().hostname("host6").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.DISABLED)),
                new ApiHost().hostname("host7").maintenanceMode(true)
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));
        assertEquals("explanation. Cert health on CM: BAD", extendedHostStatuses.statusReasonForHost(hostName("host3")));
        assertTrue(extendedHostStatuses.isAnyCertExpiring());
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host3")));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host7")));
    }

    @Test
    public void testCertExpiringEverythingElseGood() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.CONCERNING)
                                .explanation("in 30 days"))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertTrue(extendedHostStatuses.isAnyCertExpiring());
    }

    @Test
    public void testServiceBadHealth() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD)
                                .explanation("explanation"))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(roleRef("badservice", ApiHealthSummary.BAD)),
                new ApiHost().hostname("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)
                                .explanation("in 30 days"))
                        .addRoleRefsItem(roleRef("badservice2", ApiHealthSummary.BAD))
                        .addRoleRefsItem(roleRef("badservice3", ApiHealthSummary.BAD))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.isAnyCertExpiring());
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertEquals("explanation. The following services are in bad health: badservice.",
                extendedHostStatuses.statusReasonForHost(hostName("host1")));
        assertEquals("The following services are in bad health: badservice2, badservice3.",
                extendedHostStatuses.statusReasonForHost(hostName("host2")));

        extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.11"));

        assertFalse(extendedHostStatuses.isAnyCertExpiring());
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertEquals("explanation.",
                extendedHostStatuses.statusReasonForHost(hostName("host1")));
    }

    @Test
    public void testEverythingGood() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)
                                .explanation("in 30 days"))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.isAnyCertExpiring());
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
    }

    @Test
    public void filtersAppropriateHealthCheckForHost() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host")
                        .addHealthChecksItem(new ApiHealthCheck().name("fake_check").summary(ApiHealthSummary.BAD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING))
                        .addHealthChecksItem(new ApiHealthCheck().name("another").summary(ApiHealthSummary.BAD))
        );

        assertTrue(underTest.getExtendedHostStatuses(client, Optional.of("7.2.12")).isHostHealthy(hostName("host")));
    }

    @Test
    public void hostWithoutHealthCheckIsIgnored() throws ApiException {
        hostsAre(new ApiHost().hostname("hostY"));

        assertFalse(underTest.getExtendedHostStatuses(client, Optional.of("7.2.12")).getHostsHealth().containsKey(hostName("hostY")));
    }

    @Test
    public void hostWithoutAppropriateHealthCheckIsIgnored() throws ApiException {
        hostsAre(new ApiHost().hostname("hostY").addHealthChecksItem(new ApiHealthCheck().name("fake_check").summary(ApiHealthSummary.BAD)));

        assertFalse(underTest.getExtendedHostStatuses(client, Optional.of("7.2.12")).getHostsHealth().containsKey(hostName("hostY")));
    }

    private void hostsAre(ApiHost... hosts) throws ApiException {
        Arrays.stream(hosts).forEach(host -> host.addRoleRefsItem(roleRef(ApiHealthSummary.GOOD)));
        ApiHostList list = new ApiHostList().items(Arrays.asList(hosts));
        lenient().when(hostsResourceApi.readHosts(null, null, FULL_VIEW)).thenReturn(list);
        lenient().when(hostsResourceApi.readHosts(null, null, SUMMARY)).thenReturn(list);
        lenient().when(hostsResourceApi.readHosts(null, null, FULL_WITH_EXPLANATION_VIEW)).thenReturn(list);
    }

    private ApiRoleRef roleRef(ApiHealthSummary apiHealthSummary) {
        return roleRef("testService", apiHealthSummary);
    }

    private ApiRoleRef roleRef(String serviceName, ApiHealthSummary apiHealthSummary) {
        return new ApiRoleRef()
                .roleName("testRole")
                .serviceName(serviceName)
                .healthSummary(apiHealthSummary);
    }
}