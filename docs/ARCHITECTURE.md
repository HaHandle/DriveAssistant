# DriveAssistant architecture

DriveAssistant is organized around a six-step beginner training curriculum with a
short preparation module. The code is split so that scoring rules do not depend on
the vehicle SDK or the display framework.

## Layer responsibilities

- `domain`: shared training concepts and evaluator contracts.
- `feature`: rules and coaching content owned by each training step.
- `application`: training-session orchestration and feedback scheduling.
- `port`: boundaries for vehicle data, feedback delivery, and result storage.
- `infrastructure`: adapters for PLEOS/vehicle APIs and persistence.
- `presentation`: the driving-safe dashboard, live coach, and post-drive review.

## Training flow

Each feature follows the same loop:

1. Explain the goal while the vehicle is stationary.
2. Observe only the signals needed for the current step.
3. Deliver one short, prioritized coaching message at a time.
4. Produce a post-drive result with strengths, one improvement, and a next task.

Live presentation must remain glanceable. Detailed scores and history belong in the
post-drive review, not in the moving-vehicle UI.

## Dependency direction

`presentation` and `infrastructure` may depend on `application`, `feature`, and
`domain`. Core packages must not import PLEOS SDK or UI framework types.

