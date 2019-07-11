-- // Create Syncoperation table

CREATE SEQUENCE IF NOT EXISTS syncoperation_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE IF NOT EXISTS syncoperation
(
  id bigint default nextval('syncoperation_id_seq'::regclass) not null
    constraint syncoperation_pkey
      primary key,
  operationid varchar(255) unique not null,
  accountid varchar(255) not null,
  syncoperationtype varchar(255) not null,
  status varchar(255) not null,
  successlist text,
  failurelist text,
  error text,
  starttime bigint not null,
  endtime bigint
);

CREATE UNIQUE INDEX IF NOT EXISTS syncoperation_id_idx
  on syncoperation (id);

CREATE INDEX IF NOT EXISTS syncoperation_operationid_idx
  on syncoperation (operationid);

CREATE INDEX IF NOT EXISTS syncoperation_accountid_endtime_idx
  on syncoperation (accountid, endtime)

-- //@UNDO

DROP TABLE syncoperation;

DROP SEQUENCE syncoperation_id_seq;