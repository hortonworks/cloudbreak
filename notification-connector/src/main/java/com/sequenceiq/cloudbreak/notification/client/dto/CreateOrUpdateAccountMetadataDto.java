package com.sequenceiq.cloudbreak.notification.client.dto;

import java.util.ArrayList;
import java.util.List;

public record CreateOrUpdateAccountMetadataDto(
        String accountId,
        List<String> allowedDomains
) {
    public CreateOrUpdateAccountMetadataDto() {
        this(null, new ArrayList<>());
    }
}