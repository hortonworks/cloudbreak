package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.util.ArrayList;
import java.util.List;

import com.azure.storage.blob.models.PageRange;

public class PageRangeCalculator {

    public List<PageRange> getAll(ChunkCalculator chunkCalculator) {
        List<PageRange> ranges = new ArrayList<>();

        for (int c = 0; c < chunkCalculator.getChunkCountTotal(); c++) {
            ranges.add(new PageRange()
                    .setStart(chunkCalculator.getChunkStartAddress(c))
                    .setEnd(chunkCalculator.getChunkEndAddress(c)));
        }
        if(chunkCalculator.hasRemainderBytes()) {
            ranges.add(new PageRange()
                    .setStart(chunkCalculator.getRemainderBytesStart())
                    .setEnd(chunkCalculator.getFileSize() - 1));
        }
        return ranges;
    }
}
