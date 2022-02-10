package com.sequenceiq.redbeams.sync;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.DBStackRepository;

public class DBStackJobAdapter extends JobResourceAdapter<DBStack> {

    public DBStackJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public DBStackJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return DBStackStatusSyncJob.class;
    }

    @Override
    public Class<? extends JobResourceRepository<DBStack, Long>> getRepositoryClassForResource() {
        return DBStackRepository.class;
    }
}
