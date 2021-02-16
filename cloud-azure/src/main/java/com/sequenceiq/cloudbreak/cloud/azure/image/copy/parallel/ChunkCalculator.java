package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

public class ChunkCalculator {

    /**
     * Size of a chunk, expressed in bytes
     */
    private final long chunkSize;

    /**
     * Size of the file, expressed in bytes
     */
    private final long fileSize;

    /**
     * Number of chunks in total
     */
    private final long chunkCountTotal;

    /**
     * Start address in bytes of leftover bytes
     */
    private final long remainderBytesStart;

    public ChunkCalculator(long fileSize, long chunkSize) {
        this.chunkSize = chunkSize;
        this.fileSize = fileSize;

        this.chunkCountTotal = fileSize / chunkSize;
        this.remainderBytesStart = chunkCountTotal * chunkSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getChunkCountTotal() {
        return chunkCountTotal;
    }

    /**
     * Depending on {@link chunkSize} there will be a partial-chunk leftover from the copy process
     * This returns the start address expressed in bytes where from the leftover bytes can be copied.
     * The end address is the {@link fileSize - 1}.
     *
     * @return start address of remainder bytes to copy.
     */
    public long getRemainderBytesStart() {
        return remainderBytesStart;
    }

    public boolean hasRemainderBytes() {
        return remainderBytesStart < fileSize;
    }

    public long getChunkStartAddress(long id) {
        return id * chunkSize;
    }

    public long getChunkEndAddress(long id) {
        return (id + 1) * chunkSize - 1;
    }

}
