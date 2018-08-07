-- // BUG-104115 ssotype proxy rename
-- Migration SQL that makes the change goes here.

UPDATE gateway SET ssotype = (CASE
                                WHEN ssotype = 'SSO_PROVIDER'
                                    THEN 'PROXY_SSO'
                                WHEN ssotype = 'NONE'
                                    THEN 'PROXY'
                                WHEN ssotype = 'SSO_CONSUMER'
                                    THEN 'PROXY'
                              END);

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE gateway SET ssotype = (CASE
                                WHEN ssotype = 'PROXY_SSO'
                                    THEN 'SSO_PROVIDER'
                                WHEN ssotype = 'PROXY'
                                    THEN 'NONE'
                              END);


