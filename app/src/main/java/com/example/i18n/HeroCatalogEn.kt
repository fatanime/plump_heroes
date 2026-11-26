package com.example.i18n

import com.example.model.*

fun HeroType.localizedName(): String = if (!GameLocaleState.isEnglish) displayName else when (this) {
    HeroType.MELEE -> "Melee"
    HeroType.RANGED -> "Ranged"
    HeroType.HYBRID -> "Hybrid"
}

fun HeroType.localizedDescription(): String = if (!GameLocaleState.isEnglish) description else when (this) {
    HeroType.MELEE -> "Attacks the enemy front row. Has armor and increased health."
    HeroType.RANGED -> "Attacks enemies in the back line. Has no armor/shield."
    HeroType.HYBRID -> "Versatile fighter for front or back row. Has no armor/shield."
}

fun AttackPattern.localizedTitle(): String = if (!GameLocaleState.isEnglish) title else when (this) {
    AttackPattern.SINGLE -> "Single target attack"
    AttackPattern.PIERCE_LINE -> "Line pierce (hits both in column)"
    AttackPattern.ALL_ENEMIES -> "Dark magic storm (hits all enemies)"
    AttackPattern.DIAGONAL_PAIR -> "Diagonal throw (hits two diagonally)"
    AttackPattern.POISON_MELEE -> "Venomous strike (melee + poison)"
    AttackPattern.FRONT_50_BACK_25 -> "Ice wave (hits front and back row)"
}

fun CardRarity.localizedName(): String = if (!GameLocaleState.isEnglish) displayName else when (this) {
    CardRarity.BASIC -> "Basic"
    CardRarity.SPECIAL -> "Special"
    CardRarity.RARE -> "Rare"
    CardRarity.MYTHIC -> "Mythic"
}

fun Element.localizedName(): String = if (!GameLocaleState.isEnglish) displayName else when (this) {
    Element.STEEL -> "Steel"
    Element.MAGIC -> "Magic"
    Element.BLIGHT -> "Blight"
    Element.LIGHT -> "Light"
}

fun ArtifactSlot.localizedName(): String = if (!GameLocaleState.isEnglish) displayName else when (this) {
    ArtifactSlot.WEAPON -> "Weapon"
    ArtifactSlot.ARMOR -> "Armor"
    ArtifactSlot.RELIC -> "Relic"
}

private val HERO_NAMES_EN = mapOf(
    "brunhilde" to "Brunhilde",
    "olga" to "Olga",
    "berta" to "Berta",
    "witch" to "Morgana",
    "alisa" to "Alice",
    "iva" to "Iva",
    "mi_and_na" to "Mi & Na",
    "vlasta" to "Vlasta",
    "evanda" to "Evanda",
    "isolda" to "Isolda",
    "demoness" to "Lilith",
    "baker" to "Griselda",
    "elios" to "Ellios",
    "vasilisa" to "Vasilisa",
    "kadru" to "Kadru"
)

private val HERO_NAME_RU_TO_EN = mapOf(
    "Брунгильда" to "Brunhilde",
    "Ольга" to "Olga",
    "Берта" to "Berta",
    "Моргана" to "Morgana",
    "Алиса" to "Alice",
    "Ива" to "Iva",
    "Ми и На" to "Mi & Na",
    "Власта" to "Vlasta",
    "Эванда" to "Evanda",
    "Изольда" to "Isolda",
    "Лилит" to "Lilith",
    "Гризельда" to "Griselda",
    "Эллиос" to "Ellios",
    "Василиса" to "Vasilisa",
    "Кадру" to "Kadru",
    "секретный персонаж" to "secret character",
    "Секретный персонаж" to "Secret character",
    "Героиня" to "Heroine",
    "Хранительница" to "Guardian"
)

private val HERO_TITLES_EN = mapOf(
    "brunhilde" to "Huntress of the West",
    "olga" to "Knight of the North",
    "berta" to "Mischievous Pyromancer",
    "witch" to "Swamp Witch",
    "alisa" to "Warrior of the South",
    "iva" to "Forest Healer",
    "mi_and_na" to "Deadly Jesters",
    "vlasta" to "Queen of the East",
    "evanda" to "Nun of Light",
    "isolda" to "Queen of Frost",
    "demoness" to "Alluring Demoness",
    "baker" to "Wicked Pastry Chef",
    "elios" to "Albino Mage",
    "vasilisa" to "Desert Sniper",
    "kadru" to "Tartarus Reptile"
)

