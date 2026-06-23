#!/usr/bin/env python3
"""
Generate a large, well-tagged exercise library for RepLog (exercises.json).

Output schema matches DataSeeder.ExerciseData:
  name, category, equipment, type, muscles[], primaryMuscles[],
  secondaryMuscles[], movementPattern, difficulty, mediaAsset

Design goals:
- 500+ unique exercises spanning Barbell, Dumbbell, Machine, Cable,
  Bodyweight, Kettlebell, Smith Machine, EZ Bar, Band, Plate.
- Consistent primary/secondary muscle tagging and movement patterns so that
  Training DNA, recovery, muscle-gap and recommendation systems work well.
- Preserve the original 32 exercises verbatim (templates reference them by name).
"""
import json
import re

# ----- The original 32, kept verbatim so existing templates/data still match -----
ORIGINAL = json.loads(r'''
[
{"name":"Barbell Back Squat","category":"Legs","equipment":"Barbell","type":"Strength","muscles":["Quadriceps","Glutes","Hamstrings"],"primaryMuscles":["Quadriceps"],"secondaryMuscles":["Glutes","Hamstrings"],"movementPattern":"Legs \u2022 Squat","difficulty":"Intermediate","mediaAsset":"exercise_media/barbell_back_squat.gif"},
{"name":"Barbell Front Squat","category":"Legs","equipment":"Barbell","type":"Strength","muscles":["Quadriceps","Core","Glutes"],"primaryMuscles":["Quadriceps"],"secondaryMuscles":["Core","Glutes"],"movementPattern":"Legs \u2022 Squat","difficulty":"Intermediate","mediaAsset":"exercise_media/barbell_front_squat.gif"},
{"name":"Barbell Bench Press","category":"Chest","equipment":"Barbell","type":"Strength","muscles":["Pectorals","Triceps","Front Deltoids"],"primaryMuscles":["Pectorals"],"secondaryMuscles":["Triceps","Front Deltoids"],"movementPattern":"Push \u2022 Horizontal Press","difficulty":"Intermediate","mediaAsset":"exercise_media/barbell_bench_press.gif"},
{"name":"Incline Barbell Bench Press","category":"Chest","equipment":"Barbell","type":"Strength","muscles":["Pectorals","Front Deltoids","Triceps"],"primaryMuscles":["Pectorals"],"secondaryMuscles":["Front Deltoids","Triceps"],"movementPattern":"Push \u2022 Incline Press","difficulty":"Intermediate","mediaAsset":"exercise_media/incline_barbell_bench_press.gif"},
{"name":"Barbell Deadlift","category":"Back","equipment":"Barbell","type":"Strength","muscles":["Hamstrings","Glutes","Back"],"primaryMuscles":["Hamstrings"],"secondaryMuscles":["Glutes","Back"],"movementPattern":"Hinge","difficulty":"Advanced","mediaAsset":"exercise_media/barbell_deadlift.gif"},
{"name":"Barbell Romanian Deadlift","category":"Legs","equipment":"Barbell","type":"Strength","muscles":["Hamstrings","Glutes","Back"],"primaryMuscles":["Hamstrings"],"secondaryMuscles":["Glutes","Back"],"movementPattern":"Hinge","difficulty":"Intermediate","mediaAsset":"exercise_media/barbell_romanian_deadlift.gif"},
{"name":"Barbell Overhead Press","category":"Shoulders","equipment":"Barbell","type":"Strength","muscles":["Front Deltoids","Triceps","Side Deltoids"],"primaryMuscles":["Front Deltoids"],"secondaryMuscles":["Triceps","Side Deltoids"],"movementPattern":"Push \u2022 Vertical Press","difficulty":"Intermediate","mediaAsset":"exercise_media/barbell_overhead_press.gif"},
{"name":"Barbell Bent Over Row","category":"Back","equipment":"Barbell","type":"Strength","muscles":["Lats","Upper Back","Biceps"],"primaryMuscles":["Lats"],"secondaryMuscles":["Upper Back","Biceps"],"movementPattern":"Pull \u2022 Horizontal Row","difficulty":"Intermediate","mediaAsset":"exercise_media/barbell_bent_over_row.gif"},
{"name":"Pull Ups","category":"Back","equipment":"Bodyweight","type":"Strength","muscles":["Lats","Biceps","Upper Back"],"primaryMuscles":["Lats"],"secondaryMuscles":["Biceps","Upper Back"],"movementPattern":"Pull \u2022 Vertical Pull","difficulty":"Intermediate","mediaAsset":"exercise_media/pull_ups.gif"},
{"name":"Chin Ups","category":"Back","equipment":"Bodyweight","type":"Strength","muscles":["Lats","Biceps"],"primaryMuscles":["Lats"],"secondaryMuscles":["Biceps"],"movementPattern":"Pull \u2022 Vertical Pull","difficulty":"Intermediate","mediaAsset":"exercise_media/chin_ups.gif"},
{"name":"Push Ups","category":"Chest","equipment":"Bodyweight","type":"Strength","muscles":["Pectorals","Triceps","Front Deltoids"],"primaryMuscles":["Pectorals"],"secondaryMuscles":["Triceps","Front Deltoids"],"movementPattern":"Push \u2022 Horizontal Press","difficulty":"Beginner","mediaAsset":"exercise_media/push_ups.gif"},
{"name":"Tricep Dips","category":"Arms","equipment":"Bodyweight","type":"Strength","muscles":["Triceps","Pectorals"],"primaryMuscles":["Triceps"],"secondaryMuscles":["Pectorals"],"movementPattern":"Push \u2022 Vertical Press","difficulty":"Intermediate","mediaAsset":"exercise_media/tricep_dips.gif"},
{"name":"Dumbbell Bench Press","category":"Chest","equipment":"Dumbbell","type":"Strength","muscles":["Pectorals","Triceps","Front Deltoids"],"primaryMuscles":["Pectorals"],"secondaryMuscles":["Triceps","Front Deltoids"],"movementPattern":"Push \u2022 Horizontal Press","difficulty":"Beginner","mediaAsset":"exercise_media/dumbbell_bench_press.gif"},
{"name":"Incline Dumbbell Press","category":"Chest","equipment":"Dumbbell","type":"Strength","muscles":["Pectorals","Front Deltoids","Triceps"],"primaryMuscles":["Pectorals"],"secondaryMuscles":["Front Deltoids","Triceps"],"movementPattern":"Push \u2022 Incline Press","difficulty":"Beginner","mediaAsset":"exercise_media/incline_dumbbell_press.gif"},
{"name":"Dumbbell Shoulder Press","category":"Shoulders","equipment":"Dumbbell","type":"Strength","muscles":["Front Deltoids","Triceps","Side Deltoids"],"primaryMuscles":["Front Deltoids"],"secondaryMuscles":["Triceps","Side Deltoids"],"movementPattern":"Push \u2022 Vertical Press","difficulty":"Beginner","mediaAsset":"exercise_media/dumbbell_shoulder_press.gif"},
{"name":"Dumbbell Lateral Raise","category":"Shoulders","equipment":"Dumbbell","type":"Strength","muscles":["Side Deltoids"],"primaryMuscles":["Side Deltoids"],"secondaryMuscles":[],"movementPattern":"Shoulders \u2022 Lateral Raise","difficulty":"Beginner","mediaAsset":"exercise_media/dumbbell_lateral_raise.gif"},
{"name":"Dumbbell Bicep Curl","category":"Arms","equipment":"Dumbbell","type":"Strength","muscles":["Biceps"],"primaryMuscles":["Biceps"],"secondaryMuscles":[],"movementPattern":"Pull \u2022 Elbow Flexion","difficulty":"Beginner","mediaAsset":"exercise_media/dumbbell_bicep_curl.gif"},
{"name":"Hammer Curls","category":"Arms","equipment":"Dumbbell","type":"Strength","muscles":["Biceps","Forearms"],"primaryMuscles":["Biceps"],"secondaryMuscles":["Forearms"],"movementPattern":"Pull \u2022 Elbow Flexion","difficulty":"Beginner","mediaAsset":"exercise_media/hammer_curls.gif"},
{"name":"Dumbbell Row","category":"Back","equipment":"Dumbbell","type":"Strength","muscles":["Lats","Upper Back","Biceps"],"primaryMuscles":["Lats"],"secondaryMuscles":["Upper Back","Biceps"],"movementPattern":"Pull \u2022 Horizontal Row","difficulty":"Beginner","mediaAsset":"exercise_media/dumbbell_row.gif"},
{"name":"Goblet Squat","category":"Legs","equipment":"Dumbbell","type":"Strength","muscles":["Quadriceps","Glutes"],"primaryMuscles":["Quadriceps"],"secondaryMuscles":["Glutes"],"movementPattern":"Legs \u2022 Squat","difficulty":"Beginner","mediaAsset":"exercise_media/goblet_squat.gif"},
{"name":"Leg Press","category":"Legs","equipment":"Machine","type":"Strength","muscles":["Quadriceps","Glutes","Hamstrings"],"primaryMuscles":["Quadriceps"],"secondaryMuscles":["Glutes","Hamstrings"],"movementPattern":"Legs \u2022 Squat","difficulty":"Beginner","mediaAsset":"exercise_media/leg_press.gif"},
{"name":"Leg Extension","category":"Legs","equipment":"Machine","type":"Strength","muscles":["Quadriceps"],"primaryMuscles":["Quadriceps"],"secondaryMuscles":[],"movementPattern":"Legs \u2022 Knee Extension","difficulty":"Beginner","mediaAsset":"exercise_media/leg_extension.gif"},
{"name":"Seated Leg Curl","category":"Legs","equipment":"Machine","type":"Strength","muscles":["Hamstrings"],"primaryMuscles":["Hamstrings"],"secondaryMuscles":[],"movementPattern":"Legs \u2022 Knee Flexion","difficulty":"Beginner","mediaAsset":"exercise_media/seated_leg_curl.gif"},
{"name":"Calf Raises","category":"Legs","equipment":"Machine","type":"Strength","muscles":["Calves"],"primaryMuscles":["Calves"],"secondaryMuscles":[],"movementPattern":"Legs \u2022 Calf Raise","difficulty":"Beginner","mediaAsset":"exercise_media/calf_raises.gif"},
{"name":"Lat Pulldown","category":"Back","equipment":"Cable","type":"Strength","muscles":["Lats","Biceps","Upper Back"],"primaryMuscles":["Lats"],"secondaryMuscles":["Biceps","Upper Back"],"movementPattern":"Pull \u2022 Vertical Pull","difficulty":"Beginner","mediaAsset":"exercise_media/lat_pulldown.gif"},
{"name":"Cable Row","category":"Back","equipment":"Cable","type":"Strength","muscles":["Lats","Upper Back","Biceps"],"primaryMuscles":["Lats"],"secondaryMuscles":["Upper Back","Biceps"],"movementPattern":"Pull \u2022 Horizontal Row","difficulty":"Beginner","mediaAsset":"exercise_media/cable_row.gif"},
{"name":"Cable Chest Fly","category":"Chest","equipment":"Cable","type":"Strength","muscles":["Pectorals","Front Deltoids"],"primaryMuscles":["Pectorals"],"secondaryMuscles":["Front Deltoids"],"movementPattern":"Push \u2022 Chest Fly","difficulty":"Beginner","mediaAsset":"exercise_media/cable_chest_fly.gif"},
{"name":"Tricep Pushdown","category":"Arms","equipment":"Cable","type":"Strength","muscles":["Triceps"],"primaryMuscles":["Triceps"],"secondaryMuscles":[],"movementPattern":"Push \u2022 Elbow Extension","difficulty":"Beginner","mediaAsset":"exercise_media/tricep_pushdown.gif"},
{"name":"Face Pulls","category":"Shoulders","equipment":"Cable","type":"Strength","muscles":["Rear Deltoids","Upper Back"],"primaryMuscles":["Rear Deltoids"],"secondaryMuscles":["Upper Back"],"movementPattern":"Pull \u2022 Face Pull","difficulty":"Beginner","mediaAsset":"exercise_media/face_pulls.gif"},
{"name":"Plank","category":"Core","equipment":"Bodyweight","type":"Strength","muscles":["Core"],"primaryMuscles":["Core"],"secondaryMuscles":[],"movementPattern":"Core \u2022 Anti-Extension","difficulty":"Beginner","mediaAsset":"exercise_media/plank.gif"},
{"name":"Russian Twist","category":"Core","equipment":"Bodyweight","type":"Strength","muscles":["Core","Obliques"],"primaryMuscles":["Obliques"],"secondaryMuscles":["Core"],"movementPattern":"Core \u2022 Rotation","difficulty":"Beginner","mediaAsset":"exercise_media/russian_twist.gif"},
{"name":"Leg Raises","category":"Core","equipment":"Bodyweight","type":"Strength","muscles":["Core"],"primaryMuscles":["Core"],"secondaryMuscles":[],"movementPattern":"Core \u2022 Hip Flexion","difficulty":"Beginner","mediaAsset":"exercise_media/leg_raises.gif"}
]
''')

