package com.example.model

enum class HeroType(val displayName: String, val description: String) {
    MELEE("Ближний бой", "Атакует первый ряд врага. Обладает броней и повышенным здоровьем."),
    RANGED("Дальний бой", "Атакует врагов на второй линии. Нет брони/щита."),
    HYBRID("Смешанный бой", "Универсальный боец передовой или тыла. Нет брони/щита.")
}

enum class AttackPattern(val title: String) {
    SINGLE("Одиночная атака"),
    PIERCE_LINE("Пронзание линии (бьет обоих по линии)"),
    ALL_ENEMIES("Шторм темной магии (бьет по всем врагам, игнорирует щит)"),
    DIAGONAL_PAIR("Диагональный бросок (бьет двух по диагонали)"),
    POISON_MELEE("Ядовитый укус (ближняя атака + отравление)"),
    FRONT_50_BACK_25("Волна льда (атака по первой и второй линии)"),
    PIERCE_25_75("Пронзание корнями (бьет по линии)")
}

const val MAX_HERO_LEVEL = 12
const val SECRET_HERO_UNLOCK_SHARDS = 50
const val VASILISA_UNLOCK_SHARDS = 75
const val KADRU_UNLOCK_SHARDS = 75
const val KADRU_MAX_LEVEL = 10

fun HeroCard.secretUnlockShardsTarget(): Int =
    if (id == "vasilisa" || id == "kadru") VASILISA_UNLOCK_SHARDS else SECRET_HERO_UNLOCK_SHARDS

fun HeroCard.maxUpgradeLevel(): Int =
    if (id == "kadru") KADRU_MAX_LEVEL else rarity.levelCap

fun HeroCard.atkIncrementPerLevel(): Int =
    if (id == "vasilisa") 1 else 2

enum class CardRarity(val displayName: String, val levelCap: Int) {
    BASIC("Базовая", MAX_HERO_LEVEL),
    SPECIAL("Особая", MAX_HERO_LEVEL),
    RARE("Редкая", MAX_HERO_LEVEL),
    MYTHIC("Мифическая", MAX_HERO_LEVEL);

    companion object {
        fun fromString(value: String): CardRarity {
            return when (value.uppercase()) {
                "BASIC", "COMMON" -> BASIC
                "SPECIAL", "EPIC" -> SPECIAL
                "RARE" -> RARE
                "MYTHIC", "LEGENDARY" -> MYTHIC
                else -> BASIC
            }
        }
    }
}

enum class Element(val displayName: String, val symbol: String) {
    STEEL("Сталь", "🗡️"),
    MAGIC("Магия", "🔮"),
    BLIGHT("Скверна", "☠️"),
    LIGHT("Свет", "☀️");

    companion object {
        fun fromString(value: String): Element? {
            return when (value.trim().uppercase()) {
                "STEEL" -> STEEL
                "MAGIC", "FIRE", "WATER" -> MAGIC
                "BLIGHT", "DARK", "NATURE" -> BLIGHT
                "LIGHT" -> LIGHT
                else -> null
            }
        }
    }
}

val HERO_FILTER_ELEMENTS = listOf(Element.STEEL, Element.MAGIC, Element.BLIGHT, Element.LIGHT)

enum class ArtifactSlot(val displayName: String) {
    WEAPON("Оружие"),
    ARMOR("Доспех"),
    RELIC("Реликвия")
}

data class Artifact(
    val id: String,
    val name: String,
    val rarity: CardRarity,
    val slot: ArtifactSlot,
    val atkBonus: Int,
    val armorBonus: Int,
    val hpBonus: Int,
    val effectDescription: String,
    val iconSymbol: String = "💎",
    val setTag: String? = null
)

fun maxHpBonusForRarity(rarity: CardRarity): Int = when (rarity) {
    CardRarity.MYTHIC -> 50
    CardRarity.RARE -> 35
    CardRarity.SPECIAL -> 20
    CardRarity.BASIC -> 10
}

fun Artifact.withClampedHp(): Artifact {
    val cap = maxHpBonusForRarity(rarity)
    return if (hpBonus > cap) copy(hpBonus = cap) else this
}

