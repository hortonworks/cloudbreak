package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.old.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;
import com.sequenceiq.common.model.CloudIdentityType;

@Component
public class CloudStorageParametersConverter {

    public AdlsFileSystem adlsToFileSystem(AdlsCloudStorageV1Parameters source) {
        AdlsFileSystem fileSystemConfigurations = new AdlsFileSystem();
        fileSystemConfigurations.setClientId(source.getClientId());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setCredential(source.getCredential());
        fileSystemConfigurations.setTenantId(source.getTenantId());
        return fileSystemConfigurations;
    }

    public AdlsFileSystem adlsParametersToFileSystem(AdlsCloudStorageV1Parameters source) {
        AdlsFileSystem fileSystemConfigurations = new AdlsFileSystem();
        fileSystemConfigurations.setClientId(source.getClientId());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setCredential(source.getCredential());
        fileSystemConfigurations.setTenantId(source.getTenantId());
        return fileSystemConfigurations;
    }

    public CloudAdlsView adlsToCloudView(StorageIdentityBase source) {
        CloudAdlsView cloudAdlsView = new CloudAdlsView(source.getType());
        cloudAdlsView.setAccountName(source.getAdls().getAccountName());
        cloudAdlsView.setClientId(source.getAdls().getClientId());
        cloudAdlsView.setCredential(source.getAdls().getCredential());
        cloudAdlsView.setTenantId(source.getAdls().getTenantId());
        return cloudAdlsView;
    }

    public CloudAdlsView adlsToCloudView(AdlsCloudStorageV1Parameters source) {
        CloudAdlsView cloudAdlsView = new CloudAdlsView(CloudIdentityType.LOG);
        cloudAdlsView.setAccountName(source.getAccountName());
        cloudAdlsView.setClientId(source.getClientId());
        cloudAdlsView.setCredential(source.getCredential());
        cloudAdlsView.setTenantId(source.getTenantId());
        return cloudAdlsView;
    }

    public AdlsCloudStorageV1Parameters adlsFileSystemToParameters(AdlsFileSystem source) {
        AdlsCloudStorageV1Parameters adlsCloudStorageV1Parameters = new AdlsCloudStorageV1Parameters();
        adlsCloudStorageV1Parameters.setClientId(source.getClientId());
        adlsCloudStorageV1Parameters.setAccountName(source.getAccountName());
        adlsCloudStorageV1Parameters.setCredential(source.getCredential());
        adlsCloudStorageV1Parameters.setTenantId(source.getTenantId());
        return adlsCloudStorageV1Parameters;
    }

    public GcsFileSystem gcsToFileSystem(GcsCloudStorageV1Parameters source) {
        GcsFileSystem fileSystemConfigurations = new GcsFileSystem();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }

    public CloudGcsView gcsToCloudView(StorageIdentityBase source) {
        CloudGcsView cloudGcsView = new CloudGcsView(source.getType());
        cloudGcsView.setServiceAccountEmail(source.getGcs().getServiceAccountEmail());
        return cloudGcsView;
    }

    public CloudGcsView gcsToCloudView(GcsCloudStorageV1Parameters source) {
        CloudGcsView cloudGcsView = new CloudGcsView(CloudIdentityType.LOG);
        cloudGcsView.setServiceAccountEmail(source.getServiceAccountEmail());
        return cloudGcsView;
    }

    public CloudS3View s3ToCloudView(StorageIdentityBase source) {
        CloudS3View cloudS3View = new CloudS3View(source.getType());
        cloudS3View.setInstanceProfile(source.getS3().getInstanceProfile());
        return cloudS3View;
    }

    public CloudS3View s3ToCloudView(S3CloudStorageV1Parameters source) {
        CloudS3View cloudS3View = new CloudS3View(CloudIdentityType.LOG);
        cloudS3View.setInstanceProfile(source.getInstanceProfile());
        return cloudS3View;
    }

    public WasbFileSystem wasbToFileSystem(WasbCloudStorageV1Parameters source) {
        WasbFileSystem wasbFileSystem = new WasbFileSystem();
        wasbFileSystem.setSecure(source.isSecure());
        wasbFileSystem.setAccountName(source.getAccountName());
        wasbFileSystem.setAccountKey(source.getAccountKey());
        return wasbFileSystem;
    }

    public CloudWasbView wasbToCloudView(StorageIdentityBase source) {
        CloudWasbView cloudWasbView = new CloudWasbView(source.getType());
        cloudWasbView.setAccountKey(source.getWasb().getAccountKey());
        cloudWasbView.setAccountName(source.getWasb().getAccountName());
        cloudWasbView.setSecure(source.getWasb().isSecure());
        return cloudWasbView;
    }

    public CloudWasbView wasbToCloudView(WasbCloudStorageV1Parameters source) {
        CloudWasbView cloudWasbView = new CloudWasbView(CloudIdentityType.LOG);
        cloudWasbView.setAccountKey(source.getAccountKey());
        cloudWasbView.setAccountName(source.getAccountName());
        cloudWasbView.setSecure(source.isSecure());
        return cloudWasbView;
    }

    public AdlsGen2FileSystem adlsGen2ToFileSystem(AdlsGen2CloudStorageV1Parameters source) {
        AdlsGen2FileSystem adlsGen2FileSystem = new AdlsGen2FileSystem();
        adlsGen2FileSystem.setAccountName(source.getAccountName());
        adlsGen2FileSystem.setAccountKey(source.getAccountKey());
        adlsGen2FileSystem.setManagedIdentity(source.getManagedIdentity());
        return adlsGen2FileSystem;
    }

    public CloudAdlsGen2View adlsGen2ToCloudView(StorageIdentityBase source) {
        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View(source.getType());
        AdlsGen2CloudStorageV1Parameters adlsGen2 = source.getAdlsGen2();
        cloudAdlsGen2View.setAccountKey(adlsGen2.getAccountKey());
        cloudAdlsGen2View.setAccountName(adlsGen2.getAccountName());
        cloudAdlsGen2View.setSecure(adlsGen2.isSecure());
        cloudAdlsGen2View.setManagedIdentity(adlsGen2.getManagedIdentity());
        return cloudAdlsGen2View;
    }

    public CloudAdlsGen2View adlsGen2ToCloudView(AdlsGen2CloudStorageV1Parameters source) {
        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View(CloudIdentityType.LOG);
        cloudAdlsGen2View.setAccountKey(source.getAccountKey());
        cloudAdlsGen2View.setAccountName(source.getAccountName());
        cloudAdlsGen2View.setSecure(source.isSecure());
        cloudAdlsGen2View.setManagedIdentity(source.getManagedIdentity());
        return cloudAdlsGen2View;
    }
}
