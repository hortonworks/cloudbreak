package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;

@Component
public class StackTagsToTagsV4ResponseConverter {

    public TagsV4Response convert(StackTags source) {
        TagsV4Response response = new TagsV4Response();
        response.setApplication(source.getApplicationTags());
        response.setDefaults(source.getDefaultTags());
        response.setUserDefined(source.getUserDefinedTags());
        return response;
    }

}
