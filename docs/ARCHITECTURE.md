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

`FeedbackTiming.LIVE` describes when feedback would be useful; it does not by itself
authorize presentation while driving. The presentation adapter may deliver LIVE
feedback only when all of the following are confirmed:

1. The approved Pleos app category permits the specific function while driving.
2. The active regional driving policy permits it.
3. The current `CarUxRestrictions` state permits the exact UI and interaction.

If any condition is disallowed, unavailable, or unknown, the app must suppress
in-drive visual coaching and related controls. It may retain the observation for a
stationary or post-drive review. A gear or speed value alone must not be treated as
permission to show LIVE feedback.

References: [driving-safety design](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline)
and [app-category restrictions](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-category).

## Dependency direction

`presentation` and `infrastructure` may depend on `application`, `feature`, and
`domain`. Core packages must not import PLEOS SDK or UI framework types.

