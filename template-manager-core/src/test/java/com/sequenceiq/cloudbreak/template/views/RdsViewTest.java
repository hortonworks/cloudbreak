package com.sequenceiq.cloudbreak.template.views;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class RdsViewTest {

    private static final String ASSERT_ERROR_MSG = "The generated connection URL(connectionHost field) is not valid!";

    @Test
    public void testCreateRdsViewWhenConnectionUrlContainsSubprotocolAndSubname() {
        String connectionUrl = "jdbc:postgresql:subname://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com", underTest.getHost());
        Assert.assertEquals(ASSERT_ERROR_MSG, "5432", underTest.getPort());
        Assert.assertEquals(ASSERT_ERROR_MSG, "ranger", underTest.getDatabaseName());
        Assert.assertEquals("postgresql:subname", underTest.getSubprotocol());
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsProperConnectionUrl() {
        String connectionUrl = "jdbc:postgresql://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com", underTest.getHost());
        Assert.assertEquals(ASSERT_ERROR_MSG, "5432", underTest.getPort());
        Assert.assertEquals(ASSERT_ERROR_MSG, "ranger", underTest.getDatabaseName());
        Assert.assertEquals("postgresql", underTest.getSubprotocol());
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsConnectionUrlWithoutJDBCUrlPrefix() {
        String connectionUrl = "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com", underTest.getHost());
        Assert.assertEquals(ASSERT_ERROR_MSG, "5432", underTest.getPort());
        Assert.assertEquals(ASSERT_ERROR_MSG, "ranger", underTest.getDatabaseName());
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsDoublePortInUrl() {
        String connectionUrl = "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432:5432/ranger", underTest.getConnectionString());
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenRangerAndPostgreSQL() {
        String connectionUrl = "jdbc:postgresql://ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.POSTGRES, RdsType.RANGER);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432", underTest.getConnectionString());
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenHiveAndPostgreSQL() {
        String connectionUrl = "jdbc:postgresql://ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.POSTGRES, RdsType.HIVE);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "jdbc:postgresql://ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432/ranger", underTest.getConnectionString());
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenRangerAndOracle() {
        String connectionUrl = "jdbc:oracle:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12, RdsType.RANGER);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/orcl", underTest.getConnectionString());
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenHiveAndOracle() {
        String connectionUrl = "jdbc:oracle:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12, RdsType.HIVE);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "jdbc:oracle:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl", underTest.getConnectionString());
    }

    @Test
    public void testCreateRdsViewDatabaseNameWhenOracle() {
        String connectionUrl = "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "orcl", underTest.getDatabaseName());
    }

    @Test
    public void testCreateRdsViewWhenOracle() {
        String connectionUrl = "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "1521", underTest.getPort());
        Assert.assertEquals(ASSERT_ERROR_MSG, "ranger.cmseikcocinw.us-east-1.rds.amazonaws.com", underTest.getHost());
        Assert.assertEquals(ASSERT_ERROR_MSG, "oracle:thin", underTest.getSubprotocol());
        Assert.assertEquals(ASSERT_ERROR_MSG, "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521", underTest.getHostWithPortWithJdbc());
    }

    @Test
    public void testCreateRdsViewWhenOracleWithService() {
        String connectionUrl = "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/XE";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "1521", underTest.getPort());
        Assert.assertEquals(ASSERT_ERROR_MSG, "XE", underTest.getDatabaseName());
        Assert.assertEquals(ASSERT_ERROR_MSG, "ranger.cmseikcocinw.us-east-1.rds.amazonaws.com", underTest.getHost());
        Assert.assertEquals(ASSERT_ERROR_MSG, "oracle:thin", underTest.getSubprotocol());
        Assert.assertEquals(ASSERT_ERROR_MSG, "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521", underTest.getHostWithPortWithJdbc());
        Assert.assertEquals(ASSERT_ERROR_MSG, "ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/XE", underTest.getWithoutJDBCPrefix());
    }

    @Test
    public void testCreateRdsViewWhenCutDatabaseNameIfContainsMoreThanOne() {
        String connectionUrl = "jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.MYSQL);

        RdsView underTest = new RdsView(rdsConfig);

        Assert.assertEquals(ASSERT_ERROR_MSG, "jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306", underTest.getHostWithPortWithJdbc());
    }

    private RDSConfig createRdsConfig(String connectionUrl) {
        return createRdsConfig(connectionUrl, DatabaseVendor.POSTGRES, RdsType.HIVE);
    }

    private RDSConfig createRdsConfig(String connectionUrl, DatabaseVendor databaseVendor) {
        return createRdsConfig(connectionUrl, databaseVendor, RdsType.HIVE);
    }

    private RDSConfig createRdsConfig(String connectionUrl, DatabaseVendor databaseVendor, RdsType rdsType) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setConnectionURL(connectionUrl);
        rdsConfig.setConnectionPassword("admin");
        rdsConfig.setConnectionUserName("admin");
        rdsConfig.setDatabaseEngine(databaseVendor);
        rdsConfig.setType(rdsType.name());
        return rdsConfig;
    }

}