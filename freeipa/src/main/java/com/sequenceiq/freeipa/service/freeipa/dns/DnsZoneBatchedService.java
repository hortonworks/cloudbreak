package com.sequenceiq.freeipa.service.freeipa.dns;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.client.RpcBatchResult;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.operation.AbstractFreeipaOperation;
import com.sequenceiq.freeipa.client.operation.DnsRecordFindAllInZoneOperation;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.util.ThreadInterruptChecker;

@Service
public class DnsZoneBatchedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DnsZoneBatchedService.class);

    @Inject
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    @Inject
    private ThreadInterruptChecker interruptChecker;

    @Measure(DnsZoneBatchedService.class)
    public Map<String, Set<DnsRecord>> fetchDnsRecordsByZone(FreeIpaClient client, Set<String> zones) throws FreeIpaClientException, TimeoutException {
        List<RpcBatchResult<DnsRecord>> batchResults = fetchRecordsFromFreeIpa(client, zones);
        return transformResponseToMap(batchResults);
    }

    /**
     * As during a batch call, there is no proper connection between the requested zone and the returned responses, the NS record is used to identify
     * which zones records are received in that batch.
     * If there is not NS record, then the zone is ignored.
     * @param batchResults Results from FreeIPA
     * @return Map where key is zone ID
     */
    private Map<String, Set<DnsRecord>> transformResponseToMap(List<RpcBatchResult<DnsRecord>> batchResults) {
        Map<String, Set<DnsRecord>> dnsRecordsByZone = new HashMap<>();
        for (RpcBatchResult<DnsRecord> rpcBatchResults : batchResults) {
            Set<DnsRecord> zoneRecords = new HashSet<>(rpcBatchResults.getResult());
            zoneRecords.stream()
                    .filter(DnsRecord::isNsRecord)
                    .findFirst()
                    .flatMap(DnsRecord::calcZoneFromNsRecord)
                    .ifPresent(zone -> dnsRecordsByZone.put(zone, zoneRecords));
        }
        return dnsRecordsByZone;
    }

    /**
     * Creates operation for all zones to fetch all the DnsRecords for the zones.
     * These are gathered into batches based on partition size.
     * The result has to be flattened, as the batch call returns with a list of results, which contains also a list, etc.
     * @param client working FreeIPA client
     * @param zones fetch all records for these zones
     * @return flattened result of request
     * @throws FreeIpaClientException thrown when FreeIPA returns with error
     * @throws TimeoutException thrown when thread is interrupted
     */
    private List<RpcBatchResult<DnsRecord>> fetchRecordsFromFreeIpa(FreeIpaClient client, Set<String> zones) throws FreeIpaClientException, TimeoutException {
        List<Object> operations = zones.stream()
                .map(DnsRecordFindAllInZoneOperation::create)
                .map(AbstractFreeipaOperation::getOperationParamsForBatchCall)
                .collect(Collectors.toList());
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Integer partitionSize = batchPartitionSizeProperties.getByOperation(DnsRecordFindAllInZoneOperation.OPERATION_NAME);
        List<RPCResponse<DnsRecord>> rpcResponses = client.callBatchWithResult(warnings::put, operations, partitionSize, Set.of(FreeIpaErrorCodes.NOT_FOUND),
                () -> interruptChecker.throwTimeoutExIfInterrupted(), DnsRecord.class);
        if (!warnings.isEmpty()) {
            LOGGER.warn("Wasn't able to fetch every record: {}", warnings);
        }
        return rpcResponses.stream()
                .filter(Objects::nonNull)
                .map(RPCResponse::getResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .toList();
    }
}