data class HeroSkill(
    val id: String,
    val name: String,
    val description: String,
    val energyCost: Int = 2,
    val cooldownRounds: Int = 2,
    val skillType: SkillType = SkillType.OFFENSIVE,
    val unlockLevel: Int = 3
)

enum class SkillType {
    OFFENSIVE,
    DEFENSIVE,
    SUPPORT
}

data class HeroCard(
    val id: String,
    val name: String,
    val title: String,
    val heroType: HeroType,
    val rarity: CardRarity,
    val element: Element,
    val baseAtk: Int,
    val baseArmor: Int, // Ranged and Hybrid have 0 armor; Melee has reduced armor
    val baseHp: Int,
    val level: Int = 1,
    val shards: Int = 0,
    val shardsNeeded: Int = 10,
    val imageRes: Int,
    val activeSkillId: String,
    val availableSkills: List<HeroSkill>,
    val attackPattern: AttackPattern = AttackPattern.SINGLE,
    val equippedArtifactIds: List<String> = emptyList(),
    val lore: String = "",
    val isUnlocked: Boolean = true,
    val unlockPlayerLevel: Int = 1,
    val unlockCareerStageId: Int? = null,
    val isSecret: Boolean = false
) {
    fun calculateTotalAtk(artifacts: List<Artifact>): Int {
        val artifactBonus = artifacts.filter { it.id in equippedArtifactIds }.sumOf { it.atkBonus }
        val levelMultiplier = 1.0 + (level - 1) * 0.12
        return ((baseAtk * levelMultiplier).toInt() + artifactBonus).coerceAtLeast(1)
    }

    fun calculateTotalArmor(artifacts: List<Artifact>): Int {
        val artifactBonus = artifacts.filter { it.id in equippedArtifactIds }.sumOf { it.armorBonus }
        val baseArmorCalculated = if (heroType == HeroType.MELEE) {
            val levelMultiplier = 1.0 + (level - 1) * 0.08
            (baseArmor * levelMultiplier).toInt()
        } else {
            0
        }
        return (baseArmorCalculated + artifactBonus).coerceAtLeast(0)
    }

    fun calculateTotalHp(artifacts: List<Artifact>): Int {
        val artifactBonus = artifacts.filter { it.id in equippedArtifactIds }.sumOf { it.hpBonus }
        val levelMultiplier = 1.0 + (level - 1) * 0.15
        return ((baseHp * levelMultiplier).toInt() + artifactBonus).coerceAtLeast(5)
    }

    // Skills unlock progressively: 1st at lvl 3, 2nd at lvl 5, 3rd at lvl 8
    fun getUnlockedSkills(): List<HeroSkill> {
        return when {
            level >= 8 -> availableSkills.take(3)
            level >= 5 -> availableSkills.take(2)
            level >= 3 -> availableSkills.take(1)
            else -> emptyList()
        }
    }

    val hasMagicUnlocked: Boolean get() = level >= 3

    val activeSkill: HeroSkill?
        get() {
            val unlocked = getUnlockedSkills()
            return unlocked.firstOrNull { it.id == activeSkillId } ?: unlocked.firstOrNull()
        }

    fun resolveImageName(): String {
        return resolveHeroImageNameForLevel(id, level, "hero_$id")
    }

    fun withUpdatedImageForLevel(): HeroCard {
        val targetName = resolveImageName()
        return copy(imageRes = resolveHeroDrawableByName(targetName, imageRes))
    }
}

