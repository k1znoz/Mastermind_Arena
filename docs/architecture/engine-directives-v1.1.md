# Engine Directives v1.1

Allowed structural directives:

- ACCEPT_ACTION
- REJECT_ACTION
- CONTINUE_TURN
- END_TURN
- START_NEXT_TURN
- FINISH_MATCH
- CANCEL_MATCH

## Incoherent Combinations

- REJECT_ACTION + END_TURN
- REJECT_ACTION + FINISH_MATCH
- CONTINUE_TURN + END_TURN
- START_NEXT_TURN without END_TURN
- FINISH_MATCH + START_NEXT_TURN
- CANCEL_MATCH + START_NEXT_TURN

## Terminal Exclusivity

- FINISH_MATCH + CANCEL_MATCH is forbidden and treated as terminal conflict.
