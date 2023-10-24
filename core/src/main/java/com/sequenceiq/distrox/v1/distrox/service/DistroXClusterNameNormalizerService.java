package com.sequenceiq.distrox.v1.distrox.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;

@Service
public class DistroXClusterNameNormalizerService {

    private static final Pattern APPENDED_TIMESTAMP_REGEX = Pattern.compile("(.*)_(\\d{13})$");

    public void removeDeletedTimeStampFromName(StackViewV4Response stackViewV4Response) {
        if (Status.DELETE_COMPLETED == stackViewV4Response.getStatus()) {
            String clusterName = stackViewV4Response.getName();
            Matcher matcher = APPENDED_TIMESTAMP_REGEX.matcher(clusterName);
            if (matcher.matches()) {
                stackViewV4Response.setName(matcher.group(1));
                stackViewV4Response.getCluster().setName(matcher.group(1));
            }
        }
    }
}
