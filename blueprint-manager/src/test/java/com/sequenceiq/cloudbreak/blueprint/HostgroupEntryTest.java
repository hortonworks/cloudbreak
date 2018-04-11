package com.sequenceiq.cloudbreak.blueprint;


import com.sequenceiq.cloudbreak.templateprocessor.HostgroupEntry;
import org.junit.Assert;
import org.junit.Test;

import static com.sequenceiq.cloudbreak.templateprocessor.HostgroupEntry.hostgroupEntry;

public class HostgroupEntryTest {

    @Test
    public void hostgroupEntryTestWhenInitialized() {
        HostgroupEntry master = hostgroupEntry("master");
        Assert.assertEquals("master", master.getHostGroup());
    }

}