private val HERO_LORE_EN = mapOf(
    "brunhilde" to "A forest huntress ready to take on any beast.",
    "olga" to "A brave commander of Tsar Vladimir II's army.",
    "berta" to "Attacks enemies in the back line. A cheerful fire mistress who fled the Temple of Fire chasing wild adventures.",
    "witch" to "Attacks enemies in the back line. A witch from the Swamp Sea guarding her domain from strangers.",
    "alisa" to "A Valkyrie serving King Ludwig, ruler of the southern lands.",
    "iva" to "Attacks enemies in the back line. A herbalist able to mend any wounds of her allies and help them endure formidable foes.",
    "mi_and_na" to "Two rogue twin sisters striking fear into merchant caravans.",
    "vlasta" to "A wicked sorceress queen who adores executions and crimson roses above all else.",
    "evanda" to "A servant of the Temple of Light protecting the realms from fiends of the Hellish Rift.",
    "isolda" to "Attacks enemies in the back line. A frosty beauty from the North commanding snows and gales.",
    "demoness" to "A demoness from the netherworld seeking amusement among mortals.",
    "baker" to "A former royal cook banished for her foul temper, now dreaming of sweet revenge.",
    "elios" to "An illegitimate daughter of Isolda who fled from her mother's oppression.",
    "vasilisa" to "Vasilisa is a mysterious sniper of the Eastern desert who carries out Vlasta's personal contracts.",
    "kadru" to "One of Lilith's minions. Her attack is venomous, and healing will be of no use."
)

fun localizeHeroNameByIdOrRu(heroId: String?, ruName: String): String {
    if (!GameLocaleState.isEnglish) return ruName
    heroId?.let { id -> HERO_NAMES_EN[id]?.let { return it } }
    return HERO_NAME_RU_TO_EN[ruName] ?: ruName
}

fun HeroCard.localizedRarityLabel(): String =
    if (isSecret) tr("Секретная", "Secret") else rarity.localizedName()

fun HeroCard.localizedName(): String =
    if (GameLocaleState.isEnglish) HERO_NAMES_EN[id] ?: name else name

fun HeroCard.localizedTitle(): String =
    if (GameLocaleState.isEnglish) HERO_TITLES_EN[id] ?: title else title

fun HeroCard.localizedLore(): String =
    if (GameLocaleState.isEnglish) HERO_LORE_EN[id] ?: lore else lore

fun BattleCard.localizedName(): String {
    if (!GameLocaleState.isEnglish) return name
    HERO_NAMES_EN[heroId]?.let { return it }
    return HERO_NAME_RU_TO_EN[name] ?: name
}

