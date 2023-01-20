package com.sequenceiq.cloudbreak.cloud.azure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.storage.blob.models.CopyStatusType;

public class CopyState {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyState.class);

    private final CopyStatusType copyStatusType;

    private final long bytesCopied;

    private final long totalBytes;

    private CopyState(CopyStatusType copyStatusType, long bytesCopied, long totalBytes) {
        this.copyStatusType = copyStatusType;
        this.bytesCopied = bytesCopied;
        this.totalBytes = totalBytes;
    }

    public static CopyState of(CopyStatusType copyStatusType, String progress) {
        long bytesCopied = -1;
        long totalBytes = -1;
        try {
            String[] values = progress.split("/");
            if (values.length == 2) {
                bytesCopied = Long.parseLong(values[0]);
                totalBytes = Long.parseLong(values[1]);
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't parse copy progress {}. Copy status: {}, error: {}", progress, copyStatusType, e.getMessage(), e);
        }
        return new CopyState(copyStatusType, bytesCopied, totalBytes);
    }

    public CopyStatusType getCopyStatusType() {
        return copyStatusType;
    }

    public long getBytesCopied() {
        return bytesCopied;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    @Override
    public String toString() {
        return "CopyState{" +
                "copyStatusType=" + copyStatusType +
                ", bytesCopied=" + bytesCopied +
                ", totalBytes=" + totalBytes +
                '}';
    }
}
