package com.sequenceiq.cloudbreak.cm.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;

@Service
public class SMONMemoryWorkaroundService {

    private static final double GB_IN_BYTE = 1073741824;

    @Value("${cb.cm.smon.small.cluster.max.size:10}")
    private long smonSmallClusterMaxSize;

    @Value("${cb.cm.smon.medium.cluster.max.size:100}")
    private long smonMediumClusterMaxSize;

    @Value("${cb.cm.smon.large.cluster.max.size:500}")
    private long smonLargeClusterMaxSize;

    //Data lake heapsize
    @Value("${cb.cm.datalake.normal.smon.firehose.heapsize:2}")
    private long datalakeNormalFirehoseHeapsize;

    @Value("${cb.cm.datalake.extensive.smon.firehose.heapsize:2}")
    private long datalakeExtensiveFirehoseHeapsize;

    //Data lake nonJavaMemoryBytes
    @Value("${cb.cm.datalake.normal.smon.firehose.nonJavaMemoryBytes:2}")
    private long datalakeNormalFirehoseNonJavaMemoryBytes;

    @Value("${cb.cm.datalake.extensive.smon.firehose.nonJavaMemoryBytes:2}")
    private long datalakeExtensiveFirehoseNonJavaMemoryBytes;

    @Value("${cb.cm.datalake.extensive.smon.services:}")
    private Set<String> datalakeMemoryExtensiveServices;

    //Data hub heapsize
    @Value("${cb.cm.datahub.normal.smon.firehose.heapsize:2}")
    private long datahubNormalFirehoseHeapsize;

    @Value("${cb.cm.datahub.extensive.smon.firehose.heapsize:4}")
    private long datahubExtensiveFirehoseHeapsize;

    //Data hub nonJavaMemoryBytes
    @Value("${cb.cm.datahub.normal.smon.firehose.nonJavaMemoryBytes:2}")
    private long datahubNormalFirehoseNonJavaMemoryBytes;

    @Value("${cb.cm.datahub.extensive.smon.firehose.nonJavaMemoryBytes:6}")
    private long datahubExtensiveFirehoseNonJavaMemoryBytes;

    @Value("${cb.cm.datahub.extensive.smon.services:REGIONSERVER,STREAMS_MESSAGING_MANAGER_SERVER,KAFKA_BROKER,KUDU_MASTER}")
    private Set<String> datahubMemoryExtensiveServices;

    public String firehoseHeapsize(StackType stackType, Set<String> componentsByHostGroup) {
        double memoryInGig;
        if (isDataLake(stackType)) {
            memoryInGig = calculateSmonMemoryParameterBasedOnServices(
                    stackType,
                    componentsByHostGroup,
                    datalakeNormalFirehoseHeapsize,
                    datalakeExtensiveFirehoseHeapsize);
        } else {
            memoryInGig = calculateSmonMemoryParameterBasedOnServices(
                    stackType,
                    componentsByHostGroup,
                    datahubNormalFirehoseHeapsize,
                    datahubExtensiveFirehoseHeapsize);
        }
        return getMemoryInByte(memoryInGig);
    }

    public String firehoseNonJavaMemoryBytes(StackType stackType, Set<String> componentsByHostGroup, int numberOfNodes) {
        double memoryInGig;
        if (isDataLake(stackType)) {
            memoryInGig = calculateSmonMemoryParameterBasedOnServices(
                    stackType,
                    componentsByHostGroup,
                    datalakeNormalFirehoseNonJavaMemoryBytes,
                    datalakeExtensiveFirehoseNonJavaMemoryBytes);
        } else {
            memoryInGig = calculateSmonMemoryParameterBasedOnServices(
                    stackType,
                    componentsByHostGroup,
                    datahubNormalFirehoseNonJavaMemoryBytes,
                    datahubExtensiveFirehoseNonJavaMemoryBytes);
        }
        memoryInGig = getAtLeastMin(numberOfNodes, memoryInGig);
        return getMemoryInByte(memoryInGig);
    }

    //CHECKSTYLE:OFF: checkstyle:magicnumber
    private double getAtLeastMin(int numberOfNodes, double memoryInGig) {
        double minMemoryInGig;
        if (numberOfNodes <= smonSmallClusterMaxSize) {
            minMemoryInGig = 1;
        } else if (numberOfNodes <= smonMediumClusterMaxSize) {
            minMemoryInGig = 2;
        } else if (numberOfNodes <= smonLargeClusterMaxSize) {
            minMemoryInGig = 7;
        } else {
            minMemoryInGig = 11;
        }
        return Math.max(memoryInGig, minMemoryInGig);
    }
    //CHECKSTYLE:ON: checkstyle:magicnumber

    private double calculateSmonMemoryParameterBasedOnServices(
            StackType stackType,
            Set<String> componentsByHostGroup,
            double lowMemory,
            double highMemory) {
        double memoryInGig;
        if (possibleExtensiveMemoryUsage(stackType, componentsByHostGroup)) {
            memoryInGig = highMemory;
        } else {
            memoryInGig = lowMemory;
        }
        return memoryInGig;
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

    private String getMemoryInByte(double valueInGb) {
        return String.valueOf(Math.round(valueInGb * GB_IN_BYTE));
    }
}