private val SKILL_TEXTS_EN: Map<String, Pair<String, String>> = mapOf(
    "brun_skill_1" to ("Blade of Justice" to "Deals 100% damage to the enemy ahead and 25% to the adjacent enemy in the row (if none, +25% to the same target)."),
    "olga_skill_1" to ("Tsar's Shield" to "Adds 10% of max HP as armor and slows the enemy ahead."),
    "berta_skill_1" to ("Meteor Shower" to "Deals 50% of her attack damage to each enemy in the back line (or 100% if only one remains)."),
    "witch_skill_1" to ("Soul Drain" to "Deals 30% damage to both enemies in the back line and weakens their attack by 25% for 1 turn."),
    "alisa_skill_1" to ("Bastion of Defense" to "Grants every ally, including herself, a shield equal to 5% of their max HP, even if armor is zero."),
    "alisa_skill_2" to ("Hammer of Retribution" to "Deals 150% damage to one enemy."),
    "iva_skill_1" to ("Vital Sprout" to "Heals the most wounded ally for 25% of their max HP."),
    "iva_skill_2" to ("Forest Balm" to "Heals two allies for 15% of their max HP."),
    "mina_skill_1" to ("Blinding Flash" to "Blinds one enemy for 1 round (blinded enemy skips their next turn)."),
    "mina_skill_2" to ("Surprise Bomb" to "Deals 60% damage to all enemies and burns back-row cards for 20% HP over 2 rounds."),
    "vlasta_skill_1" to ("Rose Thorns" to "Poisons a target for 2 rounds, draining 20% of their max HP each turn."),
    "vlasta_skill_2" to ("Off With Their Heads!" to "Deals 120% damage and poisons the target for 2 rounds for 10% max HP per turn."),
    "vlasta_skill_3" to ("Royal Wrath" to "Deals 200% attack damage to a target and heals herself for 50% of the damage dealt."),
    "evanda_skill_1" to ("Bulwark of Light" to "Grants the ally standing behind her a shield equal to 20% of Evanda's HP."),
    "evanda_skill_2" to ("Blessing" to "Grants shields equal to 15% of max HP to two allies in the squad."),
    "evanda_skill_3" to ("Great Stand" to "Grants shields equal to 10% of max HP to the entire squad."),
    "isolda_skill_1" to ("Deep Freeze" to "Freezes any chosen enemy hero for 1 round."),
    "isolda_skill_2" to ("Icy Shower" to "Deals 30% damage to all enemies and slows them by 25% for 1 round."),
    "dem_skill_1" to ("Twin Fist" to "Deals 125% damage to both enemies in the front line."),
    "dem_skill_2" to ("Love Hex" to "Charms an enemy for 1 round (skips turn 💋) and forces them to strike an adjacent ally for 100% of their attack."),
    "dem_skill_3" to ("Abyssal Chaos" to "Deals 100% damage to both front-line enemies and 50% to both back-line enemies."),
    "baker_skill_1" to ("Blazing Rolling Pin" to "Deals 120% damage to any front-line target and burns them for 10% max HP each round for 2 rounds."),
    "baker_skill_2" to ("Explosive Pie" to "Deals 50% damage to each front-line enemy and stuns one of them for 1 round."),
    "baker_skill_3" to ("Infernal Oven" to "Deals 150% damage to the entire front line and burns them for 10% max HP per turn for 2 rounds."),
    "elios_skill_1" to ("Chilling" to "Freezes 2 enemies for 1 turn and deals 50% damage to them."),
    "elios_skill_2" to ("Black Ice" to "Weakens any 2 enemies by 50%."),
    "vasilisa_skill_1" to ("Graze Shot" to "Additionally removes 25% of the dealt damage from any enemy."),
    "vasilisa_skill_2" to ("Light Hand" to "Has a 35% chance to deal critical damage, otherwise deals normal damage."),
    "vasilisa_skill_3" to ("Zigzag" to "Additionally deals 100% damage to a second enemy diagonally."),
    "kadru_skill_1" to ("Fury" to "Increases attack power by 30% for the next turn."),
    "kadru_skill_2" to ("Venomous Pool" to "Strikes all enemies with poison. Poison deals 50% of the card's damage on the next turn.")
)

fun HeroSkill.localizedName(): String =
    if (GameLocaleState.isEnglish) SKILL_TEXTS_EN[id]?.first ?: name else name

fun HeroSkill.localizedDescription(): String =
    if (GameLocaleState.isEnglish) SKILL_TEXTS_EN[id]?.second ?: description else description

