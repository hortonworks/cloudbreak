package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthCheckV1Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaHealthDetailsDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaHealthCheckAssertion implements Assertion<FreeIpaHealthDetailsDto, FreeIpaClient> {

    private static final String HEALTHY = "HEALTHY";

    private static final Set<String> PLUGIN_STAT_PLUGINS = Set.of(
            "BACKUP_CHECK",
            "CLOUDWATCH_CHECK",
            "PORT_CHECK",
            "SERVICE_CHECK"
    );

    private static final Set<String> CHECK_IDS = Set.of(
            "backup",
            "53",
            "88",
            "464",
            "3080",
            "749",
            "80",
            "22",
            "636",
            "389"
    );

    private static final Set<String> CHECK_IDS_SERVICES = Set.of(
            "certmonger",
            "crond",
            "gssproxy",
            "httpd",
            "ipa-custodia",
            "ipa-dnskeysyncd",
            "kadmin",
            "krb5kdc",
            "nginx",
            "pki-tomcatd@pki-tomcat",
            "salt-api",
            "salt-bootstrap",
            "salt-master",
            "salt-minion",
            "sshd",
            "sssd"
    );

    private static final Map<String, Set<String>> ADDITIONAL_SERVICES_BY_OS = Map.of(
            "centos7", Set.of("named-pkcs11", "polkit"),
            "redhat8", Set.of("named-pkcs11", "polkit"),
            "redhat9", Set.of("named")
    );

    @Override
    public FreeIpaHealthDetailsDto doAssertion(TestContext testContext, FreeIpaHealthDetailsDto testDto, FreeIpaClient freeIpaClient)
            throws Exception {
        testDto.getResponse().getNodeHealthDetails().stream().forEach(nodeHealthDetail -> {
            nodeHealthDetail.getHealthChecks().forEach(healthCheckV1Response -> {
                DescribeFreeIpaResponse describeFreeIpaResponse = testContext.get(FreeIpaTestDto.class).getResponse();
                assertHost(nodeHealthDetail, healthCheckV1Response, describeFreeIpaResponse);
                assertStatus(nodeHealthDetail, healthCheckV1Response, describeFreeIpaResponse);
                assertChecks(nodeHealthDetail, healthCheckV1Response, describeFreeIpaResponse);
                assertPlugins(nodeHealthDetail, healthCheckV1Response, describeFreeIpaResponse);
            });
        });
        return testDto;
    }

    private void assertPlugins(NodeHealthDetails nodeHealthDetail, HealthCheckV1Response healthCheckV1Response,
        DescribeFreeIpaResponse describeFreeIpaResponse) {
        healthCheckV1Response.getPluginStat().stream().forEach(pluginStat -> {
            if (pluginStat.getPlugin() == null) {
                throwError("Healthcheck>PluginStat", "Plugin", nodeHealthDetail);
            }
            if (pluginStat.getHost() == null) {
                throwError("Healthcheck>PluginStat", "Host", nodeHealthDetail);
            }
            if (pluginStat.getStatus() == null) {
                throwError("Healthcheck>PluginStat", "Status", nodeHealthDetail);
            }
            if (!HEALTHY.equals(pluginStat.getStatus())) {
                throw new TestFailException(
                        String.format("%s plugin is not healthy on the freeIPA cluster: %s",
                            pluginStat.getPlugin(),
                                nodeHealthDetail)
                );
            }
        });

        Set<String> pluginIds = healthCheckV1Response.getPluginStat().stream()
                .map(e -> e.getPlugin())
                .collect(toSet());

        Set<String> missingPluginIds = PLUGIN_STAT_PLUGINS.stream()
                .filter(pluginId -> !pluginIds.contains(pluginId))
                .collect(toSet());
        if (!missingPluginIds.isEmpty()) {
            throw new TestFailException(String.format("%s plugins are missing from freeipa health: %s",
                    missingPluginIds,
                    nodeHealthDetail)
            );
        }
        if (pluginIds.size() != PLUGIN_STAT_PLUGINS.size()) {
            throw new TestFailException(String.format("Some plugins are missing from freeipa health. " +
                            "Current plugins: %s, Required plugins: %s, Response: %s",
                    pluginIds, PLUGIN_STAT_PLUGINS, nodeHealthDetail)
            );
        }
    }

    private void assertChecks(NodeHealthDetails nodeHealthDetail, HealthCheckV1Response healthCheckV1Response,
        DescribeFreeIpaResponse describeFreeIpaResponse) {
        healthCheckV1Response.getChecks().stream().forEach(check -> {
            if (check.getCheckId() == null) {
                throwError("Healthcheck>Check", "ID", nodeHealthDetail);
            }
            if (check.getPlugin() == null) {
                throwError("Healthcheck>Check", "Plugin", nodeHealthDetail);
            }
            if (check.getStatus() == null) {
                throwError("Healthcheck>Check", "Status", nodeHealthDetail);
            }
            if (!HEALTHY.equals(check.getStatus())) {
                throw new TestFailException(
                        String.format("%s check is not healthy on the freeIPA cluster: %s",
                            check.getPlugin(),
                                nodeHealthDetail
                        )
                );
            }
        });
        Set<String> checkIds = healthCheckV1Response.getChecks().stream()
                .map(e -> e.getCheckId())
                .collect(toSet());

        Set<String> missingIds = new HashSet<>();
        missingIds.addAll(CHECK_IDS);
        missingIds.addAll(CHECK_IDS_SERVICES);
        missingIds.addAll(ADDITIONAL_SERVICES_BY_OS.get(describeFreeIpaResponse.getImage().getOs().toLowerCase(Locale.ROOT)));
        missingIds.removeAll(
                CHECK_IDS.stream()
                    .filter(checkIds::contains)
                    .collect(toSet())
        );
        missingIds.removeAll(
                CHECK_IDS_SERVICES.stream()
                    .filter(e -> checkIds.contains(String.format("%s.service_loaded", e))
                            && checkIds.contains(String.format("%s.service_active", e))
                            && checkIds.contains(String.format("%s.service_sub", e)))
                    .collect(toSet())
        );
        missingIds.removeAll(
                ADDITIONAL_SERVICES_BY_OS.get(describeFreeIpaResponse.getImage().getOs().toLowerCase(Locale.ROOT)).stream()
                        .filter(e -> checkIds.contains(String.format("%s.service_loaded", e))
                                && checkIds.contains(String.format("%s.service_active", e))
                                && checkIds.contains(String.format("%s.service_sub", e)))
                        .collect(toSet())
        );
        if (!missingIds.isEmpty()) {
            throw new TestFailException(String.format("%s checks are missing from freeipa health: %s",
                    missingIds,
                    nodeHealthDetail)
            );
        }
    }

    private void assertStatus(NodeHealthDetails nodeHealthDetail, HealthCheckV1Response healthCheckV1Response,
        DescribeFreeIpaResponse describeFreeIpaResponse) {
        if (healthCheckV1Response.getStatus() == null) {
            throwError("Healthcheck", "Status", nodeHealthDetail);
        }
        if (!HEALTHY.equals(healthCheckV1Response.getStatus())) {
            throw new TestFailException(String.format("%s host is not healthy on the freeIPA cluster: %s",
                    healthCheckV1Response.getHost(),
                    nodeHealthDetail));
        }
    }

    private void assertHost(NodeHealthDetails nodeHealthDetail, HealthCheckV1Response healthCheckV1Response,
        DescribeFreeIpaResponse describeFreeIpaResponse) {
        if (healthCheckV1Response.getHost() == null) {
            throwError("Healthcheck", "Host", nodeHealthDetail);
        }
    }

    private void throwError(String objectPath, String objectName, NodeHealthDetails nodeHealthDetail) {
        throw new TestFailException(String.format("%s %s is null which is not enabled. " +
                "Please double check that health agent work properly for node: ", objectPath, objectName) + nodeHealthDetail);
    }
}
