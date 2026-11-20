package com.example.data.model

/**
 * Szablon pojedynczego losowego zadania pobocznego (Random Side Quest).
 *
 * Pola obowiązkowe:
 * - [id] — unikalny identyfikator (np. "rq_007")
 * - [emoji] — pojedyncze emoji wyświetlane na kafelku i w tytule zadania
 * - [title] — humorystyczny tytuł misji
 * - [story] — krótkie miniopowiadanie fantasy wyświetlane na kafelku i w szczegółach zadania
 * - [goal] — konkretny, prosty cel do wykonania w prawdziwym świecie
 * - [estimatedMinutes] — szacowany czas wykonania w minutach
 * - [difficultyLevel] — trudność w skali 1..5 (1=Bardzo łatwe, 2=Łatwe, 3=Średnie, 4=Trudne, 5=Ekstremalne)
 * - [attributeWeights] — rozkład EXP na atrybuty postaci, wartości sumują się do 100 (np. mapOf("Dyscyplina" to 100))
 *
 * Pola opcjonalne (mają wartości domyślne):
 * - [tags] — lista tagów (domyślnie pusta)
 * - [subtasks] — lista podzadań (domyślnie pusta)
 * - [coins] — liczba monet (domyślnie wyliczana z [difficultyLevel]: 1->2, 2->5, 3->10, 4->20, 5->40)
 * - [exp] — liczba punktów EXP (domyślnie wyliczana z [difficultyLevel]: 1->2, 2->5, 3->10, 4->20, 5->40)
 */
data class RandomSideQuestTemplate(
  val id: String,
  val emoji: String,
  val title: String,
  val story: String,
  val goal: String,
  val estimatedMinutes: Int,
  val difficultyLevel: Int,
  val attributeWeights: Map<String, Int>,
  val tags: List<String> = emptyList(),
  val subtasks: List<String> = emptyList(),
  val coins: Int = rewardForDifficultyLevel(difficultyLevel).first,
  val exp: Int = rewardForDifficultyLevel(difficultyLevel).second,
) {
  val taskDifficulty: TaskDifficulty
    get() = when (difficultyLevel.coerceIn(1, 5)) {
      1 -> TaskDifficulty.VERY_EASY
      2 -> TaskDifficulty.EASY
      3 -> TaskDifficulty.MEDIUM
      4 -> TaskDifficulty.HARD
      else -> TaskDifficulty.EXTREME
    }
}

/**
 * Katalog szablonów Random Side Questów.
 *
 * Aby dodać nową misję, skopiuj jeden z wpisów poniżej, zmień `id` na kolejne unikalne
 * (np. "rq_007") oraz uzupełnij pozostałe pola. Mechanizm losowania „Tablicy Przygód”
 * automatycznie uwzględni każdy nowy wpis bez zmian w innych plikach.
 */
