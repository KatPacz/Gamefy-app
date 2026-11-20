package com.example.data.model

import org.json.JSONObject

enum class OnboardingArea(
  val id: String,
  val label: String,
  val icon: String,
  val folderName: String,
) {
  DOM("dom", "Dom i porządki", "🏠", "Dom"),
  NAUKA("nauka", "Nauka i rozwój", "📚", "Nauka"),
  PRACA("praca", "Praca i organizacja", "💼", "Praca"),
  RUCH("ruch", "Zdrowie i aktywność", "🏃", "Zdrowie"),
  SAMOPOCZUCIE("samopoczucie", "Samopoczucie i odpoczynek", "🌿", "Samopoczucie"),
  HOBBY("hobby", "Hobby i kreatywność", "🎨", "Hobby"),
  ZWIERZETA("zwierzeta", "Opieka nad zwierzętami", "🐾", "Zwierzęta"),
  SIDE_QUESTS("side_quests", "Przygody i side questy", "🧭", "Side questy");

  companion object {
    fun fromId(id: String): OnboardingArea? = entries.find { it.id == id }
  }
}

enum class OnboardingTimePref(
  val id: String,
  val label: String,
  val subtitle: String,
) {
  VERY_SHORT("very_short", "Do 5 minut — małe kroki", "Szybkie nawyki i mikro-zadania"),
  SHORT("short", "5–15 minut — krótkie zadania", "Krótkie, konkretne aktywności"),
  MEDIUM("medium", "15–30 minut — dłuższe wyzwania", "Głębsza praca lub większe zadania"),
  ANY("any", "Nie ma to dla mnie znaczenia", "Pokaż zadania o różnym czasie trwania");

  fun matchesMinutes(minutes: Int): Boolean = when (this) {
    VERY_SHORT -> minutes <= 5
    SHORT -> minutes in 5..15
    MEDIUM -> minutes in 15..30
    ANY -> true
  }

  companion object {
    fun fromId(id: String?): OnboardingTimePref? = entries.find { it.id == id }
  }
}

enum class OnboardingDetailPref(
  val id: String,
  val label: String,
  val description: String,
) {
  STEP_BY_STEP(
    id = "step_by_step",
    label = "Krok po kroku",
    description = "Lista podzadań rozwinięta domyślnie na karcie zadania.",
  ),
  SIMPLE(
    id = "simple",
    label = "Prosto i krótko",
    description = "Widoczne głównie główne zadanie — podzadania ukryte w rozwijanej sekcji.",
  ),
  FLEXIBLE(
    id = "flexible",
    label = "Elastycznie",
    description = "Przełącznik podzadań widoczny na karcie, domyślnie zwinięty.",
  );

  companion object {
    fun fromId(id: String?): OnboardingDetailPref? = entries.find { it.id == id }
  }
}

enum class OnboardingRewardPref(
  val id: String,
  val label: String,
  val icon: String,
) {
  GAMING("gaming", "Gry i granie", "🎮"),
  MOVIES("movies", "Filmy i seriale", "🎬"),
  BOOKS("books", "Czytanie i książki", "📖"),
  ART("art", "Tworzenie i sztuka", "🎨"),
  ADVENTURES("adventures", "Wyjścia i przygody", "🧭"),
  RELAXATION("relaxation", "Relaks i odpoczynek", "🛁"),
  SHOPPING("shopping", "Drobne zakupy i zachcianki", "🛍️");

  companion object {
    fun fromId(id: String): OnboardingRewardPref? = entries.find { it.id == id }
  }
}

data class ReadyTaskTemplate(
  val id: String,
  val name: String,
  val description: String,
  val area: OnboardingArea,
  val estimatedMinutes: Int,
  val difficultyLevel: Int, // 1..5
  val attributeWeights: Map<String, Int>, // Attribute name -> percent (sums to 100)
  val suggestedCategory: TaskCategory,
  val suggestedInterval: Int = 1,
  val tags: List<String> = emptyList(),
  val subtasks: List<String> = emptyList(),
  val excludeFromAutoRecommendation: Boolean = false,
) {
  val coins: Int get() = rewardForDifficultyLevel(difficultyLevel).first
  val exp: Int get() = rewardForDifficultyLevel(difficultyLevel).second
  val taskDifficulty: TaskDifficulty get() = difficultyForLevel(difficultyLevel)
}

data class RewardCatalogTemplate(
  val id: String,
  val category: OnboardingRewardPref,
  val icon: String,
  val name: String,
  val defaultCoinPrice: Int,
  val description: String = "",
)

/**
 * Built-in reward table per difficulty level (1..5):
 * 1: 2 coins / 2 EXP
 * 2: 5 coins / 5 EXP
 * 3: 10 coins / 10 EXP
 * 4: 20 coins / 20 EXP
 * 5: 40 coins / 40 EXP
 */
fun rewardForDifficultyLevel(level: Int): Pair<Int, Int> = when (level.coerceIn(1, 5)) {
  1 -> 2 to 2
  2 -> 5 to 5
  3 -> 10 to 10
  4 -> 20 to 20
  else -> 40 to 40
}

fun difficultyForLevel(level: Int): TaskDifficulty = when (level.coerceIn(1, 5)) {
  1 -> TaskDifficulty.VERY_EASY
  2 -> TaskDifficulty.EASY
  3 -> TaskDifficulty.MEDIUM
  4 -> TaskDifficulty.HARD
  else -> TaskDifficulty.EXTREME
}

