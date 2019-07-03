-- // CB-1563_Monitor_CM_host_statuses_from_Periscope
-- Migration SQL that makes the change goes here.

ALTER TABLE "ambari" RENAME COLUMN "ambari_host" TO "host";
ALTER TABLE "ambari" RENAME COLUMN "ambari_port" TO "port";
ALTER TABLE "ambari" RENAME COLUMN "ambari_user" TO "lgn_user";
ALTER TABLE "ambari" RENAME COLUMN "ambari_pass" TO "lgn_pass";
ALTER TABLE "ambari" RENAME CONSTRAINT "ambari_pkey" TO "cluster_manager_pkey";
ALTER TABLE "ambari" ADD COLUMN "variant" varchar(255);

ALTER TABLE "ambari" RENAME TO "cluster_manager";

ALTER SEQUENCE "ambari_id_seq" RENAME TO "cluster_manager_id_seq";

ALTER TABLE "cluster" RENAME COLUMN "ambari_id" TO "cluster_manager_id";
ALTER TABLE "cluster" RENAME CONSTRAINT "fk_cluster_ambari_id" TO "fk_cluster_cluster_manager_id";

UPDATE "cluster_manager" SET variant = 'CLOUDERA_MANAGER';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE "cluster" RENAME CONSTRAINT "fk_cluster_cluster_manager_id" TO "fk_cluster_ambari_id";
ALTER TABLE "cluster" RENAME COLUMN "cluster_manager_id" TO "ambari_id";

ALTER SEQUENCE "cluster_manager_id_seq" RENAME TO "ambari_id_seq";

ALTER TABLE "cluster_manager" RENAME TO "ambari";

ALTER TABLE "ambari" RENAME COLUMN "host" TO "ambari_host";
ALTER TABLE "ambari" RENAME COLUMN "port" TO "ambari_port";
ALTER TABLE "ambari" RENAME COLUMN "lgn_user" TO "ambari_user";
ALTER TABLE "ambari" RENAME COLUMN "lgn_pass" TO "ambari_pass";
ALTER TABLE "ambari" RENAME CONSTRAINT "cluster_manager_pkey" TO "ambari_pkey";
ALTER TABLE "ambari" DROP COLUMN IF EXISTS "variant";