val RANDOM_SIDE_QUEST_TEMPLATES: List<RandomSideQuestTemplate> = listOf(
  RandomSideQuestTemplate(
    id = "rq_001",
    emoji = "🧦",
    title = "Klątwa Zaginionej Skarpety",
    story = "Królewski Urząd Tekstyliów zgłosił zaginięcie kolejnego obywatela. Ostatnio widziano go w towarzystwie drugiej skarpety. Podejrzewamy porwanie, ucieczkę albo pralkę. Władze odmawiają komentarza.",
    goal = "Otwórz szufladę ze skarpetami i znajdź przynajmniej 3 pasujące pary.",
    estimatedMinutes = 5,
    difficultyLevel = 1,
    coins = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    tags = listOf("dom", "skarpety", "porządki", "absurd"),
  ),
  RandomSideQuestTemplate(
    id = "rq_002",
    emoji = "🧙",
    title = "Minuta Proroka",
    story = "Prorok twierdzi, że potrafisz wyczuć upływ czasu. Prorok twierdzi również, że jego kapelusz jest żywy i podpowiada mu numery loterii. Dla dobra nauki sprawdzimy tylko pierwszą teorię.",
    goal = "Włącz stoper, odwróć wzrok i spróbuj zatrzymać go jak najbliżej 60 sekund, bez podglądania.",
    estimatedMinutes = 2,
    difficultyLevel = 1,
    coins = 2,
    attributeWeights = mapOf("Dyscyplina" to 50, "Kreatywność" to 50),
    tags = listOf("stoper", "eksperyment", "zabawa"),
  ),
  RandomSideQuestTemplate(
    id = "rq_003",
    emoji = "🐉",
    title = "Smok i Urząd Skarbowy",
    story = "Smok leży na górze złota i spogląda na Ciebie z niesmakiem. Najwyraźniej nie jesteś wystarczająco bogaty, by zasłużyć na pożarcie. To dobra wiadomość. Zła wiadomość jest taka, że smok właśnie otworzył punkt skupu bezużytecznych przedmiotów.",
    goal = "Znajdź jedną niepotrzebną rzecz i odłóż ją do oddania, recyklingu lub wyrzucenia.",
    estimatedMinutes = 5,
    difficultyLevel = 1,
    coins = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    tags = listOf("smok", "dom", "minimalizm", "porządki"),
  ),
  RandomSideQuestTemplate(
    id = "rq_004",
    emoji = "🧪",
    title = "Eliksir Podejrzanej Przejrzystości",
    story = "Alchemik wręcza Ci flakonik przezroczystej cieczy. »To woda« — oznajmia z dumą. Żądasz zwrotu pieniędzy, ale eliksir był darmowy. Alchemik twierdzi, że mimo to właśnie zrobił interes życia.",
    goal = "Przygotuj sobie szklankę świeżej wody. Napij się, jeśli masz ochotę i jest to odpowiednie dla Ciebie.",
    estimatedMinutes = 2,
    difficultyLevel = 1,
    coins = 2,
    attributeWeights = mapOf("Zdrowie" to 100),
    tags = listOf("woda", "alchemia", "codzienność"),
  ),
  RandomSideQuestTemplate(
    id = "rq_005",
    emoji = "📱",
    title = "Artefakt Tysiąca Odcisków",
    story = "Twój magiczny komunikator przetrwał upadki, deszcze i tysiące dotknięć. Pod grubą warstwą odcisków palców podobno znajduje się ekran. Archeolodzy są podekscytowani. Czas dokonać przełomowego odkrycia.",
    goal = "Delikatnie wyczyść ekran swojego telefonu odpowiednią ściereczką, zgodnie z zaleceniami producenta.",
    estimatedMinutes = 3,
    difficultyLevel = 1,
    coins = 2,
    attributeWeights = mapOf("Dyscyplina" to 100),
    tags = listOf("telefon", "czyszczenie", "technologia"),
  ),
  RandomSideQuestTemplate(
    id = "rq_006",
    emoji = "💀",
    title = "Nekromanta Nieprzeczytanych Wiadomości",
    story = "Nekromanta próbuje wskrzesić rozmowę sprzed trzech miesięcy. Nie odniósł sukcesu. Nawet nieumarli mają swoje granice. Mistrz Gry sugeruje, żebyś spróbował czegoś mniej dramatycznego.",
    goal = "Wyślij krótką, miłą wiadomość do kogoś bliskiego albo odpowiedz na jedną zaległą wiadomość, jeśli masz taką możliwość i ochotę.",
    estimatedMinutes = 5,
    difficultyLevel = 1,
    coins = 2,
    attributeWeights = mapOf("Dyscyplina" to 50, "Zdrowie" to 50),
    tags = listOf("relacje", "wiadomości", "kontakt"),
  ),
  // <-- NOWE MISJE DOPISUJ PONIŻEJ TEJ LINII (przed nawiasem zamykającym)
)
