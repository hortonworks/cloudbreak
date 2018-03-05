package com.sequenceiq.cloudbreak.blueprint.template.views;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;

public class DatabaseViewTest {

    @Test
    public void testInitializeDatabaseView() {
        DatabaseView databaseView = new DatabaseView(ambariDatabase());
        Assert.assertEquals("password123#$@", databaseView.getConnectionPassword());
        Assert.assertEquals("10.0.0.2:3000/ambari-database", databaseView.getConnectionURL());
        Assert.assertEquals("ambari-database-user", databaseView.getConnectionUserName());
    }

    private AmbariDatabase ambariDatabase() {
        AmbariDatabase ambariDatabase = new AmbariDatabase();
        ambariDatabase.setFancyName("mysql");
        ambariDatabase.setHost("10.0.0.2");
        ambariDatabase.setName("ambari-database");
        ambariDatabase.setPassword("password123#$@");
        ambariDatabase.setPort(3000);
        ambariDatabase.setUserName("ambari-database-user");
        ambariDatabase.setVendor("mysql");
        return ambariDatabase;
    }
}