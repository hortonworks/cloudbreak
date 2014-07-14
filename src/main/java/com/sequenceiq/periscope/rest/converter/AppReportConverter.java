package com.sequenceiq.periscope.rest.converter;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReport;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.rest.json.AppReportJson;

@Component
public class AppReportConverter extends AbstractConverter<AppReportJson, ApplicationReport> {

    @Override
    public AppReportJson convert(ApplicationReport source) {
        AppReportJson json = new AppReportJson();
        json.setAppId(source.getApplicationId().toString());
        json.setStart(source.getStartTime());
        json.setFinish(source.getFinishTime());
        json.setProgress(source.getProgress());
        json.setQueue(source.getQueue());
        json.setUrl(source.getTrackingUrl());
        json.setUser(source.getUser());
        json.setState(source.getYarnApplicationState().name());
        ApplicationResourceUsageReport usageReport = source.getApplicationResourceUsageReport();
        json.setReservedContainers(usageReport.getNumReservedContainers());
        json.setUsedContainers(usageReport.getNumUsedContainers());
        json.setUsedMemory(usageReport.getUsedResources().getMemory());
        json.setUsedVCores(usageReport.getUsedResources().getVirtualCores());
        return json;
    }

}
