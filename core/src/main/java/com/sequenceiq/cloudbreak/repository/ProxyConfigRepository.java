package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@EntityType(entityClass = ProxyConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ProxyConfigRepository extends CrudRepository<ProxyConfig, Long> {

    Set<ProxyConfig> findAllByOwner(String owner);

    @Query("SELECT p FROM ProxyConfig p WHERE (p.account= :account AND p.publicInAccount= true) OR p.owner= :user")
    Set<ProxyConfig> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    Set<ProxyConfig> findAllByAccount(String account);

    ProxyConfig findByNameAndOwner(String name, String owner);

    ProxyConfig findByNameAndAccount(String name, String account);

    ProxyConfig findByIdAndAccount(Long id, String account);

    @Query("SELECT p FROM ProxyConfig p WHERE  p.name= :name "
            + "and ((p.publicInAccount=true and p.account= :account) or p.owner= :owner)")
    ProxyConfig findByNameBasedOnAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);
}
