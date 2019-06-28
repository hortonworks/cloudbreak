package com.sequenceiq.redbeams.service.stack;

import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.DBStackRepository;

import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

@Service
public class DBStackService {

    @Inject
    private DBStackRepository dbStackRepository;

    public DBStack getById(Long id) {
        return dbStackRepository.findById(id).orElseThrow(() -> new NotFoundException(String.format("Stack [%s] not found", id)));
    }

    public Optional<DBStack> findById(Long id) {
        return dbStackRepository.findById(id);
    }

    public DBStack getByNameAndEnvironmentId(String name, String environmentId) {
        return findByNameAndEnvironmentId(name, environmentId)
            .orElseThrow(() -> new NotFoundException(String.format("Stack [%s] not found", name)));
    }

    public Optional<DBStack> findByNameAndEnvironmentId(String name, String environmentId) {
        return dbStackRepository.findByNameAndEnvironmentId(name, environmentId);
    }

    public DBStack save(DBStack dbStack) {
        return dbStackRepository.save(dbStack);
    }

    @Transactional
    public void delete(DBStack dbStack) {
        dbStackRepository.delete(dbStack);
    }
}
