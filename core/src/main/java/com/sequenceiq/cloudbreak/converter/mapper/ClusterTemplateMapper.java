package com.sequenceiq.cloudbreak.converter.mapper;

import java.io.IOException;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;

@Mapper(componentModel = "spring")
public interface ClusterTemplateMapper {

    @Mappings({
            @Mapping(target = "owner", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "workspace", ignore = true),
            @Mapping(target = "status", expression = "java(com.sequenceiq.cloudbreak.api.model.ResourceStatus.USER_MANAGED)")
    })
    ClusterTemplate mapRequestToEntity(ClusterTemplateRequest clusterTemplateRequest);

    ClusterTemplateResponse mapEntityToResponse(ClusterTemplate clusterTemplate);

    Set<ClusterTemplateResponse> mapEntityToResponse(Set<ClusterTemplate> clusterTemplates);

    default Json mapStackV2RequestToJson(StackV2Request request) {
        try {
            return new Json(request);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Couldn't convert request to json", e);
        }
    }

    default StackV2Request mapJsonToStackV2Request(Json json) {
        try {
            return json.get(StackV2Request.class);
        } catch (IOException e) {
            throw new BadRequestException("Couldn't convert json to StackV2Request", e);
        }
    }
}