fun resolveHeroImageNameForLevel(heroId: String, level: Int, fallbackName: String = "hero_$heroId"): String {
    return when (heroId) {
        "berta" -> when (level) {
            in 1..2 -> "hero_berta_0"
            in 3..4 -> "hero_berta"
            in 5..7 -> "hero_berta_1"
            in 8..9 -> "hero_berta_2"
            in 10..11 -> "hero_berta_3"
            else -> "hero_berta_max"
        }
        "olga" -> when (level) {
            in 1..2 -> "hero_olga_0"
            in 3..4 -> "hero_olga"
            in 5..7 -> "hero_olga_1"
            in 8..9 -> "hero_olga_2"
            in 10..11 -> "hero_olga_3"
            else -> "hero_olga_max"
        }
        "brunhilde" -> when (level) {
            in 1..2 -> "hero_brunhilde_0"
            in 3..4 -> "hero_brunhilde"
            in 5..7 -> "hero_brunhilde_1"
            in 8..9 -> "hero_brunhilde_2"
            in 10..11 -> "hero_brunhilde_3"
            else -> "hero_brunhilde_max"
        }
        "witch" -> when (level) {
            in 1..2 -> "hero_witch_0"
            in 3..4 -> "hero_witch"
            in 5..7 -> "hero_witch_1"
            in 8..9 -> "hero_witch_2"
            in 10..11 -> "hero_witch_3"
            else -> "hero_witch_max"
        }
        "iva" -> when (level) {
            in 1..2 -> "hero_iva_0"
            in 3..4 -> "hero_iva"
            in 5..7 -> "hero_iva_1"
            in 8..9 -> "hero_iva_2"
            in 10..11 -> "hero_iva_3"
            else -> "hero_iva_max"
        }
        "alisa" -> when (level) {
            in 1..2 -> "hero_alisa_0"
            in 3..4 -> "hero_alisa"
            in 5..7 -> "hero_alisa_1"
            in 8..9 -> "hero_alisa_2"
            in 10..11 -> "hero_alisa_3"
            else -> "hero_alisa_max"
        }
        "mi_and_na" -> when (level) {
            in 1..2 -> "hero_mi_and_na_0"
            in 3..4 -> "hero_mi_and_na"
            in 5..7 -> "hero_mi_and_na_1"
            in 8..9 -> "hero_mi_and_na_2"
            in 10..11 -> "hero_mi_and_na_3"
            else -> "hero_mi_and_na_max"
        }
        "vlasta" -> when (level) {
            in 1..2 -> "hero_vlasta_0"
            in 3..4 -> "hero_vlasta"
            in 5..7 -> "hero_vlasta_1"
            in 8..9 -> "hero_vlasta_2"
            in 10..11 -> "hero_vlasta_3"
            else -> "hero_vlasta_max"
        }
        "isolda" -> when (level) {
            in 1..2 -> "hero_isolda_0"
            in 3..4 -> "hero_isolda"
            in 5..7 -> "hero_isolda_1"
            in 8..9 -> "hero_isolda_2"
            in 10..11 -> "hero_isolda_3"
            else -> "hero_isolda_max"
        }
        "baker" -> when (level) {
            in 1..2 -> "hero_baker_0"
            in 3..4 -> "hero_baker"
            in 5..7 -> "hero_baker_1"
            in 8..9 -> "hero_baker_2"
            in 10..11 -> "hero_baker_3"
            else -> "hero_baker_max"
        }
        "evanda" -> when (level) {
            in 1..2 -> "hero_evanda_0"
            in 3..4 -> "hero_evanda"
            in 5..7 -> "hero_evanda_1"
            in 8..9 -> "hero_evanda_2"
            in 10..11 -> "hero_evanda_3"
            else -> "hero_evanda_max"
        }
        "demoness" -> when (level) {
            in 1..2 -> "hero_demoness_0"
            in 3..4 -> "hero_demoness"
            in 5..7 -> "hero_demoness_1"
            in 8..9 -> "hero_demoness_2"
            in 10..11 -> "hero_demoness_3"
            else -> "hero_demoness_max"
        }
        "elios" -> when (level) {
            in 1..2 -> "hero_elios_0"
            in 3..4 -> "hero_elios"
            in 5..7 -> "hero_elios_1"
            in 8..9 -> "hero_elios_2"
            in 10..11 -> "hero_elios_3"
            else -> "hero_elios_max"
        }
        "vasilisa" -> when (level) {
            in 1..2 -> "hero_vasilisa_0"
            in 3..4 -> "hero_vasilisa"
            in 5..7 -> "hero_vasilisa_1"
            in 8..9 -> "hero_vasilisa_2"
            in 10..11 -> "hero_vasilisa_3"
            else -> "hero_vasilisa_max"
        }
        "kadru" -> when (level) {
            in 1..2 -> "hero_kadru_0"
            in 3..4 -> "hero_kadru"
            in 5..7 -> "hero_kadru_1"
            in 8..9 -> "hero_kadru_2"
            else -> "hero_kadru_3"
        }
        "leya" -> when (level) {
            in 1..4 -> "hero_leya_0"
            in 5..7 -> "hero_leya_1"
            in 8..9 -> "hero_leya_2"
            in 10..11 -> "hero_leya_3"
            else -> "hero_leya_max"
        }
        else -> fallbackName
    }
}

