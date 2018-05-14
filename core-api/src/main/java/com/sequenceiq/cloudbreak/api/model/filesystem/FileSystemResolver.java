package com.sequenceiq.cloudbreak.api.model.filesystem;

import java.util.Map;

import javax.ws.rs.BadRequestException;

import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.v2.FileSystemV2Request;

public final class FileSystemResolver {

    private static final String NOT_SUPPORTED_FS_PROVIDED = "Unable to decide file system, none of the supported file system type has provided!";

    private static final String NOT_SUPPORTED_FS_FORMAT_WHEN_PROVIDED = "Unable to decide file system type, none of the supported file system type has "
            + "provided! Given: [%s]";

    private FileSystemResolver() {
    }

    public static FileSystemParameters decideFileSystemFromFileSystemV2Request(FileSystemV2Request source) {
        FileSystemParameters fileSystemParameters;
        if (source.getAdls() != null) {
            fileSystemParameters = source.getAdls();
        } else if (source.getGcs() != null) {
            fileSystemParameters = source.getGcs();
        } else if (source.getWasb() != null) {
            fileSystemParameters = source.getWasb();
        } else if (source.getS3() != null) {
            fileSystemParameters = source.getS3();
        } else {
            throw new BadRequestException(NOT_SUPPORTED_FS_PROVIDED);
        }
        return fileSystemParameters;
    }

    public static void setFSV2RequestFileSystemParamsByNameAndProperties(String fileSystemType, Map<String, String> properties, FileSystemV2Request request) {
        FileSystemParameters fileSystemParameters = fillFileSystemParamsByItsType(fileSystemType, properties);
        if (fileSystemParameters.getType() == FileSystemType.ADLS) {
            request.setAdls((AdlsFileSystemParameters) fileSystemParameters);
        } else if (fileSystemParameters.getType() == FileSystemType.GCS) {
            request.setGcs((GcsFileSystemParameters) fileSystemParameters);
        } else if (fileSystemParameters.getType() == FileSystemType.WASB) {
            request.setWasb((WasbFileSystemParameters) fileSystemParameters);
        } else if (fileSystemParameters.getType() == FileSystemType.S3) {
            request.setS3((S3FileSystemParameters) fileSystemParameters);
        }
    }

    private static FileSystemParameters fillFileSystemParamsByItsType(String fileSystemType, Map<String, String> properties) {
        FileSystemParameters fileSystemParameters;
        if (fileSystemType != null) {
            switch (fileSystemType) {
                case "ADLS":
                    AdlsFileSystemParameters adls = new AdlsFileSystemParameters();
                    adls.setTenantId(chooseProperty(properties, AdlsFileSystemConfiguration.TENANT_ID, AdlsFileSystemParameters.TENANT_ID));
                    adls.setCredential(chooseProperty(properties, AdlsFileSystemConfiguration.ACCESS_KEY, AdlsFileSystemParameters.CREDENTIAL));
                    adls.setClientId(chooseProperty(properties, AdlsFileSystemConfiguration.SUBSCRIPTION_ID, AdlsFileSystemParameters.CLIENT_ID));
                    adls.setAccountName(chooseProperty(properties, FileSystemConfiguration.ACCOUNT_NAME, AdlsFileSystemParameters.ACCOUNT_NAME));
                    fileSystemParameters = adls;
                    break;
                case "GCS":
                    GcsFileSystemParameters gcs = new GcsFileSystemParameters();
                    gcs.setServiceAccountEmail(properties.get(GcsFileSystemParameters.SERVICE_ACCOUNT_EMAIL));
                    gcs.setProjectId(properties.get(GcsFileSystemParameters.PROJECT_ID));
                    gcs.setDefaultBucketName(properties.get(GcsFileSystemParameters.DEFAULT_BUCKET_NAME));
                    fileSystemParameters = gcs;
                    break;
                case "WASB":
                    WasbFileSystemParameters wasb = new WasbFileSystemParameters();
                    wasb.setAccountName(properties.get(WasbFileSystemParameters.ACCOUNT_NAME));
                    wasb.setAccountKey(properties.get(WasbFileSystemParameters.ACCOUNT_KEY));
                    fileSystemParameters = wasb;
                    break;
                case "S3":
                    S3FileSystemParameters s3 = new S3FileSystemParameters();
                    s3.setInstanceProfile(properties.get(S3FileSystemParameters.INSTANCE_PROFILE));
                    fileSystemParameters = s3;
                    break;
                default:
                    throw new BadRequestException(String.format(NOT_SUPPORTED_FS_FORMAT_WHEN_PROVIDED, fileSystemType));
            }
            return fileSystemParameters;
        } else {
            throw new BadRequestException(NOT_SUPPORTED_FS_PROVIDED);
        }
    }

    private static String chooseProperty(Map<String, String> properties, String optionOne, String otherwise) {
        String resultOfOptionOne = properties.get(optionOne);
        return resultOfOptionOne != null ? resultOfOptionOne : properties.get(otherwise);
    }

}
