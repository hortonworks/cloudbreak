-- // CB-14959 Create ArchivedInstanceMetaData table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS archivedinstancemetadata
(
  variant                    varchar(255)                   NULL,
  rackid                     text                           NULL,
  availabilityzone           varchar(255)                   NULL,
  lifecycle                  varchar(255)                   NULL,
  clustermanagerserver       boolean                        NULL,
  statusreason               text                           NULL,
  image                      text                           NULL,
  instancename               varchar(255)                   NULL,
  subnetid                   varchar(255)                   NULL,
  servercert                 text                           NULL,
  instancemetadatatype       varchar(255)                   NULL,
  sshport                    integer                        NULL,
  localityindicator          varchar(255)                   NULL,
  privateid                  bigint                         NULL,
  instancegroup_id           bigint                         NULL,
  terminationdate            bigint                         NULL,
  startdate                  bigint                         NULL,
  publicip                   varchar(255)                   NULL,
  privateip                  varchar(255)                   NULL,
  discoveryfqdn              varchar(255)                   NULL,
  instancestatus             varchar(255)                   NULL,
  instanceid                 varchar(255)                   NULL,
  consulserver               boolean                        NULL,
  ambariserver               boolean                        NULL,
  id                         bigint                         NOT NULL
);

ALTER TABLE archivedinstancemetadata ADD CONSTRAINT archivedinstancemetadata_pkey PRIMARY KEY (id)

-- //@UNDO
-- SQL to undo the change goes here.

INSERT INTO instancemetadata SELECT
                                 id,
                                 ambariserver,
                                 consulserver,
                                 instanceid,
                                 instancestatus,
                                 discoveryfqdn,
                                 privateip,
                                 publicip,
                                 startdate,
                                 terminationdate,
                                 instancegroup_id,
                                 privateid,
                                 localityindicator,
                                 sshport,
                                 instancemetadatatype,
                                 servercert,
                                 subnetid,
                                 instancename,
                                 image,
                                 statusreason,
                                 clustermanagerserver,
                                 lifecycle,
                                 availabilityzone,
                                 rackid,
                                 variant
FROM archivedinstancemetadata;

DROP TABLE IF EXISTS archivedinstancemetadata;
