package com.sequenceiq.cloudbreak.telemetry;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.common.api.telemetry.model.VmLog;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class VmLogsLoaderService {

    public List<VmLog> loadVmLogs(String filePath) throws IOException {
        String json = FileReaderUtils.readFileFromClasspath(filePath);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<>() {
        });
    }
}
