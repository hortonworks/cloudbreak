package com.sequenceiq.datalake.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Entity
@EntityType(entityClass = SdxBackupRestoreSettings.class)
@Table(name = "sdxbackuprestoresettings")
public class SdxBackupRestoreSettings {

    @Id
    private String sdxClusterCrn;

    private String backupTempLocation;

    private int backupTimeoutInMinutes;

    private String restoreTempLocation;

    private int restoreTimeoutInMinutes;

    public String getSdxClusterCrn() {
        return sdxClusterCrn;
    }

    public void setSdxClusterCrn(String sdxClusterCrn) {
        this.sdxClusterCrn = sdxClusterCrn;
    }

    public String getBackupTempLocation() {
        return backupTempLocation;
    }

    public void setBackupTempLocation(String backupTempLocation) {
        this.backupTempLocation = backupTempLocation;
    }

    public int getBackupTimeoutInMinutes() {
        return backupTimeoutInMinutes;
    }

    public void setBackupTimeoutInMinutes(int backupTimeoutInMinutes) {
        this.backupTimeoutInMinutes = backupTimeoutInMinutes;
    }

    public String getRestoreTempLocation() {
        return restoreTempLocation;
    }

    public void setRestoreTempLocation(String restoreTempLocation) {
        this.restoreTempLocation = restoreTempLocation;
    }

    public int getRestoreTimeoutInMinutes() {
        return restoreTimeoutInMinutes;
    }

    public void setRestoreTimeoutInMinutes(int restoreTimeoutInMinutes) {
        this.restoreTimeoutInMinutes = restoreTimeoutInMinutes;
    }

    @Override
    public String toString() {
        return "SdxBackupRestoreSettings{" +
                "sdxClusterCrn='" + sdxClusterCrn + '\'' +
                ", backupTempLocation='" + backupTempLocation + '\'' +
                ", backupTimeoutInMinutes=" + backupTimeoutInMinutes +
                ", restoreTempLocation='" + restoreTempLocation + '\'' +
                ", restoreTimeoutInMinutes=" + restoreTimeoutInMinutes +
                '}';
    }
}
