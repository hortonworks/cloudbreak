-- // CLOUD-42526 create account preferences
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS account_preferences
(
    account CHARACTER VARYING (255) NOT NULL,
    maxNumberOfClusters BIGINT NOT NULL,
    maxNumberOfNodesPerCluster BIGINT NOT NULL,
    maxNumberOfClustersPerUser BIGINT NOT NULL,
    clusterTimeToLive BIGINT NOT NULL,
    userTimeToLive BIGINT NOT NULL,
    allowedInstanceTypes TEXT,
   PRIMARY KEY (account)
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS account_preferences;


