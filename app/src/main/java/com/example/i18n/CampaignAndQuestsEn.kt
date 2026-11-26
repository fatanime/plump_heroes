package com.example.i18n

import com.example.model.*

private val CHAPTER_TEXTS_EN = mapOf(
    1 to ("Chapter 1. Amulet of the South" to "Southern lands and secrets of King Ludwig"),
    2 to ("Chapter 2. Amulet of the East" to "Eastern Kingdom and Queen Vlasta"),
    3 to ("Chapter 3. Amulet of the North" to "Northern Realm and Isolda's frozen throne"),
    4 to ("Chapter 4. The Hellish Rift" to "Temples of the gods and the lair of the dark mistress")
)

private val STAGE_TITLES_EN = mapOf(
    101 to "1-1 Morgana's Cottage",
    102 to "1-2 Bandit Camp",
    103 to "1-3 Nameless Forest Thicket",
    104 to "1-4 Southern Kingdom Border",
    105 to "1-5 At Redwall's Gates",
    106 to "1-6 Ludwig's Treasury",
    201 to "2-1 In the Southern Woods",
    202 to "2-2 At the Eastern Border",
    203 to "2-3 Approaches to Roseville",
    204 to "2-4 Within Roseville's Walls",
    205 to "2-5 Griselda's Bakery",
    206 to "2-6 Queen Vlasta's Castle",
    301 to "3-1 Eastern Desert",
    302 to "3-2 Northern Realm Border",
    303 to "3-3 At Tuversk's Walls",
    304 to "3-4 In the Royal Capital",
    305 to "3-5 Road to Gleden",
    306 to "3-6 Isolda's Lair",
    401 to "4-1 Northern Kingdom Woods",
    402 to "4-2 By the Swamp Sea",
    403 to "4-3 Western Halls",
    404 to "4-4 Temple of Light",
    405 to "4-5 Temple of Fire",
    406 to "4-6 Foot of Mount Patreya"
)

fun CampaignChapter.localizedName(): String =
    if (GameLocaleState.isEnglish) CHAPTER_TEXTS_EN[id]?.first ?: name else name

fun CampaignChapter.localizedSubtitle(): String =
    if (GameLocaleState.isEnglish) CHAPTER_TEXTS_EN[id]?.second ?: subtitle else subtitle

fun CampaignStage.localizedTitle(): String =
    if (GameLocaleState.isEnglish) STAGE_TITLES_EN[id] ?: title else title

private val AMULET_NAMES_EN = mapOf(
    "amulet_south" to "Amulet of the South",
    "amulet_east" to "Amulet of the East",
    "amulet_north" to "Amulet of the North",
    "amulet_west" to "Amulet of the West"
)

private val AMULET_NOTES_EN = mapOf(
    "amulet_north" to "One more left!",
    "amulet_west" to "All Amulets collected!"
)

fun localizeAmuletName(id: String, fallback: String): String =
    if (GameLocaleState.isEnglish) AMULET_NAMES_EN[id] ?: fallback else fallback

fun localizeAmuletNote(id: String, fallback: String?): String? =
    if (GameLocaleState.isEnglish) AMULET_NOTES_EN[id] ?: fallback else fallback

