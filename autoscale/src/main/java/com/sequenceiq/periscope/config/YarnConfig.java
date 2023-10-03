package com.sequenceiq.periscope.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YarnConfig {

    @Value("${periscope.yarn.connection.timeout:2000}")
    private Integer connectionTimeOutMs;

    @Value("${periscope.yarn.read.timeout:9000}")
    private Integer readTimeOutMs;

    @Value("${periscope.yarn.core:8}")
    private Integer cpuCores;

    @Value("${periscope.yarn.memory:1}")
    private Long memoryInMb;

    public Integer getConnectionTimeOutMs() {
        return connectionTimeOutMs;
    }

    public Integer getReadTimeOutMs() {
        return readTimeOutMs;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public Long getMemoryInMb() {
        return memoryInMb;
    }
}
