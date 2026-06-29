# RepLog Exercise Library - Beginner Experience Deep Audit

Zero-assumption audit. Conclusions are drawn from the ACTUAL generated content for
real exercises (not from the architecture). Brutally honest; not a defence of
prior work.

## CORE QUESTION
"Could a complete beginner open any of the 516 exercises and understand what it
is, why, how, what it should feel like, and whether they can attempt it?"

VERDICT: NO. The structure is there, but the generated CONTENT is too generic and,
in many cases, factually wrong. A nervous first-timer would be misled on a
meaningful fraction of exercises.

## HARD EVIDENCE (measured across all 516)
1. ONLY 54 DISTINCT STEP-SETS for 516 exercises (avg 9.6 exercises share identical
   instructions; the most common identical step-set covers 53 exercises). Goblet
   Squat, Barbell Back Squat, Pistol Squat AND Cable Hip Abduction all get the SAME
   four steps ("sit down between your hips... drive through your feet"). The steps
   describe the FAMILY, not the exercise.
2. 8 hip ABDUCTION/ADDUCTION exercises are told to "sit down between your hips" -
   that is the wrong movement entirely. A beginner following these steps would not
   be doing the exercise at all.
3. 24 lateral-raise exercises HIGHLIGHT THE WRONG MUSCLE on the body diagram
   (FRONT_DELTS instead of side deltoids), even though the purpose text correctly
   says "side deltoids". The diagram contradicts the text.
4. 44 exercises have a GRAMMATICALLY BROKEN / duplicated purpose, e.g. Barbell
   Deadlift: "developing the hamstrings, glutes and hamstrings." This instantly
   reads as auto-generated.
5. Cable Hip Abduction purpose says "develops the abductors, glutes and overall leg
   power" but the STEPS are squat steps and the DIAGRAM highlights QUADS - three
   inconsistent signals for one exercise.

