package com.sequenceiq.environment.environment.v1;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.request.CloudStorageRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.CloudStorageResponse;
import com.sequenceiq.environment.environment.dto.CloudStorageDto;

@Component
class CloudStorageApiConverter {

    CloudStorageDto requestToDto(CloudStorageRequest request) {
        return CloudStorageDto.builder()
                .withBaseLocation(request.getBaseLocation())
                .withFileSystemType(request.getFileSystemType())
                .withS3(request.getS3())
                .withAdls(request.getAdls())
                .withAdlsGen2(request.getAdlsGen2())
                .withWasb(request.getWasb())
                .withGcs(request.getGcs())
                .build();
    }

    CloudStorageResponse dtoToResponse(CloudStorageDto dto) {
        return CloudStorageResponse.builder()
                .withBaseLocation(dto.getBaseLocation())
                .withFileSystemType(dto.getFileSystemType())
                .withS3(dto.getS3())
                .withAdls(dto.getAdls())
                .withAdlsGen2(dto.getAdlsGen2())
                .withWasb(dto.getWasb())
                .withGcs(dto.getGcs())
                .build();
    }
}