private val QUEST_TEXTS_EN: Map<String, Pair<String, String>> = mapOf(
    "q_t1_any_1" to ("First Victory" to "Win any 1 battle in Career or Arena"),
    "q_t1_career_1" to ("Career Path" to "Win 1 stage in Career mode"),
    "q_t1_arena_1" to ("Arena Debut" to "Win 1 battle in the Arena"),
    "q_t1_enemies_4" to ("Huntress" to "Defeat 4 enemy cards in any battles"),
    "q_t1_chest_1" to ("Seeker" to "Open 1 chest in the Chests tab"),
    "q_t1_sell_1" to ("Spring Cleaning" to "Sell 1 spare artifact in the Forge"),
    "q_t1_skill_2" to ("Spark of Magic" to "Use 2 hero skills in winning battles"),
    "q_t2_star_1" to ("Star Trophy" to "Earn 1 more star in Career"),
    "q_t2_any_2" to ("Double Triumph" to "Win any 2 battles (Career or Arena)"),
    "q_t2_flawless_1" to ("Flawless Step" to "Win a battle without losing any heroine"),
    "q_t2_upgrade_1" to ("Training" to "Level up any heroine by +1"),
    "q_t2_steel_1" to ("Power of Steel" to "Win a battle with a Steel element hero"),
    "q_t2_magic_1" to ("Arcane Surge" to "Win a battle with a Magic element hero"),
    "q_t2_fire_1" to ("Arcane Surge" to "Win a battle with a Magic element hero"),
    "q_t2_water_1" to ("Arcane Surge" to "Win a battle with a Magic element hero"),
    "q_t2_hybrid_1" to ("Flexible Tactics" to "Win with a Hybrid heroine in your squad"),
    "q_t3_arena_2" to ("Arena Fighter" to "Win 2 battles in the Arena"),
    "q_t3_career_3" to ("Deep March" to "Win 3 stages in Career mode"),
    "q_t3_ranged_1" to ("Distant Volley" to "Win an Arena battle with Ranged cards only"),
    "q_t3_two_hybrid_1" to ("Hybrid Duo" to "Win a battle with 2 Hybrid fighters"),
    "q_t3_skills_5" to ("Arcane Mastery" to "Use 5 hero skills in winning battles"),
    "q_t3_enemies_10" to ("Crushing Force" to "Defeat 10 enemy cards in battles"),
    "q_t3_forge_1" to ("Forge Alchemy" to "Synthesize 1 new artifact in the Forge"),
    "q_t4_arena_3" to ("Arena Champion" to "Win 3 battles in the Arena"),
    "q_t4_stars_2" to ("Career Radiance" to "Earn 2 new stars in Career"),
    "q_t4_flawless_2" to ("Perfect Formation" to "Win 2 battles with zero casualties"),
    "q_t4_boss_1" to ("Boss Hunt" to "Defeat any chapter boss in Career"),
    "q_t4_any_4" to ("Winning Streak" to "Win any 4 battles today"),
    "q_t4_chests_2" to ("Chest Call" to "Open 2 chests in the Chests tab"),
    "q_t4_light_2" to ("Sacred Blessing" to "Win 2 battles with a Light element hero"),
    "q_t4_four_ranged_1" to ("Ranger Quartet" to "Win a battle with 4 Ranged fighters"),
    "q_t5_arena_4" to ("Arena Overlord" to "Win 4 battles in the Arena today"),
    "q_t5_career_5" to ("Conqueror" to "Win 5 battles in Career mode"),
    "q_t5_ranged_2" to ("Rain of Arrows" to "Win 2 Arena battles with Ranged cards only"),
    "q_t5_two_hybrid_2" to ("Combat Harmony" to "Win 2 battles with 2 Hybrid fighters"),
    "q_t5_enemies_18" to ("Storm of Blades" to "Defeat 18 enemy cards in battles"),
    "q_t5_stars_3" to ("Star Master" to "Earn 3 new stars in Career stages"),
    "q_t5_flawless_3" to ("Immortal Squad" to "Win 3 battles without losing any card")
)

fun QuestTemplate.localizedTitle(): String =
    if (GameLocaleState.isEnglish) QUEST_TEXTS_EN[id]?.first ?: title else title

fun QuestTemplate.localizedDescription(): String =
    if (GameLocaleState.isEnglish) QUEST_TEXTS_EN[id]?.second ?: description else description

fun DailyQuestState.localizedTitle(): String =
    if (GameLocaleState.isEnglish) QUEST_TEXTS_EN[id]?.first ?: title else title

fun DailyQuestState.localizedDescription(): String =
    if (GameLocaleState.isEnglish) QUEST_TEXTS_EN[id]?.second ?: description else description

private val BOT_TITLES_EN = mapOf(
    "Гроза Севера" to "Storm of the North",
    "Чародейка Озер" to "Sorceress of the Lakes",
    "Несокрушимая" to "The Unbreakable",
    "Жрица Рассвета" to "Priestess of Dawn",
    "Повелительница Бурь" to "Mistress of Storms",
    "Стальной Кулак" to "Iron Fist",
    "Драконий Всадник" to "Dragon Rider",
    "Пышная Ведьма" to "Plump Enchantress",
    "Хранительница Огня" to "Keeper of Fire",
    "Лесная Охотница" to "Forest Huntress",
    "Повелительница Арены" to "Mistress of the Arena",
    "Легенда Колизея" to "Colosseum Legend",
    "Непобедимая Валькирия" to "Invincible Valkyrie"
)

fun localizeBotTitle(ruTitle: String): String =
    if (GameLocaleState.isEnglish) BOT_TITLES_EN[ruTitle] ?: ruTitle else ruTitle

fun FortuneWheelSector.localizedLabel(): String =
    if (!GameLocaleState.isEnglish) label else when (type) {
        FortuneWheelSectorType.GOLD_UP_TO_500 -> "up to 500"
        FortuneWheelSectorType.GOLD_UP_TO_1000 -> "up to 1000"
        FortuneWheelSectorType.SHARDS_5 -> "5 pcs"
        FortuneWheelSectorType.SHARDS_10 -> "10 pcs"
        FortuneWheelSectorType.SECRET_SHARDS_3 -> "3 secret"
        FortuneWheelSectorType.GEMS_40 -> "40"
        FortuneWheelSectorType.GEMS_100 -> "100"
        FortuneWheelSectorType.ARTIFACT_BASIC -> "Basic"
        FortuneWheelSectorType.ARTIFACT_SPECIAL -> "Special"
        FortuneWheelSectorType.EMPTY -> "Empty"
    }
