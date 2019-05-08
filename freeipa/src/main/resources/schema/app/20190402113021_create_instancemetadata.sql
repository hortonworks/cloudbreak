-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE instancemetadata_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table instancemetadata
(
  id bigint default nextval('instancemetadata_id_seq'::regclass) not null
    constraint instancemetadata_pkey
      primary key,
  instanceid varchar(255),
  instancestatus varchar(255),
  discoveryfqdn varchar(255),
  privateip varchar(255),
  publicip varchar(255),
  startdate bigint,
  terminationdate bigint,
  instancegroup_id bigint
    constraint fk_instancemetadata_instancegroup_id
      references instancegroup
      on delete cascade,
  privateid bigint,
  localityindicator varchar(255),
  sshport integer default 22,
  instancemetadatatype varchar(255),
  servercert text,
  subnetid varchar(255),
  instancename varchar(255)
);


create index instancemetadata_instancegroup_id
  on instancemetadata (instancegroup_id);



-- //@UNDO

DROP TABLE instancemetadata;

DROP SEQUENCE instancemetadata_id_seq;