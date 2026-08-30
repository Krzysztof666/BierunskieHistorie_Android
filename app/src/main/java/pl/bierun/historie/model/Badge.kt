package pl.bierun.historie.model

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val requiredPoisCount: Int
)

val allBadges = listOf(
    Badge("first_step", "Pierwszy Krok", "Odwiedź pierwszy zabytek w Bieruniu", "👟", 1),
    Badge("explorer", "Młody Gwark", "Poznaj historię 3 różnych miejsc", "⛏️", 3),
    Badge("councilor", "Bieruński Rajca", "Zalicz 5 punktów historycznych", "📜", 5),
    Badge("master", "Mistrz Historii Bierunia", "Odwiedź wszystkie punkty na szlaku!", "👑", 7)
)