fun resolveHeroDrawableByName(imageName: String, fallbackRes: Int): Int {
    return when (imageName) {
        "hero_berta_0" -> com.example.R.drawable.hero_berta_0
        "hero_berta" -> com.example.R.drawable.hero_berta
        "hero_berta_1" -> com.example.R.drawable.hero_berta_1
        "hero_berta_2" -> com.example.R.drawable.hero_berta_2
        "hero_berta_3" -> com.example.R.drawable.hero_berta_3
        "hero_berta_max" -> com.example.R.drawable.hero_berta_max
        "hero_olga_0" -> com.example.R.drawable.hero_olga_0
        "hero_olga" -> com.example.R.drawable.hero_olga
        "hero_olga_1" -> com.example.R.drawable.hero_olga_1
        "hero_olga_2" -> com.example.R.drawable.hero_olga_2
        "hero_olga_3" -> com.example.R.drawable.hero_olga_3
        "hero_olga_max" -> com.example.R.drawable.hero_olga_max
        "hero_brunhilde_0" -> com.example.R.drawable.hero_brunhilde_0
        "hero_brunhilde" -> com.example.R.drawable.hero_brunhilde
        "hero_brunhilde_1" -> com.example.R.drawable.hero_brunhilde_1
        "hero_brunhilde_2" -> com.example.R.drawable.hero_brunhilde_2
        "hero_brunhilde_3" -> com.example.R.drawable.hero_brunhilde_3
        "hero_brunhilde_max" -> com.example.R.drawable.hero_brunhilde_max
        "hero_witch_0" -> com.example.R.drawable.hero_witch_0
        "hero_witch" -> com.example.R.drawable.hero_witch
        "hero_witch_1" -> com.example.R.drawable.hero_witch_1
        "hero_witch_2" -> com.example.R.drawable.hero_witch_2
        "hero_witch_3" -> com.example.R.drawable.hero_witch_3
        "hero_witch_max" -> com.example.R.drawable.hero_witch_max
        "hero_iva_0" -> com.example.R.drawable.hero_iva_0
        "hero_iva" -> com.example.R.drawable.hero_iva
        "hero_iva_1" -> com.example.R.drawable.hero_iva_1
        "hero_iva_2" -> com.example.R.drawable.hero_iva_2
        "hero_iva_3" -> com.example.R.drawable.hero_iva_3
        "hero_iva_max" -> com.example.R.drawable.hero_iva_max
        "hero_alisa_0" -> com.example.R.drawable.hero_alisa_0
        "hero_alisa" -> com.example.R.drawable.hero_alisa
        "hero_alisa_1" -> com.example.R.drawable.hero_alisa_1
        "hero_alisa_2" -> com.example.R.drawable.hero_alisa_2
        "hero_alisa_3" -> com.example.R.drawable.hero_alisa_3
        "hero_alisa_max" -> com.example.R.drawable.hero_alisa_max
        "hero_mi_and_na_0" -> com.example.R.drawable.hero_mi_and_na_0
        "hero_mi_and_na" -> com.example.R.drawable.hero_mi_and_na
        "hero_mi_and_na_1" -> com.example.R.drawable.hero_mi_and_na_1
        "hero_mi_and_na_2" -> com.example.R.drawable.hero_mi_and_na_2
        "hero_mi_and_na_3" -> com.example.R.drawable.hero_mi_and_na_3
        "hero_mi_and_na_max" -> com.example.R.drawable.hero_mi_and_na_max
        "hero_vlasta_0" -> com.example.R.drawable.hero_vlasta_0
        "hero_vlasta" -> com.example.R.drawable.hero_vlasta
        "hero_vlasta_1" -> com.example.R.drawable.hero_vlasta_1
        "hero_vlasta_2" -> com.example.R.drawable.hero_vlasta_2
        "hero_vlasta_3" -> com.example.R.drawable.hero_vlasta_3
        "hero_vlasta_max" -> com.example.R.drawable.hero_vlasta_max
        "hero_isolda_0" -> com.example.R.drawable.hero_isolda_0
        "hero_isolda" -> com.example.R.drawable.hero_isolda
        "hero_isolda_1" -> com.example.R.drawable.hero_isolda_1
        "hero_isolda_2" -> com.example.R.drawable.hero_isolda_2
        "hero_isolda_3" -> com.example.R.drawable.hero_isolda_3
        "hero_isolda_max" -> com.example.R.drawable.hero_isolda_max
        "hero_baker_0" -> com.example.R.drawable.hero_baker_0
        "hero_baker" -> com.example.R.drawable.hero_baker
        "hero_baker_1" -> com.example.R.drawable.hero_baker_1
        "hero_baker_2" -> com.example.R.drawable.hero_baker_2
        "hero_baker_3" -> com.example.R.drawable.hero_baker_3
        "hero_baker_max" -> com.example.R.drawable.hero_baker_max
        "hero_evanda_0" -> com.example.R.drawable.hero_evanda_0
        "hero_evanda" -> com.example.R.drawable.hero_evanda
        "hero_evanda_1" -> com.example.R.drawable.hero_evanda_1
        "hero_evanda_2" -> com.example.R.drawable.hero_evanda_2
        "hero_evanda_3" -> com.example.R.drawable.hero_evanda_3
        "hero_evanda_max" -> com.example.R.drawable.hero_evanda_max
        "hero_demoness_0" -> com.example.R.drawable.hero_demoness_0
        "hero_demoness" -> com.example.R.drawable.hero_demoness
        "hero_demoness_1" -> com.example.R.drawable.hero_demoness_1
        "hero_demoness_2" -> com.example.R.drawable.hero_demoness_2
        "hero_demoness_3" -> com.example.R.drawable.hero_demoness_3
        "hero_demoness_max" -> com.example.R.drawable.hero_demoness_max
        "hero_elios_0" -> com.example.R.drawable.hero_elios_0
        "hero_elios" -> com.example.R.drawable.hero_elios
        "hero_elios_1" -> com.example.R.drawable.hero_elios_1
        "hero_elios_2" -> com.example.R.drawable.hero_elios_2
        "hero_elios_3" -> com.example.R.drawable.hero_elios_3
        "hero_elios_max" -> com.example.R.drawable.hero_elios_max
        "hero_vasilisa_0" -> com.example.R.drawable.hero_vasilisa_0
        "hero_vasilisa" -> com.example.R.drawable.hero_vasilisa
        "hero_vasilisa_1" -> com.example.R.drawable.hero_vasilisa_1
        "hero_vasilisa_2" -> com.example.R.drawable.hero_vasilisa_2
        "hero_vasilisa_3" -> com.example.R.drawable.hero_vasilisa_3
        "hero_vasilisa_max" -> com.example.R.drawable.hero_vasilisa_max
        "hero_kadru_0" -> com.example.R.drawable.hero_kadru_0
        "hero_kadru" -> com.example.R.drawable.hero_kadru
        "hero_kadru_1" -> com.example.R.drawable.hero_kadru_1
        "hero_kadru_2" -> com.example.R.drawable.hero_kadru_2
        "hero_kadru_3" -> com.example.R.drawable.hero_kadru_3
        "hero_leya_0" -> com.example.R.drawable.hero_leya_0
        "hero_leya_1" -> com.example.R.drawable.hero_leya_1
        "hero_leya_2" -> com.example.R.drawable.hero_leya_2
        "hero_leya_3" -> com.example.R.drawable.hero_leya_3
        "hero_leya_max" -> com.example.R.drawable.hero_leya_max
        else -> fallbackRes
    }
}

