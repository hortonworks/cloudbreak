package com.sequenceiq.cloudbreak.telemetry.converter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.telemetry.model.VmLog;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;

@Component
public class VmLogsToVmLogsResponseConverter {

    public VmLogsResponse convert(List<VmLog> vmLogs) {
        VmLogsResponse result = new VmLogsResponse();
        result.setLogs(Optional.ofNullable(vmLogs).orElse(List.of()).stream()
                .map(logs -> {
                    VmLog log = new VmLog();
                    log.setName(logs.getName());
                    log.setPath(logs.getPath());
                    log.setType(logs.getType());
                    log.setLabel(logs.getLabel());
                    log.setExcludes(logs.getExcludes());
                    return log;
                }).collect(Collectors.toList()));
        return result;
    }
}
