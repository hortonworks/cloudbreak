-- // updated tags on stack table
-- Migration SQL that makes the change goes here.

UPDATE stack SET tags = REPLACE(tags, '"userDefined"', '"userDefinedTags"');
-- Migration of not hdc clusters
UPDATE stack SET tags=CONCAT(SUBSTRING(tags, 0, length(tags)), ',"applicationTags":{}}') WHERE NOT (tags LIKE '%"type"%');
-- Migration of clusters which are not contains default_tags
UPDATE stack SET tags=CONCAT(SUBSTRING(tags, 0, length(tags)), ',"defaultTags":{}}') WHERE NOT (tags LIKE '%"defaultTags"%');
-- Migration of hdc clusters which are not attached to a datalake
UPDATE stack SET tags=CONCAT(SUBSTRING(tags, 0, length(tags)), ',"applicationTags":{"type":"base"}}') WHERE (tags LIKE '%"type"%' AND tags LIKE '%"base"%' AND NOT tags LIKE '%"datalakeId"%' AND NOT tags LIKE '%"applicationTags"%');
-- Migration of hdc clusters which are datalake
UPDATE stack SET tags=CONCAT(SUBSTRING(tags, 0, length(tags)), ',"applicationTags":{"type":"datalake"}}') WHERE (tags LIKE '%"datalake"%');
-- Migration of hdc clusters which are attached to datalake
UPDATE stack SET tags=CONCAT(SUBSTRING(tags, 0, length(tags)), ',"applicationTags":{', (SELECT SUBSTRING(s.tags, POSITION('"datalakeId"' IN s.tags), STRPOS(SUBSTRING(s.tags, POSITION('"datalakeId"' IN s.tags)), ',')) FROM stack s where stack.id=s.id), (SELECT SUBSTRING(s.tags, POSITION('"datalakeName"' IN s.tags), STRPOS(SUBSTRING(s.tags, POSITION('"datalakeName"' IN s.tags)), ',')) FROM stack s where stack.id=s.id), '"type":"base"', '}}') WHERE (tags LIKE '%"type"%' AND tags LIKE '%"base"%' AND tags LIKE '%"datalakeId"%' AND tags LIKE '%"datalakeName"%');

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE stack SET tags = REPLACE(tags, 'userDefinedTags', 'userDefined');

-- UPDATE stack SET tags='{"type":"base","userDefined":{}}' where id=81;
-- UPDATE stack SET tags='{"type":"base", "datalakeId":"1","datalakeName":"1","userDefined":{}}' where id=84;
-- UPDATE stack SET tags='{"userDefined":{}}' where id=95;
-- UPDATE stack SET tags='{"userDefined":{"dsfdsf":"dsfdsfsdfdsf"}}' where id=98;
-- UPDATE stack SET tags='{"userDefined":{"dsfdsf": "dsfdsfsdfdsf"}}' where id=99;
-- UPDATE stack SET tags='{"userDefined":{"dsfdsf" :"dsfdsfsdfdsf"}}' where id=94;
-- UPDATE stack SET tags='{"userDefined":{"dsfdsf" : "dsfdsfsdfdsf"}}' where id=96;
-- UPDATE stack SET tags='{"type":"datalake","userDefined":{}}' where id=100;