private val ARTIFACT_TEXTS_EN: Map<String, Pair<String, String>> = mapOf(
    "art_base_1" to ("Brunhilde's Hunting Knife" to "A trusty blade of the forest huntress."),
    "art_base_2" to ("Olga's Forged Shield" to "Sturdy shield with northern engravings."),
    "art_base_3" to ("Berta's Gauntlets" to "Fireproof combat gloves."),
    "art_base_4" to ("Morgana's Potion Vial" to "Caustic brew of the swamp witch."),
    "art_base_5" to ("Alice's Warrior Sash" to "Battle ribbon of the southern Valkyrie."),
    "art_base_6" to ("Iva's Forest Charm" to "Living amulet of the forest healer."),
    "art_trade_base_1" to ("Olga's Heavy Plate" to "Cumbersome protective armor."),
    "art_trade_base_2" to ("Brunhilde's Berserker Dagger" to "Razor edge at the cost of defense."),
    "art_trade_base_3" to ("Berta's Fiery Scroll" to "Scorching manuscript of the pyromancer."),
    "art_trade_base_4" to ("Iva's Tortoise Shell" to "Protective natural cocoon."),
    "art_trade_base_5" to ("Morgana's Toxic Phial" to "Venomous elixir from the boggy depths."),
    "art_trade_base_6" to ("Alice's Leaden Mace" to "Heavy crushing weapon."),
    "art_base_7" to ("Mi & Na's Jester Bells" to "Jingly bells of the trickster twins."),
    "art_base_8" to ("Vlasta's Scarlet Thorn" to "Sharp thorn from the eastern gardens."),
    "art_base_9" to ("Evanda's Silver Cross" to "Sacred symbol of the Temple of Light."),
    "art_base_10" to ("Isolda's Icicle Shard" to "Unmelting splinter of eternal frost."),
    "art_base_11" to ("Lilith's Spiked Bracer" to "Trinket forged in the Hellish Rift."),
    "art_base_12" to ("Griselda's Copper Pin" to "Hefty rolling pin of the royal cook."),
    "art_base_13" to ("Brunhilde's Ranger Cloak" to "Camouflage mantle of a pathfinder."),
    "art_base_14" to ("Iva's Birchbark Ward" to "Ancient talisman of the forest spirit."),
    "art_trade_spec_1" to ("Lilith's Dark Colossus" to "Monumental demonic armor."),
    "art_trade_spec_2" to ("Griselda's Red-Hot Brazier" to "Fury of Roseville's hellish bakery."),
    "art_trade_spec_3" to ("Evanda's Sacrificial Circlet" to "Sacred crown of self-sacrifice."),
    "art_trade_spec_4" to ("Mi & Na's Jester Fireworks" to "Salvo of carnival powder rockets."),
    "art_spec_1" to ("Griselda's Infernal Hearth" to "Blazing furnace of the wicked baker."),
    "art_spec_2" to ("Isolda's Permafrost Crown" to "Legendary northern crown of ice."),
    "art_spec_3" to ("Vlasta's Blood Rose Scepter" to "Emblem of the Queen of the East."),
    "art_spec_4" to ("Evanda's Celestial Aegis" to "Unbreakable bastion of faith."),
    "art_spec_5" to ("Lilith's Abyssal Heart" to "Flame straight from the depths of Patreya."),
    "art_spec_6" to ("Iva's Springwater Staff" to "Living staff of the primeval thicket."),
    "art_spec_7" to ("Alice's Bastion Cuirass" to "Defensive plate of Ludwig's Valkyrie."),
    "art_spec_8" to ("Mi & Na's Blast Orbs" to "Black-powder treats from the tricksters."),
    "art_spec_9" to ("Morgana's Swamp Totem" to "Carved fetish steeped in marsh mist."),
    "art_spec_10" to ("Brunhilde's Falcon Quiver" to "Feathered quiver of unerring woodland arrows."),
    "art_spec_11" to ("Olga's Champion Plate" to "Sturdy steel forged in northern citadels."),
    "art_spec_12" to ("Berta's First Flame Spark" to "Blazing ember crystal of an arch-pyromancer."),
    "art_trade_rare_1" to ("Isolda's Ice Monolith" to "Reinforced glacial armor."),
    "art_trade_rare_2" to ("Vlasta's Bloodied Thorn" to "Deadly weapon of the Queen of Roses."),
    "art_rare_1" to ("Brunhilde's Westwind Bow" to "Hunting bow that never misses."),
    "art_rare_2" to ("Olga's Royal Regiment Plate" to "Ceremonial armor of the northern commander."),
    "art_rare_3" to ("Berta's Spark Staff" to "Conduit of the pyromancer's flames."),
    "art_rare_4" to ("Morgana's Swamp Grimoire" to "Ancient tome of bog sorcery."),
    "art_rare_5" to ("Alice's Bastion Hammer" to "Hammer of retribution of the southern warrior."),
    "art_rare_6" to ("Iva's Elder Tree Wreath" to "Living wreath of the woodland healer."),
    "art_rare_7" to ("Mi & Na's Powder Bombs" to "Deadly surprises from the jester sisters."),
    "art_rare_8" to ("Vlasta's Crimson Queen Tiara" to "Golden crown woven with scarlet rose thorns."),
    "art_rare_9" to ("Griselda's Hearthguard Armor" to "Heavy kitchen plate tempered by roaring ovens."),
    "art_rare_10" to ("Lilith's Inferno Cuirass" to "Obsidian mail glowing with abyssal embers."),
    "art_rare_11" to ("Evanda's Dawn Lantern" to "Radiant sanctum reliquary of pure gold."),
    "art_rare_12" to ("Iva's Primal Grove Tear" to "Dewdrop crystal brimming with restorative magic."),
    "art_rare_13" to ("Olga's Northern Sunset Charm" to "Silver signet blessed by polar auroras."),
    "art_mythic_1" to ("Lilith's Crown of Rift Darkness" to "Heavy crown of the underworld sovereign."),
    "art_mythic_2" to ("Lilith's Apocalypse Blade" to "Incinerating weapon of ancient chaos."),
    "art_mythic_3" to ("Evanda's Holy Grail" to "Great relic of eternal light."),
    "art_mythic_4" to ("Isolda's Eternal Winter Crown" to "Absolute defense of the northern winds."),
    "art_mythic_5" to ("Vlasta's Crimson Moon Scepter" to "Pinnacle of eastern majesty."),
    "art_mythic_6" to ("Alice's Sunlit Hammer" to "Sacred weapon of the southern seas."),
    "art_mythic_7" to ("Iva's Tree of Life Heart" to "Inexhaustible source of natural power."),
    "art_mythic_8" to ("Brunhilde's Thunder Crossbow" to "Celestial bow of the storm huntresses."),
    "art_mythic_9" to ("Olga's Titan Armor" to "Indestructible armor of legends."),
    "art_mythic_10" to ("Berta's Infernal Rod" to "Flame born of exploding supernovas."),
    "art_mythic_11" to ("Morgana's Eye of Foresight" to "All-seeing mirror of the swamp witch."),
    "art_mythic_12" to ("Mi & Na's Starlight Crackers" to "Fireworks illuminating the night sky."),
    "art_mythic_13" to ("Griselda's Royal Oven" to "Unquenchable fire of the royal kitchen."),
    "art_mythic_14" to ("Evanda's Aegis of Eternal Dawn" to "Shield forged from first rays of sunrise."),
    "art_mythic_15" to ("Isolda's Mirror of Fates" to "Ancient glacial mirror of bygone eras."),
    "art_mythic_16" to ("Brunhilde's Ring of Seven Winds" to "Signet of the huntress of airy expanses."),
    "art_mythic_17" to ("Alice's Spear of Stardust" to "Forged tip of a fallen comet."),
    "art_mythic_18" to ("Olga's Mantle of Celestial Stars" to "Garment woven from northern constellations."),
    "art_mythic_19" to ("Berta's Dragonflame Helm" to "Scales of an ancient fire-breathing wyrm."),
    "art_mythic_20" to ("Vlasta's Blood Moon Necklace" to "Ruby weave of gothic roses."),
    "art_1" to ("Alice's Bastion Hammer" to "Hammer of retribution of the southern warrior."),
    "art_2" to ("Olga's Forged Shield" to "Sturdy shield with northern engravings."),
    "art_3" to ("Brunhilde's Hunting Knife" to "A trusty blade of the forest huntress."),
    "art_4" to ("Berta's Spark Staff" to "Conduit of the pyromancer's flames."),
    "art_5" to ("Isolda's Permafrost Crown" to "Legendary northern crown of ice."),
    "art_6" to ("Iva's Forest Charm" to "Living amulet of the forest healer."),
    "art_7" to ("Berta's Gauntlets" to "Fireproof combat gloves."),
    "art_8" to ("Mi & Na's Jester Bells" to "Jingly bells of the trickster twins."),
    "art_9" to ("Vlasta's Blood Rose Scepter" to "Emblem of the Queen of the East.")
)

