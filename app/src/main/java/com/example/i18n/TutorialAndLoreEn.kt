package com.example.i18n

import com.example.ui.components.FairySpeaker
import com.example.ui.components.TutorialFrameData
import com.example.ui.screens.HubBattleSection

fun FairySpeaker.localizedDisplayName(): String =
    if (!GameLocaleState.isEnglish) displayName else when (this) {
        FairySpeaker.GREENPIE -> "Greenpie"
        FairySpeaker.BLUESWEET -> "Bluesweet"
    }

fun getTutorialFrameEn(step: Int, playerName: String): TutorialFrameData {
    return when (step) {
        1 -> TutorialFrameData(
            step = 1,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            textTemplate = "Greetings, $playerName! Welcome to Avresia, a world ruled by magic and curves!"
        )
        2 -> TutorialFrameData(
            step = 2,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Sister, this is just another human who downloaded this game onto their phone!"
        )
        3 -> TutorialFrameData(
            step = 3,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "You're only here to admire curvy girls, aren't you? Caught you red-handed!"
        )
        4 -> TutorialFrameData(
            step = 4,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            isChoice = true,
            textTemplate = "Forgive my sister, she always ruins the mood. Shall we start the tutorial?"
        )
        5 -> TutorialFrameData(
            step = 5,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            textTemplate = "These are our heroes—or rather, heroines. They make up your battle deck."
        )
        6 -> TutorialFrameData(
            step = 6,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            textTemplate = "Right now you only have 4 girls, but soon you will unlock many more!"
        )
        7 -> TutorialFrameData(
            step = 7,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Boring! Folks like you skip the tutorial because of speeches like that, sis. Pay attention!"
        )
        8 -> TutorialFrameData(
            step = 8,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Main rule: every beauty has her own stats and skills. Check their cards for all the details."
        )
        9 -> TutorialFrameData(
            step = 9,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Find artifacts, gather hero shards, and your girls will grow much stronger!"
        )
        10 -> TutorialFrameData(
            step = 10,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            textTemplate = "Also, in our world, the stronger you become, the plumper you get!"
        )
        11 -> TutorialFrameData(
            step = 11,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "That's why the girls get curvier and heavier every time you level them up!"
        )
        12 -> TutorialFrameData(
            step = 12,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 1,
            textTemplate = "In Career you earn artifacts. When you gather extras, you can forge them into a stronger one."
        )
        13 -> TutorialFrameData(
            step = 13,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 1,
            textTemplate = "A forged artifact is stronger than old ones, but a card's level limits which rarity she can equip!"
        )
        14 -> TutorialFrameData(
            step = 14,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 1,
            textTemplate = "Level up your heroines to unlock new skills and equip even more powerful artifacts!"
        )
        15 -> TutorialFrameData(
            step = 15,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            textTemplate = "And here we have the Citadel of Avresia—or simply the Hub."
        )
        16 -> TutorialFrameData(
            step = 16,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            textTemplate = "All of the game's battle modes are located right here."
        )
        17 -> TutorialFrameData(
            step = 17,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            textTemplate = "There are quite a few of them. Let me tell you about the main ones, sister!"
        )
        18 -> TutorialFrameData(
            step = 18,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.CAREER,
            textTemplate = "Here is Career—or its pathetic excuse for one. Clear stages, crush bosses, unlock new ladies!"
        )
        19 -> TutorialFrameData(
            step = 19,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.CAREER,
            textTemplate = "An enemy squad awaits on every stage. The squad's attack power is shown right on the card."
        )
        20 -> TutorialFrameData(
            step = 20,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.CAREER,
            textTemplate = "Cards come in three types: Melee, Ranged, and Hybrid. Each strikes its own line."
        )
        21 -> TutorialFrameData(
            step = 21,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.ARENA,
            textTemplate = "Here we farm currency by fighting bots. Right, sister?"
        )
        22 -> TutorialFrameData(
            step = 22,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            targetHubSection = HubBattleSection.ARENA,
            textTemplate = "You have five Arena keys. Every entry into the Arena costs 1 key."
        )
        23 -> TutorialFrameData(
            step = 23,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            targetHubSection = HubBattleSection.ARENA,
            textTemplate = "Don't worry though, $playerName! Your keys will regenerate over time."
        )
        24 -> TutorialFrameData(
            step = 24,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            textTemplate = "The remaining modes are only available to seasoned warriors."
        )
        25 -> TutorialFrameData(
            step = 25,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 3,
            textTemplate = "Spend your currency on chests to get hero shards or artifacts! That covers everything."
        )
        26 -> TutorialFrameData(
            step = 26,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 3,
            isChoice = true,
            textTemplate = "Is everything clear, sweetie pie?"
        )
        27 -> TutorialFrameData(
            step = 27,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            textTemplate = "If you want to back up your progress, tap the gear icon at the top and save your file there."
        )
        else -> TutorialFrameData(
            step = 28,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            textTemplate = "Got lots of coins but zero gems? Tap the gems or coins icon at the top and convert your currency. Ciao!"
        )
    }
}

fun getColiseumTutorialFrameEn(step: Int): TutorialFrameData {
    return when (step) {
        1 -> TutorialFrameData(
            step = 1,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            textTemplate = "This is the Coliseum. Here, arenas follow one after another."
        )
        2 -> TutorialFrameData(
            step = 2,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            textTemplate = "So level up your ladies before coming in here!"
        )
        else -> TutorialFrameData(
            step = 3,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            textTemplate = "The further you go, the tougher the foes and the bigger the prize!"
        )
    }
}

fun getTowerTutorialFrameEn(step: Int): TutorialFrameData {
    return when (step) {
        1 -> TutorialFrameData(
            step = 1,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "Welcome to the Tower of Suffering! Muahaha!"
        )
        2 -> TutorialFrameData(
            step = 2,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "Sister! Stop making things up! This is the Tower of Trials!"
        )
        3 -> TutorialFrameData(
            step = 3,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "Depending on the day of the week, different rules apply here."
        )
        4 -> TutorialFrameData(
            step = 4,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "And sometimes there are special themed days too. Don't miss out!"
        )
        5 -> TutorialFrameData(
            step = 5,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "Unlike the Coliseum, your progress is saved until the end of the day."
        )
        else -> TutorialFrameData(
            step = 6,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "However, you only get 2 attempts per day. Good luck!"
        )
    }
}

const val WORLD_LORE_EN =
    "The eruption of Mount Patreya shook all of Avresia: rivers of lava spared the Temple of Fire, yet the ensuing earthquake tore open an ominous rift—and demonic fiends poured forth. Among those fleeing the chaos was the novice Berta. Deep in the Nameless Forest she was found by the huntress Brunhilde, who hurried to carry the warning to Tuversk.\n\n" +
        "Tsar Vladimir II dismissed the tale as tall fiction, yet Commander Olga trusted her friend Brunhilde and marched away from the heart of evil alongside her and Berta. By the Swamp Sea the young witch Morgana joined their ranks, revealing a grave new threat: the demons had learned to wear the faces of loved ones and sow discord.\n\n" +
        "Now the fate of Avresia rests in the hands of these four women. To seal the rift, they must recover the four ancient amulets of the cardinal directions (South, East, North, and West), overcome the possessed rulers, and restore peace to their homeland."
