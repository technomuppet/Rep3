# User Profile Architecture (Sprint 12, Priority 1)

## Storage decision
DataStore (existing `util/PreferencesManager.kt`), NOT Room. Rationale:
- The brief mandates DataStore for profile + legal acceptance and keeping Room for
  workout/history/analytics/training data only.
- Zero Room migrations (DB stays at v15).
- Profile is a small, single-row, frequently-read settings blob - a natural fit
  for a key/value preferences store, consistent with the existing training-profile
  keys.

## Model
`util/profile/UserProfile.kt` (pure data class, no Android deps):

```
data class UserProfile(
    val displayName: String,            // REQUIRED, non-blank
    val dateOfBirthEpochDay: Long? = null, // optional; age computed, never stored
    val heightCm: Double? = null,
    val weightKg: Double? = null,       // canonical kg; converted for display
    val useKg: Boolean = true,
    val experienceLevel: String? = null, // TrainingLevel.name
    val primaryGoal: String? = null,     // TrainingGoal.name
    val weeklyFrequency: Int? = null,
    val equipment: String? = null        // EquipmentAccess.name
)
```

- `isComplete` = `displayName.isNotBlank()` (the only required field).
- Age is ALWAYS derived from `dateOfBirthEpochDay` via `ProfileMath.ageFrom(dob, today)`
  (a pure function with an injected "today" for deterministic testing). Age is
  never persisted, so it cannot drift.

## DataStore keys (added to PreferencesManager.Keys)
- `profile_display_name` (String)
- `profile_dob_epoch_day` (Long)
- `profile_height_cm` (Double)
- `profile_weight_kg` (Double)
- (units reuse the existing `use_kg`; experience/goal/equipment/days reuse the
  existing `profile_level`/`profile_goal`/`profile_equipment`/`profile_days`
  keys to avoid duplication.)

## API on PreferencesManager
- `val userProfile: Flow<UserProfile?>` - emits null until a display name exists.
- `suspend fun setUserProfile(profile: UserProfile)` - writes all keys atomically.
- `val displayName: Flow<String?>` - convenience for greetings.

## Personalisation
A small `util/profile/Greetings.kt` pure helper produces name-aware strings
(time-of-day greeting, possessive titles). UI reads `displayName` from the
relevant ViewModel and falls back gracefully to a neutral string when null
(should not happen post-onboarding, but defensive). Surfaces: Home header,
Recovery Centre title, briefing copy, Settings header.

## Backward compatibility
Existing users (onboarding already complete, no profile) are handled by
Priority 6 version-locking: because legal acceptance did not previously exist,
the version check forces them through the new Welcome -> Profile -> Legal flow
once, capturing a display name, WITHOUT touching any workout/history/template
data (all in Room, untouched).
