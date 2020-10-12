package com.sequenceiq.cloudbreak.cm.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;

@Service
public class SMONMemoryWorkaroundService {

    private static final long GB_IN_BYTE = 1073741824;

    //Data lake heapsize
    @Value("${cb.cm.datalake.normal.smon.firehose.heapsize:}")
    private long datalakeNormalFirehoseHeapsize;

    @Value("${cb.cm.datalake.extensive.smon.firehose.heapsize:}")
    private long datalakeExtensiveFirehoseHeapsize;

    //Data lake nonJavaMemoryBytes
    @Value("${cb.cm.datalake.normal.smon.firehose.nonJavaMemoryBytes:}")
    private long datalakeNormalFirehoseNonJavaMemoryBytes;

    @Value("${cb.cm.datalake.extensive.smon.firehose.nonJavaMemoryBytes:}")
    private long datalakeExtensiveFirehoseNonJavaMemoryBytes;

    @Value("${cb.cm.datalake.extensive.smon.services:}")
    private Set<String> datalakeMemoryExtensiveServices;

    //Data hub heapsize
    @Value("${cb.cm.datahub.normal.smon.firehose.heapsize:}")
    private long datahubNormalFirehoseHeapsize;

    @Value("${cb.cm.datahub.extensive.smon.firehose.heapsize:}")
    private long datahubExtensiveFirehoseHeapsize;

    //Data hub nonJavaMemoryBytes
    @Value("${cb.cm.datahub.normal.smon.firehose.nonJavaMemoryBytes:}")
    private long datahubNormalFirehoseNonJavaMemoryBytes;

    @Value("${cb.cm.datahub.extensive.smon.firehose.nonJavaMemoryBytes:}")
    private long datahubExtensiveFirehoseNonJavaMemoryBytes;

    @Value("${cb.cm.datahub.extensive.smon.services:}")
    private Set<String> datahubMemoryExtensiveServices;

    public String firehoseHeapsize(StackType stackType, Set<String> componentsByHostGroup) {
        if (isDataLake(stackType)) {
            return calculateSmonMemoryParameterBasedOnServices(
                    stackType,
                    componentsByHostGroup,
                    datalakeNormalFirehoseHeapsize,
                    datalakeExtensiveFirehoseHeapsize);
        } else {
            return calculateSmonMemoryParameterBasedOnServices(
                    stackType,
                    componentsByHostGroup,
                    datahubNormalFirehoseHeapsize,
                    datahubExtensiveFirehoseHeapsize);
        }
    }

    public String firehoseNonJavaMemoryBytes(StackType stackType, Set<String> componentsByHostGroup) {
        if (isDataLake(stackType)) {
            return calculateSmonMemoryParameterBasedOnServices(
                    stackType,
                    componentsByHostGroup,
                    datalakeNormalFirehoseNonJavaMemoryBytes,
                    datalakeExtensiveFirehoseNonJavaMemoryBytes);
        } else {
            return calculateSmonMemoryParameterBasedOnServices(
                    stackType,
                    componentsByHostGroup,
                    datahubNormalFirehoseNonJavaMemoryBytes,
                    datahubExtensiveFirehoseNonJavaMemoryBytes);
        }
    }

    private String calculateSmonMemoryParameterBasedOnServices(
            StackType stackType,
            Set<String> componentsByHostGroup,
            long lowMemory,
            long highMemory) {
        if (possibleExtensiveMemoryUsage(stackType, componentsByHostGroup)) {
            return getMemoryInByte(highMemory);
        } else {
            return getMemoryInByte(lowMemory);
        }
    }

    private boolean possibleExtensiveMemoryUsage(StackType stackType, Set<String> componentsByHostGroup) {
        if (isDataLake(stackType)) {
            return containsMemoryExtensiveService(datalakeMemoryExtensiveServices, componentsByHostGroup);
        } else {
            return containsMemoryExtensiveService(datahubMemoryExtensiveServices, componentsByHostGroup);
        }
    }

    private boolean containsMemoryExtensiveService(Set<String> memoryExtensiveServices, Set<String> components) {
        Set<String> collect = components
                .stream()
                .filter(e -> memoryExtensiveServices.contains(e))
                .collect(Collectors.toSet());
        return !collect.isEmpty();
    }

    private boolean isDataLake(StackType stackType) {
        return stackType.equals(StackType.DATALAKE);
    }

    private String getMemoryInByte(long valueInGb) {
        return String.valueOf(valueInGb * GB_IN_BYTE);
    }
}
