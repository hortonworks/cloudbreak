-- // CB-2918 update exposesServices to ALL in cluster templates
-- Migration SQL that makes the change goes here.

update gateway_topology
set exposedservices='{"services":["ALL"]}'
where id in (
    select id
    from gateway
    where cluster_id in (
        select id
        from cluster
        where stack_id in (
            select id
            from stack
            where "type"='TEMPLATE'
        )
    )
);
-- //@UNDO
-- SQL to undo the change goes here.


