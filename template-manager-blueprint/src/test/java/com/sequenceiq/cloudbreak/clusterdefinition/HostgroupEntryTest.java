package com.sequenceiq.cloudbreak.clusterdefinition;

import static com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupEntry.hostgroupEntry;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupEntry;

public class HostgroupEntryTest {

    @Test
    public void hostgroupEntryTestWhenInitialized() {
        HostgroupEntry master = hostgroupEntry("master");
        Assert.assertEquals("master", master.getHostGroup());
    }

}