val READY_TASK_TEMPLATES: List<ReadyTaskTemplate> = listOf(
  // --- DOM I PORZĄDKI (dom) ---
  ReadyTaskTemplate(
    id = "dom_01",
    name = "Pościel łóżko",
    description = "Prosty poranny nawyk, który daje natychmiastowe poczucie porządku w pokoju.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 2,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("dom", "poranek", "nawyk", "szybkie"),
  ),
  ReadyTaskTemplate(
    id = "dom_02",
    name = "Umyj naczynia lub załaduj zmywarkę",
    description = "Czysty zlew ułatwia przygotowanie kolejnego posiłku bez zbędnego stresu.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("dom", "kuchnia", "porządki"),
  ),
  ReadyTaskTemplate(
    id = "dom_03",
    name = "Szybkie ogarnięcie pokoju",
    description = "Krótki reset przestrzeni w 3 prostych krokach bez wielkiego sprzątania.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 80, "Zdrowie" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("dom", "pokój", "porządki"),
    subtasks = listOf(
      "Odłóż rzeczy na miejsce",
      "Wyrzuć śmieci z biurka lub stolika",
      "Uporządkuj jedną widoczną powierzchnię",
    ),
  ),
  ReadyTaskTemplate(
    id = "dom_04",
    name = "Wyrzuć śmieci",
    description = "Szybkie wyniesienie śmieci i założenie nowego worka.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "porządki", "szybkie"),
  ),
  ReadyTaskTemplate(
    id = "dom_05",
    name = "Zrób pranie",
    description = "Pełny cykl prania od segregacji aż po rozwieszenie ubrań.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 70, "Siła" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "pranie", "porządki"),
    subtasks = listOf(
      "Posegreguj ubrania",
      "Nastaw pralkę",
      "Rozwieś lub przełóż do suszarki",
    ),
  ),

  // --- NAUKA I ROZWÓJ (nauka) ---
  ReadyTaskTemplate(
    id = "nauka_01",
    name = "Przeczytaj 10 stron książki",
    description = "Regularna lektura rozwojowa lub branżowa małymi porcjami każdego dnia.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "czytanie", "książka", "rozwój"),
  ),
  ReadyTaskTemplate(
    id = "nauka_02",
    name = "Nauka języka obcego — 10 minut",
    description = "Krótka powtórka słówek, fiszek lub dialogów w wybranym języku.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "języki", "fiszki"),
  ),
  ReadyTaskTemplate(
    id = "nauka_03",
    name = "Powtórz notatki z zajęć lub kursu",
    description = "Utrwalenie najważniejszych pojęć, zanim wylecą z pamięci.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "notatki", "kurs", "szkoła"),
  ),
  ReadyTaskTemplate(
    id = "nauka_04",
    name = "Obejrzyj lub wysłuchaj materiał edukacyjny",
    description = "Wykład, podcast lub lekcja wideo rozwijająca wybraną umiejętność.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 80, "Kreatywność" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "podcast", "wideo", "rozwój"),
  ),
  ReadyTaskTemplate(
    id = "nauka_05",
    name = "Sesja głębokiej nauki — 25 minut",
    description = "Skupiona sesja bez rozpraszaczy na najważniejszy temat dnia.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 60, "Dyscyplina" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "skupienie", "pomodoro"),
    subtasks = listOf(
      "Przygotuj materiały do nauki",
      "Wycisz rozpraszacze",
      "Pracuj w skupieniu przez 25 minut",
      "Zapisz 1–2 najważniejsze wnioski",
    ),
  ),

  // --- PRACA I ORGANIZACJA (praca) ---
  ReadyTaskTemplate(
    id = "praca_01",
    name = "Zaplanuj 3 najważniejsze rzeczy na dziś",
    description = "Krótki poranny plan, który porządkuje priorytety na cały dzień.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("praca", "planowanie", "priorytety", "poranek"),
    subtasks = listOf(
      "Wybierz jedno najważniejsze zadanie",
      "Dopisz maksymalnie dwa zadania dodatkowe",
      "Ustal, od czego zaczniesz",
    ),
  ),
  ReadyTaskTemplate(
    id = "praca_02",
    name = "Odpisz na zaległe wiadomości lub e-maile",
    description = "Krótki blok na zamknięcie otwartych wątków w skrzynce.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("praca", "komunikacja", "mail"),
  ),
  ReadyTaskTemplate(
    id = "praca_03",
    name = "Uporządkuj biurko lub pulpit komputera",
    description = "Czyste stanowisko pracy pomaga szybciej wejść w tryb skupienia.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 80, "Kreatywność" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "biurko", "organizacja"),
  ),
  ReadyTaskTemplate(
    id = "praca_04",
    name = "Przejrzyj listę zadań i kalendarz",
    description = "Sprawdź nadchodzące terminy i zaktualizuj plan na kolejne dni.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "kalendarz", "organizacja"),
  ),
  ReadyTaskTemplate(
    id = "praca_05",
    name = "Skupiony blok pracy — 25 minut",
    description = "Jeden konkretny blok pracy nad ważnym zadaniem bez przełączania kart.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("praca", "skupienie", "pomodoro"),
  ),

  // --- ZDROWIE I AKTYWNOŚĆ (ruch) ---
  ReadyTaskTemplate(
    id = "ruch_01",
    name = "Wypij szklankę wody",
    description = "Prosty nawyk wspierający energię i koncentrację w ciągu dnia.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 1,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zdrowie", "woda", "nawyk", "szybkie"),
  ),
  ReadyTaskTemplate(
    id = "ruch_02",
    name = "Krótkie rozciąganie — 5 minut",
    description = "Rozluźnienie barków, karku i pleców po pracy siedzącej lub po przebudzeniu.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 60, "Siła" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zdrowie", "ruch", "rozciąganie"),
  ),
  ReadyTaskTemplate(
    id = "ruch_03",
    name = "Spacer — 15 minut",
    description = "Krótki marsz na świeżym powietrzu dla głowy i kondycji.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 70, "Siła" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zdrowie", "spacer", "ruch"),
  ),
  ReadyTaskTemplate(
    id = "ruch_04",
    name = "Krótki trening w domu — 15 minut",
    description = "Kompaktowy zestaw ćwiczeń bez konieczności wychodzenia z domu.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 15,
    difficultyLevel = 3,
    attributeWeights = mapOf("Siła" to 60, "Zdrowie" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "trening", "siła"),
  ),
  ReadyTaskTemplate(
    id = "ruch_05",
    name = "Pełny trening lub aktywność — 30 minut",
    description = "Pełna jednostka treningowa, bieganie, rower, pływanie lub fitness.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 30,
    difficultyLevel = 4,
    attributeWeights = mapOf("Siła" to 60, "Zdrowie" to 30, "Dyscyplina" to 10),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "trening", "sport", "siła"),
    subtasks = listOf(
      "Krótka rozgrzewka",
      "Część główna treningu",
      "Rozciąganie po aktywności",
    ),
  ),

  // --- SAMOPOCZUCIE I ODPOCZYNEK (samopoczucie) ---
  ReadyTaskTemplate(
    id = "sam_01",
    name = "3 minuty spokojnego oddechu",
    description = "Chwila zatrzymania i powolnych oddechów, aby obniżyć napięcie.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 3,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("odpoczynek", "oddech", "spokój", "szybkie"),
  ),
  ReadyTaskTemplate(
    id = "sam_02",
    name = "Zapisz jedną rzecz, która dziś się udała",
    description = "Krótki wpis budujący uważność na codzienne postępy.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 3,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "dziennik", "wdzięczność"),
  ),
  ReadyTaskTemplate(
    id = "sam_03",
    name = "Przerwa bez telefonu — 15 minut",
    description = "Odpoczynek dla oczu i głowy z dala od ekranu i powiadomień.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 60, "Dyscyplina" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "detoks", "odpoczynek"),
  ),
  ReadyTaskTemplate(
    id = "sam_04",
    name = "Wieczorne wyciszenie przed snem",
    description = "Przyciemnienie światła, odłożenie ekranów i spokojna rutyna przed snem.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "sen", "wieczór"),
  ),
  ReadyTaskTemplate(
    id = "sam_05",
    name = "Czas dla siebie bez poczucia winy — 20 minut",
    description = "Świadomie zaplanowana chwila wyłącznie na to, co daje Ci wytchnienie.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 80, "Kreatywność" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "relaks", "odpoczynek"),
  ),

  // --- HOBBY I KREATYWNOŚĆ (hobby) ---
  ReadyTaskTemplate(
    id = "hobby_01",
    name = "10 minut na swoje hobby",
    description = "Krótka codzienna dawka pasji — rysowanie, muzyka, rękodzieło lub inny temat.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 80, "Zdrowie" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("hobby", "pasja", "kreatywność"),
  ),
  ReadyTaskTemplate(
    id = "hobby_02",
    name = "Szybki szkic, notatka lub pomysł kreatywny",
    description = "Złap jeden luźny pomysł bez oceniania — szkic, akapit albo melodia.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("hobby", "szkic", "pomysł", "szybkie"),
  ),
  ReadyTaskTemplate(
    id = "hobby_03",
    name = "Kontynuuj wybrany projekt twórczy — 20 minut",
    description = "Przesuń swój projekt do przodu o jeden konkretny krok.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "projekt", "twórczość"),
  ),
  ReadyTaskTemplate(
    id = "hobby_04",
    name = "Poznaj nową technikę lub inspirację",
    description = "Krótki poradnik, tutorial lub artykuł poszerzający warsztat.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 50, "Wiedza" to 50),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "inspiracja", "warsztat"),
  ),
  ReadyTaskTemplate(
    id = "hobby_05",
    name = "Uporządkuj materiały do swojego hobby",
    description = "Zadbaj o narzędzia i materiały, aby łatwo było wrócić do tworzenia.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 40, "Dyscyplina" to 60),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("hobby", "organizacja", "materiały"),
  ),

  // --- OPIEKA NAD ZWIERZĘTAMI (zwierzeta) ---
  ReadyTaskTemplate(
    id = "pet_01",
    name = "Sprawdź i uzupełnij świeżą wodę",
    description = "Umyj poidełko lub miskę i nalej czystej, świeżej wody.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 2,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "opieka", "woda", "szybkie"),
  ),
  ReadyTaskTemplate(
    id = "pet_02",
    name = "Nakarm zwierzę zgodnie z rutyną",
    description = "Regularna porcja pożywienia dopasowana do potrzeb Twojego podopiecznego.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "karmienie", "rutyna"),
  ),
  ReadyTaskTemplate(
    id = "pet_03",
    name = "Czas na zabawę lub kontakt — 10 minut",
    description = "Chwila pełnej uwagi, zabawy lub spokojnego kontaktu ze zwierzęciem.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "zabawa", "relacja"),
  ),
  ReadyTaskTemplate(
    id = "pet_04",
    name = "Posprzątaj miejsce, miski lub otoczenie zwierzęcia",
    description = "Utrzymanie czystości w legowisku, klatce, terrarium lub wokół misek.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 80, "Zdrowie" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "czystość", "opieka"),
  ),
  ReadyTaskTemplate(
    id = "pet_05",
    name = "Spacer lub aktywność na zewnątrz",
    description = "Wspólne wyjście i ruch na świeżym powietrzu (dla zwierząt wychodzących).",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 50, "Siła" to 30, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "spacer", "aktywność"),
    excludeFromAutoRecommendation = true,
  ),
  ReadyTaskTemplate(
    id = "pet_06",
    name = "Sprawdź zapasy karmy i akcesoriów",
    description = "Szybki przegląd zapasów, aby niczego nie zabrakło w ostatniej chwili.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "zapasy", "zakupy"),
    subtasks = listOf(
      "Sprawdź ilość karmy",
      "Sprawdź stan akcesoriów",
      "Dopisz brakujące rzeczy do listy zakupów",
    ),
  ),

  // --- PRZYGODY I SIDE QUESTY (side_quests) ---
  ReadyTaskTemplate(
    id = "quest_01",
    name = "Wyjdź na krótki spacer nową trasą",
    description = "Skręć w uliczkę lub ścieżkę, którą zazwyczaj omijasz.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "przygoda", "spacer"),
  ),
  ReadyTaskTemplate(
    id = "quest_02",
    name = "Zrób jedno zdjęcie czegoś ciekawego w okolicy",
    description = "Zauważ ciekawy detal, światło lub architekturę podczas wyjścia.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 10,
    difficultyLevel = 1,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "fotografia", "kreatywność"),
  ),
  ReadyTaskTemplate(
    id = "quest_03",
    name = "Wypróbuj nowy przepis lub napój",
    description = "Przygotuj coś, czego jeszcze nie robiłeś — prosty posiłek lub napar.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 60, "Zdrowie" to 40),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "kuchnia", "przepis"),
  ),
  ReadyTaskTemplate(
    id = "quest_04",
    name = "Odwiedź miejsce, w którym dawno nie byłeś",
    description = "Mała wyprawa do ulubionego parku, kawiarni lub punktu widokowego.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 50, "Kreatywność" to 50),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "wyjście", "przygoda"),
    subtasks = listOf(
      "Wybierz miejsce",
      "Zaplanuj krótkie wyjście",
      "Spędź tam chwilę bez pośpiechu",
    ),
  ),
  ReadyTaskTemplate(
    id = "quest_05",
    name = "Zrób małą niespodziankę sobie lub komuś bliskiemu",
    description = "Drobny miły gest — wiadomość, drobiazg albo ulubiona przekąska.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 60, "Zdrowie" to 40),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "niespodzianka", "relacje"),
  ),
  // ================================================================
  // ROZSZERZENIE KATALOGU: 209 NOWYCH ZADAŃ (41 + 209 = 250)
  // ID oryginalnych zadań są zachowane.
  // ================================================================

  // --- DODATKOWE SZABLONY: DOM (30) ---
  ReadyTaskTemplate(
    id = "dom_06",
    name = "Odłóż rzeczy z jednego stolika",
    description = "Uporządkuj jedną małą powierzchnię bez sprzątania całego pokoju.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("dom", "porządki", "małe kroki", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "dom_07",
    name = "Uporządkuj miejsce przy drzwiach",
    description = "Odłóż buty, klucze i drobiazgi na swoje miejsca.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("dom", "przedpokój", "organizacja", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "dom_08",
    name = "Przewietrz wybrany pokój",
    description = "Otwórz okno, jeśli warunki na to pozwalają, i zadbaj o komfort pomieszczenia.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 3,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 60, "Dyscyplina" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("dom", "pokój", "komfort", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "dom_09",
    name = "Sprawdź i wymień ręczniki",
    description = "Wymień używane ręczniki, jeśli wymagają prania.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "łazienka", "pranie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "dom_10",
    name = "Przetrzyj lustro w łazience",
    description = "Usuń smugi i zachlapania z jednego lustra.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "łazienka", "sprzątanie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "dom_11",
    name = "Umyj umywalkę i kran",
    description = "Oczyść umywalkę odpowiednim środkiem i wytrzyj kran.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 8,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "łazienka", "sprzątanie"),
  ),

  ReadyTaskTemplate(
    id = "dom_12",
    name = "Wyczyść toaletę",
    description = "Wykonaj podstawowe czyszczenie muszli i deski właściwym środkiem.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 12,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "łazienka", "higiena", "sprzątanie"),
  ),

  ReadyTaskTemplate(
    id = "dom_13",
    name = "Sprawdź zapasy w łazience",
    description = "Zobacz, czy wystarcza papieru, mydła i innych używanych produktów.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 4,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "łazienka", "zapasy", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "dom_14",
    name = "Uporządkuj jedną półkę",
    description = "Wybierz jedną półkę i odłóż przedmioty tak, by łatwo je znaleźć.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 80, "Kreatywność" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "półka", "porządek", "organizacja"),
    subtasks = listOf(
      "Zdejmij rzeczy z półki",
      "Przetrzyj powierzchnię",
      "Odłóż potrzebne przedmioty",
    ),
  ),

  ReadyTaskTemplate(
    id = "dom_15",
    name = "Odkurz jeden pokój",
    description = "Odkurz dostępną powierzchnię w wybranym pomieszczeniu.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 80, "Siła" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "odkurzanie", "pokój", "sprzątanie"),
  ),

  ReadyTaskTemplate(
    id = "dom_16",
    name = "Zetrzyj kurz z dostępnych mebli",
    description = "Przetrzyj kilka powierzchni w jednym pomieszczeniu.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 12,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "kurz", "meble", "sprzątanie"),
  ),

  ReadyTaskTemplate(
    id = "dom_17",
    name = "Umyj podłogę w jednym pomieszczeniu",
    description = "Umyj wybraną podłogę środkiem odpowiednim do jej rodzaju.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 80, "Siła" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "podłoga", "mycie", "sprzątanie"),
  ),

  ReadyTaskTemplate(
    id = "dom_18",
    name = "Poskładaj suche ubrania",
    description = "Złóż gotowe do schowania pranie.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "pranie", "ubrania"),
  ),

  ReadyTaskTemplate(
    id = "dom_19",
    name = "Odłóż czyste ubrania na miejsce",
    description = "Rozłóż złożone ubrania do szafek lub szafy.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "pranie", "szafa", "porządek"),
  ),

  ReadyTaskTemplate(
    id = "dom_20",
    name = "Wyczyść zlew kuchenny",
    description = "Umyj zlew i usuń resztki jedzenia z jego okolicy.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "kuchnia", "zlew", "sprzątanie"),
  ),

  ReadyTaskTemplate(
    id = "dom_21",
    name = "Przejrzyj zawartość lodówki",
    description = "Sprawdź, co już masz i co warto wykorzystać podczas najbliższych posiłków.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "kuchnia", "lodówka", "planowanie"),
  ),

  ReadyTaskTemplate(
    id = "dom_22",
    name = "Sprawdź terminy produktów spożywczych",
    description = "Przejrzyj wybraną półkę i oddziel produkty wymagające uwagi.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "kuchnia", "jedzenie", "lodówka"),
  ),

  ReadyTaskTemplate(
    id = "dom_23",
    name = "Przygotuj listę zakupów domowych",
    description = "Zapisz brakujące artykuły spożywcze i środki codziennego użytku.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "zakupy", "planowanie", "lista"),
  ),

  ReadyTaskTemplate(
    id = "dom_24",
    name = "Zrób szybki reset blatu kuchennego",
    description = "Odłóż rzeczy z blatu i przetrzyj wolną powierzchnię.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("dom", "kuchnia", "blat", "szybkie", "małe kroki"),
  ),

  ReadyTaskTemplate(
    id = "dom_25",
    name = "Przetrzyj fronty kilku szafek",
    description = "Oczyść widoczne zabrudzenia z wybranych frontów szafek.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("dom", "kuchnia", "szafki", "sprzątanie"),
  ),

  ReadyTaskTemplate(
    id = "dom_26",
    name = "Wyczyść czajnik zgodnie z instrukcją",
    description = "Wykonaj pielęgnację czajnika zalecaną przez producenta.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("dom", "kuchnia", "sprzęt", "konserwacja"),
  ),

  ReadyTaskTemplate(
    id = "dom_27",
    name = "Wypierz pościel",
    description = "Zdejmij i wypierz pościel, a następnie przygotuj ją do wysuszenia; czas dotyczy pracy aktywnej.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("dom", "sypialnia", "pranie", "pościel"),
    subtasks = listOf(
      "Zdejmij pościel",
      "Włóż odpowiedni program prania",
      "Przełóż lub rozwieś po zakończeniu",
    ),
  ),

  ReadyTaskTemplate(
    id = "dom_28",
    name = "Zmień poszewki na czyste",
    description = "Wymień poszewki na poduszkach i kołdrze.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("dom", "sypialnia", "pościel", "porządek"),
  ),

  ReadyTaskTemplate(
    id = "dom_29",
    name = "Umyj jedno okno",
    description = "Wyczyść dostępne szyby i ramy jednego okna bez wykonywania ryzykownych czynności na wysokości.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 35,
    difficultyLevel = 4,
    attributeWeights = mapOf("Dyscyplina" to 80, "Siła" to 20),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("dom", "okna", "większe porządki"),
  ),

  ReadyTaskTemplate(
    id = "dom_30",
    name = "Uporządkuj domowe dokumenty",
    description = "Posegreguj dokumenty w jednym segregatorze lub teczce.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("dom", "dokumenty", "papiery", "organizacja"),
    subtasks = listOf(
      "Wybierz jedną teczkę",
      "Oddziel nieaktualne papiery",
      "Ułóż potrzebne dokumenty",
    ),
  ),

  ReadyTaskTemplate(
    id = "dom_31",
    name = "Sprawdź zawartość domowej apteczki",
    description = "Zrób przegląd zapasów i terminów ważności produktów bez samodzielnego zmieniania leczenia.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("dom", "apteczka", "zapasy", "organizacja"),
    subtasks = listOf(
      "Sprawdź terminy ważności",
      "Zapisz brakujące produkty",
      "Odłóż rzeczy na miejsce",
    ),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "dom_32",
    name = "Przygotuj rzeczy potrzebne jutro",
    description = "Odłóż w jedno miejsce to, co przyda Ci się następnego dnia.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 7,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("dom", "wieczór", "organizacja", "planowanie"),
  ),

  ReadyTaskTemplate(
    id = "dom_33",
    name = "Uporządkuj plątaninę kabli",
    description = "Rozdziel i ułóż przewody w bezpiecznym miejscu, bez pracy przy uszkodzonych instalacjach.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("dom", "kable", "organizacja", "elektronika"),
  ),

  ReadyTaskTemplate(
    id = "dom_34",
    name = "Zorganizuj jedną część szafy",
    description = "Wybierz fragment szafy i uporządkuj ubrania lub akcesoria.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 45,
    difficultyLevel = 4,
    attributeWeights = mapOf("Dyscyplina" to 80, "Kreatywność" to 20),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("dom", "szafa", "ubrania", "większe porządki"),
    subtasks = listOf(
      "Wybierz jedną sekcję",
      "Odłóż rzeczy do zachowania",
      "Oddziel rzeczy do oddania lub naprawy",
    ),
  ),

  ReadyTaskTemplate(
    id = "dom_35",
    name = "Zrób sezonowy przegląd wybranej strefy",
    description = "Dokładniej uporządkuj jedną strefę mieszkania bez presji sprzątania wszystkiego.",
    area = OnboardingArea.DOM,
    estimatedMinutes = 90,
    difficultyLevel = 5,
    attributeWeights = mapOf("Dyscyplina" to 80, "Siła" to 20),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("dom", "sezonowe", "większe porządki", "projekt"),
    subtasks = listOf(
      "Wybierz strefę",
      "Podziel ją na mniejsze etapy",
      "Wykonaj zaplanowane porządki",
      "Odłóż narzędzia i rzeczy",
    ),
  ),

  // --- DODATKOWE SZABLONY: NAUKA (25) ---
  ReadyTaskTemplate(
    id = "nauka_06",
    name = "Otwórz materiały i wybierz temat",
    description = "Przygotuj się do nauki przez wykonanie jednego małego pierwszego kroku.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 2,
    difficultyLevel = 1,
    attributeWeights = mapOf("Wiedza" to 50, "Dyscyplina" to 50),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "start", "małe kroki", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "nauka_07",
    name = "Przejrzyj pięć fiszek",
    description = "Powtórz mały zestaw fiszek z wybranego tematu.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Wiedza" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "fiszki", "powtórka", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "nauka_08",
    name = "Powtórz pięć słówek",
    description = "Przypomnij sobie pięć słów z języka, którego się uczysz.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Wiedza" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "języki", "słówka", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "nauka_09",
    name = "Ułóż trzy zdania w języku obcym",
    description = "Wykorzystaj poznane słowa w trzech samodzielnych zdaniach.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Wiedza" to 80, "Kreatywność" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "języki", "pisanie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "nauka_10",
    name = "Rozwiąż trzy krótkie zadania",
    description = "Wybierz trzy ćwiczenia z materiału, nad którym pracujesz.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "ćwiczenia", "powtórka"),
  ),

  ReadyTaskTemplate(
    id = "nauka_11",
    name = "Sprawdź i popraw odpowiedzi",
    description = "Przejrzyj rozwiązania wcześniejszych ćwiczeń i zanotuj błędy.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 90, "Dyscyplina" to 10),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "błędy", "ćwiczenia"),
  ),

  ReadyTaskTemplate(
    id = "nauka_12",
    name = "Zapisz pytania do prowadzącego",
    description = "Zbierz w jednym miejscu kwestie, których jeszcze nie rozumiesz.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Wiedza" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "pytania", "szkoła", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "nauka_13",
    name = "Zrób mapę myśli",
    description = "Przedstaw najważniejsze pojęcia i ich połączenia na jednej stronie.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "mapa myśli", "notatki"),
    subtasks = listOf(
      "Zapisz główny temat",
      "Dodaj najważniejsze gałęzie",
      "Połącz powiązane pojęcia",
    ),
  ),

  ReadyTaskTemplate(
    id = "nauka_14",
    name = "Wyjaśnij temat własnymi słowami",
    description = "Spróbuj opowiedzieć wybrane zagadnienie tak, jak komuś początkującemu.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 90, "Kreatywność" to 10),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "zrozumienie", "powtórka"),
  ),

  ReadyTaskTemplate(
    id = "nauka_15",
    name = "Powtórz materiał z poprzedniego tygodnia",
    description = "Wróć do wcześniejszej partii materiału, aby sprawdzić zapamiętanie.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "powtórka", "systematyczność"),
  ),

  ReadyTaskTemplate(
    id = "nauka_16",
    name = "Ułóż miniquiz z pięciu pytań",
    description = "Stwórz pięć pytań sprawdzających poznany temat.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "quiz", "pytania"),
  ),

  ReadyTaskTemplate(
    id = "nauka_17",
    name = "Przeanalizuj popełnione błędy",
    description = "Wybierz kilka błędów i zanotuj, jak ich unikać.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 15,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "powtórka", "błędy"),
  ),

  ReadyTaskTemplate(
    id = "nauka_18",
    name = "Przygotuj tygodniowy plan nauki",
    description = "Podziel materiał na krótkie sesje możliwe do wykonania.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "plan", "organizacja"),
    subtasks = listOf(
      "Wypisz tematy",
      "Oceń czas potrzebny na każdy temat",
      "Rozłóż sesje w tygodniu",
    ),
  ),

  ReadyTaskTemplate(
    id = "nauka_19",
    name = "Rozbij projekt na etapy",
    description = "Zapisz mniejsze części większego zadania szkolnego lub edukacyjnego.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "projekt", "planowanie"),
  ),

  ReadyTaskTemplate(
    id = "nauka_20",
    name = "Poznaj jedno nowe pojęcie",
    description = "Znajdź definicję i przykład zastosowania wybranego terminu.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Wiedza" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "ciekawostki", "pojęcia", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "nauka_21",
    name = "Przeczytaj artykuł popularnonaukowy",
    description = "Wybierz przystępny artykuł i zapisz jedną nową informację.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 90, "Kreatywność" to 10),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "czytanie", "artykuł"),
  ),

  ReadyTaskTemplate(
    id = "nauka_22",
    name = "Zamień notatki na fiszki",
    description = "Przygotuj krótki zestaw pytań i odpowiedzi z własnych notatek.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "fiszki", "notatki"),
  ),

  ReadyTaskTemplate(
    id = "nauka_23",
    name = "Rozwiąż mały problem programistyczny",
    description = "Wybierz drobne ćwiczenie w języku, którego się uczysz.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "programowanie", "kodowanie"),
    subtasks = listOf(
      "Wybierz problem",
      "Zapisz przykładowe wejście i wynik",
      "Rozwiąż problem",
      "Sprawdź działanie",
    ),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "nauka_24",
    name = "Napisz test dla fragmentu kodu",
    description = "Przećwicz weryfikowanie działania jednej funkcji w swoim projekcie.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "programowanie", "testy"),
    subtasks = listOf(
      "Wybierz funkcję",
      "Wymyśl jeden przypadek testowy",
      "Uruchom test",
    ),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "nauka_25",
    name = "Napisz dziesięć linijek kodu ćwiczeniowego",
    description = "Poćwicz składnię lub mały element programu bez presji ukończenia projektu.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("nauka", "programowanie", "ćwiczenia"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "nauka_26",
    name = "Nagraj krótką wypowiedź w języku obcym",
    description = "Opowiedz o dowolnym temacie, aby poćwiczyć mówienie.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "języki", "mówienie"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "nauka_27",
    name = "Porównaj dwa źródła informacji",
    description = "Sprawdź, co jest wspólne, a co różni dwa materiały na ten sam temat.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "research", "źródła"),
  ),

  ReadyTaskTemplate(
    id = "nauka_28",
    name = "Wyjaśnij wątpliwość z materiału",
    description = "Wybierz jedno pytanie i poszukaj wiarygodnego objaśnienia.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "pytania", "research"),
  ),

  ReadyTaskTemplate(
    id = "nauka_29",
    name = "Zaplanuj przygotowanie do sprawdzianu",
    description = "Ustal zakres materiału i podziel go na realne części.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "egzamin", "planowanie"),
    subtasks = listOf(
      "Zapisz zakres",
      "Wybierz priorytety",
      "Rozplanuj powtórki",
    ),
  ),

  ReadyTaskTemplate(
    id = "nauka_30",
    name = "Ukończ moduł wybranego kursu",
    description = "Dokończ większy etap kursu, który realizujesz we własnym tempie.",
    area = OnboardingArea.NAUKA,
    estimatedMinutes = 60,
    difficultyLevel = 4,
    attributeWeights = mapOf("Wiedza" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("nauka", "kurs", "większy projekt"),
  ),

  // --- DODATKOWE SZABLONY: PRACA (20) ---
  ReadyTaskTemplate(
    id = "praca_06",
    name = "Wybierz jeden pierwszy krok do pracy",
    description = "Zapisz i wykonaj najmniejszą możliwą czynność rozpoczynającą zadanie.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 2,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("praca", "start", "małe kroki", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "praca_07",
    name = "Zamknij niepotrzebne karty i okna",
    description = "Zostaw tylko materiały potrzebne do aktualnego zadania.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("praca", "komputer", "skupienie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "praca_08",
    name = "Zaktualizuj status jednej sprawy",
    description = "Zapisz, co zostało zrobione i jaki jest następny krok.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("praca", "projekt", "status", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "praca_09",
    name = "Przygotuj się do spotkania",
    description = "Zapisz jego cel oraz maksymalnie trzy tematy do omówienia.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "spotkanie", "organizacja"),
  ),

  ReadyTaskTemplate(
    id = "praca_10",
    name = "Zapisz ustalenia po spotkaniu",
    description = "Zanotuj decyzje, odpowiedzialności i najbliższe działania.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "spotkanie", "notatki", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "praca_11",
    name = "Zrób krótką przerwę między blokami pracy",
    description = "Oderwij się od zadania i odpocznij przez kilka minut.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("praca", "przerwa", "samopoczucie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "praca_12",
    name = "Uporządkuj pięć plików roboczych",
    description = "Przenieś kilka plików do właściwych katalogów.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "pliki", "komputer", "porządek"),
  ),

  ReadyTaskTemplate(
    id = "praca_13",
    name = "Zarchiwizuj zakończoną sprawę",
    description = "Przenieś potrzebne materiały do archiwum projektu.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "archiwum", "dokumenty"),
  ),

  ReadyTaskTemplate(
    id = "praca_14",
    name = "Wypisz przeszkody w projekcie",
    description = "Zapisz rzeczy blokujące postęp, aby łatwiej było znaleźć rozwiązanie.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 40, "Dyscyplina" to 60),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "projekt", "planowanie"),
  ),

  ReadyTaskTemplate(
    id = "praca_15",
    name = "Zdefiniuj efekt jednego zadania",
    description = "Napisz, po czym poznasz, że konkretne zadanie zostało ukończone.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("praca", "cel", "planowanie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "praca_16",
    name = "Rozpocznij odkładane zadanie przez 5 minut",
    description = "Pracuj przez kilka minut nad sprawą, którą trudno rozpocząć.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("praca", "prokrastynacja", "małe kroki", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "praca_17",
    name = "Przejrzyj zbliżające się terminy",
    description = "Sprawdź najważniejsze daty i zobowiązania z najbliższych dni.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "terminy", "kalendarz"),
  ),

  ReadyTaskTemplate(
    id = "praca_18",
    name = "Podziel duże zadanie na części",
    description = "Zamień jedno duże zadanie na kilka kolejnych kroków.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "planowanie", "małe kroki"),
    subtasks = listOf(
      "Zapisz wynik końcowy",
      "Wypisz etapy",
      "Wybierz pierwszy krok",
    ),
  ),

  ReadyTaskTemplate(
    id = "praca_19",
    name = "Zamknij jedną drobną zaległość",
    description = "Dokończ jedną konkretną, niedużą sprawę z listy.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "zaległości", "organizacja"),
  ),

  ReadyTaskTemplate(
    id = "praca_20",
    name = "Uporządkuj kalendarz na kolejny tydzień",
    description = "Sprawdź terminy i usuń nieaktualne wpisy.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "kalendarz", "planowanie"),
  ),

  ReadyTaskTemplate(
    id = "praca_21",
    name = "Zrób przegląd zakończonych zadań",
    description = "Zapisz, co udało się ukończyć i co wymaga dalszej pracy.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "podsumowanie", "planowanie"),
  ),

  ReadyTaskTemplate(
    id = "praca_22",
    name = "Zaktualizuj jeden fragment CV",
    description = "Popraw sekcję doświadczenia, umiejętności albo osiągnięć.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 50, "Dyscyplina" to 50),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("praca", "CV", "kariera"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "praca_23",
    name = "Opisz projekt do portfolio",
    description = "Zapisz cel, swoje działania i efekt jednego projektu.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 45,
    difficultyLevel = 4,
    attributeWeights = mapOf("Wiedza" to 50, "Kreatywność" to 50),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("praca", "portfolio", "kariera"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "praca_24",
    name = "Przejrzyj miesięczne wydatki",
    description = "Porównaj zapisane koszty i zanotuj jedną obserwację.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 40, "Dyscyplina" to 60),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("finanse", "budżet", "organizacja"),
  ),

  ReadyTaskTemplate(
    id = "praca_25",
    name = "Ustal cel zawodowy na następny tydzień",
    description = "Wybierz jeden konkretny rezultat, na którym chcesz się skupić.",
    area = OnboardingArea.PRACA,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("praca", "cele", "planowanie"),
  ),

  // --- DODATKOWE SZABLONY: RUCH (25) ---
  ReadyTaskTemplate(
    id = "ruch_06",
    name = "Przejdź się przez chwilę po mieszkaniu",
    description = "Zrób krótką przerwę na delikatny ruch w odpowiednim dla siebie tempie.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 3,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zdrowie", "ruch", "dom", "małe kroki", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "ruch_07",
    name = "Zrób dwuminutową przerwę ruchową",
    description = "Wstań lub poruszaj się w sposób odpowiedni do swoich możliwości.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 2,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zdrowie", "przerwa", "ruch", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "ruch_08",
    name = "Delikatnie poruszaj barkami",
    description = "Wykonaj kilka spokojnych ruchów w bezbolesnym zakresie.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 4,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zdrowie", "mobilność", "przerwa", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "ruch_09",
    name = "Rozruszaj dłonie i nadgarstki",
    description = "Wybierz łagodną mobilizację dłoni bez forsowania stawów.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zdrowie", "mobilność", "dłonie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "ruch_10",
    name = "Zrób spokojną rozgrzewkę",
    description = "Przygotuj ciało do aktywności ruchem o niewielkiej intensywności.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 70, "Siła" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "rozgrzewka", "ruch"),
  ),

  ReadyTaskTemplate(
    id = "ruch_11",
    name = "Przygotuj się do krótkiego spaceru",
    description = "Wybierz trasę i przygotuj wygodne obuwie oraz odzież.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 3,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 60, "Zdrowie" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "spacer", "planowanie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "ruch_12",
    name = "Wykonaj 10 minut łagodnej mobilności",
    description = "Poruszaj stawami w komfortowym zakresie, bez forsowania.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 80, "Siła" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "mobilność", "rozciąganie"),
  ),

  ReadyTaskTemplate(
    id = "ruch_13",
    name = "Potańcz do dwóch ulubionych piosenek",
    description = "Wybierz ruch w rytm muzyki dopasowany do samopoczucia.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "taniec", "muzyka"),
  ),

  ReadyTaskTemplate(
    id = "ruch_14",
    name = "Wybierz się na 25-minutowy spacer",
    description = "Poświęć czas na spacer w tempie odpowiednim do możliwości.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "spacer", "aktywność"),
  ),

  ReadyTaskTemplate(
    id = "ruch_15",
    name = "Spróbuj energiczniejszego marszu",
    description = "Jeśli to dla Ciebie odpowiednie, wybierz żwawszy spacer.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 80, "Siła" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "marsz", "kondycja"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "ruch_16",
    name = "Wybierz się na przejażdżkę rowerem",
    description = "Pokonaj bezpieczną trasę w spokojnym tempie i zgodnie ze swoimi możliwościami.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 80, "Siła" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "rower", "aktywność"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "ruch_17",
    name = "Spróbuj krótkiej, łagodnej jogi",
    description = "Wykonaj znane sobie ćwiczenia o odpowiedniej intensywności.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 70, "Siła" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "joga", "mobilność"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "ruch_18",
    name = "Poćwicz równowagę z bezpiecznym podparciem",
    description = "Wykonaj proste ćwiczenia tylko wtedy, gdy są odpowiednie do Twoich możliwości.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 60, "Siła" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "równowaga", "sprawność"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "ruch_19",
    name = "Zrób krótki trening z masą ciała",
    description = "Wykonaj kilka znanych ćwiczeń w dopasowanej intensywności.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 15,
    difficultyLevel = 3,
    attributeWeights = mapOf("Siła" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "trening", "siła"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "ruch_20",
    name = "Poćwicz z gumą treningową",
    description = "Użyj sprzętu zgodnie z instrukcją i własnym planem aktywności.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Siła" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "gumy", "siła", "sprzęt"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "ruch_21",
    name = "Wykonaj trening siłowy według własnego planu",
    description = "Zrealizuj zaplanowaną jednostkę dopasowaną do możliwości.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 45,
    difficultyLevel = 4,
    attributeWeights = mapOf("Siła" to 70, "Zdrowie" to 20, "Dyscyplina" to 10),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "siła", "trening"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "ruch_22",
    name = "Wybierz się na długi spacer",
    description = "Przeznacz więcej czasu na marsz bez presji tempa.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 45,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "spacer", "aktywność"),
  ),

  ReadyTaskTemplate(
    id = "ruch_23",
    name = "Popływaj rekreacyjnie",
    description = "Wybierz odpowiedni basen i aktywność dostosowaną do umiejętności.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 40,
    difficultyLevel = 4,
    attributeWeights = mapOf("Zdrowie" to 80, "Siła" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "pływanie", "basen"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "ruch_24",
    name = "Przygotuj strój i akcesoria do ruchu",
    description = "Odłóż potrzebne rzeczy, aby ułatwić sobie rozpoczęcie aktywności.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "przygotowanie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "ruch_25",
    name = "Zaplanuj aktywność na najbliższy tydzień",
    description = "Wybierz realne terminy i rodzaje ruchu, które lubisz.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 60, "Zdrowie" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "planowanie", "aktywność"),
  ),

  ReadyTaskTemplate(
    id = "ruch_26",
    name = "Zapisz, jak się czujesz po aktywności",
    description = "Zanotuj krótką obserwację bez oceniania wyniku lub wyglądu.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zdrowie", "refleksja", "samopoczucie", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "ruch_27",
    name = "Wypróbuj nowy rodzaj ruchu",
    description = "Poznaj aktywność, którą możesz bezpiecznie dostosować do siebie.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("zdrowie", "nowości", "aktywność"),
  ),

  ReadyTaskTemplate(
    id = "ruch_28",
    name = "Zrób przerwę od siedzenia",
    description = "Wybierz kilka prostych ruchów lub zmianę pozycji ciała.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 7,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zdrowie", "przerwa", "biurko", "ruch"),
  ),

  ReadyTaskTemplate(
    id = "ruch_29",
    name = "Zaplanuj dłuższą wycieczkę pieszą",
    description = "Wybierz odpowiednią trasę i przygotuj plan wyjścia.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 90,
    difficultyLevel = 4,
    attributeWeights = mapOf("Dyscyplina" to 50, "Zdrowie" to 50),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("zdrowie", "wycieczka", "planowanie"),
    subtasks = listOf(
      "Wybierz trasę dopasowaną do możliwości",
      "Sprawdź potrzebne rzeczy",
      "Ustal plan odpoczynku",
    ),
  ),

  ReadyTaskTemplate(
    id = "ruch_30",
    name = "Zrealizuj dłuższą aktywność według własnego planu",
    description = "Wykonaj większe wyzwanie ruchowe dobrane do swoich możliwości i kondycji.",
    area = OnboardingArea.RUCH,
    estimatedMinutes = 60,
    difficultyLevel = 5,
    attributeWeights = mapOf("Siła" to 50, "Zdrowie" to 40, "Dyscyplina" to 10),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("zdrowie", "wyzwanie", "trening"),
    excludeFromAutoRecommendation = true,
  ),

  // --- DODATKOWE SZABLONY: SAM (20) ---
  ReadyTaskTemplate(
    id = "sam_06",
    name = "Sprawdź swój poziom energii",
    description = "Nazwij w myślach, czy masz dziś mniej czy więcej sił, bez oceniania siebie.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 2,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "energia", "małe kroki", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "sam_07",
    name = "Nazwij obecną emocję",
    description = "Zauważ, co czujesz w tej chwili, bez potrzeby zmieniania emocji.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 3,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "emocje", "uważność", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "sam_08",
    name = "Znajdź miły szczegół w otoczeniu",
    description = "Zwróć uwagę na jedną drobną rzecz, która sprawia Ci przyjemność.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 3,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "uważność", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "sam_09",
    name = "Zapisz jedną granicę na dziś",
    description = "Zastanów się, na co chcesz przeznaczyć energię, a na co nie.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "granice", "planowanie"),
  ),

  ReadyTaskTemplate(
    id = "sam_10",
    name = "Wybierz jedną rzecz do odłożenia",
    description = "Sprawdź, czy z listy możesz bezpiecznie odłożyć mniej pilne zadanie.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "odciążenie", "organizacja"),
  ),

  ReadyTaskTemplate(
    id = "sam_11",
    name = "Przygotuj ulubioną muzykę",
    description = "Wybierz kilka utworów, które lubisz słuchać w wolnej chwili.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 40, "Kreatywność" to 60),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "muzyka", "relaks"),
  ),

  ReadyTaskTemplate(
    id = "sam_12",
    name = "Wycisz powiadomienia na wybraną przerwę",
    description = "Skorzystaj z trybu skupienia, jeśli nie przegapisz ważnych wiadomości.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "powiadomienia", "cyfrowy odpoczynek"),
  ),

  ReadyTaskTemplate(
    id = "sam_13",
    name = "Zapisz krótką refleksję o dniu",
    description = "Wpisz jedno zdanie o tym, co było dla Ciebie ważne.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Zdrowie" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("samopoczucie", "dziennik", "pisanie"),
  ),

  ReadyTaskTemplate(
    id = "sam_14",
    name = "Urządź spokojny kącik",
    description = "Przygotuj jedno wygodne miejsce do odpoczynku.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 60, "Dyscyplina" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "dom", "relaks"),
  ),

  ReadyTaskTemplate(
    id = "sam_15",
    name = "Zaplanuj wieczór w swoim tempie",
    description = "Wybierz jedną opcjonalną czynność na przyjemniejszy wieczór.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "wieczór", "planowanie"),
  ),

  ReadyTaskTemplate(
    id = "sam_16",
    name = "Pisz swobodnie w dzienniku przez 10 minut",
    description = "Zapisz myśli bez oceniania stylu i bez konieczności pokazywania ich innym.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "dziennik", "pisanie"),
  ),

  ReadyTaskTemplate(
    id = "sam_17",
    name = "Rozpoznaj sygnały, że potrzebujesz przerwy",
    description = "Zapisz, po czym poznajesz u siebie zmęczenie lub przeciążenie.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "energia", "odpoczynek"),
  ),

  ReadyTaskTemplate(
    id = "sam_18",
    name = "Napisz do bliskiej osoby",
    description = "Wyślij życzliwą wiadomość, jeśli masz na to ochotę.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "relacje", "kontakt"),
  ),

  ReadyTaskTemplate(
    id = "sam_19",
    name = "Zrób krótkie rozluźnienie",
    description = "Wybierz komfortową, spokojną formę rozluźnienia, bez wymuszonego oddychania.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "relaks", "rozluźnienie"),
  ),

  ReadyTaskTemplate(
    id = "sam_20",
    name = "Stwórz listę rzeczy, które lubisz robić",
    description = "Zapisz kilka aktywności, po które możesz sięgnąć w wolnym czasie.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("samopoczucie", "hobby", "lista"),
  ),

  ReadyTaskTemplate(
    id = "sam_21",
    name = "Zapewnij sobie czas na przyjemną aktywność",
    description = "Wybierz coś, co naprawdę sprawia Ci przyjemność; odpoczynek nie wymaga zasłużenia.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 80, "Kreatywność" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "przyjemność", "relaks"),
  ),

  ReadyTaskTemplate(
    id = "sam_22",
    name = "Uprość plan na mniej energetyczny dzień",
    description = "Zostaw zadania konieczne i oznacz resztę jako opcjonalne.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 50, "Dyscyplina" to 50),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "małe kroki", "organizacja"),
  ),

  ReadyTaskTemplate(
    id = "sam_23",
    name = "Uporządkuj najbliższe terminy",
    description = "Wypisz sprawy z najbliższych dni, aby nie nosić ich wszystkich w pamięci.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "planowanie", "terminy"),
  ),

  ReadyTaskTemplate(
    id = "sam_24",
    name = "Zaplanuj spokojny czas bez nadmiaru bodźców",
    description = "Wybierz spokojną aktywność odpowiadającą własnym preferencjom.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "wyciszenie", "odpoczynek"),
  ),

  ReadyTaskTemplate(
    id = "sam_25",
    name = "Zrób łagodne podsumowanie tygodnia",
    description = "Zapisz, co było pomocne i co chcesz zmienić bez oceniania wyników.",
    area = OnboardingArea.SAMOPOCZUCIE,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 60, "Dyscyplina" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("samopoczucie", "podsumowanie", "dziennik"),
    subtasks = listOf(
      "Wypisz jedną dobrą rzecz",
      "Zapisz trudność bez oceniania siebie",
      "Wybierz jedną zmianę na kolejny tydzień",
    ),
  ),

  // --- DODATKOWE SZABLONY: HOBBY (30) ---
  ReadyTaskTemplate(
    id = "hobby_06",
    name = "Narysuj przedmiot stojący obok",
    description = "Poświęć kilka minut na luźny szkic zwykłego przedmiotu.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "rysowanie", "szkic"),
  ),

  ReadyTaskTemplate(
    id = "hobby_07",
    name = "Wypróbuj inne narzędzie artystyczne",
    description = "Narysuj coś narzędziem, którego rzadziej używasz.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "rysowanie", "technika"),
  ),

  ReadyTaskTemplate(
    id = "hobby_08",
    name = "Stwórz małą paletę kolorów",
    description = "Wybierz trzy do pięciu barw pasujących do wymyślonego nastroju.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "kolory", "sztuka", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "hobby_09",
    name = "Zrób mini kolaż",
    description = "Połącz wycinki, zdjęcia lub własne rysunki w jedną kompozycję.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "kolaż", "DIY", "sztuka"),
  ),

  ReadyTaskTemplate(
    id = "hobby_10",
    name = "Zrób serię pięciu zdjęć na temat",
    description = "Wybierz temat i pokaż go na kilku różnych zdjęciach.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "fotografia", "projekt"),
  ),

  ReadyTaskTemplate(
    id = "hobby_11",
    name = "Wybierz najlepsze zdjęcie tygodnia",
    description = "Przejrzyj swoje fotografie i wybierz jedno ulubione ujęcie.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "fotografia", "przegląd"),
  ),

  ReadyTaskTemplate(
    id = "hobby_12",
    name = "Edytuj jedno zdjęcie",
    description = "Popracuj nad kadrem, światłem lub kolorystyką jednej fotografii.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "fotografia", "edycja"),
  ),

  ReadyTaskTemplate(
    id = "hobby_13",
    name = "Napisz krótki wiersz",
    description = "Zapisz kilka wersów na dowolny temat.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "pisanie", "poezja"),
  ),

  ReadyTaskTemplate(
    id = "hobby_14",
    name = "Dopisz akapit do opowiadania",
    description = "Rozwiń swoją historię o niewielki fragment.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "pisanie", "opowiadanie"),
  ),

  ReadyTaskTemplate(
    id = "hobby_15",
    name = "Wymyśl postać do własnej historii",
    description = "Zapisz wygląd, motywację i jedną cechę wymyślonej postaci.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "pisanie", "postacie"),
    subtasks = listOf(
      "Wymyśl imię",
      "Zapisz cel postaci",
      "Dodaj wyróżniającą cechę",
    ),
  ),

  ReadyTaskTemplate(
    id = "hobby_16",
    name = "Stwórz playlistę tematyczną",
    description = "Wybierz utwory pasujące do określonego nastroju lub historii.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "muzyka", "playlista"),
  ),

  ReadyTaskTemplate(
    id = "hobby_17",
    name = "Poćwicz znany fragment utworu",
    description = "Zagraj lub zaśpiewaj krótki fragment we własnym tempie.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "muzyka", "instrument"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "hobby_18",
    name = "Wymyśl krótką melodię",
    description = "Stwórz prosty motyw muzyczny lub rytmiczny.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "muzyka", "kompozycja"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "hobby_19",
    name = "Zrób pięciominutową improwizację",
    description = "Wybierz instrument lub rytm i pobaw się dźwiękami.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "muzyka", "improwizacja", "szybkie"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "hobby_20",
    name = "Złóż proste origami",
    description = "Wykorzystaj kartkę i wybraną instrukcję składania papieru.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "origami", "rękodzieło"),
  ),

  ReadyTaskTemplate(
    id = "hobby_21",
    name = "Dokończ jeden etap rękodzieła",
    description = "Zajmij się wybranym małym fragmentem robótki lub projektu DIY.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "rękodzieło", "DIY"),
  ),

  ReadyTaskTemplate(
    id = "hobby_22",
    name = "Przygotuj materiały do projektu",
    description = "Wybierz potrzebne narzędzia i zorganizuj miejsce pracy.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "materiały", "organizacja"),
  ),

  ReadyTaskTemplate(
    id = "hobby_23",
    name = "Ugotuj nieznaną prostą potrawę",
    description = "Wypróbuj przepis odpowiedni do umiejętności i dostępnych składników.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("hobby", "gotowanie", "przepis"),
  ),

  ReadyTaskTemplate(
    id = "hobby_24",
    name = "Poznaj prostą technikę DIY",
    description = "Przećwicz małą czynność związaną z rękodziełem lub majsterkowaniem.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 50, "Kreatywność" to 50),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("hobby", "DIY", "nowa umiejętność"),
  ),

  ReadyTaskTemplate(
    id = "hobby_25",
    name = "Wymyśl zasady minigry",
    description = "Zaprojektuj cel, zasady i punktowanie prostej gry.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("hobby", "gry", "projektowanie"),
  ),

  ReadyTaskTemplate(
    id = "hobby_26",
    name = "Zrób szkic prototypu aplikacji",
    description = "Rozrysuj jeden ekran aplikacji na kartce lub w edytorze.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("hobby", "programowanie", "UI"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "hobby_27",
    name = "Ulepsz drobny fragment własnego kodu",
    description = "Wybierz jedną niewielką zmianę w projekcie, który już rozwijasz.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "programowanie", "projekt"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "hobby_28",
    name = "Naszkicuj interfejs własnego pomysłu",
    description = "Przedstaw układ przycisków i treści dla fikcyjnego ekranu.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("hobby", "UI", "projektowanie"),
  ),

  ReadyTaskTemplate(
    id = "hobby_29",
    name = "Zbadaj jeden temat z pasji",
    description = "Wybierz zagadnienie i zbierz trzy interesujące informacje.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "research", "ciekawostki"),
  ),

  ReadyTaskTemplate(
    id = "hobby_30",
    name = "Porównaj trzy źródła do researchu",
    description = "Wybierz źródła na jeden temat i zapisz różnice w ich opisach.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "research", "źródła"),
  ),

  ReadyTaskTemplate(
    id = "hobby_31",
    name = "Zrób zdjęcie w nietypowym świetle",
    description = "Spróbuj pokazać znajomy obiekt przy innym oświetleniu.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("hobby", "fotografia", "światło"),
  ),

  ReadyTaskTemplate(
    id = "hobby_32",
    name = "Zaplanuj kreatywny czas na weekend",
    description = "Wybierz jedną przyjemną aktywność oraz potrzebne rzeczy.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 50, "Dyscyplina" to 50),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "planowanie", "weekend"),
  ),

  ReadyTaskTemplate(
    id = "hobby_33",
    name = "Wróć do odłożonego projektu",
    description = "Otwórz starszy projekt i wykonaj jeden mały krok.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 70, "Dyscyplina" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("hobby", "projekt", "małe kroki"),
  ),

  ReadyTaskTemplate(
    id = "hobby_34",
    name = "Zrób przegląd własnych prac",
    description = "Wybierz kilka prac i zanotuj, czego nauczyła Cię ich realizacja.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("hobby", "podsumowanie", "portfolio"),
  ),

  ReadyTaskTemplate(
    id = "hobby_35",
    name = "Ukończ mały projekt kreatywny",
    description = "Doprowadź niewielki autorski projekt do satysfakcjonującego końca.",
    area = OnboardingArea.HOBBY,
    estimatedMinutes = 60,
    difficultyLevel = 4,
    attributeWeights = mapOf("Kreatywność" to 80, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("hobby", "projekt", "twórczość"),
    subtasks = listOf(
      "Wybierz zakres projektu",
      "Zrealizuj ostatnie etapy",
      "Zapisz efekt końcowy",
    ),
  ),

  // --- DODATKOWE SZABLONY: PET (29) ---
  ReadyTaskTemplate(
    id = "pet_07",
    name = "Zrób psu krótką zabawę węchową",
    description = "Zaproponuj bezpieczną aktywność węchową dostosowaną do psa.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "pies", "zabawa", "węch"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_08",
    name = "Wyjdź na krótki spacer z psem",
    description = "Wybierz trasę odpowiednią do potrzeb, wieku i kondycji psa.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 60, "Zdrowie" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "pies", "spacer"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_09",
    name = "Poćwicz znaną komendę z psem",
    description = "Wykonaj krótką, pozytywną sesję z wykorzystaniem znanej komendy.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "pies", "trening"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_10",
    name = "Przygotuj bezpieczną zabawkę węchową dla psa",
    description = "Wybierz akcesorium odpowiednie do wielkości i zachowania psa.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 50, "Dyscyplina" to 50),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "pies", "zabawa", "akcesoria"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_11",
    name = "Wyczesz psa, jeśli tego potrzebuje",
    description = "Zadbaj o sierść zgodnie z rodzajem okrywy i tolerancją zwierzęcia.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "pies", "sierść", "pielęgnacja"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_12",
    name = "Sprawdź stan obroży lub szelek",
    description = "Oceń dopasowanie i stan używanych akcesoriów spacerowych.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "pies", "bezpieczeństwo", "akcesoria"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_13",
    name = "Przygotuj potrzebne rzeczy na spacer",
    description = "Sprawdź smycz, woreczki i akcesoria przed wyjściem.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "pies", "spacer", "przygotowanie"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_14",
    name = "Pobaw się z kotem w polowanie na zabawkę",
    description = "Wybierz zabawę dopasowaną do zachowania kota i jego możliwości.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "kot", "zabawa"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_15",
    name = "Sprawdź czystość kuwety",
    description = "Obejrzyj kuwetę i wykonaj potrzebne czynności higieniczne.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "kot", "kuweta", "higiena"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_16",
    name = "Wyczyść kuwetę zgodnie z potrzebą",
    description = "Użyj odpowiednich środków i zadbaj o czyste otoczenie kuwety.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "kot", "kuweta", "sprzątanie"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_17",
    name = "Wyczesz kota, jeśli tego potrzebuje",
    description = "Wybierz odpowiednią szczotkę i uszanuj komfort kota.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "kot", "sierść", "pielęgnacja"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_18",
    name = "Sprawdź stan drapaka",
    description = "Obejrzyj, czy drapak jest stabilny i nadaje się do dalszego używania.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("zwierzęta", "kot", "drapak", "akcesoria"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_19",
    name = "Sprawdź poidełko małego ssaka",
    description = "Upewnij się, że woda jest dostępna, a poidełko działa poprawnie.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 3,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "gryzoń", "królik", "woda"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_20",
    name = "Uporządkuj środowisko małego ssaka",
    description = "Wykonaj czynności porządkowe właściwe dla danego gatunku.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "gryzoń", "królik", "czystość"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_21",
    name = "Uzupełnij siano u zwierzęcia, które go potrzebuje",
    description = "Sprawdź zapas i uzupełnij zgodnie z potrzebami gatunku.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "królik", "świnka morska", "siano"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_22",
    name = "Zaobserwuj zachowanie małego ssaka",
    description = "Poświęć chwilę na spokojną obserwację ruchu i zachowania zwierzęcia.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "gryzoń", "królik", "obserwacja"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_23",
    name = "Sprawdź wodę u ptaka",
    description = "Upewnij się, że ptak ma dostęp do wody odpowiedniej do jego potrzeb.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 4,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "ptak", "woda", "opieka"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_24",
    name = "Sprawdź zabawki i wyposażenie ptaka",
    description = "Zwróć uwagę na zużycie elementów i ewentualne zagrożenia.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "ptak", "akcesoria", "bezpieczeństwo"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_25",
    name = "Uporządkuj otoczenie ptaka",
    description = "Wykonaj potrzebne czynności porządkowe bez drażniących środków.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "ptak", "czystość"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_26",
    name = "Sprawdź działanie filtra akwarium",
    description = "Sprawdź, czy sprzęt działa zgodnie z instrukcją i potrzebami zbiornika.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "ryby", "akwarium", "filtr"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_27",
    name = "Obserwuj zachowanie ryb",
    description = "Sprawdź, czy ryby zachowują się zgodnie ze swoim zwykłym rytmem.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "ryby", "akwarium", "obserwacja"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_28",
    name = "Sprawdź parametry wody zgodnie z planem",
    description = "Wykonaj testy właściwe dla danego akwarium i zapisz odczyty.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 50, "Dyscyplina" to 50),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "ryby", "akwarium", "parametry"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_29",
    name = "Zaplanuj pielęgnację akwarium",
    description = "Sprawdź swój harmonogram i potrzebne materiały bez ustalania uniwersalnych norm.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "ryby", "akwarium", "planowanie"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_30",
    name = "Sprawdź warunki w terrarium",
    description = "Porównaj odczyty z wymaganiami konkretnego gatunku.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.DAILY,
    tags = listOf("zwierzęta", "gad", "terrarium", "warunki"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_31",
    name = "Skontroluj sprzęt terrarium",
    description = "Sprawdź wyposażenie i działanie urządzeń zgodnie z instrukcją.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "gad", "terrarium", "sprzęt"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_32",
    name = "Zapisz krótką obserwację pupila",
    description = "Zanotuj jedną zmianę lub zwyczaj, jeśli chcesz śledzić jego samopoczucie.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.WEEKLY,
    tags = listOf("zwierzęta", "obserwacja", "dziennik"),
  ),

  ReadyTaskTemplate(
    id = "pet_33",
    name = "Zaplanuj wizytę u weterynarza, jeśli potrzebna",
    description = "Sprawdź dostępne terminy i przygotuj informacje do konsultacji.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("zwierzęta", "weterynarz", "terminy"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_34",
    name = "Sprawdź terminy zaleconej profilaktyki",
    description = "Przejrzyj zalecenia specjalisty i zapisz nadchodzące terminy.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("zwierzęta", "profilaktyka", "weterynarz"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "pet_35",
    name = "Przygotuj pytania do weterynarza",
    description = "Spisz obserwacje i kwestie, które chcesz omówić podczas konsultacji.",
    area = OnboardingArea.ZWIERZETA,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.MONTHLY,
    tags = listOf("zwierzęta", "weterynarz", "notatki"),
    excludeFromAutoRecommendation = true,
  ),

  // --- DODATKOWE SZABLONY: QUEST (30) ---
  ReadyTaskTemplate(
    id = "quest_06",
    name = "Znajdź pięć rzeczy w jednym kolorze",
    description = "Rozejrzyj się i wypatrz pięć przedmiotów o wybranym kolorze.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "kolory", "zabawa", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "quest_07",
    name = "Zrób zdjęcie codziennego detalu",
    description = "Uchwyć coś zwykłego w nietypowy sposób.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 5,
    difficultyLevel = 1,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "fotografia", "szybkie"),
  ),

  ReadyTaskTemplate(
    id = "quest_08",
    name = "Zauważ trzy rodzaje roślin w okolicy",
    description = "Podczas spaceru wypatrz trzy różne rośliny bez ich zrywania.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 50, "Zdrowie" to 50),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "przyroda", "obserwacja"),
  ),

  ReadyTaskTemplate(
    id = "quest_09",
    name = "Przejdź się nieznanym fragmentem okolicy",
    description = "Wybierz bezpieczną, krótką trasę, którą rzadko odwiedzasz.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "spacer", "eksploracja"),
  ),

  ReadyTaskTemplate(
    id = "quest_10",
    name = "Odwiedź nowe miejsce niedaleko domu",
    description = "Odkryj dostępne dla Ciebie miejsce w najbliższej okolicy.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 50, "Kreatywność" to 50),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "odkrywanie", "wyjście"),
  ),

  ReadyTaskTemplate(
    id = "quest_11",
    name = "Poznaj pochodzenie ciekawego słowa",
    description = "Sprawdź etymologię jednego wyrazu i zapisz ciekawostkę.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "słowa", "ciekawostki"),
  ),

  ReadyTaskTemplate(
    id = "quest_12",
    name = "Znajdź trzy fakty o nowym temacie",
    description = "Wybierz temat, który dotąd Cię nie interesował, i sprawdź trzy rzeczy.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "research", "wiedza"),
  ),

  ReadyTaskTemplate(
    id = "quest_13",
    name = "Ułóż krótką zagadkę",
    description = "Wymyśl pytanie lub zagadkę, którą można komuś zadać.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "zagadka", "kreatywność"),
  ),

  ReadyTaskTemplate(
    id = "quest_14",
    name = "Napisz historię w pięciu zdaniach",
    description = "Stwórz krótką opowieść, która ma początek, środek i koniec.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "pisanie", "historia"),
  ),

  ReadyTaskTemplate(
    id = "quest_15",
    name = "Zaprojektuj symbol fikcyjnej drużyny",
    description = "Wymyśl nazwę i prosty znak nieistniejącej drużyny.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "rysowanie", "projektowanie"),
  ),

  ReadyTaskTemplate(
    id = "quest_16",
    name = "Narysuj mapkę miejsca z pamięci",
    description = "Spróbuj narysować układ znanego pomieszczenia lub okolicy.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "mapa", "pamięć"),
  ),

  ReadyTaskTemplate(
    id = "quest_17",
    name = "Posłuchaj nieznanego gatunku muzyki",
    description = "Odkryj jeden utwór w stylu, którego zwykle nie wybierasz.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 40, "Kreatywność" to 60),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "muzyka", "odkrywanie"),
  ),

  ReadyTaskTemplate(
    id = "quest_18",
    name = "Przygotuj prosty przepis z trzech składników",
    description = "Wybierz odpowiednią potrawę i spróbuj nowego połączenia smaków.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 70, "Wiedza" to 30),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "kuchnia", "eksperyment"),
  ),

  ReadyTaskTemplate(
    id = "quest_19",
    name = "Wymyśl papierową grę",
    description = "Zaprojektuj prostą zabawę, do której wystarczy kartka i długopis.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "gra", "projektowanie"),
  ),

  ReadyTaskTemplate(
    id = "quest_20",
    name = "Zrób trzy zdjęcia z różnych perspektyw",
    description = "Pokaż jeden obiekt na trzy sposoby.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "fotografia", "perspektywa"),
  ),

  ReadyTaskTemplate(
    id = "quest_21",
    name = "Wypatrz pięć ciekawych detali",
    description = "Podczas wyjścia znajdź pięć rzeczy, których zwykle nie zauważasz.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 15,
    difficultyLevel = 2,
    attributeWeights = mapOf("Kreatywność" to 60, "Zdrowie" to 40),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "spacer", "obserwacja"),
  ),

  ReadyTaskTemplate(
    id = "quest_22",
    name = "Odkryj bezpłatne miejsce w okolicy",
    description = "Wybierz park, skwer lub inną dostępną przestrzeń do odwiedzenia.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 50, "Wiedza" to 50),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "wyjście", "bez wydatków"),
  ),

  ReadyTaskTemplate(
    id = "quest_23",
    name = "Poznaj ofertę pobliskiej biblioteki",
    description = "Sprawdź zbiory lub dostępne wydarzenia w bibliotece, jeśli jest w Twojej okolicy.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "biblioteka", "kultura"),
    excludeFromAutoRecommendation = true,
  ),

  ReadyTaskTemplate(
    id = "quest_24",
    name = "Napisz list do siebie za rok",
    description = "Zapisz kilka myśli, które chciałbyś przeczytać w przyszłości.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 70, "Zdrowie" to 30),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "pisanie", "refleksja"),
  ),

  ReadyTaskTemplate(
    id = "quest_25",
    name = "Narysuj komiks z trzech kadrów",
    description = "Wymyśl prostą sytuację i pokaż ją w trzech obrazkach.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "komiks", "rysowanie"),
  ),

  ReadyTaskTemplate(
    id = "quest_26",
    name = "Zrób coś kreatywnego z materiałów z odzysku",
    description = "Wykorzystaj bezpieczne przedmioty, które już masz.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 80, "Wiedza" to 20),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "DIY", "recykling"),
  ),

  ReadyTaskTemplate(
    id = "quest_27",
    name = "Poznaj pięć słów w nieznanym języku",
    description = "Odkryj podstawowe wyrazy języka, którego wcześniej się nie uczyłeś.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Wiedza" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "języki", "ciekawostki"),
  ),

  ReadyTaskTemplate(
    id = "quest_28",
    name = "Wybierz inną trasę znanego spaceru",
    description = "Zmień jeden odcinek spaceru na bezpieczny i dostępny wariant.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 70, "Kreatywność" to 30),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "spacer", "nowości"),
  ),

  ReadyTaskTemplate(
    id = "quest_29",
    name = "Zrób drobny życzliwy gest",
    description = "Wybierz małe, niewymuszone działanie, które może kogoś ucieszyć.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 10,
    difficultyLevel = 2,
    attributeWeights = mapOf("Zdrowie" to 50, "Kreatywność" to 50),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "relacje", "życzliwość"),
  ),

  ReadyTaskTemplate(
    id = "quest_30",
    name = "Zaplanuj miniwyprawę bez zakupów",
    description = "Wybierz miejsce, trasę i rzeczy, które już masz pod ręką.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 20,
    difficultyLevel = 2,
    attributeWeights = mapOf("Dyscyplina" to 50, "Kreatywność" to 50),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "przygoda", "bez wydatków"),
  ),

  ReadyTaskTemplate(
    id = "quest_31",
    name = "Poznaj fragment lokalnej historii",
    description = "Przeczytaj krótką informację o wydarzeniu lub miejscu w Twojej okolicy.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Wiedza" to 100),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "historia", "research"),
  ),

  ReadyTaskTemplate(
    id = "quest_32",
    name = "Zorganizuj wieczór z aktywnością offline",
    description = "Wybierz przyjemną aktywność bez mediów społecznościowych.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 30,
    difficultyLevel = 3,
    attributeWeights = mapOf("Zdrowie" to 60, "Kreatywność" to 40),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "offline", "zabawa"),
  ),

  ReadyTaskTemplate(
    id = "quest_33",
    name = "Naucz się składać nową figurkę z papieru",
    description = "Znajdź instrukcję i spróbuj zrobić proste origami.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 20,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "origami", "DIY"),
  ),

  ReadyTaskTemplate(
    id = "quest_34",
    name = "Ułóż quiz z dziesięciu pytań",
    description = "Wymyśl krótki quiz o filmie, grze, książce lub innym temacie.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 25,
    difficultyLevel = 3,
    attributeWeights = mapOf("Kreatywność" to 60, "Wiedza" to 40),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "quiz", "hobby"),
  ),

  ReadyTaskTemplate(
    id = "quest_35",
    name = "Wybierz się na większą wycieczkę",
    description = "Odwiedź nowe miejsce według bezpiecznego, samodzielnie ustalonego planu.",
    area = OnboardingArea.SIDE_QUESTS,
    estimatedMinutes = 90,
    difficultyLevel = 5,
    attributeWeights = mapOf("Zdrowie" to 50, "Kreatywność" to 30, "Dyscyplina" to 20),
    suggestedCategory = TaskCategory.SIDEQUEST,
    tags = listOf("side quest", "wycieczka", "przygoda"),
    subtasks = listOf(
      "Wybierz cel",
      "Sprawdź trasę i warunki",
      "Przygotuj potrzebne rzeczy",
      "Zrealizuj wycieczkę",
    ),
    excludeFromAutoRecommendation = true,
  ),

)

