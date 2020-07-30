package com.sequenceiq.cloudbreak.telemetry;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.telemetry.model.VmLog;

@Service
public class VmLogsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmLogsService.class);

    private static final String VM_LOGS_DESCRIPTOR_LOCATION = "defaults/vm-logs.json";

    private List<VmLog> vmLogs;

    @PostConstruct
    public void init() {
        try {
            this.vmLogs = loadVmLogs();
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

    private List<VmLog> loadVmLogs() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(VM_LOGS_DESCRIPTOR_LOCATION);
        if (classPathResource.exists()) {
            String json = FileReaderUtils.readFileFromClasspath(VM_LOGS_DESCRIPTOR_LOCATION);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<>() {
            });
        }
        return null;
    }
}
