package com.sequenceiq.cloudbreak.cloud.aws.common;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ScriptResources;

@Service
public class AwsScriptResources implements ScriptResources {

    @Override
    public String getVolumeIdFetcherScript() {
        return "ls -la /dev/disk/by-id | grep -i \"_vol\" | grep -i -v \"part\" | " +
                "awk '{gsub(\"^../../\",\"\",$11); gsub (\"^nvme-Amazon_Elastic_Block_Store_vol\",\"vol-\",$9); print $9\" \"$11}'";
    }
}
