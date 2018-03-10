package com.sequenceiq.cloudbreak.blueprint.template.views;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class RdsViewTest {

    private static final String ASSERT_ERROR_MSG = "The generated connection URL(connectionHost field) is not valid!";

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsProperConnectionUrl() {
        String connectionUrl = "jdbc:postgresql://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432", underTest.getConnectionHost());
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsConnectionUrlWithoutPort() {
        String connectionUrl = "jdbc:postgresql://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com", underTest.getConnectionHost());
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsConnectionUrlWithoutDatabaseName() {
        String connectionUrl = "jdbc:postgresql://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432", underTest.getConnectionHost());
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsConnectionUrlWithoutJDBCUrlPrefix() {
        String connectionUrl = "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432", underTest.getConnectionHost());
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsConnectionUrlWithoutDatabaseNameAndPort() {
        String connectionUrl = "jdbc:postgresql://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com", underTest.getConnectionHost());
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsConnectionUrlWithoutDatabaseNameAndPortAndJDBCPrefix() {
        String connectionUrl = "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com", underTest.getConnectionHost());
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsDoublePortInUrl() {
        String connectionUrl = "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432:5432", underTest.getConnectionHost());
    }

    private RDSConfig createRdsConfig(String connectionUrl) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setConnectionURL(connectionUrl);
        rdsConfig.setConnectionPassword("admin");
        rdsConfig.setConnectionUserName("admin");
        rdsConfig.setAttributes(null);
        return rdsConfig;
    }

}