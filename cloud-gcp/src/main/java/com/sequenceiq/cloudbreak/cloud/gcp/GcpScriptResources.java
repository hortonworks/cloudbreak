package com.sequenceiq.cloudbreak.cloud.gcp;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ScriptResources;

@Service
public class GcpScriptResources implements ScriptResources {

    @Override
    public String getVolumeIdFetcherScript() {
        return "ls -la /dev/disk/by-id | grep -i \"google-\" | grep -i -v \"part\" | " +
                "awk '{gsub(\"^../../\",\"\",$11); gsub (\"^google-\",\"\",$9); print $9\" \"$11}'";
    }

}
