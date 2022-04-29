package com.sequenceiq.consumption.job.storage;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.consumption.configuration.repository.ConsumptionRepository;
import com.sequenceiq.consumption.domain.Consumption;

public class StorageConsumptionJobAdapter extends JobResourceAdapter<Consumption> {

    public StorageConsumptionJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public StorageConsumptionJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return StorageConsumptionJob.class;
    }

    @Override
    public Class<? extends JobResourceRepository<Consumption, Long>> getRepositoryClassForResource() {
        return ConsumptionRepository.class;
    }
}
