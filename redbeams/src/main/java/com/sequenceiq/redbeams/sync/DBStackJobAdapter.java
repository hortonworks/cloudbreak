package com.sequenceiq.redbeams.sync;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.DBStackRepository;
import com.sequenceiq.statuschecker.model.JobResourceAdapter;

public class DBStackJobAdapter extends JobResourceAdapter<DBStack> {

    public DBStackJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public DBStackJobAdapter(DBStack resource) {
        super(resource);
    }

    @Override
    public String getLocalId() {
        return String.valueOf(getResource().getId());
    }

    @Override
    public String getRemoteResourceId() {
        return getResource().getResourceCrn().toString();
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return DBStackStatusSyncJob.class;
    }

    @Override
    public Class<? extends CrudRepository<DBStack, Long>> getRepositoryClassForResource() {
        return DBStackRepository.class;
    }
}
