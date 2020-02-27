-- // CB-5688
-- Migration SQL that makes the change goes here.

UPDATE sdxcluster SET deleted = deleteddatalakes.created
FROM (SELECT datalake, created, status FROM sdxstatus WHERE status = 'DELETED' GROUP BY datalake, created, status ORDER BY created) AS deleteddatalakes
WHERE sdxcluster.deleted IS NULL AND sdxcluster.id = deleteddatalakes.datalake;

-- //@UNDO
-- SQL to undo the change goes here.
