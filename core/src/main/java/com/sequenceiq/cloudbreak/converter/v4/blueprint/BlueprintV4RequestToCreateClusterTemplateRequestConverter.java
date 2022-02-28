package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.datahub.model.CreateClusterTemplateRequest;
import com.cloudera.cdp.datahub.model.DatahubResourceTagRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;

@Component
public class BlueprintV4RequestToCreateClusterTemplateRequestConverter {

    public CreateClusterTemplateRequest convert(BlueprintV4Request source) {
        CreateClusterTemplateRequest createClusterTemplateRequest = new CreateClusterTemplateRequest();
        createClusterTemplateRequest.setClusterTemplateName(source.getName());
        createClusterTemplateRequest.setDescription(source.getDescription());
        createClusterTemplateRequest.setClusterTemplateContent(source.getBlueprint());
        createClusterTemplateRequest.setTags(convertTags(source));
        return createClusterTemplateRequest;
    }

    private List<DatahubResourceTagRequest> convertTags(BlueprintV4Request source) {
        return getIfNotNull(source.getTags(), t -> t.entrySet()
                .stream()
                .map(this::createDatahubResourceTagRequest).collect(Collectors.toList()));
    }

    private DatahubResourceTagRequest createDatahubResourceTagRequest(Entry<String, Object> e) {
        DatahubResourceTagRequest tag = new DatahubResourceTagRequest();
        tag.setKey(e.getKey());
        tag.setValue(e.getValue().toString());
        return tag;
    }
}
