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
import com.sequenceiq.cloudbreak.domain.Secret;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;

@Mapper(componentModel = "spring")
public abstract class ClusterTemplateMapper {

    @Mappings({
            @Mapping(target = "owner", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "workspace", ignore = true),
            @Mapping(target = "status", expression = "java(com.sequenceiq.cloudbreak.api.model.ResourceStatus.USER_MANAGED)"),
            @Mapping(target = "template",
                    expression = "java(new com.sequenceiq.cloudbreak.domain.Secret(mapStackV2RequestToJson(clusterTemplateRequest.getTemplate())))")
    })
    public abstract ClusterTemplate mapRequestToEntity(ClusterTemplateRequest clusterTemplateRequest);

    public abstract ClusterTemplateResponse mapEntityToResponse(ClusterTemplate clusterTemplate);

    public abstract Set<ClusterTemplateResponse> mapEntityToResponse(Set<ClusterTemplate> clusterTemplates);

    public String mapStackV2RequestToJson(StackV2Request request) {
        try {
            return new Json(request).getValue();
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Couldn't convert request to json", e);
        }
    }

    public StackV2Request mapJsonToStackV2Request(Secret templateSecret) {
        try {
            String templateContent = templateSecret.getRaw();
            return new Json(templateContent).get(StackV2Request.class);
        } catch (IOException e) {
            throw new BadRequestException("Couldn't convert json to StackV2Request", e);
        }
    }
}