fun inferHeroIdFromDrawableName(drawableName: String): String {
    val clean = drawableName.trim().lowercase()
        .substringAfterLast('/')
        .substringBefore('.')
        .removePrefix("hero_")
        .removeSuffix("_max")
        .replace(Regex("_(0|1|2|3)$"), "")
    return when {
        clean.startsWith("mi_and_na") -> "mi_and_na"
        clean in setOf("berta", "olga", "brunhilde", "witch", "iva", "alisa", "vlasta", "evanda", "isolda", "demoness", "baker", "elios", "vasilisa", "kadru", "leya") -> clean
        else -> ""
    }
}

fun resolveHeroFallbackDrawableRes(heroId: String, heroName: String = ""): Int {
    val normalizedId = heroId.trim().lowercase()
    val knownIds = setOf(
        "berta", "olga", "brunhilde", "witch", "iva", "alisa",
        "mi_and_na", "vlasta", "evanda", "isolda", "demoness", "baker", "elios", "vasilisa", "kadru", "leya"
    )
    val effectiveId = if (normalizedId in knownIds) {
        normalizedId
    } else {
        when (heroName.trim().lowercase()) {
            "берта", "berta" -> "berta"
            "ольга", "olga" -> "olga"
            "брунгильда", "brunhilde" -> "brunhilde"
            "моргана", "morgana" -> "witch"
            "ива", "iva" -> "iva"
            "алиса", "alice", "alisa" -> "alisa"
            "ми и на", "mi & na", "mi and na" -> "mi_and_na"
            "власта", "vlasta" -> "vlasta"
            "эванда", "evanda" -> "evanda"
            "изольда", "isolda" -> "isolda"
            "лилит", "lilith" -> "demoness"
            "гризельда", "griselda" -> "baker"
            "эллиос", "ellios", "elios" -> "elios"
            "василиса", "vasilisa" -> "vasilisa"
            "кадру", "kadru" -> "kadru"
            "лея", "leya" -> "leya"
            else -> ""
        }
    }
    return when (effectiveId) {
        "berta" -> com.example.R.drawable.hero_berta
        "olga" -> com.example.R.drawable.hero_olga
        "brunhilde" -> com.example.R.drawable.hero_brunhilde
        "witch" -> com.example.R.drawable.hero_witch
        "iva" -> com.example.R.drawable.hero_iva
        "alisa" -> com.example.R.drawable.hero_alisa
        "mi_and_na" -> com.example.R.drawable.hero_mi_and_na
        "vlasta" -> com.example.R.drawable.hero_vlasta
        "evanda" -> com.example.R.drawable.hero_evanda
        "isolda" -> com.example.R.drawable.hero_isolda
        "demoness" -> com.example.R.drawable.hero_demoness
        "baker" -> com.example.R.drawable.hero_baker
        "elios" -> com.example.R.drawable.hero_elios
        "vasilisa" -> com.example.R.drawable.hero_vasilisa
        "kadru" -> com.example.R.drawable.hero_kadru
        "leya" -> com.example.R.drawable.hero_leya_0
        else -> com.example.R.drawable.app_logo
    }
}

