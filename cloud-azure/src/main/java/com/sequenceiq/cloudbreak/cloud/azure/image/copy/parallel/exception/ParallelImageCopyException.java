package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel.exception;

public class ParallelImageCopyException extends RuntimeException {

    private static final String PARALLEL_IMAGE_COPY_FAILED = "Parallel image copy failed: ";

    public ParallelImageCopyException(String message) {
        super(PARALLEL_IMAGE_COPY_FAILED + message);
    }

    public ParallelImageCopyException(String message, Throwable t) {
        super(PARALLEL_IMAGE_COPY_FAILED + message, t);
    }
}
