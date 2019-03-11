package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Service
public class AwsTagPreparationService {

    private static final String CLOUDBREAK_ID = "CloudbreakId";

    private static final String CLOUDBREAK_CLUSTER_TAG = "CloudbreakClusterName";

    @Value("${cb.aws.default.cf.tag:}")
    private String defaultCloudformationTag;

    @Value("#{'${cb.aws.custom.cf.tags:}'.split(',')}")
    private List<String> customCloudformationTags;

    private Map<String, String> customTags = new HashMap<>();

    @PostConstruct
    public void init() {
        customTags = new HashMap<>();
        if (customCloudformationTags != null && !customCloudformationTags.isEmpty()) {
            customCloudformationTags.stream().filter(field -> !field.isEmpty()).forEach(field -> {
                String[] splittedField = field.split(":");
                customTags.put(splittedField[0], splittedField[1]);
            });
        }
    }

    public Collection<com.amazonaws.services.cloudformation.model.Tag> prepareCloudformationTags(AuthenticatedContext ac, Map<String, String> userDefinedTags) {
        Collection<com.amazonaws.services.cloudformation.model.Tag> tags = new ArrayList<>();
        tags.add(prepareCloudformationTag(CLOUDBREAK_CLUSTER_TAG, ac.getCloudContext().getName()));
        if (!Strings.isNullOrEmpty(defaultCloudformationTag)) {
            tags.add(prepareCloudformationTag(CLOUDBREAK_ID, defaultCloudformationTag));
        }
        tags.addAll(Stream.concat(customTags.entrySet().stream(), userDefinedTags.entrySet().stream())
                .map(entry -> prepareCloudformationTag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        return tags;
    }

    public Collection<com.amazonaws.services.ec2.model.Tag> prepareEc2Tags(AuthenticatedContext ac, Map<String, String> userDefinedTags) {
        Collection<com.amazonaws.services.ec2.model.Tag> tags = new ArrayList<>();
        tags.add(prepareEc2Tag(CLOUDBREAK_CLUSTER_TAG, ac.getCloudContext().getName()));
        if (!Strings.isNullOrEmpty(defaultCloudformationTag)) {
            tags.add(prepareEc2Tag(CLOUDBREAK_ID, defaultCloudformationTag));
        }
        tags.addAll(Stream.concat(customTags.entrySet().stream(), userDefinedTags.entrySet().stream())
                .map(entry -> prepareEc2Tag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        return tags;
    }

    private com.amazonaws.services.cloudformation.model.Tag prepareCloudformationTag(String key, String value) {
        return new com.amazonaws.services.cloudformation.model.Tag().withKey(key).withValue(value);
    }

    private com.amazonaws.services.ec2.model.Tag prepareEc2Tag(String key, String value) {
        return new com.amazonaws.services.ec2.model.Tag().withKey(key).withValue(value);
    }
}
