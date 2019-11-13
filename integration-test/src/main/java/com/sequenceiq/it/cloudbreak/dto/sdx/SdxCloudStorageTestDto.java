package com.sequenceiq.it.cloudbreak.dto.sdx;

import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;

@Prototype
public class SdxCloudStorageTestDto extends AbstractSdxTestDto<SdxCloudStorageRequest, CloudStorageResponse, SdxCloudStorageTestDto> {

    public SdxCloudStorageTestDto(SdxCloudStorageRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public SdxCloudStorageTestDto(TestContext testContext) {
        super(new SdxCloudStorageRequest(), testContext);
    }

    public SdxCloudStorageTestDto() {
        super(SdxCloudStorageTestDto.class.getSimpleName().toUpperCase());
    }

    public SdxCloudStorageTestDto valid() {
        return getCloudProvider().cloudStorage(this);
    }

    public SdxCloudStorageTestDto withS3(S3CloudStorageV1Parameters s3Parameters) {
        getRequest().setS3(s3Parameters);
        return this;
    }

    public SdxCloudStorageTestDto withAdlsGen2(AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters) {
        getRequest().setAdlsGen2(adlsGen2CloudStorageV1Parameters);
        return this;
    }

    public SdxCloudStorageTestDto withFileSystemType(FileSystemType fileSystemType) {
        getRequest().setFileSystemType(fileSystemType);
        return this;
    }

    public SdxCloudStorageTestDto withBaseLocation(String baseLocation) {
        getRequest().setBaseLocation(baseLocation);
        return this;
    }
}
