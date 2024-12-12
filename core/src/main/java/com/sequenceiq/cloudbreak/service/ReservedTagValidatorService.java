package com.sequenceiq.cloudbreak.service;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ReservedTagValidatorService {

    private static final String IS_COD_CLUSTER = "is_cod_cluster";

    public void validateInternalTags(Map<String, String> tags) {
        if (tags != null) {
            if (tags.containsKey(IS_COD_CLUSTER)) {
                throw new IllegalArgumentException(String.format("%s is a reserved tag. Please don't use it.", IS_COD_CLUSTER));
            }
        }
    }
}
