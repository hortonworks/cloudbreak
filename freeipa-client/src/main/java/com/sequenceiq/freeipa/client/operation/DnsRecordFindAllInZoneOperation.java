package com.sequenceiq.freeipa.client.operation;

import static com.sequenceiq.freeipa.client.FreeIpaClient.UNLIMITED_PARAMS;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;

public class DnsRecordFindAllInZoneOperation extends AbstractFreeipaOperation<Object> {

    public static final String OPERATION_NAME = "dnsrecord_find";

    private final String zone;

    private DnsRecordFindAllInZoneOperation(String zone) {
        this.zone = zone;
    }

    public static DnsRecordFindAllInZoneOperation create(String zone) {
        Objects.requireNonNull(zone, "Zone cannot be null");
        return new DnsRecordFindAllInZoneOperation(zone);
    }

    @Override
    public String getOperationName() {
        return OPERATION_NAME;
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(zone);
    }

    @Override
    protected Map<String, Object> getParams() {
        return UNLIMITED_PARAMS;
    }

    @Override
    public Optional<Object> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        Object response = invoke(freeIpaClient, Object.class);
        return Optional.of(response);
    }
}