val REWARD_CATALOG_TEMPLATES: List<RewardCatalogTemplate> = listOf(
  // gaming
  RewardCatalogTemplate(
    id = "rew_gaming_01",
    category = OnboardingRewardPref.GAMING,
    icon = "🎮",
    name = "30 minut grania",
    defaultCoinPrice = 25,
    description = "Pół godziny bez wyrzutów sumienia przy ulubionej grze.",
  ),
  RewardCatalogTemplate(
    id = "rew_gaming_02",
    category = OnboardingRewardPref.GAMING,
    icon = "🕹️",
    name = "Nowa gra lub dodatek",
    defaultCoinPrice = 200,
    description = "Większa nagroda za konsekwentne zbieranie monet.",
  ),
  // movies
  RewardCatalogTemplate(
    id = "rew_movies_01",
    category = OnboardingRewardPref.MOVIES,
    icon = "🎬",
    name = "Odcinek serialu",
    defaultCoinPrice = 30,
    description = "Jeden odcinek ulubionego serialu po wykonaniu zadań.",
  ),
  RewardCatalogTemplate(
    id = "rew_movies_02",
    category = OnboardingRewardPref.MOVIES,
    icon = "🍿",
    name = "Wieczór filmowy z przekąskami",
    defaultCoinPrice = 80,
    description = "Pełny seans filmowy z ulubionym napojem i przekąską.",
  ),
  // books
  RewardCatalogTemplate(
    id = "rew_books_01",
    category = OnboardingRewardPref.BOOKS,
    icon = "📖",
    name = "30 minut czytania dla przyjemności",
    defaultCoinPrice = 20,
    description = "Pół godziny z książką czytaną wyłącznie dla relaksu.",
  ),
  RewardCatalogTemplate(
    id = "rew_books_02",
    category = OnboardingRewardPref.BOOKS,
    icon = "📚",
    name = "Nowa książka",
    defaultCoinPrice = 150,
    description = "Zakup wymarzonego tytułu z listy czytelniczej.",
  ),
  // art
  RewardCatalogTemplate(
    id = "rew_art_01",
    category = OnboardingRewardPref.ART,
    icon = "🎨",
    name = "Godzina na projekt kreatywny",
    defaultCoinPrice = 35,
    description = "Nieprzerwany czas na rysowanie, muzykę lub rękodzieło.",
  ),
  RewardCatalogTemplate(
    id = "rew_art_02",
    category = OnboardingRewardPref.ART,
    icon = "🖌️",
    name = "Nowe materiały plastyczne",
    defaultCoinPrice = 140,
    description = "Nowe kredki, szkicownik, farby lub akcesoria twórcze.",
  ),
  // adventures
  RewardCatalogTemplate(
    id = "rew_adventures_01",
    category = OnboardingRewardPref.ADVENTURES,
    icon = "☕",
    name = "Kawa lub deser na mieście",
    defaultCoinPrice = 60,
    description = "Wyjście do ulubionej kawiarni lub cukierni.",
  ),
  RewardCatalogTemplate(
    id = "rew_adventures_02",
    category = OnboardingRewardPref.ADVENTURES,
    icon = "🧭",
    name = "Jednodniowa wycieczka",
    defaultCoinPrice = 250,
    description = "Wyprawa za miasto, w góry, nad wodę lub do nowego miejsca.",
  ),
  // relaxation
  RewardCatalogTemplate(
    id = "rew_relaxation_01",
    category = OnboardingRewardPref.RELAXATION,
    icon = "🛁",
    name = "Relaksująca kąpiel lub drzemka",
    defaultCoinPrice = 30,
    description = "Chwila pełnego odprężenia w ciągu dnia lub wieczorem.",
  ),
  RewardCatalogTemplate(
    id = "rew_relaxation_02",
    category = OnboardingRewardPref.RELAXATION,
    icon = "🌙",
    name = "Wieczór pełnego odpoczynku bez obowiązków",
    defaultCoinPrice = 90,
    description = "Cały wieczór tylko dla siebie bez planowania i zadań.",
  ),
  // shopping
  RewardCatalogTemplate(
    id = "rew_shopping_01",
    category = OnboardingRewardPref.SHOPPING,
    icon = "🛍️",
    name = "Mała przyjemność do 30 zł",
    defaultCoinPrice = 70,
    description = "Drobny zakup, który poprawia humor.",
  ),
  RewardCatalogTemplate(
    id = "rew_shopping_02",
    category = OnboardingRewardPref.SHOPPING,
    icon = "✨",
    name = "Większa zachcianka z listy życzeń",
    defaultCoinPrice = 220,
    description = "Wymarzony zakup odkładany na później.",
  ),
)