private val ARTIFACT_KEYS_DESCENDING = ARTIFACT_TEXTS_EN.keys.sortedByDescending { it.length }

private fun resolveArtifactEnKey(id: String): String? {
    val cleanId = id.removePrefix("forged_").substringBefore("_daily_").substringBefore("_wheel_")
    if (ARTIFACT_TEXTS_EN.containsKey(cleanId)) return cleanId
    return ARTIFACT_KEYS_DESCENDING.firstOrNull { key ->
        cleanId == key || cleanId.startsWith("${key}_")
    }
}

fun Artifact.localizedName(): String {
    if (!GameLocaleState.isEnglish) return name
    resolveArtifactEnKey(id)?.let { key ->
        ARTIFACT_TEXTS_EN[key]?.let { return it.first }
    }
    return when {
        id.startsWith("forged_") || name == "Кованый Клинок" || name == "Зачарованный Панцирь" || name == "Астральная Реликвия" -> {
            when (slot) {
                ArtifactSlot.WEAPON -> "Forged Blade"
                ArtifactSlot.ARMOR -> "Enchanted Armor"
                ArtifactSlot.RELIC -> "Astral Relic"
            }
        }
        else -> ARTIFACT_TEXTS_EN.values.firstOrNull { it.first == name }?.first ?: name
    }
}

fun Artifact.localizedEffectDescription(): String {
    if (!GameLocaleState.isEnglish) return effectDescription
    resolveArtifactEnKey(id)?.let { key ->
        ARTIFACT_TEXTS_EN[key]?.let { return it.second }
    }
    if (id.startsWith("forged_") || effectDescription.contains("к атаке") || effectDescription.contains("к броне")) {
        return "+$atkBonus ATK, +$armorBonus Armor, +$hpBonus HP"
    }
    return effectDescription
}
