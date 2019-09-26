package com.sequenceiq.environment.parameters.dao.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface BaseParametersRepository<T extends BaseParameters> extends BaseJpaRepository<T, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<BaseParameters> findByEnvironmentId(Long envId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT COUNT(ep.id) > 0 "
            + "FROM BaseParameters ep "
            + "JOIN Environment e ON e.id = ep.environment "
            + "WHERE e.accountId = :accountId "
            + "AND e.cloudPlatform = :cloudPlatform "
            + "AND e.status IN :activeStatuses "
            + "AND e.location = :location "
            + "AND ep.s3guardTableName = :dynamoTableName")
    boolean isS3GuardTableUsed(@Param("accountId") String accountId, @Param("cloudPlatform") String cloudPlatform,
            @Param("activeStatuses") Set<String> activeStatuses, @Param("location") String location, @Param("dynamoTableName") String dynamoTableName);
}
