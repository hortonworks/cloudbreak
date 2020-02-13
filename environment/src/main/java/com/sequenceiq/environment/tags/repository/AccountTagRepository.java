package com.sequenceiq.environment.tags.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.environment.tags.domain.AccountTag;

@EntityType(entityClass = AccountTag.class)
@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface AccountTagRepository extends BaseJpaRepository<AccountTag, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT a FROM AccountTag a WHERE a.accountId = :accountId AND a.archived IS FALSE")
    Set<AccountTag> findAllInAccount(@Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.WRITE)
    @Modifying
    @Query("UPDATE AccountTag a SET a.archived = TRUE WHERE a.accountId= :accountId")
    void arhiveAll(@Param("accountId") String accountId);

}
