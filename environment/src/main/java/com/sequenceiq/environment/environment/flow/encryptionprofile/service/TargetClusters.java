package com.sequenceiq.environment.environment.flow.encryptionprofile.service;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;

public record TargetClusters(List<StackViewV4Response> datalakes, List<StackViewV4Response> datahubs, List<String> datalakeFailures,
        List<String> datahubFailures) {

    public TargetClusters {
        datalakes = List.copyOf(datalakes);
        datahubs = List.copyOf(datahubs);
        datalakeFailures = List.copyOf(datalakeFailures);
        datahubFailures = List.copyOf(datahubFailures);
    }
}
