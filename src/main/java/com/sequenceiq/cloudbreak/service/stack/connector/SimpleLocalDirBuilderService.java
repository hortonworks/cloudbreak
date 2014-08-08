package com.sequenceiq.cloudbreak.service.stack.connector;

import org.springframework.stereotype.Service;

@Service
public class SimpleLocalDirBuilderService implements LocalDirBuilderService {
    @Override
    public String buildLocalDirs(Integer volumeCount) {
        StringBuilder localDirs = new StringBuilder("");
        for (int i = 1; i <= volumeCount; i++) {
            localDirs.append("/mnt/fs").append(i);
            if (i != volumeCount) {
                localDirs.append(",");
            }
        }
        return localDirs.toString();
    }
}
