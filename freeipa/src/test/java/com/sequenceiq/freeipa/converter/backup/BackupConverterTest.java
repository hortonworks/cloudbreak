package com.sequenceiq.freeipa.converter.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.configuration.BackupConfiguration;

public class BackupConverterTest {

    private static final String INSTANCE_PROFILE_VALUE = "myInstanceProfile";

    private static final String DATABUS_ENDPOINT = "myCustomEndpoint";

    private BackupConverter underTest;

    @BeforeEach
    public void setUp() {
        BackupConfiguration backupConfiguration = new BackupConfiguration(true, true, true);
        underTest = new BackupConverter(backupConfiguration, true);
    }

    @Test
    public void testConvertFromS3Request() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        logging.setS3(new S3CloudStorageV1Parameters());
        logging.setStorageLocation("s3://mybucket");
        telemetryRequest.setLogging(logging);
        // WHEN
        Backup result = underTest.convert(telemetryRequest);
        // THEN
        assertThat(result.getStorageLocation(), is("s3://mybucket"));
    }

    @Test
    public void testConvertFromAzureRequest() {
        // GIVEN
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageV1Parameters.setAccountKey("someaccount");
        logging.setAdlsGen2(adlsGen2CloudStorageV1Parameters);
        logging.setStorageLocation("abfs://mybucket@someaccount");
        telemetryRequest.setLogging(logging);
        // WHEN
        Backup result = underTest.convert(telemetryRequest);
        // THEN
        assertThat(result.getStorageLocation(), is("abfs://mybucket@someaccount"));
    }
}