## SECTION-BY-SECTION
- PURPOSE: present, <=150 chars, BUT family-generic and sometimes broken (see #4).
  The spec's gold standard ("Goblet Squat: strengthens your legs while teaching
  proper squat technique") is NOT met - ours says the same sentence for every
  squat.
- ONE-SENTENCE PLAIN-ENGLISH DESCRIPTION ("Hold a dumbbell at your chest and squat
  like sitting into a chair"): DOES NOT EXIST. There is only `purpose`. This is the
  single most valuable missing beginner element.
- STEPS: exactly 4 (good cardinality) but family-templated, so they do not teach
  the specific exercise (see #1, #2). Plain-English level is OK ("keep your back
  flat") - jargon is mostly avoided here.
- CUES: 3, memorable, plain ("Drive through your heels", "Keep your chest up").
  This is the STRONGEST section. But identical across a whole family.
- COMMON MISTAKES: useful and injury-relevant, but again family-level.
- BREATHING: clear and plain. Good.
- TEMPO: shows "2-1-2" with a plain explanation - acceptable, though "2-1-2" alone
  is jargon a beginner would not know without the explanation (which is present).
- RANGE OF MOTION: reasonable, family-level.
- SAFETY: practical; advanced moves get an extra warning. Good.
- MOVEMENT PATTERN is shown raw to the user (e.g. "Pull - Vertical Pull",
  "Core - Anti-Extension") - that is internal taxonomy/jargon, not beginner copy.
- CONFIDENCE CARD: difficulty + equipment + learning minutes + ideal experience.
  Decent. But no reassurance language.
- WHY THIS EXERCISE: three stitched sentences. Reads templated ("It appears in your
  recommendations because it fits both your goal and your experience level" on
  nearly everything). Not genuinely personalised; recovery line only with data.
- ALTERNATIVES: always shown (good); easier suggestion or an explanation.

## BEGINNER PERSONA (45, overweight, never lifted, anxious)
- First 5 seconds: they CAN see name, difficulty, equipment, muscles, a purpose.
  They CANNOT get a "do this: sit, pull to chest, return" one-liner, and the
  diagram may mislead them (lateral raise). "Can I do it?" is partly answered by
  difficulty + confidence card.
- Reassurance/emotional design: ABSENT. No "you can start with bodyweight", "this
  is one of the safest exercises", "most people learn this in one session". The
  app assumes confidence; the brief says assume uncertainty. It fails this.

## VISUAL SYSTEM
- Body diagram: front/back, primary filled / secondary outlined, legend, TalkBack
  description - good design, BUT mapping bugs (lateral raise -> front delts) make
  it actively wrong for ~24 exercises.
- Animation: a generic family stick-figure with Play/Pause/Restart. No arrows, no
  motion path, no joint-direction indicators, no labels. A beginner likely still
  could NOT perform an unfamiliar move from it alone - they would reach for YouTube.

## SEARCH (beginner intent)
Offered: muscle groups, Push/Pull/Legs/etc., Hypertrophy/Strength/Fat Loss,
First Week/Building Up/Experienced, No Equipment/Home Workout/Machine Only.
MISSING the beginner-intent vocabulary the brief asks for: "I have bad knees",
"recovering from injury", "improve posture", "improve balance", "first gym
workout", "never exercised before", "lose weight" (only "Fat Loss" jargon-ish),
"stronger legs" (only muscle names). Goal labels (Hypertrophy) are themselves
jargon for a true beginner.

## STORAGE / OFFLINE
- Fully offline, no GIF/video/API/cloud, generated at runtime, ~0 KB assets,
  minimal RAM. This is genuinely excellent and must be preserved. The fix is
  better generation rules + data corrections, NOT adding media.

## SCORES (/10) - honest
- Beginner confidence: 4/10 (structure good; no reassurance; misleads on some).
- Instruction quality: 3/10 (correct shape, but family-generic + wrong for
  abduction/adduction; 54 unique step-sets for 516 exercises).
- Accessibility: 7/10 (content descriptions, no colour-only, scalable - but
  unverified on device; movementPattern jargon hurts screen-reader clarity).
- Visual learning: 3/10 (clean but generic; mapping bugs; no arrows/path/labels;
  cannot learn a new move from it alone).
- Personalisation: 4/10 ("Why" is templated; recovery only with data).
- Storage efficiency: 10/10 (exemplary; offline, zero assets).

## WEAKNESSES RANKED BY IMPACT
1. (CRITICAL, correctness) Steps/diagram WRONG for hip abduction/adduction (8) and
   any non-squat "Legs" move funnelled into squat steps; lateral raise -> front
   delts on the diagram (24). Misleading a beginner on technique is a safety/credibility failure.
2. (HIGH) Family-generic instructions: 54 step-sets for 516 exercises. The library
   does not actually teach the SPECIFIC exercise.
3. (HIGH) Broken/duplicated purpose text (44) - reads obviously auto-generated;
   fails the "purpose should never sound generated" bar.
4. (HIGH) No one-sentence plain-English "do this" description - the highest-value
   missing beginner element.
5. (MEDIUM) No reassurance / emotional-design copy for an anxious beginner.
6. (MEDIUM) Templated "Why this exercise"; not genuinely personalised.
7. (MEDIUM) Animation lacks arrows/motion path/labels; limited teaching value.
8. (MEDIUM) Raw movementPattern taxonomy shown to users as if it were copy.
9. (MEDIUM) Search lacks beginner-intent vocabulary (bad knees, posture, first
   workout, never exercised); goal labels are jargon.

## VS MARKET LEADERS
- Strong / Hevy: minimal per-exercise instruction (often none/short). RepLog's
  STRUCTURE is broader, but their content that exists is exercise-specific. ~Equal
  to better on structure, worse on instruction specificity.
- Fitbod / JEFIT: animated/illustrated per-exercise demonstrations and specific
  cues. RepLog is WORSE on visual teaching (generic stick figure, no real
  demonstration) and instruction specificity.
- Nike Training Club: video coaching + reassuring tone. RepLog is WORSE on emotional
  design and demonstration, but BETTER on offline footprint.
- Boostcamp: program-centric. RepLog comparable on programs.
Net: RepLog leads ONLY on offline footprint and breadth of section structure; it is
behind the leaders on the thing that matters most here - actually teaching a
beginner a specific movement correctly.

## RECOMMENDED REDESIGN (for a true first-timer) - audit recommendation only
1. FIX CORRECTNESS FIRST (no new features): add movement families for hip
   abduction/adduction, knee extension/flexion, hip thrust, and fix the broken
   purpose de-duplication and the lateral-raise -> side-delt diagram mapping.
2. ADD a one-sentence plain-English "Do this" line per family/equipment
   (e.g. "Hold a dumbbell at your chest and squat like sitting into a chair").
3. MAKE steps equipment- and exercise-aware (machine vs barbell vs cable setup
   differs), so identical step-sets drop dramatically.
4. ADD reassurance copy keyed on difficulty/equipment ("Beginner-friendly - you can
   start light", "One of the safest ways to train legs").
5. Replace raw movementPattern with a friendly label.
6. Add beginner-intent search chips (First gym workout, No equipment, Lose weight,
   Improve posture, Easy on the knees) mapped to existing attributes.
7. Animation: add a direction arrow / motion path + a one-word phase label
   (Down/Up) - still code-drawn, still offline.
All of the above stay 100% offline, no media, minimal storage.

## BOTTOM LINE
Not commercially ready as a beginner teaching tool. The engineering (offline,
zero-storage generation) is excellent and worth keeping; the generated CONTENT is
too generic and, for a meaningful subset, incorrect. This is a content-quality and
data-correctness problem, not an architecture problem.
