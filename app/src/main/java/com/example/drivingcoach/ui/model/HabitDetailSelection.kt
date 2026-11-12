package com.example.drivingcoach.ui.model

/** Restore a small navigation key against current data, never a cached assessment or list position. */
fun findHabitDetail(habits: List<DrivingHabitAnalysis>, selectedKindName: String?): DrivingHabitAnalysis? =
    selectedKindName?.let { key -> habits.singleOrNull { it.kind.name == key } }
