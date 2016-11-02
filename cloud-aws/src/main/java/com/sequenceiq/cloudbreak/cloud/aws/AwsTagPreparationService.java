package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudformation.model.Tag;
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
        if (customCloudformationTags != null && customCloudformationTags.size() != 0) {
            customCloudformationTags.stream().filter(field -> !field.isEmpty()).forEach(field -> {
                String[] splittedField = field.split(":");
                customTags.put(splittedField[0], splittedField[1]);
            });
        }
    }

    public Collection<Tag> prepareTags(AuthenticatedContext ac) {
        List<com.amazonaws.services.cloudformation.model.Tag> tags = new ArrayList<>();
        tags.add(prepareTag(CLOUDBREAK_CLUSTER_TAG, ac.getCloudContext().getName()));
        if (!Strings.isNullOrEmpty(defaultCloudformationTag)) {
            tags.add(prepareTag(CLOUDBREAK_ID, defaultCloudformationTag));
        }
        tags.addAll(customTags.entrySet().stream().map(entry -> prepareTag(entry.getKey(), entry.getValue())).collect(Collectors.toList()));
        return tags;
    }

    private com.amazonaws.services.cloudformation.model.Tag prepareTag(String key, String value) {
        return new com.amazonaws.services.cloudformation.model.Tag().withKey(key).withValue(value);
    }

}
