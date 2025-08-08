package com.sequenceiq.datalake.repository;

import jakarta.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.datalake.entity.SdxBackupRestoreSettings;

@Repository
@Transactional(Transactional.TxType.REQUIRED)
public interface SdxBackupRestoreSettingsRepository extends CrudRepository<SdxBackupRestoreSettings, Long> {

    SdxBackupRestoreSettings findBySdxClusterCrn(String sdxClusterCrn);

}
