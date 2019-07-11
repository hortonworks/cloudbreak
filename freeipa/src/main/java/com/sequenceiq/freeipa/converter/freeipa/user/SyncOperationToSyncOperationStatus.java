package com.sequenceiq.freeipa.converter.freeipa.user;

import java.io.IOException;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.entity.SyncOperation;

@Component
public class SyncOperationToSyncOperationStatus  implements Converter<SyncOperation, SyncOperationStatus> {

    private final TypeReference<List<SuccessDetails>> successListTypeReference = new TypeReference<>() { };

    private final TypeReference<List<FailureDetails>> failureListTypeReference = new TypeReference<>() { };

    @Override
    public SyncOperationStatus convert(SyncOperation source) {
        try {
            Json successListJson = source.getSuccessList();
            List<SuccessDetails> successList = successListJson == null ? List.of() : successListJson.get(successListTypeReference);
            Json failureListJson = source.getFailureList();
            List<FailureDetails> failureList = failureListJson == null ? List.of() : failureListJson.get(failureListTypeReference);
            return new SyncOperationStatus(
                    source.getOperationId(),
                    source.getSyncOperationType(),
                    source.getStatus(),
                    successList,
                    failureList,
                    source.getError(),
                    source.getStartTime(),
                    source.getEndTime()
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to convert SyncOperation to SyncOperationStatus", e);
        }
    }
}
