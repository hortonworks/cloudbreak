package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.common.api.tag.response.TagsResponse;

@Component
public class StackTagsToTagsV4ResponseConverter extends AbstractConversionServiceAwareConverter<StackTags, TagsV4Response> {

    @Override
    public TagsV4Response convert(StackTags source) {
        TagsV4Response response = new TagsV4Response();
        response.setApplication(new TagsResponse(source.getApplicationTags()));
        response.setDefaults(new TagsResponse(source.getDefaultTags()));
        response.setUserDefined(new TagsResponse(source.getUserDefinedTags()));
        return response;
    }

}
