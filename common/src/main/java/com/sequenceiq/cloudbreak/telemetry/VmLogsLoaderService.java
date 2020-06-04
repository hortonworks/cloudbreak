package com.sequenceiq.cloudbreak.telemetry;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.telemetry.model.VmLogs;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class VmLogsLoaderService {

    public List<VmLogs> loadVmLogs(String filePath) throws IOException {
        String json = FileReaderUtils.readFileFromClasspath(filePath);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<>() {
        });
    }
}
