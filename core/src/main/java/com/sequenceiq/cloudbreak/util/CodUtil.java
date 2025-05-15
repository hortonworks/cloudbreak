package com.sequenceiq.cloudbreak.util;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.view.StackView;

public class CodUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodUtil.class);

    private static final String OPERATIONAL_DB = "OPERATIONAL_DB";

    private CodUtil() {

    }

    public static boolean isCodCluster(StackDto stack) {
        StackTags stackTags = getStackTags(stack.getStack());
        return isCodCluster(stackTags);
    }

    public static boolean isCodCluster(Stack stack) {
        StackTags stackTags = getStackTags(stack);
        return isCodCluster(stackTags);
    }

    private static boolean isCodCluster(StackTags stackTags) {
        if (stackTags != null) {
            String serviceType = stackTags.getApplicationTags().get(ClusterTemplateApplicationTag.SERVICE_TYPE.key());
            return OPERATIONAL_DB.equals(serviceType);
        }
        return false;
    }

    private static StackTags getStackTags(StackView stack) {
        if (stack.getTags() != null) {
            try {
                return stack.getTags().get(StackTags.class);
            } catch (IOException e) {
                LOGGER.warn("Stack related tags cannot be parsed.", e);
            }
        }
        return null;
    }
}
