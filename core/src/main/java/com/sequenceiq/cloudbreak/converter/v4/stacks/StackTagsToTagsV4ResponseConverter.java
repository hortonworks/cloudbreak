package com.sequenceiq.cloudbreak.converter.v4.stacks;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

public class StackTagsToTagsV4ResponseConverter extends AbstractConversionServiceAwareConverter<StackTags, TagsV4Response> {
    @Override
    public TagsV4Response convert(StackTags source) {
        TagsV4Response response = new TagsV4Response();
        response.setApplicationTags(source.getApplicationTags());
        response.setDefaultTags(source.getDefaultTags());
        response.setUserDefinedTags(source.getUserDefinedTags());
        return null;
    }
}
