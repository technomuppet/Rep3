# Google Play Compliance Report (Sprint 10, Priority 5)

## Foreground service
Issue (from audit): `RestTimerService` declared `foregroundServiceType="mediaPlayback"`
with the `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission. A rest timer is not media
playback; Google Play enforces that the declared FGS type matches the actual use,
and `mediaPlayback` requires an active media session — a policy/runtime mismatch.

Fix:
- `foregroundServiceType` → `shortService` (the timer is a brief, user-initiated
  countdown of a few minutes — exactly the `shortService` use case).
- Removed `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- `FOREGROUND_SERVICE_SHORT_SERVICE` was already declared; `FOREGROUND_SERVICE`
  retained. `startForeground` uses the manifest-declared type.
No behavioural change: the timer still runs in the foreground with its
notification; `shortService` permits the short lifetime a rest timer needs.

Note for the implementer on the SDK host: `shortService` foreground services have
a system time limit (~3 minutes). Rest periods are within this; if a user sets a
very long rest, confirm the service stops/transitions gracefully (it already
ticks to zero and can be skipped/extended via the notification actions).

## Permissions review (full manifest)
- POST_NOTIFICATIONS — rest-timer + completion notifications. Justified.
- FOREGROUND_SERVICE / FOREGROUND_SERVICE_SHORT_SERVICE — rest timer. Justified.
- VIBRATE — timer-complete haptics. Justified.
- WAKE_LOCK — keep timer accurate while screen off. Justified.
- WRITE_EXTERNAL_STORAGE (maxSdkVersion=28) — legacy public-Downloads export
  fallback only. Scoped correctly; not requested on API 29+.
- No INTERNET, no location, no contacts, no media-library permissions.

## Privacy / data safety
- Fully offline: no networking libraries, no INTERNET permission, no telemetry,
  no cloud, no accounts, no ads. Data is local (Room + DataStore). Export is
  user-initiated to user-visible storage (MediaStore/SAF) and device-to-device
  sharing via the OS share sheet.
- Data Safety form should declare: no data collected, no data shared, all data
  stays on device, user can export and delete their data.

## Verdict
The foreground-service compliance risk is resolved. The permission set is minimal
and justified. No remaining Play-policy blocker identified from source (final
confirmation on a release build / Play console pre-launch report recommended).