BULLET = "\u2022"

def slug(name):
    return "exercise_media/" + re.sub(r'[^a-z0-9]+', '_', name.lower()).strip('_') + ".gif"

def ex(name, category, equipment, primary, secondary, pattern, difficulty="Intermediate", type_="Strength"):
    muscles = primary + [m for m in secondary if m not in primary]
    return {
        "name": name, "category": category, "equipment": equipment, "type": type_,
        "muscles": muscles, "primaryMuscles": primary, "secondaryMuscles": secondary,
        "movementPattern": pattern, "difficulty": difficulty, "mediaAsset": slug(name),
    }

out = []
seen = set()
def add(e):
    if e["name"].lower() in seen:
        return
    seen.add(e["name"].lower())
    out.append(e)

for e in ORIGINAL:
    add(e)

# Movement-pattern constants
P_HPRESS = f"Push {BULLET} Horizontal Press"
P_IPRESS = f"Push {BULLET} Incline Press"
P_DPRESS = f"Push {BULLET} Decline Press"
P_VPRESS = f"Push {BULLET} Vertical Press"
P_FLY    = f"Push {BULLET} Chest Fly"
P_VPULL  = f"Pull {BULLET} Vertical Pull"
P_HROW   = f"Pull {BULLET} Horizontal Row"
P_FACE   = f"Pull {BULLET} Face Pull"
P_CURL   = f"Pull {BULLET} Elbow Flexion"
P_TRI    = f"Push {BULLET} Elbow Extension"
P_LAT    = f"Shoulders {BULLET} Lateral Raise"
P_REAR   = f"Shoulders {BULLET} Rear Delt"
P_SQUAT  = f"Legs {BULLET} Squat"
P_HINGE  = "Hinge"
P_LUNGE  = f"Legs {BULLET} Lunge"
P_KEXT   = f"Legs {BULLET} Knee Extension"
P_KFLEX  = f"Legs {BULLET} Knee Flexion"
P_CALF   = f"Legs {BULLET} Calf Raise"
P_HIPABD = f"Legs {BULLET} Hip Abduction"
P_HIPADD = f"Legs {BULLET} Hip Adduction"
P_CORE_AE = f"Core {BULLET} Anti-Extension"
P_CORE_FLEX = f"Core {BULLET} Trunk Flexion"
P_CORE_ROT = f"Core {BULLET} Rotation"
P_CORE_HF = f"Core {BULLET} Hip Flexion"
P_CARRY = f"Core {BULLET} Carry"
P_SHRUG = f"Pull {BULLET} Shrug"
P_FOREARM = f"Pull {BULLET} Wrist Flexion"
P_GLUTE = f"Legs {BULLET} Hip Extension"

CHEST=["Pectorals"]; TRIS=["Triceps"]; FD=["Front Deltoids"]; SD=["Side Deltoids"]; RD=["Rear Deltoids"]
LATS=["Lats"]; UB=["Upper Back"]; TRAPS=["Traps"]; BI=["Biceps"]; FA=["Forearms"]
QUAD=["Quadriceps"]; HAM=["Hamstrings"]; GLU=["Glutes"]; CALF=["Calves"]; CORE=["Core"]; OBL=["Obliques"]; BACK=["Back"]; ADD=["Adductors"]; ABD=["Abductors"]

