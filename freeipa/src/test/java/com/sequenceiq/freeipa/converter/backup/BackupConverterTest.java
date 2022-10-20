package com.sequenceiq.freeipa.converter.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.configuration.BackupConfiguration;

public class BackupConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private static final String DATABUS_ENDPOINT = "myCustomEndpoint";

    private BackupConverter underTest;

    private BackupConverter underTestBackupDisabled;

    @BeforeEach
    public void setUp() {
        BackupConfiguration backupConfiguration = new BackupConfiguration(true, true, true);
        underTest = new BackupConverter(backupConfiguration, true);
        underTestBackupDisabled = new BackupConverter(backupConfiguration, false);
    }

    @Test
    public void testConvertFromS3Request() {
        // GIVEN
        BackupRequest backupRequest = new BackupRequest();
        backupRequest.setS3(new S3CloudStorageV1Parameters());
        backupRequest.setStorageLocation("s3://mybucket");

        // WHEN
        Backup result = underTest.convert(backupRequest);
        // THEN
        assertThat(result.getStorageLocation(), is("s3://mybucket"));
    }

    @Test
    public void testConvertFromNullS3Request() {
        // GIVEN
        BackupRequest backupRequest = null;

        // WHEN
        Backup result = underTest.convert(backupRequest);
        // THEN
        assertThat(result, is(IsNull.nullValue()));
    }

    @Test
    public void testConvertFromWhenfreeIpaBackupDisabled() {
        // GIVEN
        BackupRequest backupRequest = new BackupRequest();
        backupRequest.setS3(new S3CloudStorageV1Parameters());
        backupRequest.setStorageLocation("s3://mybucket");

        // WHEN
        Backup result = underTestBackupDisabled.convert(backupRequest);
        // THEN
        assertThat(result, is(IsNull.nullValue()));
    }

    @Test
    public void testConvertFromAzureRequest() {
        // GIVEN
        BackupRequest backupRequest = new BackupRequest();
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageV1Parameters.setAccountKey("someaccount");
        backupRequest.setAdlsGen2(adlsGen2CloudStorageV1Parameters);
        backupRequest.setStorageLocation("abfs://mybucket@someaccount");

        // WHEN
        Backup result = underTest.convert(backupRequest);
        // THEN
        assertThat(result.getStorageLocation(), is("abfs://mybucket@someaccount"));
    }
}