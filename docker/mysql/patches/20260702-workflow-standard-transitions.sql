USE wo_system;

INSERT INTO wf_definition (id, name, description, definition_json, version, status)
SELECT 1,
       'standard-work-order-flow',
       'Standard work order lifecycle',
       '{"states":["DRAFT","OPEN","AI_SOLVED","ESCALATED","IN_PROGRESS","RESOLVED","CLOSED"],"initialState":"DRAFT"}',
       1,
       1
WHERE NOT EXISTS (
    SELECT 1 FROM wf_definition WHERE id = 1
);

UPDATE wf_definition
SET status = 1,
    deleted = 0
WHERE id = 1;

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'DRAFT', 'OPEN', 'submit', 'USER', 1
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'DRAFT' AND to_state = 'OPEN' AND event = 'submit');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'OPEN', 'IN_PROGRESS', 'start', 'AGENT', 2
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'OPEN' AND to_state = 'IN_PROGRESS' AND event = 'start');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'OPEN', 'ESCALATED', 'escalate', 'USER', 3
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'OPEN' AND to_state = 'ESCALATED' AND event = 'escalate');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'OPEN', 'CLOSED', 'cancel', 'USER', 4
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'OPEN' AND to_state = 'CLOSED' AND event = 'cancel');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'AI_SOLVED', 'CLOSED', 'confirm_ai_solution', 'USER', 5
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'AI_SOLVED' AND to_state = 'CLOSED' AND event = 'confirm_ai_solution');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'AI_SOLVED', 'ESCALATED', 'reject_ai_solution', 'USER', 6
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'AI_SOLVED' AND to_state = 'ESCALATED' AND event = 'reject_ai_solution');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'ESCALATED', 'IN_PROGRESS', 'claim', 'AGENT', 7
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'ESCALATED' AND to_state = 'IN_PROGRESS' AND event = 'claim');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'IN_PROGRESS', 'RESOLVED', 'resolve', 'AGENT', 8
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'IN_PROGRESS' AND to_state = 'RESOLVED' AND event = 'resolve');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'IN_PROGRESS', 'OPEN', 'reassign', 'MANAGER', 9
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'IN_PROGRESS' AND to_state = 'OPEN' AND event = 'reassign');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'RESOLVED', 'CLOSED', 'close', 'USER', 10
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'RESOLVED' AND to_state = 'CLOSED' AND event = 'close');

INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order)
SELECT 1, 'RESOLVED', 'IN_PROGRESS', 'reopen', 'USER', 11
WHERE NOT EXISTS (SELECT 1 FROM wf_transition WHERE definition_id = 1 AND from_state = 'RESOLVED' AND to_state = 'IN_PROGRESS' AND event = 'reopen');

UPDATE wf_transition
SET deleted = 0
WHERE definition_id = 1
  AND (
      (from_state = 'DRAFT' AND to_state = 'OPEN' AND event = 'submit')
      OR (from_state = 'OPEN' AND to_state = 'IN_PROGRESS' AND event = 'start')
      OR (from_state = 'OPEN' AND to_state = 'ESCALATED' AND event = 'escalate')
      OR (from_state = 'OPEN' AND to_state = 'CLOSED' AND event = 'cancel')
      OR (from_state = 'AI_SOLVED' AND to_state = 'CLOSED' AND event = 'confirm_ai_solution')
      OR (from_state = 'AI_SOLVED' AND to_state = 'ESCALATED' AND event = 'reject_ai_solution')
      OR (from_state = 'ESCALATED' AND to_state = 'IN_PROGRESS' AND event = 'claim')
      OR (from_state = 'IN_PROGRESS' AND to_state = 'RESOLVED' AND event = 'resolve')
      OR (from_state = 'IN_PROGRESS' AND to_state = 'OPEN' AND event = 'reassign')
      OR (from_state = 'RESOLVED' AND to_state = 'CLOSED' AND event = 'close')
      OR (from_state = 'RESOLVED' AND to_state = 'IN_PROGRESS' AND event = 'reopen')
  );

UPDATE wf_transition
SET required_role = 'AGENT',
    sort_order = 8
WHERE definition_id = 1
  AND from_state = 'IN_PROGRESS'
  AND to_state = 'RESOLVED'
  AND event = 'resolve';