fun resolveHeroImageForLevel(heroId: String, level: Int, defaultImageRes: Int): Int {
    val targetName = resolveHeroImageNameForLevel(heroId, level, "hero_$heroId")
    return resolveHeroDrawableByName(targetName, defaultImageRes)
}

fun resolveHeroFaceDrawableRes(heroId: String, fallbackRes: Int, heroLevel: Int = 1): Int {
    return when (heroId.trim().lowercase()) {
        "brunhilde" -> com.example.R.drawable.hero_brunhilde_face
        "olga" -> com.example.R.drawable.hero_olga_face
        "berta" -> com.example.R.drawable.hero_berta_face
        "witch" -> com.example.R.drawable.hero_witch_face
        "alisa" -> if (heroLevel >= 8) com.example.R.drawable.hero_alice_face_2 else com.example.R.drawable.hero_alice_face
        "isolda" -> when {
            heroLevel >= 8 -> com.example.R.drawable.hero_isolda_face_2
            heroLevel >= 5 -> com.example.R.drawable.hero_isolda_face_1
            else -> com.example.R.drawable.hero_isolda_face
        }
        "evanda" -> if (heroLevel >= 8) com.example.R.drawable.hero_evanda_face_2 else com.example.R.drawable.hero_evanda_face
        "vlasta" -> if (heroLevel >= 8) com.example.R.drawable.hero_vlasta_face_2 else com.example.R.drawable.hero_vlasta_face
        "iva" -> com.example.R.drawable.hero_iva_face
        "demoness" -> if (heroLevel >= 8) com.example.R.drawable.hero_demoness_face_2 else com.example.R.drawable.hero_demoness_face
        "baker" -> if (heroLevel >= 5) com.example.R.drawable.hero_baker_face_2 else com.example.R.drawable.hero_baker_face
        "mi_and_na" -> if (heroLevel >= 8) com.example.R.drawable.hero_mi_and_na_face_2 else com.example.R.drawable.hero_mi_and_na_face
        "elios" -> if (heroLevel >= 8) com.example.R.drawable.hero_elios_face_1 else com.example.R.drawable.hero_elios_face
        "vasilisa" -> if (heroLevel >= 8) com.example.R.drawable.hero_vasilisa_face_1 else com.example.R.drawable.hero_vasilisa_face
        "kadru" -> if (heroLevel >= 8) com.example.R.drawable.hero_kadru_face_1 else com.example.R.drawable.hero_kadru_face
        "leya" -> com.example.R.drawable.hero_leya_face
        else -> fallbackRes
    }
}

