-- // CLOUD-76786 separate gateway table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE gateway_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE gateway (
    id              bigint NOT NULL DEFAULT nextval('gateway_id_seq'),
    cluster_id      bigint NOT NULL,
    enablegateway   boolean DEFAULT FALSE,
    path            character varying(255),
    topologyname    character varying(255),
    exposedservices TEXT,
    ssoprovider     character varying(255),
    signkey         TEXT
);

ALTER TABLE gateway
   ADD CONSTRAINT fk_gateway_cluster FOREIGN KEY (cluster_id)
       REFERENCES cluster (id);

INSERT INTO gateway (cluster_id,
                     enablegateway,
                     path,
                     topologyname,
                     exposedservices)
   SELECT c.id as cluster_id,
          c.enableknoxgateway as enablegateway,
          'gateway' AS path,
          c.knoxtopologyname AS topologyname,
          c.exposedknoxservices AS exposedservices
     FROM cluster c;


ALTER TABLE cluster DROP COLUMN enableknoxgateway;
ALTER TABLE cluster DROP COLUMN knoxtopologyname;
ALTER TABLE cluster DROP COLUMN exposedknoxservices;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster ADD COLUMN enableknoxgateway BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE cluster ADD COLUMN knoxtopologyname TEXT;
ALTER TABLE cluster ADD COLUMN exposedknoxservices TEXT;


UPDATE cluster c SET
    enableknoxgateway = g.enablegateway,
    knoxtopologyname = g.topologyname,
    exposedknoxservices = g.exposedservices
FROM
    gateway g
WHERE
    c.id = g.cluster_id;

DROP TABLE gateway;
DROP SEQUENCE gateway_id_seq;
