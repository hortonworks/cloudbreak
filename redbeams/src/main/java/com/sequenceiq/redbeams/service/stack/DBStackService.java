package com.sequenceiq.redbeams.service.stack;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.DBStackRepository;

@Service
public class DBStackService implements PayloadContextProvider, ResourceIdProvider {

    @Inject
    private DBStackRepository dbStackRepository;

    public DBStack getById(Long id) {
        return dbStackRepository.findById(id).orElseThrow(() -> new NotFoundException(String.format("Stack [%s] not found", id)));
    }

    public Optional<DBStack> findById(Long id) {
        return dbStackRepository.findById(id);
    }

    public DBStack getByNameAndEnvironmentCrn(String name, String environmentCrn) {
        return findByNameAndEnvironmentCrn(name, environmentCrn)
            .orElseThrow(() -> new NotFoundException(String.format("Stack [%s] in environment [%s] not found", name, environmentCrn)));
    }

    public Optional<DBStack> findByNameAndEnvironmentCrn(String name, String environmentCrn) {
        return dbStackRepository.findByNameAndEnvironmentId(name, environmentCrn);
    }

    public DBStack getByCrn(String crn) {
        return dbStackRepository.findByResourceCrn(crn)
                .orElseThrow(() -> new NotFoundException(String.format("Stack with crn [%s] not found", crn)));
    }

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        return getByCrn(resourceCrn).getId();
    }

    public Set<Long> findAllDeleting() {
        return dbStackRepository.findAllByStatusIn(Status.getDeletingStatuses());
    }

    public Set<Long> findAllDeletingById(Set<Long> dbStackIds) {
        return dbStackRepository.findAllByIdInAndStatusIn(dbStackIds, Status.getDeletingStatuses());
    }

    public Set<JobResource> findAllForAutoSync() {
        return dbStackRepository.findAllDbStackByStatusIn(Status.getAutoSyncStatuses());
    }

    public DBStack save(DBStack dbStack) {
        return dbStackRepository.save(dbStack);
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        Optional<DBStack> dbStackOpt = findById(resourceId);
        if (dbStackOpt.isPresent()) {
            DBStack dbStack = dbStackOpt.get();
            return PayloadContext.create(dbStack.getResourceCrn(), dbStack.getCloudPlatform());
        }
        return null;
    }

    @Transactional
    public void delete(DBStack dbStack) {
        dbStackRepository.delete(dbStack);
    }

    @Transactional
    public void delete(Long dbStackId) {
        dbStackRepository.deleteById(dbStackId);
    }

}
