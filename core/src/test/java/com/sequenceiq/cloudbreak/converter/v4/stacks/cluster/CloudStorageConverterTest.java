package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.PerformanceMode;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.ThroughputMode;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.common.api.cloudstorage.AwsEfsParameters;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.EfsCloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
public class CloudStorageConverterTest {

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private final CloudStorageConverter underTest = new CloudStorageConverter();

    @BeforeEach
    public void setUp() {
        if (StringUtils.isEmpty(ThreadBasedUserCrnProvider.getUserCrn())) {
            ThreadBasedUserCrnProvider.setUserCrn("crn:cdp:iam:us-west-1:1234:user:1");
        }
    }

    @Test
    public void testRequestToAdditionalFileSystem() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        AwsStorageParameters awsStorageParameters = new AwsStorageParameters();
        awsStorageParameters.setEfsParameters(getEfsParameters());

        StorageIdentityBase storageIdentity = new StorageIdentityBase();
        storageIdentity.setEfs(new EfsCloudStorageV1Parameters());
        cloudStorageRequest.setIdentities(List.of(storageIdentity));
        cloudStorageRequest.setLocations(List.of(new StorageLocationBase()));
        cloudStorageRequest.setAws(awsStorageParameters);

        FileSystem efsFileSystem = underTest.requestToAdditionalFileSystem(cloudStorageRequest);

        assertNotNull(efsFileSystem);
        assertEquals(FileSystemType.EFS, efsFileSystem.getType());

        CloudStorage cloudStorage = efsFileSystem.getCloudStorage();
        assertNotNull(cloudStorage.getCloudIdentities().get(0).getEfsIdentity());

        Map<String, Object> configurations = efsFileSystem.getConfigurations().getMap();
        assertNotNull(configurations);
        assertEquals(true, configurations.get(CloudEfsConfiguration.KEY_ENCRYPTED));
        assertEquals(PerformanceMode.GENERALPURPOSE.toString(), configurations.get(CloudEfsConfiguration.KEY_PERFORMANCE_MODE));
        assertEquals(ThroughputMode.BURSTING.toString(), configurations.get(CloudEfsConfiguration.KEY_THROUGHPUT_MODE));
    }

    private AwsEfsParameters getEfsParameters() {
        String fileSystemName = "lina-efs-0127-1";
        Map<String, String> tags = new HashMap<>();
        tags.put(CloudEfsConfiguration.KEY_FILESYSTEM_TAGS_NAME, fileSystemName);

        AwsEfsParameters efsParameters = new AwsEfsParameters();
        efsParameters.setName(fileSystemName);
        efsParameters.setEncrypted(true);
        efsParameters.setFileSystemTags(tags);
        efsParameters.setPerformanceMode(PerformanceMode.GENERALPURPOSE.toString());
        efsParameters.setThroughputMode(ThroughputMode.BURSTING.toString());
        efsParameters.setAssociatedInstanceGroupNames(List.of("core", "master"));

        return efsParameters;
    }
}
