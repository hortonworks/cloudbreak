package com.sequenceiq.freeipa.service.tag;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class TagConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagConfigService.class);

    public Map<String, SaltPillarProperties> createTagsPillarConfig(Stack stack) {
        if (stack.getTags() != null && isNotBlank(stack.getTags().getValue())) {
            try {
                StackTags stackTags = stack.getTags().get(StackTags.class);
                Map<String, Object> tags = new HashMap<>(stackTags.getDefaultTags());
                Map<String, Object> applicationTags = new HashMap<>(stackTags.getApplicationTags());
                tags.putAll(applicationTags);
                return Map.of("tags", new SaltPillarProperties("/tags/init.sls",
                        Collections.singletonMap("tags", tags)));
            } catch (Exception e) {
                LOGGER.debug("Exception during reading default tags.", e);
                return Map.of();
            }
        } else {
            return Map.of();
        }
    }
}
