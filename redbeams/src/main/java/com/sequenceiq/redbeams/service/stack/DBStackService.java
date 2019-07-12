package com.sequenceiq.redbeams.service.stack;

import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.DBStackRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;

@Service
public class DBStackService {

    @Inject
    private DBStackRepository dbStackRepository;

    @Inject
    private CrnService crnService;

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
        return getByCrn(Crn.safeFromString(crn))
                .orElseThrow(() -> new NotFoundException(String.format("Stack with crn [%s] not found", crn)));
    }

    public Optional<DBStack> getByCrn(Crn crn) {
        return dbStackRepository.findByResourceCrn(crn);

    }

    public DBStack save(DBStack dbStack) {
        return dbStackRepository.save(dbStack);
    }

    @Transactional
    public void delete(DBStack dbStack) {
        dbStackRepository.delete(dbStack);
    }
}