const val ENEMY_PORTRAIT_TIER_0 = 0
const val ENEMY_PORTRAIT_TIER_STANDARD = 1
const val ENEMY_PORTRAIT_TIER_1 = 2
const val ENEMY_PORTRAIT_TIER_2 = 3
const val ENEMY_PORTRAIT_TIER_3 = 4

fun resolveEnemyPortraitTierForCareerStage(chapterId: Int, stageNumber: Int): Int {
    val stageIndex = ((chapterId - 1) * 6 + stageNumber).coerceAtLeast(1)
    return when {
        stageIndex <= 5 -> ENEMY_PORTRAIT_TIER_0        // 1-1 .. 1-5
        stageIndex <= 11 -> ENEMY_PORTRAIT_TIER_STANDARD // 1-6 .. 2-5
        stageIndex <= 17 -> ENEMY_PORTRAIT_TIER_1        // 2-6 .. 3-5
        stageIndex <= 23 -> ENEMY_PORTRAIT_TIER_2        // 3-6 .. 4-5
        else -> ENEMY_PORTRAIT_TIER_3                    // 4-6 (never _max)
    }
}

fun resolveHeroImageForEnemyTier(heroId: String, tier: Int, defaultImageRes: Int): Int {
    val visualLevel = when (tier.coerceIn(ENEMY_PORTRAIT_TIER_0, ENEMY_PORTRAIT_TIER_3)) {
        ENEMY_PORTRAIT_TIER_0 -> 1
        ENEMY_PORTRAIT_TIER_STANDARD -> 3
        ENEMY_PORTRAIT_TIER_1 -> 5
        ENEMY_PORTRAIT_TIER_2 -> 8
        else -> 10 // Cap at _3, never _max for enemies
    }
    return resolveHeroImageForLevel(heroId, visualLevel, defaultImageRes)
}

fun calculateCareerStageEnemyHps(stage: CampaignStage, baseTemplates: List<HeroCard>): List<Int> {
    val stageIndex = ((stage.chapterId - 1) * 6 + stage.stageNumber).coerceAtLeast(1)
    val stageProgressionMult = 1.0 + (stageIndex - 1) * 0.04
    return stage.enemyHeroIds.mapIndexed { slotIndex, heroId ->
        val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
        val enemyLevel = stage.enemyLevels.getOrElse(slotIndex) { 1 }
        val levelMult = (1.0 + (enemyLevel - 1) * 0.16) * (if (stage.isBoss) 1.25 else 1.0) * 1.25 * stageProgressionMult
        (baseHero.baseHp * levelMult).toInt()
    }
}

