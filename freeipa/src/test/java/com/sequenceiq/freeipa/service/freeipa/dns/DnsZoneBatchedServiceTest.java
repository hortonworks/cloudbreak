package com.sequenceiq.freeipa.service.freeipa.dns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.client.RpcBatchResult;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.operation.DnsRecordFindAllInZoneOperation;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.util.ThreadInterruptChecker;

@ExtendWith(MockitoExtension.class)
public class DnsZoneBatchedServiceTest {

    private static final String ZONE1 = "example.com";

    private static final String ZONE2 = "test.org";

    private static final Integer PARTITION_SIZE = 10;

    @Mock
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    @Mock
    private ThreadInterruptChecker interruptChecker;

    @Mock
    private FreeIpaClient freeIpaClient;

    @InjectMocks
    private DnsZoneBatchedService underTest;

    @Test
    public void testFetchDnsRecordsByZoneSuccess() throws Exception {
        // GIVEN
        Set<String> zones = Set.of(ZONE1, ZONE2);

        DnsRecord nsRecord1 = createNsRecord(ZONE1);
        DnsRecord aRecord1 = createARecord("host1." + ZONE1, "192.168.1.1");
        DnsRecord nsRecord2 = createNsRecord(ZONE2);
        DnsRecord aRecord2 = createARecord("host2." + ZONE2, "192.168.1.2");


        RpcBatchResult<DnsRecord> batchResult1 = createBatchResult(List.of(nsRecord1, aRecord1));
        RpcBatchResult<DnsRecord> batchResult2 = createBatchResult(List.of(nsRecord2, aRecord2));

        RPCResponse<DnsRecord> rpcResponse1 = createRpcResponse(List.of(batchResult1));
        RPCResponse<DnsRecord> rpcResponse2 = createRpcResponse(List.of(batchResult2));

        when(batchPartitionSizeProperties.getByOperation(DnsRecordFindAllInZoneOperation.OPERATION_NAME))
                .thenReturn(PARTITION_SIZE);
        when(freeIpaClient.callBatchWithResult(any(), any(), eq(PARTITION_SIZE), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)),
                any(), any()))
                .thenReturn((List) List.of(rpcResponse1, rpcResponse2));

        // WHEN
        Map<String, Set<DnsRecord>> result = underTest.fetchDnsRecordsByZone(freeIpaClient, zones);

        // THEN
        assertEquals(2, result.size());
        assertTrue(result.containsKey(ZONE1 + "."));
        assertTrue(result.containsKey(ZONE2 + "."));
        assertEquals(2, result.get(ZONE1 + ".").size());
        assertEquals(2, result.get(ZONE2 + ".").size());
    }

    @Test
    public void testFetchDnsRecordsByZoneWithEmptyZones() throws Exception {
        // GIVEN
        Set<String> zones = Collections.emptySet();

        when(batchPartitionSizeProperties.getByOperation(DnsRecordFindAllInZoneOperation.OPERATION_NAME))
                .thenReturn(PARTITION_SIZE);
        when(freeIpaClient.callBatchWithResult(any(), any(), eq(PARTITION_SIZE), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)),
                any(), any()))
                .thenReturn(Collections.emptyList());

        // WHEN
        Map<String, Set<DnsRecord>> result = underTest.fetchDnsRecordsByZone(freeIpaClient, zones);

        // THEN
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFetchDnsRecordsByZoneWithNoNsRecord() throws Exception {
        // GIVEN
        Set<String> zones = Set.of(ZONE1);

        DnsRecord aRecord = createARecord("host1." + ZONE1, "192.168.1.1");
        RpcBatchResult<DnsRecord> batchResult = createBatchResult(List.of(aRecord));
        RPCResponse<DnsRecord> rpcResponse = createRpcResponse(List.of(batchResult));

        when(batchPartitionSizeProperties.getByOperation(DnsRecordFindAllInZoneOperation.OPERATION_NAME))
                .thenReturn(PARTITION_SIZE);
        when(freeIpaClient.callBatchWithResult(any(), any(), eq(PARTITION_SIZE), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)),
                any(), any()))
                .thenReturn((List) Arrays.asList(rpcResponse));

        // WHEN
        Map<String, Set<DnsRecord>> result = underTest.fetchDnsRecordsByZone(freeIpaClient, zones);

        // THEN
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFetchDnsRecordsByZoneWithNullResponses() throws Exception {
        // GIVEN
        Set<String> zones = Set.of(ZONE1);

        when(batchPartitionSizeProperties.getByOperation(DnsRecordFindAllInZoneOperation.OPERATION_NAME))
                .thenReturn(PARTITION_SIZE);
        when(freeIpaClient.callBatchWithResult(any(), any(), eq(PARTITION_SIZE), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)),
                any(), any()))
                .thenReturn((List) Arrays.asList(null, createRpcResponse(null)));

        // WHEN
        Map<String, Set<DnsRecord>> result = underTest.fetchDnsRecordsByZone(freeIpaClient, zones);

        // THEN
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFetchDnsRecordsByZoneThrowsFreeIpaClientException() throws Exception {
        // GIVEN
        Set<String> zones = Set.of(ZONE1);
        FreeIpaClientException exception = new FreeIpaClientException("Test exception");

        when(batchPartitionSizeProperties.getByOperation(DnsRecordFindAllInZoneOperation.OPERATION_NAME))
                .thenReturn(PARTITION_SIZE);
        when(freeIpaClient.callBatchWithResult(any(), any(), eq(PARTITION_SIZE), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)),
                any(), any()))
                .thenThrow(exception);

        // WHEN & THEN
        assertThrows(FreeIpaClientException.class, () -> underTest.fetchDnsRecordsByZone(freeIpaClient, zones));
    }

    @Test
    public void testFetchDnsRecordsByZoneThrowsTimeoutException() throws Exception {
        // GIVEN
        Set<String> zones = Set.of(ZONE1);

        when(batchPartitionSizeProperties.getByOperation(DnsRecordFindAllInZoneOperation.OPERATION_NAME))
                .thenReturn(PARTITION_SIZE);
        when(freeIpaClient.callBatchWithResult(any(), any(), eq(PARTITION_SIZE), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)),
                any(), any()))
                .thenThrow(new TimeoutException("Thread interrupted"));

        // WHEN & THEN
        assertThrows(TimeoutException.class, () -> underTest.fetchDnsRecordsByZone(freeIpaClient, zones));
    }

    @Test
    public void testFetchDnsRecordsByZoneWithWarnings() throws Exception {
        // GIVEN
        Set<String> zones = Set.of(ZONE1);

        DnsRecord nsRecord = createNsRecord(ZONE1);
        RpcBatchResult<DnsRecord> batchResult = createBatchResult(List.of(nsRecord));
        RPCResponse<DnsRecord> rpcResponse = createRpcResponse(List.of(batchResult));

        when(batchPartitionSizeProperties.getByOperation(DnsRecordFindAllInZoneOperation.OPERATION_NAME))
                .thenReturn(PARTITION_SIZE);
        when(freeIpaClient.callBatchWithResult(any(), any(), eq(PARTITION_SIZE), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)),
                any(), any()))
                .thenAnswer(invocation -> {
                    // Simulate warnings being added
                    Object warningsObj = invocation.getArgument(0);
                    if (warningsObj instanceof BiConsumer) {
                        ((BiConsumer<String, String>) warningsObj).accept("zone1", "warning1");
                    }
                    return List.of(rpcResponse);
                });

        // WHEN
        Map<String, Set<DnsRecord>> result = underTest.fetchDnsRecordsByZone(freeIpaClient, zones);

        // THEN
        assertEquals(1, result.size());
        assertTrue(result.containsKey(ZONE1 + "."));
    }

    @Test
    public void testFetchDnsRecordsByZoneWithMixedResults() throws Exception {
        // GIVEN
        Set<String> zones = Set.of(ZONE1, ZONE2);

        // Zone1 has NS record, Zone2 doesn't
        DnsRecord nsRecord1 = createNsRecord(ZONE1);
        DnsRecord aRecord1 = createARecord("host1." + ZONE1, "192.168.1.1");
        DnsRecord aRecord2 = createARecord("host2." + ZONE2, "192.168.1.2");

        RpcBatchResult<DnsRecord> batchResult1 = createBatchResult(List.of(nsRecord1, aRecord1));
        RpcBatchResult<DnsRecord> batchResult2 = createBatchResult(List.of(aRecord2));

        RPCResponse<DnsRecord> rpcResponse1 = createRpcResponse(List.of(batchResult1));
        RPCResponse<DnsRecord> rpcResponse2 = createRpcResponse(List.of(batchResult2));

        when(batchPartitionSizeProperties.getByOperation(DnsRecordFindAllInZoneOperation.OPERATION_NAME))
                .thenReturn(PARTITION_SIZE);
        when(freeIpaClient.callBatchWithResult(any(), any(), eq(PARTITION_SIZE), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)),
                any(), any()))
                .thenReturn((List) List.of(rpcResponse1, rpcResponse2));

        // WHEN
        Map<String, Set<DnsRecord>> result = underTest.fetchDnsRecordsByZone(freeIpaClient, zones);

        // THEN
        assertEquals(1, result.size());
        assertTrue(result.containsKey(ZONE1 + "."));
        assertEquals(2, result.get(ZONE1 + ".").size());
    }

    private DnsRecord createNsRecord(String zone) {
        DnsRecord nsRecord = new DnsRecord();
        nsRecord.setIdnsname("@");
        nsRecord.setNsrecord(List.of("ns1." + zone + "."));
        nsRecord.setDn("idnsname=" + zone + ".,cn=dns,dc=example,dc=com");
        return nsRecord;
    }

    private DnsRecord createARecord(String name, String ip) {
        DnsRecord aRecord = new DnsRecord();
        aRecord.setIdnsname(name);
        aRecord.setArecord(List.of(ip));
        return aRecord;
    }

    private RpcBatchResult<DnsRecord> createBatchResult(List<DnsRecord> records) {
        RpcBatchResult<DnsRecord> batchResult = new RpcBatchResult<>();
        batchResult.setResult(records);
        return batchResult;
    }

    private RPCResponse<DnsRecord> createRpcResponse(List<RpcBatchResult<DnsRecord>> results) {
        RPCResponse<DnsRecord> rpcResponse = new RPCResponse<>();
        rpcResponse.setResults(results);
        return rpcResponse;
    }
}