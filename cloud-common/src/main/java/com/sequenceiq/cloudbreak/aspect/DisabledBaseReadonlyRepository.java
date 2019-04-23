package com.sequenceiq.cloudbreak.aspect;

import java.io.Serializable;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
@DisableHasPermission
public interface DisabledBaseReadonlyRepository<T, ID extends Serializable> extends Repository<T, ID> {
}