fun resolveArenaPortraitTierByChapterHp(
    arenaTotalHp: Int,
    chapters: List<CampaignChapter>,
    baseTemplates: List<HeroCard>
): Int {
    val stagesById = chapters.flatMap { it.stages }.associateBy { it.id }
    fun stageHp(id: Int): Int? = stagesById[id]?.let { calculateCareerStageEnemyHps(it, baseTemplates).sum() }

    val tier0Max = stageHp(105) ?: 223
    val stdMin = listOfNotNull(stageHp(106), stageHp(201)).minOrNull() ?: 255
    val stdThreshold = (tier0Max + stdMin) / 2 // ~239

    val stdMax = stageHp(205) ?: 331
    val t1Min = listOfNotNull(stageHp(206), stageHp(301)).minOrNull() ?: 380
    val t1Threshold = (stdMax + t1Min) / 2 // ~355

    val t1Max = stageHp(305) ?: 464
    val t2Threshold = t1Max + 1 // ~465

    val t2Max = stageHp(405) ?: 510
    val t3Hp = stageHp(406) ?: 677
    val t3Threshold = (t2Max + t3Hp) / 2 // ~593

    return when {
        arenaTotalHp < stdThreshold -> ENEMY_PORTRAIT_TIER_0
        arenaTotalHp < t1Threshold -> ENEMY_PORTRAIT_TIER_STANDARD
        arenaTotalHp < t2Threshold -> ENEMY_PORTRAIT_TIER_1
        arenaTotalHp < t3Threshold -> ENEMY_PORTRAIT_TIER_2
        else -> ENEMY_PORTRAIT_TIER_3
    }
}

fun getUnlockedHeroGalleryItems(heroId: String, currentLevel: Int, currentImageRes: Int): List<Pair<String, Int>> {
    val tierMinLevels = if (heroId == "kadru") listOf(1, 3, 5, 8, 10) else listOf(1, 3, 5, 8, 10, 12)
    val evolvingIds = setOf("berta", "olga", "brunhilde", "witch", "iva", "alisa", "mi_and_na", "vlasta", "evanda", "isolda", "demoness", "baker", "elios", "vasilisa", "kadru", "leya")
    if (heroId !in evolvingIds) {
        return listOf("hero_$heroId" to currentImageRes)
    }
    val maxCap = if (heroId == "kadru") KADRU_MAX_LEVEL else MAX_HERO_LEVEL
    val seen = LinkedHashMap<String, Int>()
    tierMinLevels
        .filter { it <= currentLevel.coerceIn(1, maxCap) }
        .forEach { tierLevel ->
            val key = resolveHeroImageNameForLevel(heroId, tierLevel, "hero_$heroId")
            val resId = resolveHeroDrawableByName(key, currentImageRes)
            if (key !in seen) {
                seen[key] = resId
            }
        }
    if (seen.isEmpty()) {
        val fallbackKey = resolveHeroImageNameForLevel(heroId, currentLevel, "hero_$heroId")
        seen[fallbackKey] = currentImageRes
    }
    return seen.entries.map { it.key to it.value }
}

fun getUnlockedHeroGalleryImages(heroId: String, currentLevel: Int, currentImageRes: Int): List<Int> {
    return getUnlockedHeroGalleryItems(heroId, currentLevel, currentImageRes).map { it.second }.distinct()
}

/**
 * Artifact level restrictions:
 * - BASIC: hero level 1+
 * - SPECIAL: hero level 3+
 * - RARE: hero level 5+
 * - MYTHIC: hero level 7+
 */
fun canHeroEquipArtifact(heroLevel: Int, rarity: CardRarity): Boolean {
    return when (rarity) {
        CardRarity.BASIC -> true
        CardRarity.SPECIAL -> heroLevel >= 3
        CardRarity.RARE -> heroLevel >= 5
        CardRarity.MYTHIC -> heroLevel >= 7
    }
}

fun getArtifactRequiredHeroLevel(rarity: CardRarity): Int {
    return when (rarity) {
        CardRarity.BASIC -> 1
        CardRarity.SPECIAL -> 3
        CardRarity.RARE -> 5
        CardRarity.MYTHIC -> 7
    }
}
