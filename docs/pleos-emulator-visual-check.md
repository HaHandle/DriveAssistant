# Pleos emulator visual review

This checklist is for visual review of PR #7 only. Do not assess driving signals, score calculations, or behavior, and do not run automated or functional tests.

Review the current `design/pleos-ui` branch head. Record the commit SHA and the emulator profile used.

## Screens and conditions

- Home, active-session, result, and restricted-state screens, where available.
- Landscape and portrait, if the emulator profile supports both.
- Day and night appearance.
- Accessibility font scale at 1.0 and 1.3, if available.

## Check visually

- Text, buttons, and cards are not clipped, overlapped, or hidden by system bars or the app-window edges.
- Paragraphs remain easy to scan; note any that exceed the Pleos guide's three-line recommendation.
- Primary and secondary actions are visually distinct and large enough to identify at a glance.
- Light and dark palettes preserve readable text and visible card/button boundaries.
- Report the visible text sizes and any screen-size-specific layout breakage. If physical 5 mm / 3 mm text height cannot be established from the emulator profile, mark it unverified rather than guessing.

## Report on PR #7

- Emulator image/profile and app commit SHA:
- Resolution and density shown by the emulator:
- Screen, orientation, theme, and font scale checked:
- Pass/fail for clipping, overlap, system insets, and readability:
- Screenshots attached:
- Exact screen and issue for each finding (if any):
- Unavailable checks and why:
