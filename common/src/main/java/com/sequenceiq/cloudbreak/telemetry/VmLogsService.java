package com.sequenceiq.cloudbreak.telemetry;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.telemetry.model.VmLog;

@Service
public class VmLogsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmLogsService.class);

    private static final String VM_LOGS_DESCRIPTOR_LOCATION = "defaults/vm-logs.json";

    private final VmLogsLoaderService vmLogsLoaderService;

    private List<VmLog> vmLogs;

    public VmLogsService(VmLogsLoaderService vmLogsLoaderService) {
        this.vmLogsLoaderService = vmLogsLoaderService;
    }

    @PostConstruct
    public void init() {
        try {
            this.vmLogs = vmLogsLoaderService.loadVmLogs(VM_LOGS_DESCRIPTOR_LOCATION);
        } catch (IOException e) {
            LOGGER.warn("Static VM Log descriptors could not be initialized!", e);
        }
    }

    public List<VmLog> getVmLogs() {
        if (this.vmLogs == null) {
            init();
        }
        return this.vmLogs;
    }
}