/**
 * Pure local deterministic recommendation algorithm for onboarding:
 * - If Q1 (selectedAreas) is empty -> returns empty list (never assumes interests).
 * - Excludes templates marked excludeFromAutoRecommendation (e.g. pet_05) and duplicates.
 * - Strictly applies Q2 time filter when set (shows fewer items instead of mismatched ones).
 * - Prioritizes simpler tasks first inside each selected area.
 * - Round-robins across selected areas for diversity, capped at maxResults (default 8).
 */
fun recommendOnboardingTasks(
  selectedAreas: List<OnboardingArea>,
  timePref: OnboardingTimePref?,
  existingTemplateIds: Set<String> = emptySet(),
  maxResults: Int = 8,
): List<ReadyTaskTemplate> {
  if (selectedAreas.isEmpty()) return emptyList()
  val uniqueAreas = selectedAreas.distinct().take(3)
  val effectiveTime = timePref ?: OnboardingTimePref.ANY

  val queues: List<ArrayDeque<ReadyTaskTemplate>> = uniqueAreas.map { area ->
    val matching = READY_TASK_TEMPLATES
      .filter { tpl ->
        tpl.area == area &&
          !tpl.excludeFromAutoRecommendation &&
          tpl.id !in existingTemplateIds &&
          effectiveTime.matchesMinutes(tpl.estimatedMinutes)
      }
      .sortedWith(
        compareBy<ReadyTaskTemplate> { it.difficultyLevel }
          .thenBy { it.estimatedMinutes }
          .thenBy { it.id }
      )
    ArrayDeque(matching)
  }

  val result = mutableListOf<ReadyTaskTemplate>()
  val seenIds = mutableSetOf<String>()

  while (result.size < maxResults) {
    var addedAnyInRound = false
    for (queue in queues) {
      if (result.size >= maxResults) break
      while (queue.isNotEmpty()) {
        val candidate = queue.removeFirst()
        if (seenIds.add(candidate.id)) {
          result.add(candidate)
          addedAnyInRound = true
          break
        }
      }
    }
    if (!addedAnyInRound) break
  }

  return result
}