# ============================= CHEST =============================
chest = [
 ("Decline Barbell Bench Press","Barbell",CHEST,TRIS+FD,P_DPRESS,"Intermediate"),
 ("Close Grip Bench Press","Barbell",TRIS,CHEST+FD,P_HPRESS,"Intermediate"),
 ("Wide Grip Bench Press","Barbell",CHEST,TRIS+FD,P_HPRESS,"Intermediate"),
 ("Floor Press","Barbell",CHEST,TRIS,P_HPRESS,"Intermediate"),
 ("Decline Dumbbell Press","Dumbbell",CHEST,TRIS+FD,P_DPRESS,"Beginner"),
 ("Flat Dumbbell Fly","Dumbbell",CHEST,FD,P_FLY,"Beginner"),
 ("Incline Dumbbell Fly","Dumbbell",CHEST,FD,P_FLY,"Beginner"),
 ("Dumbbell Pullover","Dumbbell",CHEST,LATS,P_FLY,"Intermediate"),
 ("Neutral Grip Dumbbell Press","Dumbbell",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Squeeze Press","Dumbbell",CHEST,TRIS,P_HPRESS,"Beginner"),
 ("Machine Chest Press","Machine",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Incline Machine Press","Machine",CHEST,FD+TRIS,P_IPRESS,"Beginner"),
 ("Decline Machine Press","Machine",CHEST,TRIS,P_DPRESS,"Beginner"),
 ("Pec Deck Fly","Machine",CHEST,FD,P_FLY,"Beginner"),
 ("Hammer Strength Chest Press","Machine",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Smith Machine Bench Press","Smith Machine",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Smith Machine Incline Press","Smith Machine",CHEST,FD+TRIS,P_IPRESS,"Beginner"),
 ("Low Cable Fly","Cable",CHEST,FD,P_FLY,"Beginner"),
 ("High Cable Fly","Cable",CHEST,FD,P_FLY,"Beginner"),
 ("Cable Crossover","Cable",CHEST,FD,P_FLY,"Beginner"),
 ("Standing Cable Press","Cable",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Incline Cable Press","Cable",CHEST,FD+TRIS,P_IPRESS,"Beginner"),
 ("Single Arm Cable Fly","Cable",CHEST,FD,P_FLY,"Intermediate"),
 ("Incline Push Ups","Bodyweight",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Decline Push Ups","Bodyweight",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Diamond Push Ups","Bodyweight",TRIS,CHEST,P_HPRESS,"Intermediate"),
 ("Wide Push Ups","Bodyweight",CHEST,TRIS,P_HPRESS,"Beginner"),
 ("Chest Dips","Bodyweight",CHEST,TRIS+FD,P_DPRESS,"Intermediate"),
 ("Plyometric Push Ups","Bodyweight",CHEST,TRIS,P_HPRESS,"Advanced"),
 ("Archer Push Ups","Bodyweight",CHEST,TRIS,P_HPRESS,"Advanced"),
 ("Banded Push Ups","Band",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Banded Chest Press","Band",CHEST,TRIS,P_HPRESS,"Beginner"),
 ("Banded Chest Fly","Band",CHEST,FD,P_FLY,"Beginner"),
 ("Kettlebell Floor Press","Kettlebell",CHEST,TRIS,P_HPRESS,"Beginner"),
 ("Kettlebell Push Up","Kettlebell",CHEST,TRIS,P_HPRESS,"Intermediate"),
 ("Svend Press","Plate",CHEST,FD,P_FLY,"Beginner"),
]
for n,eq,p,s,pat,d in chest: add(ex(n,"Chest",eq,p,s,pat,d))

# ============================= BACK =============================
back = [
 ("Deficit Deadlift","Barbell",HAM,GLU+BACK,P_HINGE,"Advanced"),
 ("Snatch Grip Deadlift","Barbell",BACK,HAM+TRAPS,P_HINGE,"Advanced"),
 ("Sumo Deadlift","Barbell",GLU,HAM+QUAD,P_HINGE,"Intermediate"),
 ("Rack Pull","Barbell",BACK,TRAPS+GLU,P_HINGE,"Intermediate"),
 ("Pendlay Row","Barbell",LATS,UB+BI,P_HROW,"Intermediate"),
 ("Yates Row","Barbell",LATS,UB+BI,P_HROW,"Intermediate"),
 ("Barbell Shrug","Barbell",TRAPS,UB,P_SHRUG,"Beginner"),
 ("T-Bar Row","Barbell",LATS,UB+BI,P_HROW,"Intermediate"),
 ("Chest Supported Row","Dumbbell",UB,LATS+RD,P_HROW,"Beginner"),
 ("Incline Chest Supported Dumbbell Row","Dumbbell",UB,LATS+RD,P_HROW,"Beginner"),
 ("Single Arm Dumbbell Row","Dumbbell",LATS,UB+BI,P_HROW,"Beginner"),
 ("Dumbbell Pullover (Back)","Dumbbell",LATS,CHEST,P_VPULL,"Intermediate"),
 ("Dumbbell Shrug","Dumbbell",TRAPS,UB,P_SHRUG,"Beginner"),
 ("Renegade Row","Dumbbell",LATS,CORE+UB,P_HROW,"Advanced"),
 ("Seated Cable Row","Cable",LATS,UB+BI,P_HROW,"Beginner"),
 ("Wide Grip Cable Row","Cable",UB,LATS+RD,P_HROW,"Beginner"),
 ("Single Arm Cable Row","Cable",LATS,UB+BI,P_HROW,"Beginner"),
 ("Straight Arm Pulldown","Cable",LATS,UB,P_VPULL,"Beginner"),
 ("Wide Grip Lat Pulldown","Cable",LATS,BI+UB,P_VPULL,"Beginner"),
 ("Close Grip Lat Pulldown","Cable",LATS,BI,P_VPULL,"Beginner"),
 ("Neutral Grip Lat Pulldown","Cable",LATS,BI+UB,P_VPULL,"Beginner"),
 ("Cable Shrug","Cable",TRAPS,UB,P_SHRUG,"Beginner"),
 ("Cable Pullover","Cable",LATS,CHEST,P_VPULL,"Intermediate"),
 ("Machine Row","Machine",LATS,UB+BI,P_HROW,"Beginner"),
 ("Hammer Strength Row","Machine",LATS,UB+BI,P_HROW,"Beginner"),
 ("Machine Pulldown","Machine",LATS,BI+UB,P_VPULL,"Beginner"),
 ("Machine Reverse Fly","Machine",RD,UB,P_REAR,"Beginner"),
 ("Machine Shrug","Machine",TRAPS,UB,P_SHRUG,"Beginner"),
 ("Back Extension","Machine",BACK,GLU+HAM,P_HINGE,"Beginner"),
 ("Smith Machine Row","Smith Machine",LATS,UB+BI,P_HROW,"Beginner"),
 ("Smith Machine Shrug","Smith Machine",TRAPS,UB,P_SHRUG,"Beginner"),
 ("Wide Grip Pull Ups","Bodyweight",LATS,BI+UB,P_VPULL,"Intermediate"),
 ("Neutral Grip Pull Ups","Bodyweight",LATS,BI,P_VPULL,"Intermediate"),
 ("Weighted Pull Ups","Bodyweight",LATS,BI+UB,P_VPULL,"Advanced"),
 ("Inverted Row","Bodyweight",UB,LATS+BI,P_HROW,"Beginner"),
 ("Australian Pull Up","Bodyweight",UB,LATS+BI,P_HROW,"Beginner"),
 ("Superman","Bodyweight",BACK,GLU,P_HINGE,"Beginner"),
 ("Kettlebell Swing","Kettlebell",GLU,HAM+BACK,P_HINGE,"Intermediate"),
 ("Kettlebell Single Arm Row","Kettlebell",LATS,UB+BI,P_HROW,"Beginner"),
 ("Kettlebell Deadlift","Kettlebell",HAM,GLU+BACK,P_HINGE,"Beginner"),
 ("Banded Lat Pulldown","Band",LATS,BI,P_VPULL,"Beginner"),
 ("Banded Row","Band",LATS,UB+BI,P_HROW,"Beginner"),
 ("Banded Pull Apart","Band",RD,UB,P_REAR,"Beginner"),
 ("Banded Good Morning","Band",HAM,GLU+BACK,P_HINGE,"Beginner"),
]
for n,eq,p,s,pat,d in back: add(ex(n,"Back",eq,p,s,pat,d))

# ============================= SHOULDERS =============================
sh = [
 ("Seated Barbell Overhead Press","Barbell",FD,TRIS+SD,P_VPRESS,"Intermediate"),
 ("Push Press","Barbell",FD,TRIS+SD,P_VPRESS,"Advanced"),
 ("Behind The Neck Press","Barbell",FD,SD+TRIS,P_VPRESS,"Advanced"),
 ("Barbell Upright Row","Barbell",SD,TRAPS,P_LAT,"Intermediate"),
 ("Seated Dumbbell Press","Dumbbell",FD,TRIS+SD,P_VPRESS,"Beginner"),
 ("Arnold Press","Dumbbell",FD,SD+TRIS,P_VPRESS,"Intermediate"),
 ("Dumbbell Front Raise","Dumbbell",FD,SD,P_LAT,"Beginner"),
 ("Seated Dumbbell Lateral Raise","Dumbbell",SD,[],P_LAT,"Beginner"),
 ("Leaning Dumbbell Lateral Raise","Dumbbell",SD,[],P_LAT,"Intermediate"),
 ("Bent Over Dumbbell Reverse Fly","Dumbbell",RD,UB,P_REAR,"Beginner"),
 ("Dumbbell Rear Delt Row","Dumbbell",RD,UB,P_REAR,"Beginner"),
 ("Dumbbell Upright Row","Dumbbell",SD,TRAPS,P_LAT,"Beginner"),
 ("Cable Lateral Raise","Cable",SD,[],P_LAT,"Beginner"),
 ("Cable Front Raise","Cable",FD,SD,P_LAT,"Beginner"),
 ("Cable Rear Delt Fly","Cable",RD,UB,P_REAR,"Beginner"),
 ("Cable Upright Row","Cable",SD,TRAPS,P_LAT,"Beginner"),
 ("Cable Reverse Fly","Cable",RD,UB,P_REAR,"Beginner"),
 ("Machine Shoulder Press","Machine",FD,TRIS+SD,P_VPRESS,"Beginner"),
 ("Machine Lateral Raise","Machine",SD,[],P_LAT,"Beginner"),
 ("Reverse Pec Deck","Machine",RD,UB,P_REAR,"Beginner"),
 ("Smith Machine Overhead Press","Smith Machine",FD,TRIS+SD,P_VPRESS,"Beginner"),
 ("Smith Machine Upright Row","Smith Machine",SD,TRAPS,P_LAT,"Beginner"),
 ("Pike Push Ups","Bodyweight",FD,TRIS,P_VPRESS,"Intermediate"),
 ("Handstand Push Ups","Bodyweight",FD,TRIS+SD,P_VPRESS,"Advanced"),
 ("Banded Lateral Raise","Band",SD,[],P_LAT,"Beginner"),
 ("Banded Shoulder Press","Band",FD,TRIS,P_VPRESS,"Beginner"),
 ("Banded Face Pull","Band",RD,UB,P_FACE,"Beginner"),
 ("Kettlebell Overhead Press","Kettlebell",FD,TRIS+SD,P_VPRESS,"Intermediate"),
 ("Kettlebell Push Press","Kettlebell",FD,TRIS+SD,P_VPRESS,"Intermediate"),
 ("Plate Front Raise","Plate",FD,SD,P_LAT,"Beginner"),
]
for n,eq,p,s,pat,d in sh: add(ex(n,"Shoulders",eq,p,s,pat,d))

# ============================= ARMS (biceps/triceps/forearms) =============================
arms = [
 ("Barbell Curl","Barbell",BI,FA,P_CURL,"Beginner"),
 ("EZ Bar Curl","EZ Bar",BI,FA,P_CURL,"Beginner"),
 ("Preacher Curl","EZ Bar",BI,[],P_CURL,"Beginner"),
 ("Spider Curl","EZ Bar",BI,[],P_CURL,"Intermediate"),
 ("Reverse Barbell Curl","Barbell",FA,BI,P_CURL,"Beginner"),
 ("Drag Curl","Barbell",BI,[],P_CURL,"Intermediate"),
 ("Incline Dumbbell Curl","Dumbbell",BI,[],P_CURL,"Beginner"),
 ("Concentration Curl","Dumbbell",BI,[],P_CURL,"Beginner"),
 ("Seated Dumbbell Curl","Dumbbell",BI,FA,P_CURL,"Beginner"),
 ("Cross Body Hammer Curl","Dumbbell",BI,FA,P_CURL,"Beginner"),
 ("Zottman Curl","Dumbbell",BI,FA,P_CURL,"Intermediate"),
 ("Dumbbell Preacher Curl","Dumbbell",BI,[],P_CURL,"Beginner"),
 ("Cable Bicep Curl","Cable",BI,FA,P_CURL,"Beginner"),
 ("Cable Hammer Curl","Cable",BI,FA,P_CURL,"Beginner"),
 ("Cable Rope Curl","Cable",BI,FA,P_CURL,"Beginner"),
 ("High Cable Curl","Cable",BI,[],P_CURL,"Beginner"),
 ("Machine Preacher Curl","Machine",BI,[],P_CURL,"Beginner"),
 ("Machine Bicep Curl","Machine",BI,[],P_CURL,"Beginner"),
 ("Band Bicep Curl","Band",BI,FA,P_CURL,"Beginner"),
 ("Close Grip Push Ups","Bodyweight",TRIS,CHEST,P_HPRESS,"Beginner"),
 ("Bench Dips","Bodyweight",TRIS,CHEST,P_TRI,"Beginner"),
 ("Skull Crushers","EZ Bar",TRIS,[],P_TRI,"Intermediate"),
 ("Barbell Skull Crushers","Barbell",TRIS,[],P_TRI,"Intermediate"),
 ("Close Grip EZ Bar Press","EZ Bar",TRIS,CHEST,P_HPRESS,"Beginner"),
 ("Dumbbell Overhead Tricep Extension","Dumbbell",TRIS,[],P_TRI,"Beginner"),
 ("Single Arm Overhead Extension","Dumbbell",TRIS,[],P_TRI,"Beginner"),
 ("Dumbbell Tricep Kickback","Dumbbell",TRIS,[],P_TRI,"Beginner"),
 ("Tate Press","Dumbbell",TRIS,[],P_TRI,"Intermediate"),
 ("Cable Overhead Tricep Extension","Cable",TRIS,[],P_TRI,"Beginner"),
 ("Cable Rope Pushdown","Cable",TRIS,[],P_TRI,"Beginner"),
 ("Cable Single Arm Pushdown","Cable",TRIS,[],P_TRI,"Beginner"),
 ("Cable Kickback","Cable",TRIS,[],P_TRI,"Beginner"),
 ("V-Bar Pushdown","Cable",TRIS,[],P_TRI,"Beginner"),
 ("Machine Tricep Extension","Machine",TRIS,[],P_TRI,"Beginner"),
 ("Machine Dip","Machine",TRIS,CHEST,P_TRI,"Beginner"),
 ("Band Tricep Pushdown","Band",TRIS,[],P_TRI,"Beginner"),
 ("Band Overhead Extension","Band",TRIS,[],P_TRI,"Beginner"),
 ("Barbell Wrist Curl","Barbell",FA,[],P_FOREARM,"Beginner"),
 ("Dumbbell Wrist Curl","Dumbbell",FA,[],P_FOREARM,"Beginner"),
 ("Reverse Wrist Curl","Dumbbell",FA,[],P_FOREARM,"Beginner"),
 ("Cable Wrist Curl","Cable",FA,[],P_FOREARM,"Beginner"),
 ("Wrist Roller","Plate",FA,[],P_FOREARM,"Intermediate"),
 ("Farmers Carry","Dumbbell",FA,TRAPS+CORE,P_CARRY,"Beginner"),
]
for n,eq,p,s,pat,d in arms: add(ex(n,"Arms",eq,p,s,pat,d))

# ============================= LEGS =============================
legs = [
 ("Barbell Box Squat","Barbell",QUAD,GLU+HAM,P_SQUAT,"Intermediate"),
 ("Barbell Pause Squat","Barbell",QUAD,GLU,P_SQUAT,"Advanced"),
 ("Barbell Hip Thrust","Barbell",GLU,HAM,P_GLUTE,"Intermediate"),
 ("Barbell Glute Bridge","Barbell",GLU,HAM,P_GLUTE,"Beginner"),
 ("Barbell Lunge","Barbell",QUAD,GLU+HAM,P_LUNGE,"Intermediate"),
 ("Barbell Walking Lunge","Barbell",QUAD,GLU+HAM,P_LUNGE,"Intermediate"),
 ("Barbell Step Up","Barbell",QUAD,GLU,P_LUNGE,"Intermediate"),
 ("Barbell Good Morning","Barbell",HAM,GLU+BACK,P_HINGE,"Intermediate"),
 ("Barbell Calf Raise","Barbell",CALF,[],P_CALF,"Beginner"),
 ("Bulgarian Split Squat","Dumbbell",QUAD,GLU+HAM,P_LUNGE,"Intermediate"),
 ("Dumbbell Lunge","Dumbbell",QUAD,GLU+HAM,P_LUNGE,"Beginner"),
 ("Dumbbell Walking Lunge","Dumbbell",QUAD,GLU+HAM,P_LUNGE,"Beginner"),
 ("Dumbbell Reverse Lunge","Dumbbell",QUAD,GLU+HAM,P_LUNGE,"Beginner"),
 ("Dumbbell Step Up","Dumbbell",QUAD,GLU,P_LUNGE,"Beginner"),
 ("Dumbbell Romanian Deadlift","Dumbbell",HAM,GLU+BACK,P_HINGE,"Beginner"),
 ("Dumbbell Stiff Leg Deadlift","Dumbbell",HAM,GLU,P_HINGE,"Beginner"),
 ("Dumbbell Goblet Lunge","Dumbbell",QUAD,GLU,P_LUNGE,"Beginner"),
 ("Dumbbell Calf Raise","Dumbbell",CALF,[],P_CALF,"Beginner"),
 ("Dumbbell Hip Thrust","Dumbbell",GLU,HAM,P_GLUTE,"Beginner"),
 ("Hack Squat","Machine",QUAD,GLU,P_SQUAT,"Intermediate"),
 ("Pendulum Squat","Machine",QUAD,GLU,P_SQUAT,"Intermediate"),
 ("Belt Squat","Machine",QUAD,GLU,P_SQUAT,"Intermediate"),
 ("Lying Leg Curl","Machine",HAM,[],P_KFLEX,"Beginner"),
 ("Standing Leg Curl","Machine",HAM,[],P_KFLEX,"Beginner"),
 ("Standing Calf Raise","Machine",CALF,[],P_CALF,"Beginner"),
 ("Seated Calf Raise","Machine",CALF,[],P_CALF,"Beginner"),
 ("Leg Press Calf Raise","Machine",CALF,[],P_CALF,"Beginner"),
 ("Hip Abduction Machine","Machine",ABD,GLU,P_HIPABD,"Beginner"),
 ("Hip Adduction Machine","Machine",ADD,[],P_HIPADD,"Beginner"),
 ("Glute Kickback Machine","Machine",GLU,HAM,P_GLUTE,"Beginner"),
 ("Smith Machine Squat","Smith Machine",QUAD,GLU+HAM,P_SQUAT,"Beginner"),
 ("Smith Machine Lunge","Smith Machine",QUAD,GLU,P_LUNGE,"Beginner"),
 ("Smith Machine Calf Raise","Smith Machine",CALF,[],P_CALF,"Beginner"),
 ("Smith Machine Hip Thrust","Smith Machine",GLU,HAM,P_GLUTE,"Beginner"),
 ("Smith Machine Bulgarian Split Squat","Smith Machine",QUAD,GLU,P_LUNGE,"Intermediate"),
 ("Cable Pull Through","Cable",GLU,HAM,P_HINGE,"Beginner"),
 ("Cable Kickback (Glute)","Cable",GLU,HAM,P_GLUTE,"Beginner"),
 ("Cable Hip Abduction","Cable",ABD,GLU,P_HIPABD,"Beginner"),
 ("Bodyweight Squat","Bodyweight",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Pistol Squat","Bodyweight",QUAD,GLU,P_SQUAT,"Advanced"),
 ("Jump Squat","Bodyweight",QUAD,GLU,P_SQUAT,"Intermediate"),
 ("Walking Lunge","Bodyweight",QUAD,GLU+HAM,P_LUNGE,"Beginner"),
 ("Reverse Lunge","Bodyweight",QUAD,GLU,P_LUNGE,"Beginner"),
 ("Glute Bridge","Bodyweight",GLU,HAM,P_GLUTE,"Beginner"),
 ("Single Leg Glute Bridge","Bodyweight",GLU,HAM,P_GLUTE,"Beginner"),
 ("Nordic Curl","Bodyweight",HAM,[],P_KFLEX,"Advanced"),
 ("Sissy Squat","Bodyweight",QUAD,[],P_KEXT,"Advanced"),
 ("Wall Sit","Bodyweight",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Standing Calf Raise (Bodyweight)","Bodyweight",CALF,[],P_CALF,"Beginner"),
 ("Box Jump","Bodyweight",QUAD,GLU,P_SQUAT,"Intermediate"),
 ("Kettlebell Goblet Squat","Kettlebell",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Kettlebell Lunge","Kettlebell",QUAD,GLU+HAM,P_LUNGE,"Beginner"),
 ("Kettlebell Romanian Deadlift","Kettlebell",HAM,GLU,P_HINGE,"Beginner"),
 ("Kettlebell Sumo Squat","Kettlebell",QUAD,GLU+ADD,P_SQUAT,"Beginner"),
 ("Banded Squat","Band",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Banded Glute Bridge","Band",GLU,HAM,P_GLUTE,"Beginner"),
 ("Banded Lateral Walk","Band",ABD,GLU,P_HIPABD,"Beginner"),
 ("Banded Leg Curl","Band",HAM,[],P_KFLEX,"Beginner"),
]
for n,eq,p,s,pat,d in legs: add(ex(n,"Legs",eq,p,s,pat,d))

# ============================= CORE =============================
core = [
 ("Hanging Leg Raise","Bodyweight",CORE,OBL,P_CORE_HF,"Intermediate"),
 ("Hanging Knee Raise","Bodyweight",CORE,[],P_CORE_HF,"Beginner"),
 ("Captains Chair Leg Raise","Machine",CORE,[],P_CORE_HF,"Beginner"),
 ("Crunches","Bodyweight",CORE,[],P_CORE_FLEX,"Beginner"),
 ("Bicycle Crunches","Bodyweight",OBL,CORE,P_CORE_ROT,"Beginner"),
 ("Sit Ups","Bodyweight",CORE,[],P_CORE_FLEX,"Beginner"),
 ("Cable Crunch","Cable",CORE,[],P_CORE_FLEX,"Beginner"),
 ("Cable Woodchopper","Cable",OBL,CORE,P_CORE_ROT,"Beginner"),
 ("Machine Crunch","Machine",CORE,[],P_CORE_FLEX,"Beginner"),
 ("Ab Wheel Rollout","Bodyweight",CORE,[],P_CORE_AE,"Advanced"),
 ("Side Plank","Bodyweight",OBL,CORE,P_CORE_AE,"Beginner"),
 ("Mountain Climbers","Bodyweight",CORE,[],P_CORE_HF,"Beginner"),
 ("Dead Bug","Bodyweight",CORE,[],P_CORE_AE,"Beginner"),
 ("Bird Dog","Bodyweight",CORE,GLU,P_CORE_AE,"Beginner"),
 ("Hollow Body Hold","Bodyweight",CORE,[],P_CORE_AE,"Intermediate"),
 ("Flutter Kicks","Bodyweight",CORE,[],P_CORE_HF,"Beginner"),
 ("V-Ups","Bodyweight",CORE,OBL,P_CORE_FLEX,"Intermediate"),
 ("Toes To Bar","Bodyweight",CORE,LATS,P_CORE_HF,"Advanced"),
 ("Pallof Press","Cable",OBL,CORE,P_CORE_ROT,"Beginner"),
 ("Hanging Windshield Wipers","Bodyweight",OBL,CORE,P_CORE_ROT,"Advanced"),
 ("Weighted Plank","Plate",CORE,[],P_CORE_AE,"Beginner"),
 ("Decline Sit Up","Bodyweight",CORE,[],P_CORE_FLEX,"Beginner"),
 ("Reverse Crunch","Bodyweight",CORE,[],P_CORE_HF,"Beginner"),
 ("Suitcase Carry","Dumbbell",OBL,FA+CORE,P_CARRY,"Beginner"),
]
for n,eq,p,s,pat,d in core: add(ex(n,"Core",eq,p,s,pat,d))

# ============================= CARDIO / CONDITIONING =============================
cardio = [
 ("Treadmill Run","Machine",["Cardiovascular"],QUAD+CALF,"Conditioning","Beginner","Cardio"),
 ("Treadmill Incline Walk","Machine",["Cardiovascular"],GLU+CALF,"Conditioning","Beginner","Cardio"),
 ("Stationary Bike","Machine",["Cardiovascular"],QUAD,"Conditioning","Beginner","Cardio"),
 ("Rowing Machine","Machine",["Cardiovascular"],BACK+LATS,"Conditioning","Beginner","Cardio"),
 ("Stair Climber","Machine",["Cardiovascular"],GLU+QUAD,"Conditioning","Beginner","Cardio"),
 ("Elliptical","Machine",["Cardiovascular"],QUAD+GLU,"Conditioning","Beginner","Cardio"),
 ("Assault Bike","Machine",["Cardiovascular"],QUAD+BACK,"Conditioning","Intermediate","Cardio"),
 ("Ski Erg","Machine",["Cardiovascular"],LATS+CORE,"Conditioning","Intermediate","Cardio"),
 ("Burpees","Bodyweight",["Cardiovascular"],QUAD+CHEST,"Conditioning","Intermediate","Cardio"),
 ("Jumping Jacks","Bodyweight",["Cardiovascular"],CALF,"Conditioning","Beginner","Cardio"),
 ("High Knees","Bodyweight",["Cardiovascular"],QUAD,"Conditioning","Beginner","Cardio"),
 ("Jump Rope","Bodyweight",["Cardiovascular"],CALF,"Conditioning","Beginner","Cardio"),
 ("Battle Ropes","Cable",["Cardiovascular"],SD+CORE,"Conditioning","Intermediate","Cardio"),
 ("Sled Push","Machine",QUAD,GLU+CALF,"Conditioning","Intermediate","Cardio"),
 ("Sled Pull","Cable",BACK,LATS+BI,"Conditioning","Intermediate","Cardio"),
 ("Kettlebell Snatch","Kettlebell",GLU,SD+BACK,"Conditioning","Advanced","Cardio"),
 ("Kettlebell Clean and Press","Kettlebell",FD,GLU+TRIS,"Conditioning","Advanced","Cardio"),
 ("Kettlebell Turkish Get Up","Kettlebell",CORE,SD+GLU,"Conditioning","Advanced","Cardio"),
 ("Mountain Climber Sprint","Bodyweight",["Cardiovascular"],CORE,"Conditioning","Beginner","Cardio"),
 ("Box Step Overs","Bodyweight",QUAD,GLU,"Conditioning","Beginner","Cardio"),
]
for row in cardio:
    n,eq,p,s,pat,d,t = row
    add(ex(n,"Cardio",eq,p,s,pat,d,t))

# ============================= OLYMPIC / POWER =============================
power = [
 ("Power Clean","Barbell",["Full Body"],GLU+TRAPS,f"Pull {BULLET} Olympic","Advanced"),
 ("Hang Clean","Barbell",["Full Body"],GLU+TRAPS,f"Pull {BULLET} Olympic","Advanced"),
 ("Clean and Jerk","Barbell",["Full Body"],GLU+FD,f"Pull {BULLET} Olympic","Advanced"),
 ("Snatch","Barbell",["Full Body"],GLU+SD,f"Pull {BULLET} Olympic","Advanced"),
 ("Power Snatch","Barbell",["Full Body"],GLU+SD,f"Pull {BULLET} Olympic","Advanced"),
 ("Clean Pull","Barbell",TRAPS,GLU+BACK,P_HINGE,"Advanced"),
 ("High Pull","Barbell",TRAPS,SD+BI,P_LAT,"Advanced"),
 ("Thruster","Barbell",QUAD,FD+GLU,P_VPRESS,"Advanced"),
 ("Dumbbell Thruster","Dumbbell",QUAD,FD+GLU,P_VPRESS,"Intermediate"),
 ("Dumbbell Snatch","Dumbbell",GLU,SD+BACK,f"Pull {BULLET} Olympic","Advanced"),
 ("Dumbbell Clean","Dumbbell",GLU,TRAPS,f"Pull {BULLET} Olympic","Advanced"),
 ("Medicine Ball Slam","Plate",CORE,LATS+SD,P_CORE_FLEX,"Intermediate"),
]
for n,eq,p,s,pat,d in power: add(ex(n,"Full Body",eq,p,s,pat,d))

# ============================= EXPANSION BATCH 2 (to reach 500+) =============================
# More chest variations
chest2 = [
 ("Reverse Grip Bench Press","Barbell",CHEST,TRIS+FD,P_IPRESS,"Intermediate"),
 ("Guillotine Press","Barbell",CHEST,FD,P_HPRESS,"Advanced"),
 ("Larsen Press","Barbell",CHEST,TRIS,P_HPRESS,"Intermediate"),
 ("Spoto Press","Barbell",CHEST,TRIS,P_HPRESS,"Intermediate"),
 ("Incline Close Grip Bench Press","Barbell",TRIS,CHEST+FD,P_IPRESS,"Intermediate"),
 ("Low Incline Dumbbell Press","Dumbbell",CHEST,FD+TRIS,P_IPRESS,"Beginner"),
 ("High Incline Dumbbell Press","Dumbbell",FD,CHEST+TRIS,P_IPRESS,"Beginner"),
 ("Alternating Dumbbell Press","Dumbbell",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Twisting Dumbbell Press","Dumbbell",CHEST,FD,P_HPRESS,"Beginner"),
 ("Deficit Push Ups","Bodyweight",CHEST,TRIS,P_HPRESS,"Intermediate"),
 ("Ring Push Ups","Bodyweight",CHEST,TRIS+CORE,P_HPRESS,"Advanced"),
 ("Weighted Push Ups","Plate",CHEST,TRIS+FD,P_HPRESS,"Intermediate"),
 ("Weighted Chest Dips","Bodyweight",CHEST,TRIS+FD,P_DPRESS,"Advanced"),
 ("Seated Machine Fly","Machine",CHEST,FD,P_FLY,"Beginner"),
 ("Iso-Lateral Chest Press","Machine",CHEST,TRIS+FD,P_HPRESS,"Beginner"),
 ("Cable Decline Press","Cable",CHEST,TRIS,P_DPRESS,"Beginner"),
 ("Kneeling Cable Crossover","Cable",CHEST,FD,P_FLY,"Beginner"),
]
for n,eq,p,s,pat,d in chest2: add(ex(n,"Chest",eq,p,s,pat,d))

back2 = [
 ("Meadows Row","Barbell",LATS,UB+BI,P_HROW,"Intermediate"),
 ("Seal Row","Barbell",UB,LATS+RD,P_HROW,"Intermediate"),
 ("Landmine Row","Barbell",LATS,UB+BI,P_HROW,"Intermediate"),
 ("Chest Supported T-Bar Row","Machine",UB,LATS+BI,P_HROW,"Beginner"),
 ("Trap Bar Deadlift","Barbell",GLU,QUAD+BACK,P_HINGE,"Intermediate"),
 ("Stiff Leg Deadlift","Barbell",HAM,GLU+BACK,P_HINGE,"Intermediate"),
 ("Block Pull","Barbell",BACK,GLU+TRAPS,P_HINGE,"Intermediate"),
 ("Wide Grip Barbell Row","Barbell",UB,LATS+RD,P_HROW,"Intermediate"),
 ("Underhand Barbell Row","Barbell",LATS,BI+UB,P_HROW,"Intermediate"),
 ("Helms Row","Dumbbell",LATS,UB+BI,P_HROW,"Beginner"),
 ("Kroc Row","Dumbbell",LATS,UB+TRAPS,P_HROW,"Advanced"),
 ("Gironda Row","Cable",LATS,UB,P_HROW,"Intermediate"),
 ("Half Kneeling Lat Pulldown","Cable",LATS,BI,P_VPULL,"Beginner"),
 ("Behind Neck Pulldown","Cable",LATS,UB,P_VPULL,"Intermediate"),
 ("Reverse Grip Pulldown","Cable",LATS,BI,P_VPULL,"Beginner"),
 ("Archer Pull Ups","Bodyweight",LATS,BI,P_VPULL,"Advanced"),
 ("Commando Pull Ups","Bodyweight",LATS,BI+UB,P_VPULL,"Advanced"),
 ("Scapular Pull Ups","Bodyweight",UB,LATS,P_VPULL,"Beginner"),
 ("Muscle Up","Bodyweight",LATS,CHEST+TRIS,P_VPULL,"Advanced"),
 ("Hyperextension","Bodyweight",BACK,GLU+HAM,P_HINGE,"Beginner"),
 ("Reverse Hyperextension","Machine",GLU,HAM+BACK,P_GLUTE,"Intermediate"),
 ("Kettlebell High Pull","Kettlebell",TRAPS,SD,P_LAT,"Intermediate"),
]
for n,eq,p,s,pat,d in back2: add(ex(n,"Back",eq,p,s,pat,d))

sh2 = [
 ("Z Press","Barbell",FD,TRIS+CORE,P_VPRESS,"Advanced"),
 ("Bradford Press","Barbell",FD,SD+TRIS,P_VPRESS,"Intermediate"),
 ("Single Arm Dumbbell Press","Dumbbell",FD,TRIS+CORE,P_VPRESS,"Beginner"),
 ("Half Kneeling Dumbbell Press","Dumbbell",FD,TRIS+CORE,P_VPRESS,"Beginner"),
 ("Lu Raise","Dumbbell",SD,FD,P_LAT,"Intermediate"),
 ("Dumbbell Y Raise","Dumbbell",SD,RD,P_LAT,"Beginner"),
 ("Dumbbell Scott Press","Dumbbell",FD,SD,P_VPRESS,"Intermediate"),
 ("Dumbbell Cuban Press","Dumbbell",SD,RD+FD,P_VPRESS,"Intermediate"),
 ("Prone Incline Reverse Fly","Dumbbell",RD,UB,P_REAR,"Beginner"),
 ("Cable Lateral Raise (Behind Back)","Cable",SD,[],P_LAT,"Beginner"),
 ("Cable Y Raise","Cable",SD,RD,P_LAT,"Beginner"),
 ("Single Arm Cable Lateral Raise","Cable",SD,[],P_LAT,"Beginner"),
 ("Seated Machine Rear Delt","Machine",RD,UB,P_REAR,"Beginner"),
 ("Plate Around The World","Plate",FD,SD,P_LAT,"Beginner"),
 ("Wall Walk","Bodyweight",FD,SD+CORE,P_VPRESS,"Advanced"),
 ("Banded Pull Apart (High)","Band",RD,UB,P_REAR,"Beginner"),
]
for n,eq,p,s,pat,d in sh2: add(ex(n,"Shoulders",eq,p,s,pat,d))

arms2 = [
 ("Bayesian Cable Curl","Cable",BI,[],P_CURL,"Intermediate"),
 ("Cable Preacher Curl","Cable",BI,[],P_CURL,"Beginner"),
 ("Cable Concentration Curl","Cable",BI,[],P_CURL,"Beginner"),
 ("21s Curl","Barbell",BI,[],P_CURL,"Intermediate"),
 ("Reverse EZ Bar Curl","EZ Bar",FA,BI,P_CURL,"Beginner"),
 ("Wide Grip Barbell Curl","Barbell",BI,[],P_CURL,"Beginner"),
 ("Close Grip Barbell Curl","Barbell",BI,FA,P_CURL,"Beginner"),
 ("Waiter Curl","Dumbbell",BI,[],P_CURL,"Beginner"),
 ("Incline Hammer Curl","Dumbbell",BI,FA,P_CURL,"Beginner"),
 ("Lying Cable Curl","Cable",BI,[],P_CURL,"Intermediate"),
 ("JM Press","Barbell",TRIS,CHEST,P_TRI,"Advanced"),
 ("Dumbbell Floor Skull Crusher","Dumbbell",TRIS,[],P_TRI,"Intermediate"),
 ("Cable Overhead Rope Extension","Cable",TRIS,[],P_TRI,"Beginner"),
 ("Reverse Grip Pushdown","Cable",TRIS,[],P_TRI,"Beginner"),
 ("Cross Cable Extension","Cable",TRIS,[],P_TRI,"Intermediate"),
 ("Bench Dip (Weighted)","Plate",TRIS,CHEST,P_TRI,"Intermediate"),
 ("Behind Head Cable Extension","Cable",TRIS,[],P_TRI,"Beginner"),
 ("Reverse Grip Tricep Pushdown","Cable",TRIS,[],P_TRI,"Beginner"),
 ("Plate Pinch Carry","Plate",FA,[],P_CARRY,"Intermediate"),
 ("Behind Back Wrist Curl","Barbell",FA,[],P_FOREARM,"Beginner"),
 ("Pronation Curl","Dumbbell",FA,BI,P_FOREARM,"Beginner"),
 ("Cable Reverse Curl","Cable",FA,BI,P_CURL,"Beginner"),
]
for n,eq,p,s,pat,d in arms2: add(ex(n,"Arms",eq,p,s,pat,d))

legs2 = [
 ("Barbell Anderson Squat","Barbell",QUAD,GLU,P_SQUAT,"Advanced"),
 ("Zercher Squat","Barbell",QUAD,GLU+CORE,P_SQUAT,"Advanced"),
 ("Safety Bar Squat","Barbell",QUAD,GLU+BACK,P_SQUAT,"Intermediate"),
 ("Tempo Squat","Barbell",QUAD,GLU,P_SQUAT,"Intermediate"),
 ("Barbell Hip Thrust (Paused)","Barbell",GLU,HAM,P_GLUTE,"Intermediate"),
 ("Barbell Reverse Lunge","Barbell",QUAD,GLU+HAM,P_LUNGE,"Intermediate"),
 ("Barbell Split Squat","Barbell",QUAD,GLU,P_LUNGE,"Intermediate"),
 ("Deficit Reverse Lunge","Dumbbell",QUAD,GLU,P_LUNGE,"Intermediate"),
 ("Dumbbell Curtsy Lunge","Dumbbell",GLU,QUAD+ABD,P_LUNGE,"Beginner"),
 ("Dumbbell Lateral Lunge","Dumbbell",ADD,QUAD+GLU,P_LUNGE,"Beginner"),
 ("Dumbbell Single Leg RDL","Dumbbell",HAM,GLU,P_HINGE,"Intermediate"),
 ("Dumbbell Cossack Squat","Dumbbell",ADD,QUAD+GLU,P_LUNGE,"Intermediate"),
 ("Front Foot Elevated Split Squat","Dumbbell",QUAD,GLU,P_LUNGE,"Beginner"),
 ("Heels Elevated Goblet Squat","Dumbbell",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Single Leg Press","Machine",QUAD,GLU,P_SQUAT,"Beginner"),
 ("V-Squat Machine","Machine",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Seated Hip Abduction","Machine",ABD,GLU,P_HIPABD,"Beginner"),
 ("Seated Hip Adduction","Machine",ADD,[],P_HIPADD,"Beginner"),
 ("Donkey Calf Raise","Machine",CALF,[],P_CALF,"Beginner"),
 ("Tibialis Raise","Bodyweight",CALF,[],P_CALF,"Beginner"),
 ("Smith Machine Romanian Deadlift","Smith Machine",HAM,GLU,P_HINGE,"Beginner"),
 ("Smith Machine Good Morning","Smith Machine",HAM,GLU+BACK,P_HINGE,"Intermediate"),
 ("Cable Squat","Cable",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Cable Romanian Deadlift","Cable",HAM,GLU,P_HINGE,"Beginner"),
 ("Banded Hip Thrust","Band",GLU,HAM,P_GLUTE,"Beginner"),
 ("Banded Monster Walk","Band",ABD,GLU,P_HIPABD,"Beginner"),
 ("Banded Kickback","Band",GLU,HAM,P_GLUTE,"Beginner"),
 ("Step Down","Bodyweight",QUAD,GLU,P_LUNGE,"Beginner"),
 ("Shrimp Squat","Bodyweight",QUAD,GLU,P_SQUAT,"Advanced"),
 ("Cossack Squat","Bodyweight",ADD,QUAD+GLU,P_LUNGE,"Intermediate"),
 ("Single Leg Calf Raise","Bodyweight",CALF,[],P_CALF,"Beginner"),
 ("Kettlebell Single Leg Deadlift","Kettlebell",HAM,GLU,P_HINGE,"Intermediate"),
 ("Kettlebell Cossack Squat","Kettlebell",ADD,QUAD+GLU,P_LUNGE,"Intermediate"),
 ("Kettlebell Step Up","Kettlebell",QUAD,GLU,P_LUNGE,"Beginner"),
]
for n,eq,p,s,pat,d in legs2: add(ex(n,"Legs",eq,p,s,pat,d))

core2 = [
 ("Cable Pallof Hold","Cable",OBL,CORE,P_CORE_ROT,"Beginner"),
 ("Landmine Twist","Barbell",OBL,CORE,P_CORE_ROT,"Intermediate"),
 ("Russian Twist (Weighted)","Plate",OBL,CORE,P_CORE_ROT,"Beginner"),
 ("Standing Cable Crunch","Cable",CORE,[],P_CORE_FLEX,"Beginner"),
 ("Decline Cable Crunch","Cable",CORE,[],P_CORE_FLEX,"Beginner"),
 ("Weighted Russian Twist","Dumbbell",OBL,CORE,P_CORE_ROT,"Beginner"),
 ("Plank Shoulder Taps","Bodyweight",CORE,SD,P_CORE_AE,"Beginner"),
 ("RKC Plank","Bodyweight",CORE,[],P_CORE_AE,"Intermediate"),
 ("Copenhagen Plank","Bodyweight",ADD,CORE,P_CORE_AE,"Advanced"),
 ("L-Sit","Bodyweight",CORE,QUAD,P_CORE_HF,"Advanced"),
 ("Dragon Flag","Bodyweight",CORE,[],P_CORE_FLEX,"Advanced"),
 ("Cable Side Bend","Cable",OBL,[],P_CORE_ROT,"Beginner"),
 ("Dumbbell Side Bend","Dumbbell",OBL,[],P_CORE_ROT,"Beginner"),
 ("Windmill","Kettlebell",OBL,SD+CORE,P_CORE_ROT,"Intermediate"),
 ("Stir The Pot","Bodyweight",CORE,SD,P_CORE_AE,"Intermediate"),
 ("Jackknife","Bodyweight",CORE,[],P_CORE_FLEX,"Intermediate"),
 ("Banded Pallof Press","Band",OBL,CORE,P_CORE_ROT,"Beginner"),
 ("Standing Band Anti-Rotation","Band",OBL,CORE,P_CORE_ROT,"Beginner"),
]
for n,eq,p,s,pat,d in core2: add(ex(n,"Core",eq,p,s,pat,d))

# Generated grip/angle variations for major lifts to deepen the library
GRIP_VARIANTS = [
 # (base, category, equipment, primary, secondary, pattern, difficulty, [variant adjectives])
 ("Bench Press","Chest","Barbell",CHEST,TRIS+FD,P_HPRESS,"Intermediate",["Paused","Tempo","Pin"]),
 ("Overhead Press","Shoulders","Barbell",FD,TRIS+SD,P_VPRESS,"Intermediate",["Paused","Tempo"]),
 ("Squat","Legs","Barbell",QUAD,GLU+HAM,P_SQUAT,"Intermediate",["Paused 3 Second","Tempo","Pin"]),
 ("Deadlift","Back","Barbell",HAM,GLU+BACK,P_HINGE,"Advanced",["Paused","Tempo"]),
 ("Lateral Raise","Shoulders","Dumbbell",SD,[],P_LAT,"Beginner",["Paused","Partial","Lean Away"]),
 ("Bicep Curl","Arms","Dumbbell",BI,[],P_CURL,"Beginner",["Paused","Tempo","Cheat"]),
 ("Row","Back","Dumbbell",LATS,UB+BI,P_HROW,"Beginner",["Paused","Tempo"]),
 ("Lat Pulldown","Back","Cable",LATS,BI+UB,P_VPULL,"Beginner",["Paused","Tempo","Single Arm"]),
 ("Leg Extension","Legs","Machine",QUAD,[],P_KEXT,"Beginner",["Paused","Single Leg","Tempo"]),
 ("Leg Curl","Legs","Machine",HAM,[],P_KFLEX,"Beginner",["Single Leg","Tempo"]),
 ("Tricep Pushdown","Arms","Cable",TRIS,[],P_TRI,"Beginner",["Single Arm","Tempo","Paused"]),
]
for base,cat,eq,p,s,pat,d,variants in GRIP_VARIANTS:
    for v in variants:
        add(ex(f"{v} {base}", cat, eq, p, s, pat, d))

# ============================= EXPANSION BATCH 3 (push past 500) =============================
batch3 = [
 # Chest
 ("Chest","Incline Cable Crossover","Cable",CHEST,FD,P_FLY,"Beginner"),
 ("Chest","Low to High Cable Fly","Cable",CHEST,FD,P_FLY,"Beginner"),
 ("Chest","High to Low Cable Fly","Cable",CHEST,FD,P_FLY,"Beginner"),
 ("Chest","Standing Single Arm Cable Press","Cable",CHEST,TRIS+CORE,P_HPRESS,"Beginner"),
 ("Chest","Machine Decline Fly","Machine",CHEST,FD,P_FLY,"Beginner"),
 ("Chest","Smith Machine Decline Press","Smith Machine",CHEST,TRIS,P_DPRESS,"Beginner"),
 ("Chest","Dumbbell Around The World","Dumbbell",CHEST,FD,P_FLY,"Intermediate"),
 ("Chest","Spoto Dumbbell Press","Dumbbell",CHEST,TRIS,P_HPRESS,"Intermediate"),
 # Back
 ("Back","Straight Bar Cable Row","Cable",LATS,UB+BI,P_HROW,"Beginner"),
 ("Back","Rope Cable Row","Cable",UB,LATS+RD,P_HROW,"Beginner"),
 ("Back","Cable Face Pull (Seated)","Cable",RD,UB,P_FACE,"Beginner"),
 ("Back","Machine High Row","Machine",LATS,UB+BI,P_HROW,"Beginner"),
 ("Back","Machine Low Row","Machine",LATS,UB+BI,P_HROW,"Beginner"),
 ("Back","Iso-Lateral Pulldown","Machine",LATS,BI,P_VPULL,"Beginner"),
 ("Back","Barbell Rack Pull (Below Knee)","Barbell",BACK,TRAPS+GLU,P_HINGE,"Intermediate"),
 ("Back","Dumbbell Deadlift","Dumbbell",HAM,GLU+BACK,P_HINGE,"Beginner"),
 ("Back","Dumbbell Reverse Fly (Seated)","Dumbbell",RD,UB,P_REAR,"Beginner"),
 # Shoulders
 ("Shoulders","Standing Barbell Press (Strict)","Barbell",FD,TRIS+SD,P_VPRESS,"Intermediate"),
 ("Shoulders","Dumbbell W Press","Dumbbell",FD,RD+SD,P_VPRESS,"Intermediate"),
 ("Shoulders","Cable Rear Delt Row","Cable",RD,UB,P_REAR,"Beginner"),
 ("Shoulders","Machine Front Raise","Machine",FD,SD,P_LAT,"Beginner"),
 ("Shoulders","Incline Dumbbell Lateral Raise","Dumbbell",SD,[],P_LAT,"Intermediate"),
 ("Shoulders","Dumbbell Powell Raise","Dumbbell",RD,SD,P_REAR,"Intermediate"),
 # Arms
 ("Arms","Cable EZ Bar Curl","Cable",BI,FA,P_CURL,"Beginner"),
 ("Arms","Machine Hammer Curl","Machine",BI,FA,P_CURL,"Beginner"),
 ("Arms","Dumbbell Reverse Curl","Dumbbell",FA,BI,P_CURL,"Beginner"),
 ("Arms","Barbell Spider Curl","Barbell",BI,[],P_CURL,"Intermediate"),
 ("Arms","Standing Cable Overhead Extension","Cable",TRIS,[],P_TRI,"Beginner"),
 ("Arms","Dumbbell Seated Overhead Extension","Dumbbell",TRIS,[],P_TRI,"Beginner"),
 ("Arms","EZ Bar Overhead Extension","EZ Bar",TRIS,[],P_TRI,"Beginner"),
 ("Arms","Rolling Dumbbell Extension","Dumbbell",TRIS,[],P_TRI,"Intermediate"),
 ("Arms","Reverse Wrist Curl (Barbell)","Barbell",FA,[],P_FOREARM,"Beginner"),
 ("Arms","Cable Reverse Wrist Curl","Cable",FA,[],P_FOREARM,"Beginner"),
 # Legs
 ("Legs","Hack Squat (Reverse)","Machine",GLU,HAM,P_SQUAT,"Intermediate"),
 ("Legs","Machine Hip Thrust","Machine",GLU,HAM,P_GLUTE,"Beginner"),
 ("Legs","Dumbbell Frog Pump","Dumbbell",GLU,[],P_GLUTE,"Beginner"),
 ("Legs","Cable Glute Pull Through","Cable",GLU,HAM,P_HINGE,"Beginner"),
 ("Legs","Dumbbell Heel Elevated Squat","Dumbbell",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Legs","Barbell Jefferson Squat","Barbell",QUAD,GLU,P_SQUAT,"Advanced"),
 ("Legs","Landmine Squat","Barbell",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Legs","Landmine Reverse Lunge","Barbell",QUAD,GLU+HAM,P_LUNGE,"Intermediate"),
 ("Legs","Seated Leg Press (Vertical)","Machine",QUAD,GLU,P_SQUAT,"Beginner"),
 ("Legs","Adductor Cable Pull","Cable",ADD,[],P_HIPADD,"Beginner"),
 ("Legs","Standing Glute Kickback (Cable)","Cable",GLU,HAM,P_GLUTE,"Beginner"),
 ("Legs","Banded Terminal Knee Extension","Band",QUAD,[],P_KEXT,"Beginner"),
 ("Legs","Single Leg Hip Thrust (Weighted)","Dumbbell",GLU,HAM,P_GLUTE,"Intermediate"),
 ("Legs","Dumbbell Box Squat","Dumbbell",QUAD,GLU,P_SQUAT,"Beginner"),
 # Core
 ("Core","Cable Reverse Crunch","Cable",CORE,[],P_CORE_HF,"Beginner"),
 ("Core","Weighted Decline Sit Up","Plate",CORE,[],P_CORE_FLEX,"Beginner"),
 ("Core","Hanging Oblique Raise","Bodyweight",OBL,CORE,P_CORE_ROT,"Intermediate"),
 ("Core","Cable Pallof Rotation","Cable",OBL,CORE,P_CORE_ROT,"Beginner"),
 ("Core","Weighted Plank Drag","Plate",CORE,SD,P_CORE_AE,"Intermediate"),
 ("Core","Kneeling Cable Oblique Crunch","Cable",OBL,CORE,P_CORE_ROT,"Beginner"),
 ("Core","Stability Ball Crunch","Bodyweight",CORE,[],P_CORE_FLEX,"Beginner"),
 ("Core","Stability Ball Rollout","Bodyweight",CORE,[],P_CORE_AE,"Intermediate"),
 # Conditioning / kettlebell
 ("Cardio","Kettlebell Goblet Carry","Kettlebell",CORE,QUAD+FA,P_CARRY,"Beginner"),
 ("Cardio","Dumbbell Farmers Walk","Dumbbell",FA,TRAPS+CORE,P_CARRY,"Beginner"),
 ("Cardio","Bear Crawl","Bodyweight",CORE,SD+QUAD,P_CARRY,"Beginner"),
 ("Cardio","Sandbag Carry","Plate",CORE,BACK+FA,P_CARRY,"Intermediate"),
 ("Cardio","Wall Ball","Plate",QUAD,FD+CHEST,P_VPRESS,"Intermediate"),
]
for cat,n,eq,p,s,pat,d in batch3: add(ex(n,cat,eq,p,s,pat,d))

if __name__ == "__main__":


    import sys
    path = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/exercises.json"
    with open(path, "w") as f:
        json.dump(out, f, indent=2, ensure_ascii=False)
    cats = {}
    eqs = {}
    for e in out:
        cats[e["category"]] = cats.get(e["category"], 0) + 1
        eqs[e["equipment"]] = eqs.get(e["equipment"], 0) + 1
    print(f"TOTAL: {len(out)}")
    print("By category:", dict(sorted(cats.items())))
    print("By equipment:", dict(sorted(eqs.items())))
