package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.model.*
import kotlin.random.Random

class GameDataRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)

    // Predefined Hero Cards:
    // Level 1: Player has ONLY 4 playable cards (2 Melee, 2 Ranged):
    // 1. Брунгильда (Melee) - Unlocked
    // 2. Ольга (Melee) - Unlocked
    // 3. Берта (Ranged) - Unlocked
    // 4. Моргана (Ranged) - Unlocked
    // All other cards are initially locked and hidden ("без указания что это за карты"),
    // unlocking as the player gains experience and advances through career!
    fun getInitialHeroCards(): List<HeroCard> {
        return listOf(
            // --- 4 PLAYABLE STARTING HEROES (BASIC) ---
            HeroCard(
                id = "brunhilde",
                name = "Брунгильда",
                title = "Охотница Запада",
                heroType = HeroType.MELEE,
                rarity = CardRarity.BASIC,
                element = Element.STEEL,
                baseAtk = 8,
                baseArmor = 3,
                baseHp = 42,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_brunhilde_0,
                activeSkillId = "brun_skill_1",
                availableSkills = listOf(
                    HeroSkill("brun_skill_1", "Клинок справедливости", "Наносит 100% урона врагу спереди и 25% соседнему врагу на линии (если соседа нет — +25% тому же врагу).", energyCost = 2, skillType = SkillType.OFFENSIVE, unlockLevel = 3)
                ),
                lore = "Лесная охотница, готовая пойти на любого зверя.",
                isUnlocked = true, // Playable card 1 (Melee 1)
                unlockPlayerLevel = 1
            ),
            HeroCard(
                id = "olga",
                name = "Ольга",
                title = "Рыцарь Севера",
                heroType = HeroType.MELEE,
                rarity = CardRarity.BASIC,
                element = Element.STEEL,
                baseAtk = 7,
                baseArmor = 3,
                baseHp = 38,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_olga_0,
                activeSkillId = "olga_skill_1",
                availableSkills = listOf(
                    HeroSkill("olga_skill_1", "Щит царя", "Плюс 10% от здоровья к броне и замедляет врага перед собой", energyCost = 2, skillType = SkillType.DEFENSIVE, unlockLevel = 3)
                ),
                lore = "Отважная командующая войском царя Владимира II.",
                isUnlocked = true, // Playable card 2 (Melee 2)
                unlockPlayerLevel = 1
            ),
            HeroCard(
                id = "berta",
                name = "Берта",
                title = "Озорная Пиромантка",
                heroType = HeroType.RANGED,
                rarity = CardRarity.BASIC,
                element = Element.MAGIC,
                baseAtk = 12,
                baseArmor = 0,
                baseHp = 17,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_berta_0,
                activeSkillId = "berta_skill_1",
                availableSkills = listOf(
                    HeroSkill("berta_skill_1", "Метеоритный дождь", "Нанести каждому врагу на второй линии по 50% от своего урона (или 100%, если остался один враг)", energyCost = 2, skillType = SkillType.OFFENSIVE, unlockLevel = 3)
                ),
                lore = "Атакует врагов на второй линии. Неунывающая повелительница огня, сбежавшая из Храма Огня и жаждущая приключений на свою голову.",
                isUnlocked = true, // Playable card 3 (Ranged 1)
                unlockPlayerLevel = 1
            ),
            HeroCard(
                id = "witch",
                name = "Моргана",
                title = "Болотная Ведьма",
                heroType = HeroType.RANGED,
                rarity = CardRarity.BASIC,
                element = Element.BLIGHT,
                baseAtk = 11,
                baseArmor = 0,
                baseHp = 17,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_witch_0,
                activeSkillId = "witch_skill_1",
                availableSkills = listOf(
                    HeroSkill("witch_skill_1", "Вытягивание сил", "Наносит по 30% урона обоим врагам на второй линии и с вероятностью 50% ослабляет их атаку вдвое на один ход.", energyCost = 2, skillType = SkillType.OFFENSIVE, unlockLevel = 3)
                ),
                lore = "Атакует врагов на второй линии. Ведьма с Болотного Моря, оберегающая свои владения от чужаков.",
                isUnlocked = true, // Playable card 4 (Ranged 2)
                unlockPlayerLevel = 1
            ),

            // --- CHAPTER 1 HEROES (SPECIAL / ОСОБАЯ) ---
            HeroCard(
                id = "alisa",
                name = "Алиса",
                title = "Воительница Юга",
                heroType = HeroType.MELEE,
                rarity = CardRarity.SPECIAL,
                element = Element.STEEL,
                baseAtk = 8,
                baseArmor = 4,
                baseHp = 46,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_alisa_0,
                activeSkillId = "alisa_skill_1",
                availableSkills = listOf(
                    HeroSkill("alisa_skill_1", "Бастион обороны", "Каждому союзнику, в том числе и себе 10% от их максимального здоровья в щит, даже если он нулевой.", energyCost = 2, skillType = SkillType.DEFENSIVE, unlockLevel = 3),
                    HeroSkill("alisa_skill_2", "Молот возмездия", "150% урон одному врагу", energyCost = 3, skillType = SkillType.OFFENSIVE, unlockLevel = 5)
                ),
                lore = "Валькирия на службе короля Людвига, правителя южных земель.",
                isUnlocked = false, // Hidden & Locked
                unlockPlayerLevel = 4,
                unlockCareerStageId = 106 // Доступна после прохождения босса 1 главы (1-6 Власта)
            ),
            HeroCard(
                id = "iva",
                name = "Ива",
                title = "Лесная целительница",
                heroType = HeroType.RANGED,
                rarity = CardRarity.SPECIAL,
                element = Element.LIGHT,
                baseAtk = 8,
                baseArmor = 0,
                baseHp = 20,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_iva_0,
                activeSkillId = "iva_skill_1",
                availableSkills = listOf(
                    HeroSkill("iva_skill_1", "Живительный росток", "Исцеляет одного наиболее раненого союзника на 25% от его максимального здоровья", energyCost = 2, skillType = SkillType.SUPPORT, unlockLevel = 3),
                    HeroSkill("iva_skill_2", "Древесный бальзам", "Исцеляет двух любых союзников на 20% от их максимального здоровья", energyCost = 3, skillType = SkillType.SUPPORT, unlockLevel = 5)
                ),
                lore = "Атакует врагов на второй линии. Травница, способная исцелить любые раны соратниц и помочь им выстоять даже против грозных противников.",
                isUnlocked = false,
                unlockPlayerLevel = 4,
                unlockCareerStageId = 106 // Доступна после прохождения босса 1 главы (1-6 Власта)
            ),
            HeroCard(
                id = "mi_and_na",
                name = "Ми и На",
                title = "Смертоносные Шутихи",
                heroType = HeroType.HYBRID,
                rarity = CardRarity.SPECIAL,
                element = Element.MAGIC,
                baseAtk = 5,
                baseArmor = 0,
                baseHp = 19,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_mi_and_na_0,
                attackPattern = AttackPattern.ALL_ENEMIES,
                activeSkillId = "mina_skill_1",
                availableSkills = listOf(
                    HeroSkill("mina_skill_1", "Световая вспышка", "Ослепляет одного из врагов на 1 раунд (ослеплённый враг пропускает следующий ход).", energyCost = 2, skillType = SkillType.OFFENSIVE, unlockLevel = 3),
                    HeroSkill("mina_skill_2", "Бомба с сюрпризом", "Нанести всем врагам 60% урона и ожог картам второй линии с уроном в 20% на 2 раунда", energyCost = 3, skillType = SkillType.OFFENSIVE, unlockLevel = 5)
                ),
                lore = "Две сестры-разбойницы, наводящие страх на торговые караваны.",
                isUnlocked = false,
                unlockPlayerLevel = 8,
                unlockCareerStageId = 306 // Доступны после прохождения босса 3 главы (3-6 Изольда)
            ),
            HeroCard(
                id = "leya",
                name = "Лея",
                title = "Лесная дикарка",
                heroType = HeroType.HYBRID,
                rarity = CardRarity.SPECIAL,
                element = Element.LIGHT,
                baseAtk = 8,
                baseArmor = 0,
                baseHp = 24,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_leya_0,
                attackPattern = AttackPattern.PIERCE_25_75,
                activeSkillId = "leya_skill_1",
                availableSkills = listOf(
                    HeroSkill(
                        "leya_skill_1",
                        "Усиленный корень",
                        "Повышает атаку выбранной союзницы на 25% на следующий ход.",
                        energyCost = 2,
                        skillType = SkillType.SUPPORT,
                        unlockLevel = 3
                    ),
                    HeroSkill(
                        "leya_skill_2",
                        "Очищение",
                        "Снимает с выбранной союзницы негативные эффекты.",
                        energyCost = 3,
                        skillType = SkillType.SUPPORT,
                        unlockLevel = 5
                    )
                ),
                lore = "Дикарка из лесов Северного царства, умеющая повелевать деревьями и живыми корнями.",
                isUnlocked = false,
                unlockPlayerLevel = 8,
                unlockCareerStageId = 306 // Доступна после прохождения босса 3 главы (3-6 Изольда)
            ),

            // --- CHAPTER 2 HEROES (RARE / РЕДКАЯ) ---
            HeroCard(
                id = "vlasta",
                name = "Власта",
                title = "Королева Востока",
                heroType = HeroType.MELEE,
                rarity = CardRarity.RARE,
                element = Element.BLIGHT,
                baseAtk = 9,
                baseArmor = 3,
                baseHp = 45,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_vlasta_0,
                attackPattern = AttackPattern.SINGLE,
                activeSkillId = "vlasta_skill_1",
                availableSkills = listOf(
                    HeroSkill("vlasta_skill_1", "Шипы розы", "Отравляет цель на 2 раунда, снимая 20% от максимального здоровья каждый ход.", energyCost = 2, skillType = SkillType.OFFENSIVE, unlockLevel = 3),
                    HeroSkill("vlasta_skill_2", "Голову с плеч!", "Наносит 120% урона цели и отравляет цель на 2 раунда на 10% от максимального здоровья каждый ход.", energyCost = 3, skillType = SkillType.OFFENSIVE, unlockLevel = 5),
                    HeroSkill("vlasta_skill_3", "Королевский гнев", "Наносит 200% атаки цели, а себя лечит на 50% от уровня урона.", energyCost = 4, skillType = SkillType.OFFENSIVE, unlockLevel = 8)
                ),
                lore = "Злая королева - волшебница, больше всего на свете любящая казни и красные розы.",
                isUnlocked = false,
                unlockPlayerLevel = 6,
                unlockCareerStageId = 206 // Доступна после прохождения босса 2 главы (2-6 Власта)
            ),
            HeroCard(
                id = "evanda",
                name = "Эванда",
                title = "Монахиня Света",
                heroType = HeroType.MELEE,
                rarity = CardRarity.RARE,
                element = Element.LIGHT,
                baseAtk = 7,
                baseArmor = 4,
                baseHp = 44,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_evanda_0,
                activeSkillId = "evanda_skill_1",
                availableSkills = listOf(
                    HeroSkill("evanda_skill_1", "Оплот защиты", "Добавь щит позади стоящему в размере 20% от ее здоровья.", energyCost = 2, skillType = SkillType.DEFENSIVE, unlockLevel = 3),
                    HeroSkill("evanda_skill_2", "Благословение", "Добавь щит двум любым персонажам в отряде в размере 15% от их максимального здоровья", energyCost = 3, skillType = SkillType.DEFENSIVE, unlockLevel = 5),
                    HeroSkill("evanda_skill_3", "Великое стояние", "Щит всем в отряде в размере 10% от их максимального здоровья", energyCost = 4, skillType = SkillType.DEFENSIVE, unlockLevel = 8)
                ),
                lore = "Служительница храма Света, защищающая остальные Земли от дьявольских существ из Адского Разлома.",
                isUnlocked = false,
                unlockPlayerLevel = 10,
                unlockCareerStageId = 404 // Доступна после прохождения уровня 4-4
            ),
            HeroCard(
                id = "isolda",
                name = "Изольда",
                title = "Королева холода",
                heroType = HeroType.RANGED,
                rarity = CardRarity.RARE,
                element = Element.MAGIC,
                baseAtk = 11,
                baseArmor = 0,
                baseHp = 20,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_isolda_0,
                activeSkillId = "isolda_skill_1",
                availableSkills = listOf(
                    HeroSkill("isolda_skill_1", "Холодильник", "Замораживает любого персонажа врага на выбор на один раунд.", energyCost = 2, skillType = SkillType.OFFENSIVE, unlockLevel = 3),
                    HeroSkill("isolda_skill_2", "Ледяной душ", "Наносит по 30% урона всем врагам и замедляет их на один раунд на 25%", energyCost = 3, skillType = SkillType.OFFENSIVE, unlockLevel = 5)
                ),
                lore = "Атакует врагов на второй линии. Холодная красавица с севера, правящая снегами и ветрами.",
                isUnlocked = false,
                unlockPlayerLevel = 8,
                unlockCareerStageId = 306 // Доступна после прохождения босса 3 главы (3-6 Изольда)
            ),
            HeroCard(
                id = "demoness",
                name = "Лилит",
                title = "Манящая Дьяволица",
                heroType = HeroType.HYBRID,
                rarity = CardRarity.RARE,
                element = Element.BLIGHT,
                baseAtk = 8,
                baseArmor = 0,
                baseHp = 29,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_demoness_0,
                attackPattern = AttackPattern.PIERCE_LINE,
                activeSkillId = "dem_skill_1",
                availableSkills = listOf(
                    HeroSkill("dem_skill_1", "Двойной кулак", "125% урон по обоим врагам на первой линии", energyCost = 2, skillType = SkillType.OFFENSIVE, unlockLevel = 3),
                    HeroSkill("dem_skill_2", "Приворот", "Привораживает врага на 1 раунд (пропускает ход 💋) и заставляет атаковать соседнего союзника на 100% своей атаки", energyCost = 3, skillType = SkillType.OFFENSIVE, unlockLevel = 5),
                    HeroSkill("dem_skill_3", "Хаос Бездны", "100% урон по двум врагам на первой линии и 50% по двум врагам на второй линии", energyCost = 4, skillType = SkillType.OFFENSIVE, unlockLevel = 8)
                ),
                lore = "Демоница из нижнего мира, ищущая себе развлечений среди смертных.",
                isUnlocked = false,
                unlockPlayerLevel = 10,
                unlockCareerStageId = 406 // Доступна после прохождения босса 4 главы (4-6 Лилит)
            ),
            HeroCard(
                id = "baker",
                name = "Гризельда",
                title = "Злая Пироженщица",
                heroType = HeroType.HYBRID,
                rarity = CardRarity.RARE,
                element = Element.MAGIC,
                baseAtk = 8,
                baseArmor = 0,
                baseHp = 28,
                level = 1,
                shards = 0,
                shardsNeeded = 10,
                imageRes = R.drawable.hero_baker_0,
                attackPattern = AttackPattern.DIAGONAL_PAIR,
                activeSkillId = "baker_skill_1",
                availableSkills = listOf(
                    HeroSkill("baker_skill_1", "Пылающая скалка", "120% урон любому персонажу на первой линии и поджигает его на 2 раунда на 10% от его максимального здоровья каждый раунд", energyCost = 2, skillType = SkillType.OFFENSIVE, unlockLevel = 3),
                    HeroSkill("baker_skill_2", "Взрывной пирог", "50% урон каждому противнику на первой линии и оглушает одного из них на один раунд", energyCost = 3, skillType = SkillType.OFFENSIVE, unlockLevel = 5),
                    HeroSkill("baker_skill_3", "Адская печь", "150% урона по всей первой линии и поджигает ее на 10% от его максимального здоровья на 2 раунда каждый ход", energyCost = 4, skillType = SkillType.OFFENSIVE, unlockLevel = 8)
                ),
                lore = "Бывшая королевская кухарка, выгнанная за скверный характер и теперь мечтающая отомстить.",
                isUnlocked = false,
                unlockPlayerLevel = 6,
                unlockCareerStageId = 206 // Доступна после прохождения босса 2 главы (2-6 Власта)
            ),

            // --- SECRET HEROES (СЕКРЕТНЫЕ) ---
            HeroCard(
                id = "elios",
                name = "Эллиос",
                title = "Маг-альбинос",
                heroType = HeroType.HYBRID,
                rarity = CardRarity.MYTHIC,
                element = Element.MAGIC,
                baseAtk = 7,
                baseArmor = 0,
                baseHp = 24,
                level = 1,
                shards = 0,
                shardsNeeded = SECRET_HERO_UNLOCK_SHARDS,
                imageRes = R.drawable.hero_elios_0,
                attackPattern = AttackPattern.FRONT_50_BACK_25,
                activeSkillId = "elios_skill_1",
                availableSkills = listOf(
                    HeroSkill(
                        "elios_skill_1",
                        "Остужение",
                        "Замораживает двух врагов на 1 ход и наносит им урон в размере 50%.",
                        energyCost = 2,
                        skillType = SkillType.OFFENSIVE,
                        unlockLevel = 3
                    ),
                    HeroSkill(
                        "elios_skill_2",
                        "Гололед",
                        "Два любых противника ослаблены на 50%.",
                        energyCost = 4,
                        skillType = SkillType.OFFENSIVE,
                        unlockLevel = 5
                    )
                ),
                lore = "Внебрачная дочь Изольды, сбежавшая от гнета своей матери.",
                isUnlocked = false,
                unlockPlayerLevel = Int.MAX_VALUE,
                unlockCareerStageId = null,
                isSecret = true
            ),
            HeroCard(
                id = "vasilisa",
                name = "Василиса",
                title = "Снайпер Пустыни",
                heroType = HeroType.HYBRID,
                rarity = CardRarity.MYTHIC,
                element = Element.STEEL,
                baseAtk = 10,
                baseArmor = 0,
                baseHp = 20,
                level = 1,
                shards = 0,
                shardsNeeded = VASILISA_UNLOCK_SHARDS,
                imageRes = R.drawable.hero_vasilisa_0,
                attackPattern = AttackPattern.SINGLE,
                activeSkillId = "vasilisa_skill_1",
                availableSkills = listOf(
                    HeroSkill(
                        "vasilisa_skill_1",
                        "По касательной",
                        "Дополнительно снимает 25% от нанесенного урона с любого врага.",
                        energyCost = 2,
                        skillType = SkillType.OFFENSIVE,
                        unlockLevel = 3
                    ),
                    HeroSkill(
                        "vasilisa_skill_2",
                        "Легкая рука",
                        "С вероятностью 35% наносит критический урон, в ином случае обычный.",
                        energyCost = 3,
                        skillType = SkillType.OFFENSIVE,
                        unlockLevel = 5
                    ),
                    HeroSkill(
                        "vasilisa_skill_3",
                        "Зигзак",
                        "Наносит дополнительно 100% второму врагу по диагонали",
                        energyCost = 4,
                        skillType = SkillType.OFFENSIVE,
                        unlockLevel = 8
                    )
                ),
                lore = "Василиса — таинственный снайпер пустыни Востока, выполняющая личные заказы Власты.",
                isUnlocked = false,
                unlockPlayerLevel = Int.MAX_VALUE,
                unlockCareerStageId = null,
                isSecret = true
            ),
            HeroCard(
                id = "kadru",
                name = "Кадру",
                title = "Ящер Тартара",
                heroType = HeroType.MELEE,
                rarity = CardRarity.MYTHIC,
                element = Element.BLIGHT,
                baseAtk = 8,
                baseArmor = 3,
                baseHp = 40,
                level = 1,
                shards = 0,
                shardsNeeded = KADRU_UNLOCK_SHARDS,
                imageRes = R.drawable.hero_kadru_0,
                attackPattern = AttackPattern.POISON_MELEE,
                activeSkillId = "kadru_skill_1",
                availableSkills = listOf(
                    HeroSkill(
                        "kadru_skill_1",
                        "Ярость",
                        "Увеличивает силу атаки на 30% на следующий ход",
                        energyCost = 2,
                        skillType = SkillType.DEFENSIVE,
                        unlockLevel = 3
                    ),
                    HeroSkill(
                        "kadru_skill_2",
                        "Ядовитая лужа",
                        "Поражает всех врагов ядом. Яд снимет 50% от урона карты на 1 сл ход.",
                        energyCost = 3,
                        skillType = SkillType.OFFENSIVE,
                        unlockLevel = 5
                    )
                ),
                lore = "Одна из преспешниц Лилит. Её атака ядовита, а лечение не поможет.",
                isUnlocked = false,
                unlockPlayerLevel = Int.MAX_VALUE,
                unlockCareerStageId = null,
                isSecret = true
            )
        )
    }

    // Artifacts ONLY drop from career missions: starting inventory is EMPTY!
    fun getInitialArtifacts(): List<Artifact> {
        return emptyList()
    }

    fun getArtifactCatalog(): Map<String, Artifact> {
        val allArtifacts = listOf(
            // =========================================================================
            // БАЗОВЫЕ АРТЕФАКТЫ (20 шт):
            // 60% (12 шт) с отрицательными свойствами: ATK +1..+4, ARMOR +1..+2, HP +5..+15, штрафы -1..-2.
            // 40% (8 шт) с положительными свойствами.
            // =========================================================================
            // 12 базовых с отрицательными свойствами:
            Artifact("art_base_1", "Охотничий нож Брунгильды", CardRarity.BASIC, ArtifactSlot.WEAPON, 4, -1, 8, "Верное лезвие лесной охотницы.", "🗡️", "HUNTER_SET"),
            Artifact("art_base_2", "Кованый щит Ольги", CardRarity.BASIC, ArtifactSlot.ARMOR, -1, 2, 12, "Прочный щит с северной гравировкой.", "🛡️", "NORTH_SET"),
            Artifact("art_base_3", "Рукавицы Берты", CardRarity.BASIC, ArtifactSlot.ARMOR, 3, -1, 6, "Огнеупорные боевые перчатки.", "🧤", "PYRO_SET"),
            Artifact("art_base_4", "Флакон отваров Морганы", CardRarity.BASIC, ArtifactSlot.RELIC, 3, -1, 10, "Едкий настой болотной ведьмы.", "🧪", "WITCH_SET"),
            Artifact("art_base_5", "Повязка Воительницы Алисы", CardRarity.BASIC, ArtifactSlot.RELIC, -1, 2, 14, "Боевая лента валькирии с юга.", "🎗️", "VALKYRIE_SET"),
            Artifact("art_base_6", "Лесной оберег Ивы", CardRarity.BASIC, ArtifactSlot.RELIC, -1, 1, 15, "Живой амулет целительницы.", "🌿", "HEALER_SET"),
            Artifact("art_trade_base_1", "Тяжелые латы Ольги", CardRarity.BASIC, ArtifactSlot.ARMOR, -2, 2, 14, "Неповоротливая защитная броня.", "🛡️", "NORTH_SET"),
            Artifact("art_trade_base_2", "Берсеркский кинжал Брунгильды", CardRarity.BASIC, ArtifactSlot.WEAPON, 4, -2, 6, "Острое лезвие ценой защиты.", "🗡️", "HUNTER_SET"),
            Artifact("art_trade_base_3", "Огненный свиток Берты", CardRarity.BASIC, ArtifactSlot.RELIC, 3, -1, 9, "Обжигающий манускрипт пиромантки.", "📜", "PYRO_SET"),
            Artifact("art_trade_base_4", "Черепаший панцирь Ивы", CardRarity.BASIC, ArtifactSlot.ARMOR, -2, 2, 12, "Защитный природный кокон.", "🐢", "HEALER_SET"),
            Artifact("art_trade_base_5", "Токсичный флакон Морганы", CardRarity.BASIC, ArtifactSlot.WEAPON, 4, -1, 7, "Ядовитое зелье болотных топей.", "🧪", "WITCH_SET"),
            Artifact("art_trade_base_6", "Свинцовая булава Алисы", CardRarity.BASIC, ArtifactSlot.WEAPON, 4, -2, 10, "Тяжелое сокрушительное оружие.", "🔨", "VALKYRIE_SET"),

            // 8 базовых без штрафов:
            Artifact("art_base_7", "Бубенцы Шутих Ми и На", CardRarity.BASIC, ArtifactSlot.RELIC, 2, 1, 8, "Звонкие шутовские колокольчики.", "🔔", "JESTER_SET"),
            Artifact("art_base_8", "Шип Алой Розы Власты", CardRarity.BASIC, ArtifactSlot.WEAPON, 3, 0, 6, "Острый шип восточных садов.", "🌹", "QUEEN_SET"),
            Artifact("art_base_9", "Серебряный крест Эванды", CardRarity.BASIC, ArtifactSlot.RELIC, 1, 2, 10, "Священный символ храма Света.", "📿", "LIGHT_SET"),
            Artifact("art_base_10", "Ледяная сосулька Изольды", CardRarity.BASIC, ArtifactSlot.WEAPON, 3, 0, 7, "Незамерзающий осколок льда.", "❄️", "FROST_SET"),
            Artifact("art_base_11", "Шипастый браслет Лилит", CardRarity.BASIC, ArtifactSlot.ARMOR, 2, 1, 9, "Украшение из Адского Разлома.", "🖤", "DEMON_SET"),
            Artifact("art_base_12", "Медная скалка Гризельды", CardRarity.BASIC, ArtifactSlot.WEAPON, 3, 1, 8, "Увесистый инструмент поварихи.", "🥖", "BAKER_SET"),
            Artifact("art_base_13", "Походный плащ Брунгильды", CardRarity.BASIC, ArtifactSlot.ARMOR, 0, 2, 12, "Маскировочная накидка следопыта.", "🧥", "HUNTER_SET"),
            Artifact("art_base_14", "Обережная береста Ивы", CardRarity.BASIC, ArtifactSlot.RELIC, 1, 1, 11, "Древний талисман лесного духа.", "🍂", "HEALER_SET"),

            // =========================================================================
            // ОСОБЫЕ АРТЕФАКТЫ (16 шт):
            // ATK +5..+9, ARMOR +3..+5, HP +12..+20 (строго выше Базовых, ниже Редких)
            // Все именные (без Эллиос), 6 Оружий / 5 Доспехов / 5 Реликвий.
            // =========================================================================
            Artifact("art_trade_spec_1", "Колосс Тьмы Лилит", CardRarity.SPECIAL, ArtifactSlot.ARMOR, 5, 5, 38, "Монументальный демонический доспех.", "🖤", "DEMON_SET"),
            Artifact("art_trade_spec_2", "Раскаленная жаровня Гризельды", CardRarity.SPECIAL, ArtifactSlot.WEAPON, 9, 3, 24, "Ярость адской пекарни Розвиля.", "🔥", "BAKER_SET"),
            Artifact("art_trade_spec_3", "Жертвенный венец Эванды", CardRarity.SPECIAL, ArtifactSlot.RELIC, 5, 4, 36, "Священный венец самопожертвования.", "✨", "LIGHT_SET"),
            Artifact("art_trade_spec_4", "Шутовской фейерверк Ми и На", CardRarity.SPECIAL, ArtifactSlot.WEAPON, 8, 3, 26, "Залп ярмарочных пороховых ракет.", "🎆", "JESTER_SET"),
            Artifact("art_spec_1", "Горнило Адской Печи Гризельды", CardRarity.SPECIAL, ArtifactSlot.WEAPON, 8, 3, 28, "Раскаленный очаг злой кухарки.", "🔥", "BAKER_SET"),
            Artifact("art_spec_2", "Корона Вечной Мерзлоты Изольды", CardRarity.SPECIAL, ArtifactSlot.ARMOR, 5, 5, 36, "Легендарный северный ледяной венец.", "👑", "FROST_SET"),
            Artifact("art_spec_3", "Скипетр Кровавой Розы Власты", CardRarity.SPECIAL, ArtifactSlot.WEAPON, 9, 3, 28, "Символ власти королевы Востока.", "🥀", "QUEEN_SET"),
            Artifact("art_spec_4", "Эгида Небесного Храма Эванды", CardRarity.SPECIAL, ArtifactSlot.ARMOR, 6, 5, 38, "Несокрушимый бастион веры.", "🛡️", "LIGHT_SET"),
            Artifact("art_spec_5", "Сердце Адской Бездны Лилит", CardRarity.SPECIAL, ArtifactSlot.RELIC, 7, 3, 30, "Пламя из самого пекла Патрея.", "🖤", "DEMON_SET"),
            Artifact("art_spec_6", "Посох Вешних Вод Ивы", CardRarity.SPECIAL, ArtifactSlot.RELIC, 6, 3, 34, "Живой посох первозданной чащи.", "🌸", "HEALER_SET"),
            Artifact("art_spec_7", "Бастионный панцирь Алисы", CardRarity.SPECIAL, ArtifactSlot.ARMOR, 6, 4, 32, "Защитные латы валькирии Людвига.", "🛡️", "VALKYRIE_SET"),
            Artifact("art_spec_8", "Взрывные шары Ми и На", CardRarity.SPECIAL, ArtifactSlot.WEAPON, 8, 4, 28, "Пороховые гостинцы плутовок.", "🧨", "JESTER_SET"),
            Artifact("art_spec_9", "Болотный тотем Морганы", CardRarity.SPECIAL, ArtifactSlot.RELIC, 7, 4, 30, "Резной амулет туманных болот.", "🔮", "WITCH_SET"),
            Artifact("art_spec_10", "Колчан Соколиного Глаза Брунгильды", CardRarity.SPECIAL, ArtifactSlot.WEAPON, 9, 3, 26, "Меткие стрелы лесной охотницы.", "🏹", "HUNTER_SET"),
            Artifact("art_spec_11", "Богатырские латы Ольги", CardRarity.SPECIAL, ArtifactSlot.ARMOR, 6, 5, 40, "Прочная сталь северных цитаделей.", "🛡️", "NORTH_SET"),
            Artifact("art_spec_12", "Искра Первого Пламени Берты", CardRarity.SPECIAL, ArtifactSlot.RELIC, 7, 3, 32, "Пылающий кристалл верховной пиромантки.", "🔥", "PYRO_SET"),

            // =========================================================================
            // РЕДКИЕ АРТЕФАКТЫ (15 шт):
            // ATK +10..+16, ARMOR +6..+8, HP +22..+35 (строго выше Особых, ниже Мифических)
            // Все именные (без Эллиос), 5 Оружий / 5 Доспехов / 5 Реликвий.
            // =========================================================================
            Artifact("art_trade_rare_1", "Ледяной монолит Изольды", CardRarity.RARE, ArtifactSlot.ARMOR, 10, 8, 68, "Усиленная ледниковая броня.", "❄️", "FROST_SET"),
            Artifact("art_trade_rare_2", "Кровавый шип Власты", CardRarity.RARE, ArtifactSlot.WEAPON, 16, 6, 48, "Смертоносное орудие королевы роз.", "🥀", "QUEEN_SET"),
            Artifact("art_rare_1", "Лук Западных Ветров Брунгильды", CardRarity.RARE, ArtifactSlot.WEAPON, 14, 6, 50, "Охотничий лук, разящий без промаха.", "🏹", "HUNTER_SET"),
            Artifact("art_rare_2", "Латы Царского Полка Ольги", CardRarity.RARE, ArtifactSlot.ARMOR, 11, 8, 64, "Парадная броня командующей Севера.", "🥋", "NORTH_SET"),
            Artifact("art_rare_3", "Огненный посох Искр Берты", CardRarity.RARE, ArtifactSlot.WEAPON, 15, 6, 46, "Средоточие пламени пиромантки.", "🔥", "PYRO_SET"),
            Artifact("art_rare_4", "Гримуар Болотных Тайн Морганы", CardRarity.RARE, ArtifactSlot.RELIC, 12, 7, 58, "Древний фолиант болотной магии.", "📖", "WITCH_SET"),
            Artifact("art_rare_5", "Бастионный молот Алисы", CardRarity.RARE, ArtifactSlot.WEAPON, 13, 7, 54, "Молот возмездия южной воительницы.", "🔨", "VALKYRIE_SET"),
            Artifact("art_rare_6", "Целебный венок Древ Ивы", CardRarity.RARE, ArtifactSlot.RELIC, 11, 6, 64, "Живой венок лесной целительницы.", "🌺", "HEALER_SET"),
            Artifact("art_rare_7", "Пороховые бомбы Ми и На", CardRarity.RARE, ArtifactSlot.WEAPON, 16, 6, 46, "Смертоносные сюрпризы сестер-шутих.", "💣", "JESTER_SET"),
            Artifact("art_rare_8", "Тиара Алой Королевы Власты", CardRarity.RARE, ArtifactSlot.ARMOR, 12, 7, 60, "Золотая корона с шипами алых роз.", "👑", "QUEEN_SET"),
            Artifact("art_rare_9", "Броня Адского Горна Гризельды", CardRarity.RARE, ArtifactSlot.ARMOR, 11, 8, 66, "Тяжелая броня, закаленная в печах кухни.", "🥖", "BAKER_SET"),
            Artifact("art_rare_10", "Панцирь Инферно Лилит", CardRarity.RARE, ArtifactSlot.ARMOR, 12, 8, 62, "Обсидиановые латы с алым жаром бездны.", "🖤", "DEMON_SET"),
            Artifact("art_rare_11", "Лампада Рассвета Эванды", CardRarity.RARE, ArtifactSlot.RELIC, 12, 7, 62, "Сияющий храмовый реликварий чистого золота.", "🏆", "LIGHT_SET"),
            Artifact("art_rare_12", "Слеза Древней Чащи Ивы", CardRarity.RARE, ArtifactSlot.RELIC, 11, 7, 64, "Кристалл росы с живой силой природы.", "🌿", "HEALER_SET"),
            Artifact("art_rare_13", "Талисман Северного Заката Ольги", CardRarity.RARE, ArtifactSlot.RELIC, 13, 6, 56, "Серебряный знак полярного сияния.", "📿", "NORTH_SET"),

            // =========================================================================
            // МИФИЧЕСКИЕ АРТЕФАКТЫ (20 шт):
            // ATK +18..+28, ARMOR +9..+14, HP +38..+50 (строго сильнее Редких по всем статам!)
            // Все 20 именные в честь героинь (без Эллиос), 7 Оружий / 7 Доспехов / 6 Реликвий.
            // =========================================================================
            Artifact("art_mythic_1", "Корона Тьмы Лилит", CardRarity.MYTHIC, ArtifactSlot.ARMOR, 18, 13, 100, "Тяжелая корона владычицы преисподней.", "👑", "DEMON_SET"),
            Artifact("art_mythic_2", "Клинок Апокалипсиса Лилит", CardRarity.MYTHIC, ArtifactSlot.WEAPON, 28, 9, 90, "Испепеляющее оружие древнего хаоса.", "🗡️", "DEMON_SET"),
            Artifact("art_mythic_3", "Священный Грааль Эванды", CardRarity.MYTHIC, ArtifactSlot.RELIC, 20, 11, 96, "Великая реликвия вечного света.", "🏆", "LIGHT_SET"),
            Artifact("art_mythic_4", "Венец Вечной Зимы Изольды", CardRarity.MYTHIC, ArtifactSlot.ARMOR, 19, 13, 98, "Абсолютная защита северных ветров.", "❄️", "FROST_SET"),
            Artifact("art_mythic_5", "Скипетр Багряной Луны Власты", CardRarity.MYTHIC, ArtifactSlot.WEAPON, 26, 10, 86, "Средоточие восточного величия.", "🥀", "QUEEN_SET"),
            Artifact("art_mythic_6", "Солнечный Молот Алисы", CardRarity.MYTHIC, ArtifactSlot.WEAPON, 25, 10, 90, "Священное орудие южных морей.", "🔨", "VALKYRIE_SET"),
            Artifact("art_mythic_7", "Сердце Древа Жизни Ивы", CardRarity.MYTHIC, ArtifactSlot.RELIC, 20, 11, 100, "Неиссякаемый исток природной силы.", "🌿", "HEALER_SET"),
            Artifact("art_mythic_8", "Громовой Арбалет Брунгильды", CardRarity.MYTHIC, ArtifactSlot.WEAPON, 27, 9, 82, "Небесный арбалет грозовых охотниц.", "🏹", "HUNTER_SET"),
            Artifact("art_mythic_9", "Латы Титана Ольги", CardRarity.MYTHIC, ArtifactSlot.ARMOR, 18, 14, 98, "Несокрушимый доспех богатырей.", "🛡️", "NORTH_SET"),
            Artifact("art_mythic_10", "Инфернальный Жезл Берты", CardRarity.MYTHIC, ArtifactSlot.WEAPON, 28, 9, 80, "Пламя рождения сверхновых звезд.", "🔥", "PYRO_SET"),
            Artifact("art_mythic_11", "Око Предвидения Морганы", CardRarity.MYTHIC, ArtifactSlot.RELIC, 22, 10, 94, "Всевидящее зеркало болотной ведьмы.", "🔮", "WITCH_SET"),
            Artifact("art_mythic_12", "Звездные Шутихи Ми и На", CardRarity.MYTHIC, ArtifactSlot.WEAPON, 26, 9, 84, "Салют, озаряющий небесный свод.", "🎆", "JESTER_SET"),
            Artifact("art_mythic_13", "Царская Печь Гризельды", CardRarity.MYTHIC, ArtifactSlot.ARMOR, 21, 12, 96, "Неугасимый огонь королевской кухни.", "🥖", "BAKER_SET"),
            Artifact("art_mythic_14", "Эгида Вечной Зари Эванды", CardRarity.MYTHIC, ArtifactSlot.ARMOR, 19, 13, 100, "Щит первых лучей солнца храма Света.", "☀️", "LIGHT_SET"),
            Artifact("art_mythic_15", "Зеркало Судеб Изольды", CardRarity.MYTHIC, ArtifactSlot.RELIC, 23, 10, 92, "Ледяное полотно минувших эпох королевы.", "🪞", "FROST_SET"),
            Artifact("art_mythic_16", "Перстень Семи Ветров Брунгильды", CardRarity.MYTHIC, ArtifactSlot.RELIC, 24, 9, 88, "Кольцо охотницы воздушных просторов.", "💍", "HUNTER_SET"),
            Artifact("art_mythic_17", "Копье Звездной Пыли Алисы", CardRarity.MYTHIC, ArtifactSlot.WEAPON, 27, 10, 86, "Кованый наконечник упавшей кометы.", "⚡", "VALKYRIE_SET"),
            Artifact("art_mythic_18", "Мантия Небесных Созвездий Ольги", CardRarity.MYTHIC, ArtifactSlot.ARMOR, 20, 12, 96, "Одеяние из созвездий северного неба.", "✨", "NORTH_SET"),
            Artifact("art_mythic_19", "Шлем Драконьего Пламени Берты", CardRarity.MYTHIC, ArtifactSlot.ARMOR, 22, 11, 92, "Чешуя древнего огнедышащего змея.", "🐉", "PYRO_SET"),
            Artifact("art_mythic_20", "Ожерелье Кровавой Луны Власты", CardRarity.MYTHIC, ArtifactSlot.RELIC, 25, 10, 88, "Рубиновое плетение готических роз.", "📿", "QUEEN_SET"),

            // Обратная совместимость для старых сохранений
            Artifact("art_1", "Бастионный молот Алисы", CardRarity.RARE, ArtifactSlot.WEAPON, 13, 7, 54, "Молот возмездия южной воительницы.", "🔨", "VALKYRIE_SET"),
            Artifact("art_2", "Кованый щит Ольги", CardRarity.BASIC, ArtifactSlot.ARMOR, -1, 2, 12, "Прочный щит с северной гравировкой.", "🛡️", "NORTH_SET"),
            Artifact("art_3", "Охотничий нож Брунгильды", CardRarity.BASIC, ArtifactSlot.WEAPON, 4, -1, 8, "Верное лезвие лесной охотницы.", "🗡️", "HUNTER_SET"),
            Artifact("art_4", "Огненный посох Искр Берты", CardRarity.RARE, ArtifactSlot.WEAPON, 15, 6, 46, "Средоточие пламени пиромантки.", "🔥", "PYRO_SET"),
            Artifact("art_5", "Корона Вечной Мерзлоты Изольды", CardRarity.SPECIAL, ArtifactSlot.ARMOR, 5, 5, 36, "Легендарный северный ледяной венец.", "👑", "FROST_SET"),
            Artifact("art_6", "Лесной оберег Ивы", CardRarity.BASIC, ArtifactSlot.RELIC, -1, 1, 15, "Живой амулет целительницы.", "🌿", "HEALER_SET"),
            Artifact("art_7", "Рукавицы Берты", CardRarity.BASIC, ArtifactSlot.ARMOR, 3, -1, 6, "Огнеупорные боевые перчатки.", "🧤", "PYRO_SET"),
            Artifact("art_8", "Бубенцы Шутих Ми и На", CardRarity.BASIC, ArtifactSlot.RELIC, 2, 1, 8, "Звонкие шутовские колокольчики.", "🔔", "JESTER_SET"),
            Artifact("art_9", "Скипетр Кровавой Розы Власты", CardRarity.SPECIAL, ArtifactSlot.WEAPON, 9, 3, 28, "Символ власти королевы Востока.", "🥀", "QUEEN_SET")
        )
        val halvedArtifacts = allArtifacts.map { art ->
            val halvedHp = art.hpBonus / 2
            val cap = maxHpBonusForRarity(art.rarity)
            art.copy(hpBonus = halvedHp.coerceAtMost(cap))
        }
        return halvedArtifacts.associateBy { it.id }
    }

    fun getCampaignChapters(): List<CampaignChapter> {
        val heroMap = getInitialHeroCards().associateBy { it.id }
        val rawChapters = listOf(
            CampaignChapter(
                id = 1,
                name = "Глава 1. Амулет Юга",
                subtitle = "Южные земли и тайны короля Людвига",
                stages = listOf(
                    CampaignStage(
                        id = 101, chapterId = 1, stageNumber = 1,
                        title = "1-1 Домик Морганы",
                        recommendedPower = 5,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("brunhilde", "olga", "berta", "witch"),
                        enemyLevels = listOf(1, 1, 1, 1),
                        stars = 0, isUnlocked = true,
                        rewardGold = 165, rewardGems = 10, rewardExp = 35,
                        rewardArtifactId = "art_base_1",
                        battleBgRes = R.drawable.bg_morgana
                    ),
                    CampaignStage(
                        id = 102, chapterId = 1, stageNumber = 2,
                        title = "1-2 Лагерь бандитов",
                        recommendedPower = 10,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("brunhilde", "olga", "berta", "witch"),
                        enemyLevels = listOf(1, 1, 1, 1),
                        stars = 0, isUnlocked = false,
                        rewardGold = 225, rewardGems = 15, rewardExp = 45,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_brunhilde
                    ),
                    CampaignStage(
                        id = 103, chapterId = 1, stageNumber = 3,
                        title = "1-3 Чаща Безымянного леса",
                        recommendedPower = 16,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("brunhilde", "olga", "iva", "witch"),
                        enemyLevels = listOf(2, 2, 2, 2),
                        stars = 0, isUnlocked = false,
                        rewardGold = 280, rewardGems = 18, rewardExp = 60,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_iva
                    ),
                    CampaignStage(
                        id = 104, chapterId = 1, stageNumber = 4,
                        title = "1-4 На границе Южного королевства",
                        recommendedPower = 22,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("brunhilde", "olga", "iva", "witch"),
                        enemyLevels = listOf(2, 2, 2, 2),
                        stars = 0, isUnlocked = false,
                        rewardGold = 340, rewardGems = 20, rewardExp = 75,
                        rewardArtifactId = "art_base_4",
                        battleBgRes = R.drawable.bg_iva
                    ),
                    CampaignStage(
                        id = 105, chapterId = 1, stageNumber = 5,
                        title = "1-5 У стен Рэдвола",
                        recommendedPower = 30,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("brunhilde", "olga", "iva", "berta"),
                        enemyLevels = listOf(3, 3, 3, 3),
                        stars = 0, isUnlocked = false,
                        rewardGold = 420, rewardGems = 25, rewardExp = 90,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_alisa
                    ),
                    CampaignStage(
                        id = 106, chapterId = 1, stageNumber = 6,
                        title = "1-6 У сокровищницы Людвига",
                        recommendedPower = 40,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("alisa", "brunhilde", "iva", "witch"),
                        enemyLevels = listOf(3, 2, 2, 2),
                        stars = 0, isUnlocked = false, isBoss = true,
                        rewardGold = 560, rewardGems = 35, rewardExp = 120,
                        rewardShardsHeroId = "alisa",
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_alisa_2
                    )
                )
            ),
            CampaignChapter(
                id = 2,
                name = "Глава 2. Амулет Востока",
                subtitle = "Восточное королевство и королева Власта",
                stages = listOf(
                    CampaignStage(
                        id = 201, chapterId = 2, stageNumber = 1,
                        title = "2-1 В южных лесах",
                        recommendedPower = 48,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("alisa", "brunhilde", "witch", "iva"),
                        enemyLevels = listOf(3, 3, 3, 3),
                        stars = 0, isUnlocked = false,
                        rewardGold = 450, rewardGems = 25, rewardExp = 100,
                        rewardArtifactId = "art_spec_7",
                        battleBgRes = R.drawable.bg_iva
                    ),
                    CampaignStage(
                        id = 202, chapterId = 2, stageNumber = 2,
                        title = "2-2 На границе с Востоком",
                        recommendedPower = 54,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("alisa", "olga", "witch", "iva"),
                        enemyLevels = listOf(3, 3, 3, 3),
                        stars = 0, isUnlocked = false,
                        rewardGold = 520, rewardGems = 30, rewardExp = 115,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_vasilisa
                    ),
                    CampaignStage(
                        id = 203, chapterId = 2, stageNumber = 3,
                        title = "2-3 На подступах к Розвилю",
                        recommendedPower = 60,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("alisa", "brunhilde", "witch", "iva"),
                        enemyLevels = listOf(4, 4, 4, 4),
                        stars = 0, isUnlocked = false,
                        rewardGold = 600, rewardGems = 35, rewardExp = 135,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_vasilisa
                    ),
                    CampaignStage(
                        id = 204, chapterId = 2, stageNumber = 4,
                        title = "2-4 В стенах Розвиля",
                        recommendedPower = 66,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("alisa", "olga", "witch", "iva"),
                        enemyLevels = listOf(4, 4, 4, 4),
                        stars = 0, isUnlocked = false,
                        rewardGold = 700, rewardGems = 40, rewardExp = 155,
                        rewardArtifactId = "art_spec_1",
                        battleBgRes = R.drawable.bg_griselda_2
                    ),
                    CampaignStage(
                        id = 205, chapterId = 2, stageNumber = 5,
                        title = "2-5 В пекарне Гризельды",
                        recommendedPower = 72,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("olga", "brunhilde", "baker", "iva"),
                        enemyLevels = listOf(4, 4, 4, 4),
                        stars = 0, isUnlocked = false,
                        rewardGold = 800, rewardGems = 45, rewardExp = 180,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_griselda
                    ),
                    CampaignStage(
                        id = 206, chapterId = 2, stageNumber = 6,
                        title = "2-6 В замке королевы Власты",
                        recommendedPower = 80,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("vlasta", "olga", "baker", "mi_and_na"),
                        enemyLevels = listOf(5, 3, 3, 3),
                        stars = 0, isUnlocked = false, isBoss = true,
                        rewardGold = 975, rewardGems = 60, rewardExp = 240,
                        rewardShardsHeroId = "vlasta",
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_vlasta
                    )
                )
            ),
            CampaignChapter(
                id = 3,
                name = "Глава 3. Амулет Севера",
                subtitle = "Северное царство и ледяной трон Изольды",
                stages = listOf(
                    CampaignStage(
                        id = 301, chapterId = 3, stageNumber = 1,
                        title = "3-1 Восточная пустыня",
                        recommendedPower = 85,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("vlasta", "alisa", "baker", "iva"),
                        enemyLevels = listOf(4, 4, 4, 4),
                        stars = 0, isUnlocked = false,
                        rewardGold = 825, rewardGems = 50, rewardExp = 200,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_vasilisa
                    ),
                    CampaignStage(
                        id = 302, chapterId = 3, stageNumber = 2,
                        title = "3-2 На границе Северного царства",
                        recommendedPower = 88,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("vlasta", "alisa", "baker", "iva"),
                        enemyLevels = listOf(4, 4, 4, 4),
                        stars = 0, isUnlocked = false,
                        rewardGold = 950, rewardGems = 55, rewardExp = 230,
                        rewardArtifactId = "art_rare_6",
                        battleBgRes = R.drawable.bg_leya
                    ),
                    CampaignStage(
                        id = 303, chapterId = 3, stageNumber = 3,
                        title = "3-3 У стен Туверска",
                        recommendedPower = 92,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("vlasta", "alisa", "baker", "iva"),
                        enemyLevels = listOf(5, 5, 5, 5),
                        stars = 0, isUnlocked = false,
                        rewardGold = 1100, rewardGems = 60, rewardExp = 260,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_olga
                    ),
                    CampaignStage(
                        id = 304, chapterId = 3, stageNumber = 4,
                        title = "3-4 В Столице царства",
                        recommendedPower = 96,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("vlasta", "olga", "baker", "mi_and_na"),
                        enemyLevels = listOf(5, 5, 5, 5),
                        stars = 0, isUnlocked = false,
                        rewardGold = 1250, rewardGems = 65, rewardExp = 290,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_mi_and_na
                    ),
                    CampaignStage(
                        id = 305, chapterId = 3, stageNumber = 5,
                        title = "3-5 На пути к Гледену",
                        recommendedPower = 100,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("vlasta", "alisa", "mi_and_na", "baker"),
                        enemyLevels = listOf(5, 5, 5, 5),
                        stars = 0, isUnlocked = false,
                        rewardGold = 1400, rewardGems = 70, rewardExp = 320,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_isolda
                    ),
                    CampaignStage(
                        id = 306, chapterId = 3, stageNumber = 6,
                        title = "3-6 В логове Изольды",
                        recommendedPower = 106,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("isolda", "vlasta", "mi_and_na", "baker"),
                        enemyLevels = listOf(5, 5, 3, 4),
                        stars = 0, isUnlocked = false, isBoss = true,
                        rewardGold = 1800, rewardGems = 100, rewardExp = 450,
                        rewardShardsHeroId = "isolda",
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_isolda_2
                    )
                )
            ),
            CampaignChapter(
                id = 4,
                name = "Глава 4. Адский Разлом",
                subtitle = "Храмы богов и логово повелительницы тьмы",
                stages = listOf(
                    CampaignStage(
                        id = 401, chapterId = 4, stageNumber = 1,
                        title = "4-1 В лесах Северного Царства",
                        recommendedPower = 110,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("leya", "alisa", "isolda", "mi_and_na"),
                        enemyLevels = listOf(5, 5, 5, 5),
                        stars = 0, isUnlocked = false,
                        rewardGold = 1400, rewardGems = 70, rewardExp = 300,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_leya
                    ),
                    CampaignStage(
                        id = 402, chapterId = 4, stageNumber = 2,
                        title = "4-2 У Болотного Моря",
                        recommendedPower = 115,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("brunhilde", "olga", "witch", "leya"),
                        enemyLevels = listOf(5, 5, 5, 5),
                        stars = 0, isUnlocked = false,
                        rewardGold = 1550, rewardGems = 75, rewardExp = 330,
                        rewardArtifactId = "art_mythic_11",
                        battleBgRes = R.drawable.bg_morgana
                    ),
                    CampaignStage(
                        id = 403, chapterId = 4, stageNumber = 3,
                        title = "4-3 Западные чертоги",
                        recommendedPower = 120,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("brunhilde", "leya", "isolda", "iva"),
                        enemyLevels = listOf(6, 6, 5, 5),
                        stars = 0, isUnlocked = false,
                        rewardGold = 1700, rewardGems = 80, rewardExp = 360,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_brunhilde
                    ),
                    CampaignStage(
                        id = 404, chapterId = 4, stageNumber = 4,
                        title = "4-4 Храм Света",
                        recommendedPower = 125,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("evanda", "vlasta", "isolda", "iva"),
                        enemyLevels = listOf(6, 5, 5, 5),
                        stars = 0, isUnlocked = false,
                        rewardGold = 1900, rewardGems = 90, rewardExp = 400,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_evanda
                    ),
                    CampaignStage(
                        id = 405, chapterId = 4, stageNumber = 5,
                        title = "4-5 Храм Огня",
                        recommendedPower = 130,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("vlasta", "brunhilde", "berta", "isolda"),
                        enemyLevels = listOf(6, 6, 5, 5),
                        stars = 0, isUnlocked = false,
                        rewardGold = 2100, rewardGems = 100, rewardExp = 450,
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_berta
                    ),
                    CampaignStage(
                        id = 406, chapterId = 4, stageNumber = 6,
                        title = "4-6 У подножья Патрея",
                        recommendedPower = 140,
                        requiredPlayerLevel = 1,
                        enemyHeroIds = listOf("evanda", "vlasta", "isolda", "demoness"),
                        enemyLevels = listOf(3, 5, 5, 8),
                        stars = 0, isUnlocked = false, isBoss = true,
                        rewardGold = 2600, rewardGems = 150, rewardExp = 600,
                        rewardShardsHeroId = "demoness",
                        rewardArtifactId = null,
                        battleBgRes = R.drawable.bg_lilith
                    )
                )
            )
        )
        return rawChapters.map { chapter ->
            chapter.copy(
                stages = chapter.stages.map { stage ->
                    val targetAtk = getStageTargetAtk(stage.id)
                    stage.copy(totalEnemyAtk = targetAtk, recommendedPower = targetAtk)
                }
            )
        }
    }

    fun generateArenaOpponents(
        playerLevel: Int,
        playerTrophies: Int,
        playerDeckAtk: Int = 0,
        playerDeckHp: Int = 0
    ): List<ArenaBot> {
        val botTitles = listOf(
            "Гроза Севера", "Чародейка Озер", "Несокрушимая", "Жрица Рассвета",
            "Повелительница Бурь", "Стальной Кулак", "Драконий Всадник", "Пышная Ведьма",
            "Хранительница Огня", "Лесная Охотница"
        )
        val hardBotTitles = listOf(
            "Повелительница Арены", "Легенда Колизея", "Непобедимая Валькирия"
        )
        // Strictly separate front row (Melee or Hybrid) and back row (Ranged or Hybrid): Melee is NEVER placed in back row slots 2 and 3!
        val frontRowCandidates = listOf("brunhilde", "olga", "vlasta", "alisa", "evanda", "demoness", "baker", "mi_and_na", "leya")
        val backRowCandidates = listOf("berta", "witch", "isolda", "iva", "demoness", "baker", "mi_and_na", "leya")

        val heroNameMap = mapOf(
            "brunhilde" to "Брунгильда",
            "olga" to "Ольга",
            "berta" to "Берта",
            "witch" to "Моргана",
            "alisa" to "Алиса",
            "iva" to "Ива",
            "mi_and_na" to "Ми и На",
            "leya" to "Лея",
            "vlasta" to "Власта",
            "evanda" to "Эванда",
            "isolda" to "Изольда",
            "demoness" to "Лилит",
            "baker" to "Гризельда"
        )

        val baseLevel = playerLevel.coerceAtLeast(1)
        val effectivePlayerAtk = if (playerDeckAtk > 0) playerDeckAtk else (baseLevel * 12 + 24)
        val effectivePlayerHp = if (playerDeckHp > 0) playerDeckHp else (baseLevel * 35 + 80)

        val regularBots = (0..2).map { index ->
            val ratingOffset = (-20..30).random() + (index * 15)
            val botRating = (playerTrophies + ratingOffset).coerceAtLeast(50)

            val frontTeam = frontRowCandidates.shuffled().take(2)
            val backTeam = backRowCandidates.shuffled().take(2)
            val teamIds = frontTeam + backTeam
            val teamLevels = teamIds.map { baseLevel }

            val atkMultiplier = (1.10 + (index * 0.04)) * 0.90 * 1.05 // +5% ATK proportionally to previous Arena regular bots
            val hpMultiplier = (1.00 + (index * 0.04)) * 0.85 * 1.05  // +5% HP proportionally to previous Arena regular bots

            // Name of the opponent MUST be the name of one of the 4 cards in the bot's deck!
            val chosenHeroId = teamIds.random()
            val botName = heroNameMap[chosenHeroId] ?: "Героиня"
            val botTitle = botTitles.random()

            ArenaBot(
                name = botName,
                title = botTitle,
                rating = botRating,
                avatarRes = R.drawable.app_logo,
                heroIds = teamIds,
                heroLevels = teamLevels,
                targetTotalAtk = kotlin.math.ceil(effectivePlayerAtk * atkMultiplier).toInt(),
                targetTotalHp = kotlin.math.ceil(effectivePlayerHp * hpMultiplier).toInt(),
                isHardChallenge = false,
                rewardGold = null,
                rewardGems = 25,
                rewardExp = 60
            )
        }

        // Additional Hard Challenger Bot: +5% ATK/HP over previous (+41.75% ATK, +20.75% HP), elevated rewards (500 coins, 30 gems, 100 EXP)
        val hardFrontTeam = frontRowCandidates.shuffled().take(2)
        val hardBackTeam = backRowCandidates.shuffled().take(2)
        val hardTeamIds = hardFrontTeam + hardBackTeam
        val hardTeamLevels = hardTeamIds.map { (baseLevel + 1).coerceAtMost(MAX_HERO_LEVEL) }
        val hardChosenHeroId = hardTeamIds.random()
        val hardBot = ArenaBot(
            name = heroNameMap[hardChosenHeroId] ?: "Героиня",
            title = hardBotTitles.random(),
            rating = (playerTrophies + 65 + (0..25).random()).coerceAtLeast(80),
            avatarRes = R.drawable.app_logo,
            heroIds = hardTeamIds,
            heroLevels = hardTeamLevels,
            targetTotalAtk = kotlin.math.ceil(effectivePlayerAtk * 1.35 * 1.05).toInt(),
            targetTotalHp = kotlin.math.ceil(effectivePlayerHp * 1.15 * 1.05).toInt(),
            isHardChallenge = true,
            rewardGold = 500,
            rewardGems = 30,
            rewardExp = 100
        )

        return regularBots + hardBot
    }

    fun calculateColiseumRewards(battleNumber: Int): Pair<Int, Int> {
        val n = battleNumber.coerceAtLeast(1)
        val coins = when (n) {
            1 -> 100
            2 -> 250
            3 -> 450
            4 -> 700
            5 -> 1000
            else -> {
                var c = 1000
                for (step in 6..n) {
                    c += (step + 1) * 50
                }
                c
            }
        }
        val gems = when (n) {
            1 -> 10
            2 -> 20
            3 -> 35
            4 -> 55
            5 -> 80
            6 -> 100
            else -> 100 + (n - 6) * 20
        }
        return coins to gems
    }

    fun calculateColiseumScalingMultiplier(battleNumber: Int): Double {
        return when (battleNumber.coerceAtLeast(1)) {
            1 -> 0.30 // -70% weaker than player
            2 -> 0.45 // -55% weaker than player
            3 -> 0.70 // -30% weaker than player
            4 -> 0.85 // -15% weaker than player
            5 -> 1.00 // Same level as player
            else -> 1.00 + (battleNumber - 5) * 0.15 // +15%, +30%, ...
        }
    }

    fun generateColiseumOpponent(
        battleNumber: Int,
        playerDeckAtk: Int,
        playerDeckHp: Int,
        avgHeroLevel: Int,
        unlockedSecretHeroIds: Set<String> = emptySet(),
        secretSpawnRoll: Int = Random.nextInt(100)
    ): ArenaBot {
        val frontRowCandidates = listOf("brunhilde", "olga", "vlasta", "alisa", "evanda", "demoness", "baker", "mi_and_na", "leya")
        val backRowCandidates = listOf("berta", "witch", "isolda", "iva", "demoness", "baker", "mi_and_na", "leya")
        val heroNameMap = mapOf(
            "brunhilde" to "Брунгильда",
            "olga" to "Ольга",
            "berta" to "Берта",
            "witch" to "Моргана",
            "alisa" to "Алиса",
            "iva" to "Ива",
            "mi_and_na" to "Ми и На",
            "leya" to "Лея",
            "vlasta" to "Власта",
            "evanda" to "Эванда",
            "isolda" to "Изольда",
            "demoness" to "Лилит",
            "baker" to "Гризельда",
            "elios" to "Эллиос",
            "vasilisa" to "Василиса",
            "kadru" to "Кадру"
        )
        val frontTeam = frontRowCandidates.shuffled().take(2).toMutableList()
        val backTeam = backRowCandidates.filter { it !in frontTeam }.shuffled().take(2).toMutableList()
        while (backTeam.size < 2) {
            backTeam.add(backRowCandidates.first { it !in backTeam })
        }

        val eligibleSecrets = unlockedSecretHeroIds.filter { it == "elios" || it == "vasilisa" || it == "kadru" }
        if (battleNumber >= 6 && eligibleSecrets.isNotEmpty() && secretSpawnRoll < 30) {
            val chosenSecret = eligibleSecrets.random()
            val slotChoice = if (chosenSecret == "kadru") Random.nextInt(2) else Random.nextInt(4)
            if (slotChoice < 2) {
                frontTeam[slotChoice] = chosenSecret
            } else {
                backTeam[slotChoice - 2] = chosenSecret
            }
        }
        val teamIds = frontTeam + backTeam

        val effectiveLevel = (avgHeroLevel.coerceAtLeast(1) + ((battleNumber - 5).coerceAtLeast(0) / 2))
            .coerceIn(1, MAX_HERO_LEVEL)
        val teamLevels = teamIds.map { effectiveLevel }

        val effectivePlayerAtk = playerDeckAtk.coerceAtLeast(40)
        val effectivePlayerHp = playerDeckHp.coerceAtLeast(120)
        val mult = calculateColiseumScalingMultiplier(battleNumber)
        val targetAtk = kotlin.math.ceil(effectivePlayerAtk * mult).toInt().coerceAtLeast(4)
        val targetHp = kotlin.math.ceil(effectivePlayerHp * mult).toInt().coerceAtLeast(4)
        val (rewardCoins, rewardGems) = calculateColiseumRewards(battleNumber)

        val chosenHeroId = teamIds.random()
        val botName = heroNameMap[chosenHeroId] ?: "Героиня"

        return ArenaBot(
            name = botName,
            title = "Боец Колизея №$battleNumber",
            rating = 1000 + battleNumber * 50,
            avatarRes = R.drawable.app_logo,
            heroIds = teamIds,
            heroLevels = teamLevels,
            targetTotalAtk = targetAtk,
            targetTotalHp = targetHp,
            isHardChallenge = battleNumber >= 3,
            rewardGold = rewardCoins,
            rewardGems = rewardGems,
            rewardExp = 0
        )
    }

    private fun buildRuleCompliantTowerTeam(
        rule: TowerDayRule,
        floor: Int,
        baseTemplates: List<HeroCard>,
        wednesdayElement: Element?
    ): List<String> {
        val rulePool = when (rule) {
            TowerDayRule.MONDAY_STEEL -> baseTemplates.filter {
                it.heroType == HeroType.MELEE || it.heroType == HeroType.HYBRID
            }.ifEmpty { baseTemplates }
            TowerDayRule.FRIDAY_COMMONERS_VALOR -> baseTemplates.filter {
                it.rarity == CardRarity.BASIC || it.rarity == CardRarity.SPECIAL
            }.ifEmpty { baseTemplates }
            else -> baseTemplates
        }

        val (frontPriority, frontFallback) = when (rule) {
            TowerDayRule.WEDNESDAY_ELEMENTAL_STORM -> {
                val matching = rulePool.filter { it.heroType != HeroType.RANGED && it.element == wednesdayElement }
                val others = rulePool.filter { it.heroType != HeroType.RANGED && it.element != wednesdayElement }
                matching to others
            }
            TowerDayRule.SATURDAY_BOSS_SIEGE -> {
                val highTier = rulePool.filter { it.heroType != HeroType.RANGED && (it.rarity == CardRarity.RARE || it.rarity == CardRarity.MYTHIC) }
                val others = rulePool.filter { it.heroType != HeroType.RANGED && it !in highTier }
                highTier to others
            }
            else -> rulePool.filter { it.heroType != HeroType.RANGED } to emptyList()
        }

        val (backPriority, backFallback) = when (rule) {
            TowerDayRule.MONDAY_STEEL -> rulePool.filter { it.heroType == HeroType.HYBRID } to emptyList()
            TowerDayRule.TUESDAY_ARCANE_VOLLEY -> {
                val ranged = rulePool.filter { it.heroType == HeroType.RANGED }
                val hybrid = rulePool.filter { it.heroType == HeroType.HYBRID }
                ranged to hybrid
            }
            TowerDayRule.WEDNESDAY_ELEMENTAL_STORM -> {
                val matching = rulePool.filter { it.heroType != HeroType.MELEE && it.element == wednesdayElement }
                val others = rulePool.filter { it.heroType != HeroType.MELEE && it.element != wednesdayElement }
                matching to others
            }
            TowerDayRule.SATURDAY_BOSS_SIEGE -> {
                val highTier = rulePool.filter { it.heroType != HeroType.MELEE && (it.rarity == CardRarity.RARE || it.rarity == CardRarity.MYTHIC) }
                val others = rulePool.filter { it.heroType != HeroType.MELEE && it !in highTier }
                highTier to others
            }
            else -> rulePool.filter { it.heroType != HeroType.MELEE } to emptyList()
        }

        val safeFront = (frontPriority + frontFallback).ifEmpty { rulePool }
        val safeBack = (backPriority + backFallback).ifEmpty {
            rulePool.filter { it.heroType != HeroType.MELEE }.ifEmpty { rulePool }
        }

        val dayIndex = rule.ordinal
        val picked = mutableListOf<String>()
        fun pickNext(priority: List<HeroCard>, fallback: List<HeroCard>, combinedSafe: List<HeroCard>, offset: Int): String {
            val priorityFresh = priority.filter { it.id !in picked }
            val fallbackFresh = fallback.filter { it.id !in picked }
            val source = when {
                priorityFresh.isNotEmpty() -> priorityFresh
                fallbackFresh.isNotEmpty() -> fallbackFresh
                priority.isNotEmpty() -> priority
                else -> combinedSafe
            }
            return source[(dayIndex + floor * 2 + offset) % source.size].id
        }

        // Front row (slots 0..1)
        picked.add(pickNext(frontPriority, frontFallback, safeFront, 0))
        picked.add(pickNext(frontPriority, frontFallback, safeFront, 1))
        // Back row (slots 2..3) — never Melee
        picked.add(pickNext(backPriority, backFallback, safeBack, 2))
        picked.add(pickNext(backPriority, backFallback, safeBack, 3))
        return picked
    }

    fun getTowerFloorsForDay(
        rule: TowerDayRule,
        playerLevel: Int,
        avgDeckLevel: Int,
        unlockedSecretHeroIds: Set<String> = emptySet(),
        wednesdayElement: Element? = null
    ): List<TowerFloorPreview> {
        val baseTemplates = getInitialHeroCards().filter { !it.isSecret || it.id in unlockedSecretHeroIds }
        val effectiveBaseLevel = maxOf(avgDeckLevel, ((playerLevel + 1) / 2).coerceAtLeast(1)).coerceIn(1, MAX_HERO_LEVEL - 1)
        val activeWedElement = wednesdayElement ?: resolveWednesdayTowerElement()

        val titlesRu = listOf(
            "Первый ярус", "Зал рун", "Средний бастион", "Чертоги бури", "Вершина шпиля"
        )
        val titlesEn = listOf(
            "Lower Sanctum", "Rune Hall", "Middle Bastion", "Storm Chambers", "Spire Summit"
        )

        return (1..TOWER_TOTAL_FLOORS).map { floor ->
            val levelOffset = floor - 2
            val baseEnemyLvl = (effectiveBaseLevel + levelOffset).coerceIn(1, MAX_HERO_LEVEL)
            val isBossFloor = floor == TOWER_TOTAL_FLOORS || rule == TowerDayRule.SATURDAY_BOSS_SIEGE
            val enemyIds = buildRuleCompliantTowerTeam(rule, floor, baseTemplates, activeWedElement)
            val enemyLevels = listOf(
                baseEnemyLvl,
                (baseEnemyLvl + (if (floor >= 3) 1 else 0)).coerceAtMost(MAX_HERO_LEVEL),
                baseEnemyLvl,
                (baseEnemyLvl + (if (isBossFloor) 1 else 0)).coerceAtMost(MAX_HERO_LEVEL)
            )
            val rawGold = 180 + floor * 70 + playerLevel * 12
            val rewardGold = if (rule == TowerDayRule.SUNDAY_GOLDEN_ASSAULT) rawGold * 2 else rawGold
            val rawGems = 12 + floor * 6
            val rewardGems = if (rule == TowerDayRule.SATURDAY_BOSS_SIEGE) rawGems + 10 else rawGems
            val rewardExp = 45 + floor * 20
            val bonusShards = if (floor == TOWER_TOTAL_FLOORS) 10 else 0

            TowerFloorPreview(
                floor = floor,
                titleRu = if (isBossFloor) "${titlesRu[floor - 1]} (Босс)" else titlesRu[floor - 1],
                titleEn = if (isBossFloor) "${titlesEn[floor - 1]} (Boss)" else titlesEn[floor - 1],
                enemyHeroIds = enemyIds,
                enemyLevels = enemyLevels,
                rewardGold = rewardGold,
                rewardGems = rewardGems,
                rewardExp = rewardExp,
                bonusShards = bonusShards,
                isBoss = isBossFloor
            )
        }
    }

    fun getTowerRoofFloorForDay(
        rule: TowerDayRule,
        playerLevel: Int,
        avgDeckLevel: Int,
        unlockedSecretHeroIds: Set<String> = emptySet(),
        wednesdayElement: Element? = null
    ): TowerFloorPreview {
        val mainFloors = getTowerFloorsForDay(
            rule = rule,
            playerLevel = playerLevel,
            avgDeckLevel = avgDeckLevel,
            unlockedSecretHeroIds = unlockedSecretHeroIds,
            wednesdayElement = wednesdayElement
        )
        val floor5 = mainFloors.last()
        val baseTemplates = getInitialHeroCards().filter { !it.isSecret || it.id in unlockedSecretHeroIds }
        val activeWedElement = wednesdayElement ?: resolveWednesdayTowerElement()
        val effectiveBaseLevel = maxOf(avgDeckLevel, ((playerLevel + 1) / 2).coerceAtLeast(1)).coerceIn(1, MAX_HERO_LEVEL)
        val roofLevel = (effectiveBaseLevel + 3).coerceIn(1, MAX_HERO_LEVEL)
        val enemyIds = buildRuleCompliantTowerTeam(rule, TOWER_ROOF_FLOOR, baseTemplates, activeWedElement)

        return TowerFloorPreview(
            floor = TOWER_ROOF_FLOOR,
            titleRu = "Крыша (Секретный этап)",
            titleEn = "The Roof (Secret Floor)",
            enemyHeroIds = enemyIds,
            enemyLevels = listOf(roofLevel, roofLevel, roofLevel, roofLevel),
            rewardGold = floor5.rewardGold * 2,
            rewardGems = floor5.rewardGems * 2,
            rewardExp = floor5.rewardExp * 2,
            bonusShards = (floor5.bonusShards * 2).coerceAtLeast(20),
            isBoss = true
        )
    }

    fun savePlayerProfile(profile: PlayerProfile) {
        prefs.edit().apply {
            putString("profile_name", profile.name)
            putInt("profile_level", profile.level)
            putInt("profile_exp", profile.exp)
            putInt("profile_max_exp", profile.maxExp)
            putInt("profile_gold", profile.gold)
            putInt("profile_gems", profile.gems)
            putInt("profile_keys", profile.arenaKeys)
            putInt("profile_trophies", profile.trophies)
            putString("active_deck", profile.activeDeckHeroIds.joinToString(",") { it ?: "null" })
            putBoolean("google_synced", profile.isGoogleSynced)
            putString("google_email", profile.googleAccountEmail ?: "")
            putLong("last_sync", profile.lastSyncTimestamp)
            putLong("last_arena_key_regen", profile.lastArenaKeyRegenTimestamp)
            putString("last_daily_claim_date", profile.lastDailyClaimDate ?: "")
            putLong("last_daily_claim_time", profile.lastDailyClaimTimestamp)
            putInt("daily_streak", profile.dailyStreak)
            putLong("max_seen_timestamp", profile.maxSeenTimestamp)
            putBoolean("tutorial_completed", profile.isTutorialCompleted)
            putBoolean("story_read", profile.isStoryRead)
            putInt("battles_won", profile.battlesWon)
            putInt("battles_lost", profile.battlesLost)
            putString("found_amulets", profile.foundAmulets.joinToString(","))
            putBoolean("campaign_100_achievement_shown", profile.isCampaign100AchievementShown)
            putInt("last_celebrated_level", profile.lastCelebratedLevel)
            putInt("coliseum_won", profile.coliseumWon)
            putInt("coliseum_lost", profile.coliseumLost)
            putLong("last_coliseum_timestamp", profile.lastColiseumTimestamp)
            putString("coliseum_entries_date", profile.coliseumEntriesDate ?: "")
            putInt("coliseum_entries_used_today", profile.coliseumEntriesUsedToday)
            putBoolean("coliseum_tutorial_completed", profile.isColiseumTutorialCompleted)
            putString("tower_progress_date", profile.towerProgressDate ?: "")
            putInt("tower_cleared_floor_today", profile.towerClearedFloorToday)
            putInt("tower_attempts_used_today", profile.towerAttemptsUsedToday)
            putInt("tower_total_wins", profile.towerTotalWins)
            putBoolean("tower_tutorial_completed", profile.isTowerTutorialCompleted)
            putString("tower_floor_stars_today", profile.towerFloorStarsToday.joinToString(","))
            putBoolean("tower_roof_cleared_today", profile.towerRoofClearedToday)
            putInt("tower_roof_stars_today", profile.towerRoofStarsToday)
            putInt("tower_damaged_floor", profile.towerDamagedFloor)
            putString("tower_saved_enemy_hps", profile.towerSavedEnemyHps.joinToString(","))
            putString("tower_deck", profile.towerDeckHeroIds.joinToString(",") { it ?: "null" })
            putString("viewed_gallery_keys", profile.viewedGalleryImageKeys.joinToString(","))
            putBoolean("sound_muted", profile.isSoundMuted)
            putInt("skipped_update_version_code", profile.skippedUpdateVersionCode)
            putLong("last_career_auto_battle_timestamp", profile.lastCareerAutoBattleTimestamp)
            apply()
        }
    }

    fun loadPlayerProfile(): PlayerProfile {
        if (!prefs.contains("profile_name")) {
            return PlayerProfile(
                isSoundMuted = prefs.getBoolean("sound_muted", false),
                skippedUpdateVersionCode = prefs.getInt("skipped_update_version_code", 0)
            )
        }
        val name = prefs.getString("profile_name", "Хранительница") ?: "Хранительница"
        val level = prefs.getInt("profile_level", 1)
        val exp = prefs.getInt("profile_exp", 0)
        val maxExp = prefs.getInt("profile_max_exp", 100)
        val gold = prefs.getInt("profile_gold", 2000)
        val gems = prefs.getInt("profile_gems", 100)
        val keys = prefs.getInt("profile_keys", 5)
        val trophies = prefs.getInt("profile_trophies", 100)
        val deckStr = prefs.getString("active_deck", "brunhilde,olga,berta,witch") ?: "brunhilde,olga,berta,witch"
        val activeDeck = deckStr.split(",").map { if (it == "null" || it.isBlank()) null else it }
        val isGoogleSynced = prefs.getBoolean("google_synced", false)
        val googleEmail = prefs.getString("google_email", null)?.takeIf { it.isNotBlank() }
        val lastSync = prefs.getLong("last_sync", 0L)
        val lastRegen = prefs.getLong("last_arena_key_regen", System.currentTimeMillis())
        val lastDailyClaimDate = prefs.getString("last_daily_claim_date", null)?.takeIf { it.isNotBlank() }
        val lastDailyClaimTime = prefs.getLong("last_daily_claim_time", 0L)
        val dailyStreak = prefs.getInt("daily_streak", 0)
        val maxSeenTimestamp = prefs.getLong("max_seen_timestamp", System.currentTimeMillis())
        val tutorialCompleted = prefs.getBoolean("tutorial_completed", false)
        val storyRead = prefs.getBoolean("story_read", false)
        val battlesWon = prefs.getInt("battles_won", 0)
        val battlesLost = prefs.getInt("battles_lost", 0)
        val foundAmuletsStr = prefs.getString("found_amulets", "") ?: ""
        val foundAmulets = if (foundAmuletsStr.isBlank()) emptyList() else foundAmuletsStr.split(",").filter { it.isNotBlank() }
        val campaign100AchievementShown = prefs.getBoolean("campaign_100_achievement_shown", false)
        val lastCelebratedLevel = if (prefs.contains("last_celebrated_level")) {
            prefs.getInt("last_celebrated_level", level)
        } else {
            level
        }
        val coliseumWon = prefs.getInt("coliseum_won", 0)
        val coliseumLost = prefs.getInt("coliseum_lost", 0)
        val lastColiseumTimestamp = prefs.getLong("last_coliseum_timestamp", 0L)
        val coliseumEntriesDate = prefs.getString("coliseum_entries_date", null)?.takeIf { it.isNotBlank() }
        val coliseumEntriesUsedToday = prefs.getInt("coliseum_entries_used_today", 0)
        val coliseumTutorialCompleted = prefs.getBoolean("coliseum_tutorial_completed", false)
        val towerProgressDate = prefs.getString("tower_progress_date", null)?.takeIf { it.isNotBlank() }
        val towerClearedFloorToday = prefs.getInt("tower_cleared_floor_today", 0)
        val towerAttemptsUsedToday = prefs.getInt("tower_attempts_used_today", 0)
        val towerTotalWins = prefs.getInt("tower_total_wins", 0)
        val towerTutorialCompleted = prefs.getBoolean("tower_tutorial_completed", false)
        val towerStarsStr = prefs.getString("tower_floor_stars_today", "") ?: ""
        val towerFloorStarsToday = if (towerStarsStr.isBlank()) emptyList() else {
            towerStarsStr.split(",").mapNotNull { it.toIntOrNull() }
        }
        val towerRoofClearedToday = prefs.getBoolean("tower_roof_cleared_today", false)
        val towerRoofStarsToday = prefs.getInt("tower_roof_stars_today", 0)
        val towerDamagedFloor = prefs.getInt("tower_damaged_floor", 0)
        val towerSavedHpsStr = prefs.getString("tower_saved_enemy_hps", "") ?: ""
        val towerSavedEnemyHps = if (towerSavedHpsStr.isBlank()) emptyList() else {
            towerSavedHpsStr.split(",").mapNotNull { it.toIntOrNull() }
        }
        val towerDeckStr = prefs.getString("tower_deck", "") ?: ""
        val towerDeckHeroIds = if (towerDeckStr.isBlank()) emptyList() else {
            towerDeckStr.split(",").map { if (it == "null" || it.isBlank()) null else it }
        }
        val viewedGalleryStr = prefs.getString("viewed_gallery_keys", "") ?: ""
        val viewedGalleryImageKeys = if (viewedGalleryStr.isBlank()) emptySet() else {
            viewedGalleryStr.split(",").filter { it.isNotBlank() }.toSet()
        }

        return PlayerProfile(
            name = name,
            level = level,
            exp = exp,
            maxExp = maxExp,
            gold = gold,
            gems = gems,
            arenaKeys = keys,
            trophies = trophies,
            activeDeckHeroIds = activeDeck,
            isGoogleSynced = isGoogleSynced,
            googleAccountEmail = googleEmail,
            lastSyncTimestamp = lastSync,
            lastArenaKeyRegenTimestamp = lastRegen,
            lastDailyClaimDate = lastDailyClaimDate,
            lastDailyClaimTimestamp = lastDailyClaimTime,
            dailyStreak = dailyStreak,
            maxSeenTimestamp = maxSeenTimestamp,
            isTutorialCompleted = tutorialCompleted,
            isStoryRead = storyRead,
            battlesWon = battlesWon,
            battlesLost = battlesLost,
            foundAmulets = foundAmulets,
            isCampaign100AchievementShown = campaign100AchievementShown,
            lastCelebratedLevel = lastCelebratedLevel,
            coliseumWon = coliseumWon,
            coliseumLost = coliseumLost,
            lastColiseumTimestamp = lastColiseumTimestamp,
            coliseumEntriesDate = coliseumEntriesDate,
            coliseumEntriesUsedToday = coliseumEntriesUsedToday,
            isColiseumTutorialCompleted = coliseumTutorialCompleted,
            towerProgressDate = towerProgressDate,
            towerClearedFloorToday = towerClearedFloorToday,
            towerAttemptsUsedToday = towerAttemptsUsedToday,
            towerTotalWins = towerTotalWins,
            isTowerTutorialCompleted = towerTutorialCompleted,
            towerFloorStarsToday = towerFloorStarsToday,
            towerRoofClearedToday = towerRoofClearedToday,
            towerRoofStarsToday = towerRoofStarsToday,
            towerDamagedFloor = towerDamagedFloor,
            towerSavedEnemyHps = towerSavedEnemyHps,
            towerDeckHeroIds = towerDeckHeroIds,
            viewedGalleryImageKeys = viewedGalleryImageKeys,
            isSoundMuted = prefs.getBoolean("sound_muted", false),
            skippedUpdateVersionCode = prefs.getInt("skipped_update_version_code", 0),
            lastCareerAutoBattleTimestamp = prefs.getLong("last_career_auto_battle_timestamp", 0L)
        )
    }

    fun saveArtifacts(artifacts: List<Artifact>) {
        val jsonArray = org.json.JSONArray()
        for (art in artifacts) {
            val obj = org.json.JSONObject().apply {
                put("id", art.id)
                put("name", art.name)
                put("rarity", art.rarity.name)
                put("slot", art.slot.name)
                put("atkBonus", art.atkBonus)
                put("armorBonus", art.armorBonus)
                put("hpBonus", art.hpBonus)
                put("effectDescription", art.effectDescription)
                put("iconSymbol", art.iconSymbol)
                put("setTag", art.setTag ?: "")
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("saved_artifacts", jsonArray.toString()).apply()
    }

    fun loadArtifacts(): List<Artifact> {
        val savedStr = prefs.getString("saved_artifacts", null) ?: return emptyList()
        try {
            val jsonArray = org.json.JSONArray(savedStr)
            val list = mutableListOf<Artifact>()
            val catalog = getArtifactCatalog()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val rawId = obj.getString("id")
                val catalogItem = catalog[rawId]
                    ?: catalog.entries.firstOrNull { rawId.startsWith(it.key) }?.value
                    ?: catalog.values.firstOrNull { it.name == obj.optString("name") }
                val desc = obj.optString("effectDescription", "").ifBlank {
                    catalogItem?.effectDescription ?: ""
                }
                list.add(
                    Artifact(
                        id = rawId,
                        name = obj.getString("name"),
                        rarity = CardRarity.fromString(obj.optString("rarity", "BASIC")),
                        slot = ArtifactSlot.valueOf(obj.optString("slot", "WEAPON")),
                        atkBonus = obj.optInt("atkBonus", 0),
                        armorBonus = obj.optInt("armorBonus", 0),
                        hpBonus = obj.optInt("hpBonus", 0),
                        effectDescription = desc,
                        iconSymbol = obj.optString("iconSymbol", catalogItem?.iconSymbol ?: "💎"),
                        setTag = obj.optString("setTag", catalogItem?.setTag ?: "").takeIf { it.isNotBlank() }
                    ).withClampedHp()
                )
            }
            return list
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun saveCampaignStars(starsMap: Map<Int, Int>) {
        val obj = org.json.JSONObject()
        for ((stageId, stars) in starsMap) {
            obj.put(stageId.toString(), stars)
        }
        prefs.edit().putString("saved_campaign_stars", obj.toString()).apply()
    }

    fun loadCampaignStars(): Map<Int, Int> {
        val savedStr = prefs.getString("saved_campaign_stars", null) ?: return emptyMap()
        try {
            val obj = org.json.JSONObject(savedStr)
            val map = mutableMapOf<Int, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k.toInt()] = obj.getInt(k)
            }
            return map
        } catch (_: Exception) {
            return emptyMap()
        }
    }

    fun saveCampaignClearCounts(countsMap: Map<Int, Int>) {
        val obj = org.json.JSONObject()
        for ((stageId, count) in countsMap) {
            if (count > 0) {
                obj.put(stageId.toString(), count)
            }
        }
        prefs.edit().putString("saved_campaign_clear_counts", obj.toString()).apply()
    }

    fun loadCampaignClearCounts(): Map<Int, Int> {
        val savedStr = prefs.getString("saved_campaign_clear_counts", null) ?: return emptyMap()
        try {
            val obj = org.json.JSONObject(savedStr)
            val map = mutableMapOf<Int, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k.toInt()] = obj.getInt(k).coerceAtLeast(0)
            }
            return map
        } catch (_: Exception) {
            return emptyMap()
        }
    }

    fun saveHeroesState(heroes: List<HeroCard>) {
        val jsonArray = org.json.JSONArray()
        for (hero in heroes) {
            val obj = org.json.JSONObject().apply {
                put("id", hero.id)
                put("level", hero.level)
                put("shards", hero.shards)
                put("shardsNeeded", hero.shardsNeeded)
                put("isUnlocked", hero.isUnlocked)
                put("activeSkillId", hero.activeSkillId)
                put("equippedArtifacts", org.json.JSONArray(hero.equippedArtifactIds))
                put("baseAtk", hero.baseAtk)
                put("baseArmor", hero.baseArmor)
                put("baseHp", hero.baseHp)
                put("imageResName", hero.resolveImageName())
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("saved_heroes", jsonArray.toString()).apply()
    }

    fun loadHeroesState(defaultHeroes: List<HeroCard>): List<HeroCard> {
        val savedStr = prefs.getString("saved_heroes", null) ?: return defaultHeroes.map { it.withUpdatedImageForLevel() }
        try {
            val jsonArray = org.json.JSONArray(savedStr)
            val map = mutableMapOf<String, org.json.JSONObject>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                map[obj.getString("id")] = obj
            }
            return defaultHeroes.map { hero ->
                val saved = map[hero.id]
                if (saved != null) {
                    val eqArray = saved.optJSONArray("equippedArtifacts")
                    val eqList = mutableListOf<String>()
                    if (eqArray != null) {
                        for (j in 0 until eqArray.length()) {
                            eqList.add(eqArray.getString(j))
                        }
                    }
                    val loadedLevel = saved.optInt("level", hero.level).coerceIn(1, hero.maxUpgradeLevel())
                    val extraLevels = (loadedLevel - hero.level).coerceAtLeast(0)
                    val expectedAtk = hero.baseAtk + extraLevels * hero.atkIncrementPerLevel()
                    val expectedArmor = hero.baseArmor + (if (hero.heroType == HeroType.MELEE) extraLevels else 0)
                    val expectedHp = hero.baseHp + extraLevels * 6
                    val loadedAtk = if (saved.has("baseAtk")) saved.optInt("baseAtk", expectedAtk).coerceAtLeast(expectedAtk) else expectedAtk
                    val loadedArmor = if (saved.has("baseArmor")) saved.optInt("baseArmor", expectedArmor).coerceAtLeast(expectedArmor) else expectedArmor
                    val loadedHp = if (saved.has("baseHp")) saved.optInt("baseHp", expectedHp).coerceAtLeast(expectedHp) else expectedHp
                    val targetImageName = resolveHeroImageNameForLevel(
                        hero.id,
                        loadedLevel,
                        saved.optString("imageResName", hero.resolveImageName())
                    )
                    hero.copy(
                        level = loadedLevel,
                        shards = if (loadedLevel >= hero.maxUpgradeLevel()) 0 else saved.optInt("shards", hero.shards),
                        shardsNeeded = saved.optInt("shardsNeeded", hero.shardsNeeded),
                        isUnlocked = saved.optBoolean("isUnlocked", hero.isUnlocked),
                        activeSkillId = saved.optString("activeSkillId", hero.activeSkillId),
                        equippedArtifactIds = eqList,
                        baseAtk = loadedAtk,
                        baseArmor = loadedArmor,
                        baseHp = loadedHp,
                        imageRes = resolveHeroDrawableByName(targetImageName, hero.imageRes)
                    )
                } else hero.withUpdatedImageForLevel()
            }
        } catch (_: Exception) {
            return defaultHeroes
        }
    }

data class SaveImportResult(
    val profile: PlayerProfile,
    val heroes: List<HeroCard>,
    val artifacts: List<Artifact>,
    val campaignStars: Map<Int, Int> = emptyMap(),
    val campaignClearCounts: Map<Int, Int> = emptyMap()
) {
    val first: PlayerProfile get() = profile
    val second: List<HeroCard> get() = heroes
    val third: List<Artifact> get() = artifacts
}

    fun exportSaveToJson(
        profile: PlayerProfile,
        heroes: List<HeroCard>,
        artifacts: List<Artifact>,
        campaignStars: Map<Int, Int> = emptyMap(),
        campaignClearCounts: Map<Int, Int> = emptyMap()
    ): String {
        val root = org.json.JSONObject()
        root.put("version", com.example.BuildConfig.VERSION_NAME)
        root.put("timestamp", System.currentTimeMillis())

        val pObj = org.json.JSONObject().apply {
            put("name", profile.name)
            put("level", profile.level)
            put("exp", profile.exp)
            put("maxExp", profile.maxExp)
            put("gold", profile.gold)
            put("gems", profile.gems)
            put("arenaKeys", profile.arenaKeys)
            put("trophies", profile.trophies)
            put("activeDeck", org.json.JSONArray(profile.activeDeckHeroIds.map { it ?: "" }))
            put("isGoogleSynced", profile.isGoogleSynced)
            put("googleAccountEmail", profile.googleAccountEmail ?: "")
            put("lastSyncTimestamp", profile.lastSyncTimestamp)
            put("lastArenaKeyRegenTimestamp", profile.lastArenaKeyRegenTimestamp)
            put("lastDailyClaimDate", profile.lastDailyClaimDate ?: "")
            put("lastDailyClaimTimestamp", profile.lastDailyClaimTimestamp)
            put("dailyStreak", profile.dailyStreak)
            put("maxSeenTimestamp", profile.maxSeenTimestamp)
            put("isTutorialCompleted", profile.isTutorialCompleted)
            put("isStoryRead", profile.isStoryRead)
            put("battlesWon", profile.battlesWon)
            put("battlesLost", profile.battlesLost)
            put("foundAmulets", org.json.JSONArray(profile.foundAmulets))
            put("isCampaign100AchievementShown", profile.isCampaign100AchievementShown)
            put("lastCelebratedLevel", profile.lastCelebratedLevel)
            put("coliseumWon", profile.coliseumWon)
            put("coliseumLost", profile.coliseumLost)
            put("lastColiseumTimestamp", profile.lastColiseumTimestamp)
            put("coliseumEntriesDate", profile.coliseumEntriesDate ?: "")
            put("coliseumEntriesUsedToday", profile.coliseumEntriesUsedToday)
            put("isColiseumTutorialCompleted", profile.isColiseumTutorialCompleted)
            put("towerProgressDate", profile.towerProgressDate ?: "")
            put("towerClearedFloorToday", profile.towerClearedFloorToday)
            put("towerAttemptsUsedToday", profile.towerAttemptsUsedToday)
            put("towerTotalWins", profile.towerTotalWins)
            put("isTowerTutorialCompleted", profile.isTowerTutorialCompleted)
            put("towerFloorStarsToday", org.json.JSONArray(profile.towerFloorStarsToday))
            put("towerRoofClearedToday", profile.towerRoofClearedToday)
            put("towerRoofStarsToday", profile.towerRoofStarsToday)
            put("towerDamagedFloor", profile.towerDamagedFloor)
            put("towerSavedEnemyHps", org.json.JSONArray(profile.towerSavedEnemyHps))
            put("towerDeckHeroIds", org.json.JSONArray(profile.towerDeckHeroIds.map { it ?: "" }))
            put("viewedGalleryImageKeys", org.json.JSONArray(profile.viewedGalleryImageKeys.toList()))
            put("isSoundMuted", profile.isSoundMuted)
            put("skippedUpdateVersionCode", profile.skippedUpdateVersionCode)
            put("lastCareerAutoBattleTimestamp", profile.lastCareerAutoBattleTimestamp)
        }
        root.put("profile", pObj)

        val hArray = org.json.JSONArray()
        for (hero in heroes) {
            val hObj = org.json.JSONObject().apply {
                put("id", hero.id)
                put("level", hero.level)
                put("shards", hero.shards)
                put("shardsNeeded", hero.shardsNeeded)
                put("isUnlocked", hero.isUnlocked)
                put("activeSkillId", hero.activeSkillId)
                put("equippedArtifactIds", org.json.JSONArray(hero.equippedArtifactIds))
                put("baseAtk", hero.baseAtk)
                put("baseArmor", hero.baseArmor)
                put("baseHp", hero.baseHp)
                put("imageResName", hero.resolveImageName())
            }
            hArray.put(hObj)
        }
        root.put("heroes", hArray)

        val aArray = org.json.JSONArray()
        for (art in artifacts) {
            val aObj = org.json.JSONObject().apply {
                put("id", art.id)
                put("name", art.name)
                put("rarity", art.rarity.name)
                put("slot", art.slot.name)
                put("atkBonus", art.atkBonus)
                put("armorBonus", art.armorBonus)
                put("hpBonus", art.hpBonus)
                put("effectDescription", art.effectDescription)
                put("iconSymbol", art.iconSymbol)
                put("setTag", art.setTag ?: "")
            }
            aArray.put(aObj)
        }
        root.put("artifacts", aArray)

        val starsObj = org.json.JSONObject()
        for ((stageId, stars) in campaignStars) {
            starsObj.put(stageId.toString(), stars)
        }
        root.put("campaignStars", starsObj)

        val clearCountsObj = org.json.JSONObject()
        for ((stageId, count) in campaignClearCounts) {
            if (count > 0) {
                clearCountsObj.put(stageId.toString(), count)
            }
        }
        root.put("campaignClearCounts", clearCountsObj)

        return root.toString(2)
    }

    fun importSaveFromJson(jsonString: String, defaultHeroes: List<HeroCard>): SaveImportResult? {
        try {
            val root = org.json.JSONObject(jsonString)
            val pObj = root.getJSONObject("profile")

            val deckArray = pObj.getJSONArray("activeDeck")
            val deckList = mutableListOf<String?>()
            for (i in 0 until deckArray.length()) {
                val s = deckArray.getString(i)
                deckList.add(if (s.isEmpty() || s == "null") null else s)
            }

            val amuletsArray = pObj.optJSONArray("foundAmulets")
            val amuletsList = mutableListOf<String>()
            if (amuletsArray != null) {
                for (i in 0 until amuletsArray.length()) {
                    amuletsList.add(amuletsArray.getString(i))
                }
            }

            val towerStarsArray = pObj.optJSONArray("towerFloorStarsToday")
            val towerFloorStarsList = mutableListOf<Int>()
            if (towerStarsArray != null) {
                for (i in 0 until towerStarsArray.length()) {
                    towerFloorStarsList.add(towerStarsArray.optInt(i, 0))
                }
            }
            val towerSavedHpsArray = pObj.optJSONArray("towerSavedEnemyHps")
            val towerSavedHpsList = mutableListOf<Int>()
            if (towerSavedHpsArray != null) {
                for (i in 0 until towerSavedHpsArray.length()) {
                    towerSavedHpsList.add(towerSavedHpsArray.optInt(i, 0))
                }
            }
            val towerDeckArray = pObj.optJSONArray("towerDeckHeroIds")
            val towerDeckList = mutableListOf<String?>()
            if (towerDeckArray != null) {
                for (i in 0 until towerDeckArray.length()) {
                    val s = towerDeckArray.optString(i, "")
                    towerDeckList.add(if (s.isEmpty() || s == "null") null else s)
                }
            }
            val viewedGalleryArray = pObj.optJSONArray("viewedGalleryImageKeys")
            val viewedGallerySet = mutableSetOf<String>()
            if (viewedGalleryArray != null) {
                for (i in 0 until viewedGalleryArray.length()) {
                    val s = viewedGalleryArray.optString(i, "")
                    if (s.isNotBlank()) viewedGallerySet.add(s)
                }
            }

            val importedLevel = pObj.optInt("level", 1)
            val profile = PlayerProfile(
                name = pObj.optString("name", "Хранительница"),
                level = importedLevel,
                exp = pObj.optInt("exp", 0),
                maxExp = pObj.optInt("maxExp", 100),
                gold = pObj.optInt("gold", 3500),
                gems = pObj.optInt("gems", 240),
                arenaKeys = pObj.optInt("arenaKeys", 5),
                trophies = pObj.optInt("trophies", 100),
                activeDeckHeroIds = deckList,
                isGoogleSynced = pObj.optBoolean("isGoogleSynced", false),
                googleAccountEmail = pObj.optString("googleAccountEmail", "").takeIf { it.isNotBlank() },
                lastSyncTimestamp = pObj.optLong("lastSyncTimestamp", 0L),
                lastArenaKeyRegenTimestamp = pObj.optLong("lastArenaKeyRegenTimestamp", System.currentTimeMillis()),
                lastDailyClaimDate = pObj.optString("lastDailyClaimDate", "").takeIf { it.isNotBlank() },
                lastDailyClaimTimestamp = pObj.optLong("lastDailyClaimTimestamp", 0L),
                dailyStreak = pObj.optInt("dailyStreak", 0),
                maxSeenTimestamp = pObj.optLong("maxSeenTimestamp", System.currentTimeMillis()),
                isTutorialCompleted = pObj.optBoolean("isTutorialCompleted", true),
                isStoryRead = pObj.optBoolean("isStoryRead", false),
                battlesWon = pObj.optInt("battlesWon", 0),
                battlesLost = pObj.optInt("battlesLost", 0),
                foundAmulets = amuletsList,
                isCampaign100AchievementShown = pObj.optBoolean("isCampaign100AchievementShown", false),
                lastCelebratedLevel = pObj.optInt("lastCelebratedLevel", importedLevel),
                coliseumWon = pObj.optInt("coliseumWon", 0),
                coliseumLost = pObj.optInt("coliseumLost", 0),
                lastColiseumTimestamp = pObj.optLong("lastColiseumTimestamp", 0L),
                coliseumEntriesDate = pObj.optString("coliseumEntriesDate", "").takeIf { it.isNotBlank() },
                coliseumEntriesUsedToday = pObj.optInt("coliseumEntriesUsedToday", 0),
                isColiseumTutorialCompleted = pObj.optBoolean("isColiseumTutorialCompleted", false),
                towerProgressDate = pObj.optString("towerProgressDate", "").takeIf { it.isNotBlank() },
                towerClearedFloorToday = pObj.optInt("towerClearedFloorToday", 0),
                towerAttemptsUsedToday = pObj.optInt("towerAttemptsUsedToday", 0),
                towerTotalWins = pObj.optInt("towerTotalWins", 0),
                isTowerTutorialCompleted = pObj.optBoolean("isTowerTutorialCompleted", false),
                towerFloorStarsToday = towerFloorStarsList,
                towerRoofClearedToday = pObj.optBoolean("towerRoofClearedToday", false),
                towerRoofStarsToday = pObj.optInt("towerRoofStarsToday", 0),
                towerDamagedFloor = pObj.optInt("towerDamagedFloor", 0),
                towerSavedEnemyHps = towerSavedHpsList,
                towerDeckHeroIds = towerDeckList,
                viewedGalleryImageKeys = viewedGallerySet,
                isSoundMuted = pObj.optBoolean("isSoundMuted", false),
                skippedUpdateVersionCode = pObj.optInt("skippedUpdateVersionCode", 0),
                lastCareerAutoBattleTimestamp = pObj.optLong("lastCareerAutoBattleTimestamp", 0L)
            )

            val hArray = root.optJSONArray("heroes")
            val heroMap = mutableMapOf<String, org.json.JSONObject>()
            if (hArray != null) {
                for (i in 0 until hArray.length()) {
                    val o = hArray.getJSONObject(i)
                    heroMap[o.getString("id")] = o
                }
            }

            val heroes = defaultHeroes.map { defaultHero ->
                val saved = heroMap[defaultHero.id]
                if (saved != null) {
                    val eqArray = saved.optJSONArray("equippedArtifactIds")
                    val eqList = mutableListOf<String>()
                    if (eqArray != null) {
                        for (j in 0 until eqArray.length()) {
                            eqList.add(eqArray.getString(j))
                        }
                    }
                    val loadedLevel = saved.optInt("level", defaultHero.level).coerceIn(1, defaultHero.maxUpgradeLevel())
                    val extraLevels = (loadedLevel - defaultHero.level).coerceAtLeast(0)
                    val expectedAtk = defaultHero.baseAtk + extraLevels * defaultHero.atkIncrementPerLevel()
                    val expectedArmor = defaultHero.baseArmor + (if (defaultHero.heroType == HeroType.MELEE) extraLevels else 0)
                    val expectedHp = defaultHero.baseHp + extraLevels * 6
                    val loadedAtk = if (saved.has("baseAtk")) saved.optInt("baseAtk", expectedAtk).coerceAtLeast(expectedAtk) else expectedAtk
                    val loadedArmor = if (saved.has("baseArmor")) saved.optInt("baseArmor", expectedArmor).coerceAtLeast(expectedArmor) else expectedArmor
                    val loadedHp = if (saved.has("baseHp")) saved.optInt("baseHp", expectedHp).coerceAtLeast(expectedHp) else expectedHp
                    val targetImageName = resolveHeroImageNameForLevel(
                        defaultHero.id,
                        loadedLevel,
                        saved.optString("imageResName", defaultHero.resolveImageName())
                    )
                    defaultHero.copy(
                        level = loadedLevel,
                        shards = if (loadedLevel >= defaultHero.maxUpgradeLevel()) 0 else saved.optInt("shards", defaultHero.shards),
                        shardsNeeded = saved.optInt("shardsNeeded", defaultHero.shardsNeeded),
                        isUnlocked = saved.optBoolean("isUnlocked", defaultHero.isUnlocked),
                        activeSkillId = saved.optString("activeSkillId", defaultHero.activeSkillId),
                        equippedArtifactIds = eqList,
                        baseAtk = loadedAtk,
                        baseArmor = loadedArmor,
                        baseHp = loadedHp,
                        imageRes = resolveHeroDrawableByName(targetImageName, defaultHero.imageRes)
                    )
                } else defaultHero.withUpdatedImageForLevel()
            }

            val aArray = root.optJSONArray("artifacts")
            val artifacts = mutableListOf<Artifact>()
            val catalog = getArtifactCatalog()
            if (aArray != null) {
                for (i in 0 until aArray.length()) {
                    val aObj = aArray.getJSONObject(i)
                    val rawId = aObj.getString("id")
                    val catalogItem = catalog[rawId]
                        ?: catalog.entries.firstOrNull { rawId.startsWith(it.key) }?.value
                        ?: catalog.values.firstOrNull { it.name == aObj.optString("name") }
                    val desc = aObj.optString("effectDescription", "").ifBlank {
                        catalogItem?.effectDescription ?: ""
                    }
                    artifacts.add(
                        Artifact(
                            id = rawId,
                            name = aObj.getString("name"),
                            rarity = CardRarity.fromString(aObj.optString("rarity", "BASIC")),
                            slot = ArtifactSlot.valueOf(aObj.optString("slot", "WEAPON")),
                            atkBonus = aObj.optInt("atkBonus", 0),
                            armorBonus = aObj.optInt("armorBonus", 0),
                            hpBonus = aObj.optInt("hpBonus", 0),
                            effectDescription = desc,
                            iconSymbol = aObj.optString("iconSymbol", catalogItem?.iconSymbol ?: "💎"),
                            setTag = aObj.optString("setTag", catalogItem?.setTag ?: "").takeIf { it.isNotBlank() }
                        ).withClampedHp()
                    )
                }
            }

            val starsObj = root.optJSONObject("campaignStars")
            val campaignStars = mutableMapOf<Int, Int>()
            if (starsObj != null) {
                val keys = starsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    campaignStars[k.toInt()] = starsObj.getInt(k)
                }
            }
            // Always overwrite campaign stars in SharedPreferences to match imported save
            saveCampaignStars(campaignStars)

            val clearCountsObj = root.optJSONObject("campaignClearCounts")
            val campaignClearCounts = mutableMapOf<Int, Int>()
            if (clearCountsObj != null) {
                val keys = clearCountsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    campaignClearCounts[k.toInt()] = clearCountsObj.getInt(k).coerceAtLeast(0)
                }
            }
            saveCampaignClearCounts(campaignClearCounts)

            savePlayerProfile(profile)
            saveHeroesState(heroes)
            saveArtifacts(artifacts)

            return SaveImportResult(profile, heroes, artifacts, campaignStars, campaignClearCounts)
        } catch (_: Exception) {
            return null
        }
    }

    fun getDailyQuestCatalog(): List<QuestTemplate> = listOf(
        // Tier 1 (Самые простые: 120-150 золота или 2 частицы)
        QuestTemplate("q_t1_any_1", "Первая победа", "Выиграй 1 любой бой в Карьере или на Арене", QuestType.WIN_ANY_BATTLES, 1, 1, QuestRewardType.GOLD, 120),
        QuestTemplate("q_t1_career_1", "Путь Карьеры", "Одержи победу на 1 этапе Карьеры", QuestType.WIN_CAREER_BATTLES, 1, 1, QuestRewardType.GOLD, 130),
        QuestTemplate("q_t1_arena_1", "Выход на Арену", "Одержи победу в 1 бою на Арене", QuestType.WIN_ARENA_BATTLES, 1, 1, QuestRewardType.GOLD, 150),
        QuestTemplate("q_t1_enemies_4", "Охотница", "Победи 4 вражеские карты в любых боях", QuestType.DEFEAT_ENEMIES, 1, 4, QuestRewardType.GOLD, 140),
        QuestTemplate("q_t1_chest_1", "Искательница", "Открой 1 любой сундук во вкладке Сундуки", QuestType.OPEN_CHESTS, 1, 1, QuestRewardType.SHARDS, 2),
        QuestTemplate("q_t1_sell_1", "Чистка сумки", "Продай 1 лишний артефакт в Кузнице", QuestType.SELL_ARTIFACT, 1, 1, QuestRewardType.GOLD, 120),
        QuestTemplate("q_t1_skill_2", "Искра магии", "Примени 2 навыка героинь в победных боях", QuestType.CAST_SKILLS, 1, 2, QuestRewardType.GOLD, 140),

        // Tier 2 (Лёгкие: 220-250 золота или 3 частицы)
        QuestTemplate("q_t2_star_1", "Звёздный трофей", "Добудь ещё 1 звезду в Карьере", QuestType.EARN_CAREER_STARS, 2, 1, QuestRewardType.SHARDS, 3),
        QuestTemplate("q_t2_any_2", "Двойной триумф", "Выиграй 2 любых боя (Карьера или Арена)", QuestType.WIN_ANY_BATTLES, 2, 2, QuestRewardType.GOLD, 230),
        QuestTemplate("q_t2_flawless_1", "Безупречный шаг", "Победи в бою, не потеряв ни одной героини", QuestType.WIN_NO_LOSSES, 2, 1, QuestRewardType.GOLD, 250),
        QuestTemplate("q_t2_upgrade_1", "Тренировка", "Повысь уровень любой героини на +1", QuestType.UPGRADE_HERO, 2, 1, QuestRewardType.SHARDS, 3),
        QuestTemplate("q_t2_steel_1", "Сила стали", "Победи в бою с картой стихии Сталь", QuestType.WIN_WITH_ELEMENT, 2, 1, QuestRewardType.GOLD, 220, requiredElement = Element.STEEL),
        QuestTemplate("q_t2_magic_1", "Сила магии", "Победи в бою с картой стихии Магия", QuestType.WIN_WITH_ELEMENT, 2, 1, QuestRewardType.GOLD, 220, requiredElement = Element.MAGIC),
        QuestTemplate("q_t2_hybrid_1", "Гибкая тактика", "Победи с картой смешанного боя в отряде", QuestType.WIN_WITH_HYBRID, 2, 1, QuestRewardType.GOLD, 240),

        // Tier 3 (Средние: 350-380 золота или 5 частиц)
        QuestTemplate("q_t3_arena_2", "Боец Арены", "Выиграй 2 сражения на Арене", QuestType.WIN_ARENA_BATTLES, 3, 2, QuestRewardType.GOLD, 360),
        QuestTemplate("q_t3_career_3", "Поход вглубь", "Одержи 3 победы на этапах Карьеры", QuestType.WIN_CAREER_BATTLES, 3, 3, QuestRewardType.GOLD, 350),
        QuestTemplate("q_t3_ranged_1", "Залп издалека", "Выиграй бой на Арене только дальним боем", QuestType.WIN_ARENA_RANGED_ONLY, 3, 1, QuestRewardType.SHARDS, 5),
        QuestTemplate("q_t3_two_hybrid_1", "Гибридный дуэт", "Выиграй бой с 2 бойцами смешанного боя", QuestType.WIN_TWO_HYBRID, 3, 1, QuestRewardType.SHARDS, 5),
        QuestTemplate("q_t3_skills_5", "Мастерство чар", "Примени 5 навыков героинь в победных боях", QuestType.CAST_SKILLS, 3, 5, QuestRewardType.GOLD, 380),
        QuestTemplate("q_t3_enemies_10", "Сокрушение", "Одолей 10 вражеских карт в боях", QuestType.DEFEAT_ENEMIES, 3, 10, QuestRewardType.GOLD, 360),
        QuestTemplate("q_t3_forge_1", "Алхимия кузни", "Синтезируй 1 новый артефакт в Кузнице", QuestType.FORGE_COMBINE, 3, 1, QuestRewardType.SHARDS, 5),

        // Tier 4 (Сложные: 500-550 золота или 7-8 частиц)
        QuestTemplate("q_t4_arena_3", "Чемпион Арены", "Выиграй 3 боя на Арене", QuestType.WIN_ARENA_BATTLES, 4, 3, QuestRewardType.GOLD, 550),
        QuestTemplate("q_t4_stars_2", "Сияние Карьеры", "Добудь 2 новые звезды в Карьере", QuestType.EARN_CAREER_STARS, 4, 2, QuestRewardType.SHARDS, 7),
        QuestTemplate("q_t4_flawless_2", "Идеальный строй", "Одержи 2 победы без единой потери", QuestType.WIN_NO_LOSSES, 4, 2, QuestRewardType.GOLD, 520),
        QuestTemplate("q_t4_boss_1", "Охота на босса", "Победи босса любой главы в Карьере", QuestType.WIN_CAREER_BOSS, 4, 1, QuestRewardType.SHARDS, 8),
        QuestTemplate("q_t4_any_4", "Серия побед", "Выиграй 4 любых сражения за день", QuestType.WIN_ANY_BATTLES, 4, 4, QuestRewardType.GOLD, 540),
        QuestTemplate("q_t4_chests_2", "Зов сундуков", "Открой 2 сундука во вкладке Сундуки", QuestType.OPEN_CHESTS, 4, 2, QuestRewardType.SHARDS, 7),
        QuestTemplate("q_t4_light_2", "Благословение", "Выиграй 2 боя с картой стихии Света", QuestType.WIN_WITH_ELEMENT, 4, 2, QuestRewardType.GOLD, 500, requiredElement = Element.LIGHT),
        QuestTemplate("q_t4_four_ranged_1", "Квартет стрелков", "Выиграй бой с 4 бойцами дальнего боя", QuestType.WIN_FOUR_RANGED, 4, 1, QuestRewardType.SHARDS, 8),

        // Tier 5 (Мастерские: 800-900 золота или 10-12 частиц)
        QuestTemplate("q_t5_arena_4", "Владыка Арены", "Одержи 4 победы на Арене за день", QuestType.WIN_ARENA_BATTLES, 5, 4, QuestRewardType.GOLD, 850),
        QuestTemplate("q_t5_career_5", "Покорительница", "Выиграй 5 сражений в Карьере", QuestType.WIN_CAREER_BATTLES, 5, 5, QuestRewardType.SHARDS, 10),
        QuestTemplate("q_t5_ranged_2", "Дождь стрел", "Выиграй 2 боя на Арене только дальним боем", QuestType.WIN_ARENA_RANGED_ONLY, 5, 2, QuestRewardType.SHARDS, 12),
        QuestTemplate("q_t5_two_hybrid_2", "Гармония боя", "Выиграй 2 боя с 2 бойцами смешанного боя", QuestType.WIN_TWO_HYBRID, 5, 2, QuestRewardType.SHARDS, 11),
        QuestTemplate("q_t5_enemies_18", "Буря клинков", "Уничтожь 18 вражеских карт в боях", QuestType.DEFEAT_ENEMIES, 5, 18, QuestRewardType.GOLD, 800),
        QuestTemplate("q_t5_stars_3", "Мастер звёзд", "Добудь 3 новые звезды на уровнях Карьеры", QuestType.EARN_CAREER_STARS, 5, 3, QuestRewardType.SHARDS, 12),
        QuestTemplate("q_t5_flawless_3", "Бессмертный отряд", "Выиграй 3 боя без потери ни одной карты", QuestType.WIN_NO_LOSSES, 5, 3, QuestRewardType.GOLD, 900)
    )

    fun saveDailyQuests(quests: List<DailyQuestState>) {
        val jsonArray = org.json.JSONArray()
        for (q in quests) {
            val obj = org.json.JSONObject().apply {
                put("id", q.id)
                put("title", q.title)
                put("description", q.description)
                put("type", q.type.name)
                put("difficultyTier", q.difficultyTier)
                put("targetCount", q.targetCount)
                put("currentProgress", q.currentProgress)
                put("rewardType", q.rewardType.name)
                put("rewardAmount", q.rewardAmount)
                put("isRewardClaimed", q.isRewardClaimed)
                put("requiredElement", q.requiredElement?.name ?: "")
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("daily_quests_json", jsonArray.toString()).apply()
    }

    fun loadDailyQuests(): List<DailyQuestState> {
        val savedStr = prefs.getString("daily_quests_json", null) ?: return emptyList()
        return try {
            val jsonArray = org.json.JSONArray(savedStr)
            val result = mutableListOf<DailyQuestState>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val elemStr = obj.optString("requiredElement", "")
                val reqElem = elemStr.takeIf { it.isNotBlank() }?.let { Element.fromString(it) }
                result.add(
                    DailyQuestState(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        type = QuestType.valueOf(obj.getString("type")),
                        difficultyTier = obj.optInt("difficultyTier", 1),
                        targetCount = obj.optInt("targetCount", 1),
                        currentProgress = obj.optInt("currentProgress", 0),
                        rewardType = QuestRewardType.valueOf(obj.optString("rewardType", "GOLD")),
                        rewardAmount = obj.optInt("rewardAmount", 100),
                        isRewardClaimed = obj.optBoolean("isRewardClaimed", false),
                        requiredElement = reqElem
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDailyQuestDate(dateStr: String) {
        prefs.edit().putString("daily_quests_date", dateStr).apply()
    }

    fun loadDailyQuestDate(): String? =
        prefs.getString("daily_quests_date", null)?.takeIf { it.isNotBlank() }

    fun savePreviousDailyQuestIds(ids: Set<String>) {
        prefs.edit().putString("prev_daily_quest_ids", ids.joinToString(",")).apply()
    }

    fun loadPreviousDailyQuestIds(): Set<String> {
        val raw = prefs.getString("prev_daily_quest_ids", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    companion object {
        fun getStageTargetAtk(stageId: Int): Int = when (stageId) {
            101 -> 40
            102 -> 43
            103 -> 46
            104 -> 49
            105 -> 51
            106 -> 60
            201 -> 54
            202 -> 58
            203 -> 61
            204 -> 63
            205 -> 65
            206 -> 75
            301 -> 70
            302 -> 74
            303 -> 78
            304 -> 82
            305 -> 86
            306 -> 100
            401 -> 95
            402 -> 102
            403 -> 109
            404 -> 115
            405 -> 122
            406 -> 140
            else -> 40
        }
    }
}
