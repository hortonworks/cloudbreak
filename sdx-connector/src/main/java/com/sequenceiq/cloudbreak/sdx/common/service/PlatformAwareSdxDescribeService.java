package com.sequenceiq.cloudbreak.sdx.common.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.model.ApiEndPoint;
import com.cloudera.api.swagger.model.ApiMapEntry;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;

public interface PlatformAwareSdxDescribeService extends PlatformAwareSdxCommonService {

    Logger LOGGER = LoggerFactory.getLogger(PlatformAwareSdxDescribeService.class);

    String HIVE_SVC_NAME = "hive";

    Optional<String> getRemoteDataContext(String crn);

    Set<String> listSdxCrns(String environmentCrn);

    Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn);

    Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn);

    default Map<String, String> getHmsServiceConfig(String crn) {
        try {
            Optional<String> remoteDataContext = getRemoteDataContext(crn);
            if (remoteDataContext.isPresent()) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setDefaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY));
                ApiRemoteDataContext apiRemoteDataContext = objectMapper.readValue(remoteDataContext.get(), ApiRemoteDataContext.class);
                Optional<ApiEndPoint> hiveApiEndpoint = apiRemoteDataContext.getEndPoints().stream()
                        .filter(apiEndPoint -> StringUtils.equals(apiEndPoint.getName(), HIVE_SVC_NAME)).findFirst();
                if (hiveApiEndpoint.isPresent()) {
                    return hiveApiEndpoint.get().getServiceConfigs().stream().collect(Collectors.toMap(ApiMapEntry::getKey, ApiMapEntry::getValue));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get HMS config: ", e);
        }
        return Map.of();
    }
}
