package com.sequenceiq.redbeams.service.stack;

import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.DBStackRepository;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

@Service
public class DBStackService {

    @Inject
    private DBStackRepository dbStackRepository;

    public DBStack getById(Long id) {
        return dbStackRepository.findById(id).orElseThrow(() -> new NotFoundException(String.format("Stack [%s] not found", id)));
    }

    public DBStack save(DBStack dbStack) {
        return dbStackRepository.save(dbStack);
    }

}
