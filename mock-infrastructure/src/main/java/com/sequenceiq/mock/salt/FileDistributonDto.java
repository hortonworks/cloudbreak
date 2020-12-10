package com.sequenceiq.mock.salt;

public class FileDistributonDto {

    private String mockUuid;

    private String fileName;

    private long size;

    private String contentType;

    public FileDistributonDto(String mockUuid, String fileName, long size, String contentType) {
        this.mockUuid = mockUuid;
        this.fileName = fileName;
        this.size = size;
        this.contentType = contentType;
    }

    public String getMockUuid() {
        return mockUuid;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }
}
