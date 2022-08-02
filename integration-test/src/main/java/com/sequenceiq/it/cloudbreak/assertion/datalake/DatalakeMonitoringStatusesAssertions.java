package com.sequenceiq.it.cloudbreak.assertion.datalake;

import static java.lang.String.format;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.common.api.command.RemoteCommandsExecutionResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackRemoteTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DatalakeMonitoringStatusesAssertions implements Assertion<StackRemoteTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeMonitoringStatusesAssertions.class);

    private final List<String> acceptableNokNames;

    public DatalakeMonitoringStatusesAssertions(List<String> acceptableNokNames) {
        this.acceptableNokNames = acceptableNokNames;
    }

    @Override
    public StackRemoteTestDto doAssertion(TestContext testContext, StackRemoteTestDto stackRemoteTestDto, CloudbreakClient cloudbreakClient)
            throws Exception {
        RemoteCommandsExecutionResponse response = stackRemoteTestDto.getResponse();

        for (Map.Entry<String, String> monitoringStatusReport : response.getResults().entrySet()) {
            try {
                List<String> statusCategories = List.of("services", "scrapping", "metrics");
                Map<String, Map<String, String>> fetchedMonitoringStatus = JsonUtil.readValue(monitoringStatusReport.getValue(),
                        new TypeReference<Map<String, Map<String, String>>>() { });
                statusCategories.forEach(statusCategory -> {
                    Map<String, String> statusesNotOkInCategory = fetchedMonitoringStatus.entrySet().stream()
                            .filter(categories -> statusCategory.equalsIgnoreCase(categories.getKey()))
                            .flatMap(selectedCategory -> selectedCategory.getValue().entrySet().stream()
                                    .filter(servicesInCategory -> "NOK".equalsIgnoreCase(servicesInCategory.getValue())))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    if (MapUtils.isNotEmpty(statusesNotOkInCategory)) {
                        Log.log(LOGGER, format(" Found 'Not OK' Monitoring statuses '%s' at '%s' instance. " +
                                "However this is acceptable! ", statusesNotOkInCategory, monitoringStatusReport.getKey()));
                        statusesNotOkInCategory.forEach((service, status) -> {
                            if (acceptableNokNames.stream()
                                    .anyMatch(nokAccepted -> nokAccepted.equalsIgnoreCase(service))) {
                                Log.log(LOGGER, format(" Found Monitoring '%s' where %s' is 'Not OK' at '%s' instance. " +
                                        "However this is acceptable! ", statusCategory.toUpperCase(), service, monitoringStatusReport.getKey()));
                            } else {
                                Log.error(LOGGER, format(" Found Monitoring '%s' where '%s' is 'Not OK' at '%s' instance! ", statusCategory.toUpperCase(),
                                        service, monitoringStatusReport.getKey()));
                                throw new TestFailException(format("Found Monitoring '%s' where '%s' is 'Not OK' at '%s' instance!",
                                        statusCategory.toUpperCase(), service, monitoringStatusReport.getKey()));
                            }
                        });
                    }
                });
            } catch (IOException | IllegalStateException e) {
                Log.error(LOGGER, " Cannot parse Common Monitoring Status Report JSON! ");
                throw new TestFailException("Cannot parse Common Monitoring Status Report JSON: ", e);
            }
        }
        return stackRemoteTestDto;
    }
}
