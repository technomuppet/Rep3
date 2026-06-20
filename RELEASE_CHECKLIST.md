# RepLog Release Checklist

Use this before uploading a production Android App Bundle.

## Code and build

- Open the project in Android Studio with JDK 17+.
- Run a clean debug build.
- Run a release build with minification enabled.
- Test database migration from v1/v2/v3/v4/v5/v6/v7 to v8.
- Test first-launch onboarding on a clean install.
- Test active workout recovery after force-stopping/reopening where possible.
- Test CSV export and share sheet.
- Test JSON backup, external import, local restore and prescription preservation.
- Test bodyweight logging, goal line, rolling average and deletion.
- Test template creation, editing, reorder and deletion.
- Test advanced set types, RPE, tempo and superset assignment.
- Test custom plate lists in kg and lb modes.
- Verify every feature is available without purchase or account setup.

## Store identity

- Replace placeholder app icon.
- Replace package name if needed.
- Add real versionCode/versionName strategy.
- Add support email.
- Publish privacy policy URL.
- Prepare screenshots for phone sizes.
- Prepare feature graphic.
- Prepare short and long descriptions.

## Play Console

- Create app listing.
- Complete Data Safety form.
- Add privacy policy URL.
- Configure app access declarations if needed.
- Configure content rating.
- Configure target audience.
- Upload internal testing AAB first.

## Release signing

- Generate upload key securely.
- Enable Play App Signing.
- Store keystore outside the repo.
- Do not commit signing credentials.

## Final smoke test

- Install internal test build from Play.
- Complete onboarding.
- Start workout from template.
- Log sets and PR.
- Finish workout and view summary.
- Check Progress tab updates.
- Export CSV and JSON.
- Restore JSON into a fresh install.
- Confirm no billing screens, locked features or purchase prompts appear.
