-- // clustercomponents on cluster object
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE clustercomponent_table START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE clustercomponent
(
   id              bigint PRIMARY KEY NOT NULL DEFAULT nextval('clustercomponent_table'),
   componenttype   varchar (63) NOT NULL,
   name            varchar (255) NOT NULL,
   cluster_id        bigint NOT NULL,
   attributes      text NOT NULL
);

ALTER TABLE clustercomponent
   ADD CONSTRAINT fk_clustercomponent_cluster FOREIGN KEY (cluster_id)
       REFERENCES cluster (id);


ALTER TABLE clustercomponent
   ADD CONSTRAINT uk_clustercomponent_componenttype_name_cluster UNIQUE
          (componenttype, name, cluster_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE clustercomponent;

DROP SEQUENCE clustercomponent_table;
