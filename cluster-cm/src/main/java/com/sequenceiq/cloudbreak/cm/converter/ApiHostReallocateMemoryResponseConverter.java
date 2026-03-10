package com.sequenceiq.cloudbreak.cm.converter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.cloudera.api.swagger.model.ApiConfigRecord;
import com.cloudera.api.swagger.model.ApiHostReallocateMemoryResponse;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupRef;
import com.cloudera.api.swagger.model.ApiServiceRef;
import com.cloudera.api.swagger.model.AutoConfigApplicability;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.JvmConfigApplicability;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.JvmConfigRecord;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.ResetJvmParamsDiff;

public final class ApiHostReallocateMemoryResponseConverter {

    private ApiHostReallocateMemoryResponseConverter() {
    }

    public static ResetJvmParamsDiff convert(ApiHostReallocateMemoryResponse source) {
        if (source == null) {
            return new ResetJvmParamsDiff();
        }
        return new ResetJvmParamsDiff(
                convertConfigRecords(source.getConfigsBefore()),
                convertConfigRecords(source.getConfigsAfter()));
    }

    private static List<JvmConfigRecord> convertConfigRecords(List<ApiConfigRecord> records) {
        if (records == null) {
            return Collections.emptyList();
        }
        return records.stream()
                .map(ApiHostReallocateMemoryResponseConverter::convertConfigRecord)
                .collect(Collectors.toList());
    }

    private static JvmConfigRecord convertConfigRecord(ApiConfigRecord source) {
        return new JvmConfigRecord(
                source.getName(),
                source.getValue(),
                extractRoleConfigGroupName(source.getRcg()),
                extractClusterName(source.getService()),
                extractServiceName(source.getService()),
                convertApplicability(source.getApplicability()));
    }

    private static String extractRoleConfigGroupName(ApiRoleConfigGroupRef rcg) {
        return Optional.ofNullable(rcg)
                .map(ApiRoleConfigGroupRef::getRoleConfigGroupName)
                .orElse(null);
    }

    private static String extractClusterName(ApiServiceRef serviceRef) {
        return Optional.ofNullable(serviceRef)
                .map(ApiServiceRef::getClusterName)
                .orElse(null);
    }

    private static String extractServiceName(ApiServiceRef serviceRef) {
        return Optional.ofNullable(serviceRef)
                .map(ApiServiceRef::getServiceName)
                .orElse(null);
    }

    private static JvmConfigApplicability convertApplicability(AutoConfigApplicability applicability) {
        if (applicability == null) {
            return null;
        }
        return switch (applicability) {
            case RECONFIGURABLE -> JvmConfigApplicability.RECONFIGURABLE;
            case UNAFFECTED_DUE_TO_EQUAL_VALUE -> JvmConfigApplicability.UNAFFECTED_DUE_TO_EQUAL_VALUE;
            case UNAFFECTED_CONFIGURED_BY_USER -> JvmConfigApplicability.UNAFFECTED_CONFIGURED_BY_USER;
            default -> throw new IllegalStateException("Unexpected value: " + applicability);
        };
    }
}
