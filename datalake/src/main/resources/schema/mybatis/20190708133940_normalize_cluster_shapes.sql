-- // CB-1988 normalize cluster shapes to enums. All previous clusters should be Custom
-- Migration SQL that makes the change goes here.

UPDATE sdxcluster SET clustershape = 'CUSTOM';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE sdxcluster SET clustershape = 'big';
