-- // CB-2902 Experimental features migration step 2
-- Migration SQL that makes the change goes here.

WITH subquery AS (
SELECT
 environment.id AS id,
 (SELECT row_to_json(json_input)
 FROM (SELECT environment.tunnel, environment.idbroker_mapping_source) AS json_input) AS json_out
FROM environment)
update environment
set experimentalfeatures = subquery.json_out
from subquery where environment.id = subquery.id AND experimentalfeatures is NULL;


-- //@UNDO
-- SQL to undo the change goes here.

