package com.leaderboardkit.domain.model

enum class SortDirection {
    /** Highest score first. The common case (points, kills, coins...). */
    Descending,

    /** Lowest score first. Use for measurements where less is better (race time, errors...). */
    Ascending,
}
