-- // CB-3585_rename_syncoperation_to_operation

alter table syncoperation rename column syncoperationtype to operationtype;

alter table syncoperation rename to operation;

 -- //@UNDO

alter table operation rename column operationtype to syncoperationtype;

alter table operation rename to syncoperation;
