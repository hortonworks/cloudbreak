-- // CLOUD-720 ambari stack details
-- Migration SQL that makes the change goes here.

CREATE TABLE ambaristackdetails
(
   id             BIGINT NOT NULL,
   os             VARCHAR (255),
   stack          VARCHAR (255),
   stackbaseurl   VARCHAR (255),
   stackrepoid    VARCHAR (255),
   utilsbaseurl   VARCHAR (255),
   utilsrepoid    VARCHAR (255),
   verify         BOOLEAN DEFAULT TRUE NOT NULL,
   version        VARCHAR (255),
   PRIMARY KEY (id)
);

CREATE SEQUENCE amb_stack_table START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE cluster ADD COLUMN ambaristackdetails_id BIGINT REFERENCES ambaristackdetails(id);
ALTER TABLE cluster ADD COLUMN username VARCHAR (255);
ALTER TABLE cluster ADD COLUMN password VARCHAR (255);
ALTER TABLE cluster ADD COLUMN ambariip VARCHAR (255);

UPDATE cluster SET username=stack.username FROM stack WHERE stack.cluster_id=cluster.id;
UPDATE cluster SET password=stack.password FROM stack WHERE stack.cluster_id=cluster.id;
UPDATE cluster SET ambariip=stack.ambariip FROM stack WHERE stack.cluster_id=cluster.id;

ALTER TABLE stack DROP COLUMN username;
ALTER TABLE stack DROP COLUMN password;
ALTER TABLE stack DROP COLUMN ambariip;

-- //@UNDO
-- SQL to undo the change goes here.