fun recommendOnboardingRewards(
  selectedRewardPrefs: List<OnboardingRewardPref>,
  existingRewardTemplateIds: Set<String> = emptySet(),
): List<RewardCatalogTemplate> {
  if (selectedRewardPrefs.isEmpty()) return emptyList()
  val uniquePrefs = selectedRewardPrefs.distinct()
  return uniquePrefs.flatMap { pref ->
    REWARD_CATALOG_TEMPLATES.filter { it.category == pref && it.id !in existingRewardTemplateIds }
  }
}

/**
 * Deterministic largest-remainder EXP distribution across attributes so that the sum
 * of awarded attribute EXP equals totalExp whenever weights are valid and non-empty.
 * Example: totalExp = 10, weights = {Wiedza: 70, Dyscyplina: 30} -> {Wiedza: 7, Dyscyplina: 3}.
 */
fun distributeWeightedExp(
  totalExp: Int,
  weights: Map<String, Int>,
): Map<String, Int> {
  if (totalExp <= 0 || weights.isEmpty()) return emptyMap()
  val positive = weights.filterValues { it > 0 }
  if (positive.isEmpty()) return emptyMap()
  val weightSum = positive.values.sum()
  if (weightSum <= 0) return emptyMap()

  val entries = positive.entries.toList()
  val rawShares = entries.map { (key, w) ->
    val exact = totalExp.toDouble() * w.toDouble() / weightSum.toDouble()
    val floored = kotlin.math.floor(exact).toInt()
    Triple(key, floored, exact - floored)
  }.toMutableList()

  var remainder = totalExp - rawShares.sumOf { it.second }
  val sortedByRemainder = rawShares.indices.sortedWith(
    compareByDescending<Int> { rawShares[it].third }
      .thenByDescending { positive[rawShares[it].first] ?: 0 }
      .thenBy { rawShares[it].first }
  )
  for (idx in sortedByRemainder) {
    if (remainder <= 0) break
    val cur = rawShares[idx]
    rawShares[idx] = cur.copy(second = cur.second + 1)
    remainder--
  }

  return rawShares.associate { it.first to it.second }.filterValues { it > 0 }
}

fun parseAttributeWeightsJson(json: String): Map<String, Int> {
  if (json.isBlank()) return emptyMap()
  return runCatching {
    val obj = JSONObject(json)
    val result = linkedMapOf<String, Int>()
    val keys = obj.keys()
    while (keys.hasNext()) {
      val k = keys.next()
      val v = obj.optInt(k, 0)
      if (v > 0) result[k] = v
    }
    result
  }.getOrDefault(emptyMap())
}

fun toAttributeWeightsJson(weights: Map<String, Int>): String {
  val positive = weights.filterValues { it > 0 }
  if (positive.isEmpty()) return ""
  val obj = JSONObject()
  positive.forEach { (k, v) -> obj.put(k, v) }
  return obj.toString()
}
