package com.leaderboardkit.sample

sealed interface Destination {
    data object Home : Destination
    data object GlobalBoard : Destination
    data object FriendsBoard : Destination
    data object WeeklyBoard : Destination
    data object CustomTheme : Destination
    data object CustomRow : Destination
}
