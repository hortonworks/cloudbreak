-- // CB-1129-rename-clusterdefinition-to-blueprint
-- Migration SQL that makes the change goes here.

ALTER INDEX clusterdefinition_id_idx RENAME TO blueprint_id_idx;
ALTER INDEX clusterdefinition_name_idx RENAME TO blueprint_name_idx;
ALTER INDEX clusterdefinition_org_id_idx RENAME TO blueprint_org_id_idx;

ALTER TABLE ONLY servicedescriptor RENAME COLUMN clusterdefinitionparams TO blueprintparams;
ALTER TABLE ONLY servicedescriptor RENAME COLUMN clusterdefinitionsecretparams TO blueprintsecretparams;

ALTER TABLE ONLY cluster RENAME COLUMN extendedclusterdefinitiontext TO extendedblueprinttext;
ALTER TABLE ONLY cluster RENAME COLUMN clusterdefinition_id TO blueprint_id;
ALTER TABLE ONLY cluster RENAME CONSTRAINT fk_cluster_clusterdefinition_id TO fk_cluster_blueprint_id;

ALTER SEQUENCE clusterdefinition_id_seq RENAME TO blueprint_id_seq;

ALTER TABLE ONLY clusterdefinition RENAME CONSTRAINT clusterdefinition_pkey TO blueprint_pkey;
ALTER TABLE ONLY clusterdefinition RENAME CONSTRAINT clusterdefinitionname_in_org_unique TO blueprintname_in_org_unique;
ALTER TABLE ONLY clusterdefinition RENAME CONSTRAINT fk_clusterdefinition_organization TO fk_blueprint_organization;
ALTER TABLE ONLY clusterdefinition RENAME COLUMN clusterdefinitiontext TO blueprinttext;
ALTER TABLE IF EXISTS clusterdefinition RENAME TO blueprint;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE IF EXISTS blueprint RENAME TO clusterdefinition;
ALTER TABLE ONLY clusterdefinition RENAME COLUMN blueprinttext TO clusterdefinitiontext;
ALTER TABLE ONLY clusterdefinition RENAME CONSTRAINT blueprint_pkey TO clusterdefinition_pkey;
ALTER TABLE ONLY clusterdefinition RENAME CONSTRAINT blueprintname_in_org_unique TO clusterdefinitionname_in_org_unique;
ALTER TABLE ONLY clusterdefinition RENAME CONSTRAINT fk_blueprint_organization TO fk_clusterdefinition_organization;

ALTER SEQUENCE blueprint_id_seq RENAME TO clusterdefinition_id_seq;

ALTER TABLE ONLY cluster RENAME CONSTRAINT fk_cluster_blueprint_id TO fk_cluster_clusterdefinition_id;
ALTER TABLE ONLY cluster RENAME COLUMN blueprint_id TO clusterdefinition_id;
ALTER TABLE ONLY cluster RENAME COLUMN extendedblueprinttext TO extendedclusterdefinitiontext;

ALTER TABLE ONLY servicedescriptor RENAME COLUMN blueprintparams TO clusterdefinitionparams;
ALTER TABLE ONLY servicedescriptor RENAME COLUMN blueprintsecretparams TO clusterdefinitionsecretparams;

ALTER INDEX blueprint_id_idx RENAME TO clusterdefinition_id_idx;
ALTER INDEX blueprint_name_idx RENAME TO clusterdefinition_name_idx;
ALTER INDEX blueprint_org_id_idx RENAME TO clusterdefinition_org_id_idx;


