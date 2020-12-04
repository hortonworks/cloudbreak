package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.sync;

import java.util.ArrayList;
import java.util.List;

import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.specialized.PageBlobClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.ChunkCalculator;

public class CopyTaskFactory {

    public List<Runnable> createCopyTasks(PageBlobClient destinationPageBlobClient, String sourceBlobUrl, PageBlobRequestConditions destinationRequestConditions, ChunkCalculator chunkCalculator) {
        List<Runnable> byteCopyTasks = new ArrayList<>();
        for (int c = 0; c < chunkCalculator.getChunkCountTotal(); c++) {
            ByteCopyTask byteCopyTask = new ByteCopyTask(
                    destinationPageBlobClient,
                    sourceBlobUrl,
                    chunkCalculator.getChunkStartAddress(c),
                    chunkCalculator.getChunkEndAddress(c),
                    destinationRequestConditions
            );
            byteCopyTasks.add(byteCopyTask);
        }

        if (chunkCalculator.hasRemainderBytes()) {
            byteCopyTasks.add(new ByteCopyTask(
                            destinationPageBlobClient,
                            sourceBlobUrl,
                            chunkCalculator.getRemainderBytesStart(),
                            chunkCalculator.getFileSize() - 1,
                            destinationRequestConditions
                    )
            );
        }

        return byteCopyTasks;
    }
}
