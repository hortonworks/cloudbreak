package com.sequenceiq.cloudbreak.job.nodestatus;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;

@Component
public class NodeStatusCheckerJobInitializer extends AbstractStackJobInitializer {

    @Inject
    private NodeStatusCheckerJobService nodeStatusCheckerJobService;

    @Override
    public void initJobs() {
        getAliveJobResources()
                .forEach(s -> nodeStatusCheckerJobService.schedule(new NodeStatusJobAdapter(s)));
    }
}
