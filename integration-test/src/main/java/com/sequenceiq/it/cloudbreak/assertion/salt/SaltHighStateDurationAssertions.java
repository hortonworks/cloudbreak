package com.sequenceiq.it.cloudbreak.assertion.salt;

import static java.lang.String.format;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.salt.SaltHighstateReport;
import com.sequenceiq.it.cloudbreak.salt.SaltStateReport;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshSaltExecutionMetricsActions;

@Component
public class SaltHighStateDurationAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltHighStateDurationAssertions.class);

    @Inject
    private SshSaltExecutionMetricsActions saltMetrics;

    public FreeIpaTestDto saltHighStateDurationLimits(TestContext testContext, FreeIpaTestDto freeIpaTestDto, FreeIpaClient freeIpaClient) {
        Map<String, Double> fetchedFreeIpaSaltDurations = fetchSaltDurations("freeipa");
        List<SaltHighstateReport> freeIpaSaltHighstateReports = saltMetrics.getSaltExecutionMetrics(freeIpaTestDto.getEnvironmentCrn(),
                freeIpaTestDto.getName(), freeIpaClient, "freeipa", testContext);
        Table<String, String, Double> saltStatesTotalDurations = validateSaltStatesTotalDurations(freeIpaSaltHighstateReports, fetchedFreeIpaSaltDurations);
        saltMetrics.writeSaltStatesTotalDurationsReportsToFiles(testContext, "freeipa", saltStatesTotalDurations);
        return freeIpaTestDto;
    }

    public SdxTestDto saltHighStateDurationLimits(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        Map<String, Double> fetchedSdxSaltDurations = fetchSaltDurations("sdx");
        List<SaltHighstateReport> sdxSaltHighstateReports = saltMetrics.getSaltExecutionMetrics(sdxTestDto.getResponse().getEnvironmentCrn(),
                sdxTestDto.getName(), sdxClient, "sdx", testContext);
        Table<String, String, Double> saltStatesTotalDurations = validateSaltStatesTotalDurations(sdxSaltHighstateReports, fetchedSdxSaltDurations);
        saltMetrics.writeSaltStatesTotalDurationsReportsToFiles(testContext, "sdx", saltStatesTotalDurations);
        return sdxTestDto;
    }

    public DistroXTestDto saltHighStateDurationLimits(TestContext testContext, DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        Map<String, Double> fetchedDistroxSaltDurations = fetchSaltDurations("distrox");
        List<SaltHighstateReport> distroxSaltHighstateReports = saltMetrics.getSaltExecutionMetrics(distroXTestDto.getResponse().getEnvironmentCrn(),
                distroXTestDto.getName(), cloudbreakClient, "distrox", testContext);
        Table<String, String, Double> saltStatesTotalDurations = validateSaltStatesTotalDurations(distroxSaltHighstateReports, fetchedDistroxSaltDurations);
        saltMetrics.writeSaltStatesTotalDurationsReportsToFiles(testContext, "distrox", saltStatesTotalDurations);
        return distroXTestDto;
    }

    private Map<String, Double> fetchSaltDurations(String serviceName) {
        try {
            switch (serviceName) {
                case "freeipa":
                    return JsonUtil.readValue(
                            FileReaderUtils.readFileFromClasspathQuietly("salt/freeipa_state_duration.json"),
                            new TypeReference<>() {
                            });
                case "sdx":
                    return JsonUtil.readValue(
                            FileReaderUtils.readFileFromClasspathQuietly("salt/sdx_state_duration.json"),
                            new TypeReference<>() {
                            });
                case "distrox":
                    return JsonUtil.readValue(
                            FileReaderUtils.readFileFromClasspathQuietly("salt/distrox_state_duration.json"),
                            new TypeReference<>() {
                            });
                default:
                    throw new TestFailException(format("Provided service '%s' is not available for fetching SaltHighstateReports", serviceName));
            }
        } catch (IOException e) {
            throw new TestFailException("Cannot find or open Salt state properties from classpath, because of: ", e);
        }
    }

    private Table<String, String, Double> validateSaltStatesTotalDurations(List<SaltHighstateReport> saltHighstateReports,
            Map<String, Double> fetchedSaltDurations) {
        Table<String, String, Double> saltStatesTotalDurations = Tables.synchronizedTable(HashBasedTable.create());
        if (CollectionUtils.isNotEmpty(saltHighstateReports)) {
            LOGGER.info("Number of Salt High State reports: {}", saltHighstateReports.size());
            for (SaltHighstateReport saltHighstateReport : saltHighstateReports) {
                String jobID = saltHighstateReport.getJid();
                Map<String, List<SaltStateReport>> saltStateReportByInstance = saltHighstateReport.getInstances();
                LOGGER.info("Generating the state duration reports for '{}' job!", jobID);
                for (Entry<String, List<SaltStateReport>> saltStateReportEntry : saltStateReportByInstance.entrySet()) {
                    String instance = saltStateReportEntry.getKey();
                    List<SaltStateReport> stateReports = saltStateReportEntry.getValue();
                    stateReports.forEach(stateReport -> {
                        String instanceKey = StringUtils.join(List.of(jobID, instance), ".");
                        saltStatesTotalDurations.put(instanceKey, stateReport.getState(), stateReport.getTotalDuration());

                        Double duration = fetchedSaltDurations.entrySet().stream()
                                .filter(entry -> StringUtils.equalsIgnoreCase(stateReport.getState(), entry.getKey()))
                                .map(Entry::getValue)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(0.000001d);
                        if (duration.compareTo(0.000001d) != 0 && stateReport.getTotalDuration() > duration) {
                            Log.warn(LOGGER, format(" The duration '%.2f' of instance state ('%s'::'%s') exceeds the limit '%.2f'! ",
                                    stateReport.getTotalDuration(), instanceKey, stateReport.getState(), duration));
                        }
                    });
                }
            }
            LOGGER.info("Total durations for Salt states by job: {}", saltStatesTotalDurations);
        } else {
            Log.warn(LOGGER, " Salt High State reports are missing! Salt states' duration validation is not possible right now! ");
        }
        LOGGER.info("Total durations for Salt states by job: {}", saltStatesTotalDurations);
        return saltStatesTotalDurations;
    }
}
