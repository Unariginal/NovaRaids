package me.unariginal.novaraids.ai

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.moves.categories.DamageCategories
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.battles.*
import com.cobblemon.mod.common.battles.ai.strongBattleAI.AIUtility
import com.cobblemon.mod.common.battles.ai.strongBattleAI.ActiveTracker
import com.cobblemon.mod.common.battles.ai.strongBattleAI.TrackerPokemon
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.pokemon.status.PersistentStatus
import kotlin.math.absoluteValue
import kotlin.random.Random

class StrongBattleAIFix(skill: Int) : BattleAI {
    private val skill = skill.coerceIn(0, 5)
    private val speedTierCoefficient = 4.0 //todo set back to 6 to how it was
    private var trickRoomCoefficient = 1.0
    private val typeMatchupWeightConsideration = 2.5 // value of a good or bad type matchup
    private val moveDamageWeightConsideration = 0.8 // value of a good or bad move matchup
    private val antiBoostWeightConsideration = 25 // value of a mon with moves that remove stat boosts
    private val hpWeightConsideration = 0.25 // how much HP difference is a consideration for switchins
    private val hpFractionCoefficient = 0.4 // how much HP differences should be taken into account for switch ins
    private val boostWeightCoefficient = 1 // the amount of boosts considered a baseline to be removed
    private val switchOutMatchupThreshold = 0 // todo change this to get it feeling just right (-7 never switches)
    private val selfKoMoveMatchupThreshold = 0.3
    private val trickRoomThreshold = 85
    private val recoveryMoveThreshold = 0.50
    private val accuracySwitchThreshold = -3
    private val hpSwitchOutThreshold = .3 // percent of HP needed to be considered for switchout
    private val randomProtectChance = 0.3 // percent chance of a protect move being used with 1 turn in between
    private val statusDamageConsiderationThreshold = 0.8 // the percentage of health that a move can do where the AI won't go for a status move (i.e if I have a move that can do 80% of health as damage, then why go for status?)

    // create the active pokemon tracker here
    private val activeTracker = ActiveTracker()

    fun cleanUp() {
        activeTracker.currentRoom = null
        activeTracker.currentTerrain = null
        activeTracker.currentWeather = null

        activeTracker.alliedSide.screenCondition = null
        activeTracker.alliedSide.tailwindCondition = null
        activeTracker.alliedSide.sideHazards = emptyList()
        activeTracker.alliedSide.actors.forEach { actor ->
            actor.party.forEach { pokemon ->
                pokemon.pokemon = null
                pokemon.currentAbility = null
                pokemon.moves = emptyList()
                pokemon.species = null
                pokemon.form = null
            }
            actor.party.clear()
            actor.activePokemon.forEach { pokemon ->
                pokemon.pokemon = null
                pokemon.currentAbility = null
                pokemon.moves = emptyList()
                pokemon.species = null
                pokemon.form = null
            }
            actor.activePokemon.clear()
        }
        activeTracker.alliedSide.actors.clear()

        activeTracker.opponentSide.screenCondition = null
        activeTracker.opponentSide.tailwindCondition = null
        activeTracker.opponentSide.sideHazards = emptyList()
        activeTracker.opponentSide.actors.forEach { actor ->
            actor.party.forEach { pokemon ->
                pokemon.pokemon = null
                pokemon.currentAbility = null
                pokemon.moves = emptyList()
                pokemon.species = null
                pokemon.form = null
            }
            actor.party.clear()
            actor.activePokemon.forEach { pokemon ->
                pokemon.pokemon = null
                pokemon.currentAbility = null
                pokemon.moves = emptyList()
                pokemon.species = null
                pokemon.form = null
            }
            actor.activePokemon.clear()
        }
        activeTracker.opponentSide.actors.clear()
    }

    // skill check that will be used for if the AI will make a successful Move decision
    fun checkSkillLevel(): Boolean {
        if (skill == 5) {
            return true
        }
        val randomNumber = Random.nextInt(100)
        // Map skill level to the desired probability
        return randomNumber < skill * 20
    }

    // skill check that will be used for if the AI will make a successful Switch Out decision
    fun checkSwitchOutSkill(): Boolean {
        // Generate a random number between 0 and 1
        val randomNumber = Random.nextDouble()

        // Determine the chance skill check will succeed based on skill level
        val chance = when (skill) {
            in 0..2 -> 0.0
            3 -> 0.20
            4 -> 0.60
            5 -> 1.00
            else -> 0.0 // if skillLevel is out of expected range
        }

        // Check if the random number is less than the chance
        return randomNumber <= chance
    }

    fun calculateDamage(move: Pair<InBattleMove, MoveTemplate>, attacker: ActiveBattlePokemon, attackerTracker: TrackerPokemon): Pair<ActiveBattlePokemon?, Double> {
        if (move.first.target in setOf(MoveTarget.self, MoveTarget.adjacentAlly, MoveTarget.adjacentAllyOrSelf,
                MoveTarget.allyTeam, MoveTarget.allies, MoveTarget.allySide, MoveTarget.scripted, MoveTarget.randomNormal))
            return null to 0.0 // we do not care about moves that dont target opponents during damage calculations
        val singleTargetList = move.first.target.targetList.invoke(attacker)?.map { it as ActiveBattlePokemon }?.filter { it.battlePokemon != null }
        val multiTargetList = attacker.getMultiTargetList(move.first.target)?.map { it as ActiveBattlePokemon }?.filter { it.battlePokemon != null }
        if (singleTargetList != null && singleTargetList.isNotEmpty()) {
            // figure out target that will take the most damage
            val potentialTargets = activeTracker.opponentSide.activePokemon
                .filter { opp -> opp.id in singleTargetList.map { it.battlePokemon!!.uuid } }
                .map { opp -> singleTargetList.first { it.battlePokemon!!.uuid == opp.id} to calculateDamage(move.first, attackerTracker, opp) }

            return potentialTargets.maxBy { it.second }
        } else if (multiTargetList != null && multiTargetList.isNotEmpty()) {
            val (allies, opponents) = multiTargetList.partition { it.isAllied(attacker) }
            // weigh up allied damage against opponents
            val totalOpponentDamage = activeTracker.opponentSide.activePokemon
                .filter { opp -> opp.id in opponents.map { it.battlePokemon!!.uuid } }
                .sumOf { calculateDamage(move.first, attackerTracker, it) }
            val totalAlliedDamage = activeTracker.alliedSide.activePokemon
                .filter { opp -> opp.id in allies.map { it.battlePokemon!!.uuid } }
                .sumOf { calculateDamage(move.first, attackerTracker, it) }

            return null to totalOpponentDamage - totalAlliedDamage //TODO REVIEW THIS LOGIC AFTER SOME TESTING
        }
        // this case should not be possible, but you never know with custom stuff...
        return null to 0.0
    }

    // function for calculating the Damage of the move sent in
    fun calculateDamage(move: InBattleMove, attacker: TrackerPokemon, opponent: TrackerPokemon): Double {
        // HOW DAMAGE IS ACTUALLY CALCULATED
        // REFERENCES: https://bulbapedia.bulbagarden.net/wiki/Damage
        // Damage = (((((2 * pokemon.level) / 5 ) + 2) * move.power * (mon.attackStat / opponent.defenseStat)) / 50 + 2)
        // Damage *= Targets // 0.75 (0.5 in Battle Royals) if the move has more than one target when the move is executed, and 1 otherwise.
        // Damage *= PB // 0.25 (0.5 in Generation VI) if the move is the second strike of Parental Bond, and 1 otherwise
        // Damage *= Weather // 1.5 if a Water-type move is being used during rain or a Fire-type move or Hydro Steam during harsh sunlight, and 0.5 if a Water-type move (besides Hydro Steam) is used during harsh sunlight or a Fire-type move during rain, and 1 otherwise or if any Pokémon on the field have the Ability Cloud Nine or Air Lock.
        // Damage *= GlaiveRush // 2 if the target used the move Glaive Rush in the previous turn, or 1 otherwise.
        // Damage *= Critical // 1.5 (2 in Generation V) for a critical hit, and 1 otherwise. Decimals are rounded down to the nearest integer. It is always 1 if the target's Ability is Battle Armor or Shell Armor or if the target is under the effect of Lucky Chant.
        // Damage *= randomNumber // random number between .85 and 1.00
        // Damage *= STAB // 1.5 if mon.types is equal to move.type or if it is a combined Pledge move || 2.0 if it has adaptability || Terra gimmick has other rules
        // Damage *= Type // type damage multipliers || CHeck website for additional rules for some moves
        // Damage *= Burn // 0.5 if the pokemon is burned, its Ability is not Guts, and the used move is a physical move (other than Facade from Generation VI onward), and 1
        // Damage *= Other // 1 in most cases, and a different multiplier when specific interactions of moves, Abilities, or items take effect, in this order
        // Damage *= ZMove // 1 usually OR 0.25 if the move is a Z-Move, Max Move, or G-Max Move being used into a protection move
        // Damage *= TeraShield // ONLY for Terra raid battles
        val moveData = Moves.getByName(move.id) ?: return 0.0
        //value *= moveData.accuracy // todo look into better way to take accuracy into account

        if (AIUtility.typeImmuneAbilities[opponent.currentAbility?.name ?: "unknown"] == moveData.getEffectiveElementalType(attacker.pokemon) ||
            (opponent.currentAbility?.name == "suctioncup" && move.id == "roar" || move.id == "whirlwind")
        ) {
            return 0.0 // move would be absorbed/negated by ability no matter the damage
        }

        val physicalRatio = statEstimationActive(attacker, Stats.ATTACK) / statEstimationActive(opponent, Stats.DEFENCE)
        val specialRatio = statEstimationActive(attacker, Stats.SPECIAL_ATTACK) / statEstimationActive(opponent, Stats.SPECIAL_DEFENCE)

        // Attempt at better estimation
        val movePower = moveData.power
        val pokemonLevel = attacker.pokemon!!.level
        val statRatio = if (moveData.damageCategory == DamageCategories.PHYSICAL) physicalRatio else specialRatio

        val STAB = when {
            moveData.getEffectiveElementalType(attacker.pokemon) in attacker.pokemon!!.types && attacker.pokemon!!.ability.name == "adaptability" -> 2.0
            moveData.getEffectiveElementalType(attacker.pokemon) in attacker.pokemon!!.types -> 1.5
            else -> 1.0
        }
        val weather = when {
            // Sunny Weather
            activeTracker.currentWeather == "sunny" && (moveData.getEffectiveElementalType(attacker.pokemon) == ElementalTypes.FIRE || moveData.name == "hydrosteam") -> 1.5
            activeTracker.currentWeather == "sunny" && moveData.getEffectiveElementalType(attacker.pokemon) == ElementalTypes.WATER -> 0.5

            // Rainy Weather
            activeTracker.currentWeather == "raining" && moveData.getEffectiveElementalType(attacker.pokemon) == ElementalTypes.WATER-> 1.5
            activeTracker.currentWeather == "raining" && moveData.getEffectiveElementalType(attacker.pokemon) == ElementalTypes.FIRE-> 0.5

            // Add other cases below for weather

            else -> 1.0
        }
        val damageTypeMultiplier = moveDamageMultiplier(attacker, moveData, opponent)
        val burn = when {
            (opponent.pokemon?.status?.status?.showdownName ?: opponent.currentStatus) == Statuses.BURN.showdownName && moveData.damageCategory == DamageCategories.PHYSICAL -> 0.5
            else -> 1.0
        }
        val hitsExpected = expectedHits(moveData)

        var damage = (((((2 * pokemonLevel) / 5 ) + 2) * movePower * statRatio) / 50 + 2)
        damage *= weather
        damage *= STAB
        damage *= damageTypeMultiplier
        damage *= burn
        damage *= hitsExpected

        return damage
    }

    // function for finding the most damaging moves in the moveset
    fun mostDamagingMove(selectedMove: InBattleMove, moveset: ShowdownMoveset?, mon: TrackerPokemon, opponent: TrackerPokemon): Boolean {

        if (moveset != null) {
            for (move in moveset.moves.filter { !it.disabled && it.id != selectedMove.id}) {

                if (calculateDamage(move, mon, opponent) > calculateDamage(selectedMove, mon, opponent)) {
                    return false
                }
            }

            return calculateDamage(selectedMove, mon, opponent) > 0
        }
        else
            return false
    }

    override fun choose(
        activeBattlePokemon: ActiveBattlePokemon,
        battle: PokemonBattle,
        aiSide: BattleSide,
        moveset: ShowdownMoveset?,
        forceSwitch: Boolean
    ): ShowdownActionResponse {
        updateActiveTracker(aiSide, battle)

        val actorTracker = activeTracker.alliedSide.actors.first { it.uuid == activeBattlePokemon.actor.uuid }
        val availableSwitches = actorTracker.party
            .map {Pair(it, activeBattlePokemon.actor.pokemonList.first { poke -> poke.uuid == it.id })}
            .filter { it.second.canBeSentOut() }

        if (forceSwitch || activeBattlePokemon.isGone()) {
            if (battle.turn == 1) {
                val switchTo = activeBattlePokemon.actor.pokemonList.filter { it.canBeSentOut() }.randomOrNull()
                    ?: return DefaultActionResponse()
                switchTo.willBeSwitchedIn = true
                return SwitchActionResponse(switchTo.uuid)
            }
            else {
                val bestEstimation = availableSwitches.maxByOrNull { estimateMatchup(activeBattlePokemon, aiSide, battle, it.first) }
                    ?: return PassActionResponse

                bestEstimation.second.willBeSwitchedIn = true
                return SwitchActionResponse(bestEstimation.second.uuid)
            }
        }
        if (moveset == null) {
            return PassActionResponse
        }
        // if a move must be used (like recharge) is in moves list then do that since you have to
        moveset.moves.firstOrNull {it.mustBeUsed() }?.let {
            return@choose chooseMove(it, activeBattlePokemon)
        }

        val activeTrackerPokemon = activeTracker.alliedSide.activePokemon.first { it.id == activeBattlePokemon.battlePokemon!!.uuid }
        val opponents = activeTracker.opponentSide.activePokemon

        // Update protect count if it's on cooldown and implement a random reduction to the count to not be predictable
        if (activeTrackerPokemon.protectCount > 0) {
            if (Random.nextDouble() < randomProtectChance) {
                // 30% chance to decrease by 2
                activeTrackerPokemon.protectCount = (activeTrackerPokemon.protectCount - 2).coerceAtLeast(0)
            } else {
                // 70% chance to decrease by 1
                activeTrackerPokemon.protectCount = (activeTrackerPokemon.protectCount - 1).coerceAtLeast(0)
            }
        }

        val availableMoves = moveset.moves.filter { it.canBeUsed() }.map { Pair(it, Moves.getByName(it.id)!!)}

        if (!checkSkillLevel()){
            val move = availableMoves
                .filter { it.first.target.targetList(activeBattlePokemon)?.isEmpty() != true }
                .randomOrNull()
                ?: return MoveActionResponse("struggle")

            return chooseMove(move.first, activeBattlePokemon)
        }

        // switch out based on current matchup on the field
        if (checkSwitchOutSkill() && shouldSwitchOut(aiSide, battle, activeBattlePokemon, moveset)) {
            considerSwitching(activeBattlePokemon, activeTrackerPokemon, opponents, availableMoves, availableSwitches, battle, aiSide)
        }
        activeTrackerPokemon.firstTurn = false

        // Decision-making based on move availability and switch-out condition
        if (!shouldSwitchOut(aiSide, battle, activeBattlePokemon, moveset)) {
            val nRemainingMons = activeTracker.alliedSide.actors.sumOf { actor -> actor.party.filter { it.currentHpPercent > 0 }.size }
            val nOppRemainingMons = activeTracker.opponentSide.actors.sumOf { actor -> actor.party.filter { it.currentHpPercent > 0 }.size }

            // Sleep Talk when asleep
            availableMoves.firstOrNull { activeTrackerPokemon.currentStatus == Statuses.SLEEP.showdownName && it.first.id == "sleeptalk" }?.let {
                return chooseMove(it.first, activeBattlePokemon)
            }


            // Fake Out
            availableMoves.firstOrNull { it.first.id == "fakeout" && activeTrackerPokemon.firstTurn }?.let { fakeOut ->
                val targets = fakeOut.first.target.targetList.invoke(activeBattlePokemon)?.map { it as ActiveBattlePokemon }?.filter { it.battlePokemon != null } ?: emptyList()
                val validTarget = opponents.firstOrNull { opp -> opp.id in targets.map { it.battlePokemon!!.uuid } && ElementalTypes.GHOST !in (opp.form?.types ?: opp.species!!.types)}
                activeTrackerPokemon.firstTurn = false
                return chooseMove(fakeOut.first, activeBattlePokemon, targets.firstOrNull { validTarget != null && validTarget.id == it.battlePokemon!!.uuid})
            }

            activeTrackerPokemon.firstTurn = false


            // Explosion/Self destruct
            availableMoves.firstOrNull {
                (it.first.id == "explosion" || it.first.id == "selfdestruct")
                        && activeTrackerPokemon.currentHpPercent < selfKoMoveMatchupThreshold
                        && opponents.any { opponent -> ElementalTypes.GHOST !in (opponent.form?.types ?: opponent.species!!.types) && opponent.currentHpPercent > 0.5 }
            }?.let {
                return chooseMove(it.first, activeBattlePokemon)
            }

            // Self recovery moves
            availableMoves.firstOrNull { activeTrackerPokemon.currentHpPercent < recoveryMoveThreshold && it.first.id in AIUtility.selfRecoveryMoves }?.let {
                return chooseMove(it.first, activeBattlePokemon)
            }

            // Deal with non-weather related field changing effects
            for (move in availableMoves) {

                // Tailwind
                if (move.first.id == "tailwind" && move.first.id != activeTracker.alliedSide.tailwindCondition &&
                    availableSwitches.size > 2) {
                    return chooseMove(move.first, activeBattlePokemon)
                }

                // Trick room
                if (move.first.id == "trickroom" && move.first.id != activeTracker.currentRoom
                    && availableSwitches.count { statEstimationActive(it.first, Stats.SPEED) <= trickRoomThreshold } >= 2) {
                    return chooseMove(move.first, activeBattlePokemon)
                }

                // todo find a way to get list of active screens
                // Aurora veil
                if (move.first.id == "auroraveil" && move.first.id != activeTracker.alliedSide.screenCondition
                    && activeTracker.currentWeather in listOf("hail", "snow")) {
                    return chooseMove(move.first, activeBattlePokemon)
                }

                // todo find a way to get list of active screens
                // Light Screen
                if (move.first.id == "lightscreen" && move.first.id != activeTracker.alliedSide.screenCondition
                    && opponents.any { (it.species?.baseStats?.get(Stats.SPECIAL_ATTACK) ?: 0) > (it.species?.baseStats?.get(Stats.ATTACK) ?: 0) }
                    && availableSwitches.size > 1) {
                    return chooseMove(move.first, activeBattlePokemon)
                }

                // todo find a way to get list of active screens
                // Reflect
                if (move.first.id == "reflect" && move.first.id != activeTracker.alliedSide.screenCondition
                    && opponents.any { (it.species?.baseStats?.get(Stats.ATTACK) ?: 0) > (it.species?.baseStats?.get(Stats.SPECIAL_ATTACK) ?: 0) }
                    && availableSwitches.size > 1) {
                    return chooseMove(move.first, activeBattlePokemon)
                }
            }

            // Entry hazard setup and removal
            for (move in availableMoves) {
                // Setup
                if (nOppRemainingMons >= 3 && move.first.id in AIUtility.entryHazards
                    && !activeTracker.opponentSide.sideHazards.contains(move.first.id)) {
                    return chooseMove(move.first, activeBattlePokemon)
                }

                // Removal
                if (nRemainingMons >= 2 && move.first.id in AIUtility.antiHazardsMoves
                    && activeTracker.alliedSide.sideHazards.isNotEmpty()) {
                    return chooseMove(move.first, activeBattlePokemon)
                }
            }

            considerAntiBoost(activeBattlePokemon, activeTrackerPokemon, opponents, availableMoves)

            // Court Change
            for (move in availableMoves) {
                if (move.first.id == "courtchange"
                    && (!AIUtility.entryHazards.none { it in activeTracker.alliedSide.sideHazards }
                            || setOf("tailwind", "lightscreen", "reflect").any { it in activeTracker.opponentSide.sideHazards })
                    && setOf("tailwind", "lightscreen", "reflect").none { it in activeTracker.opponentSide.sideHazards }
                    && AIUtility.entryHazards.none { it in activeTracker.opponentSide.sideHazards }) {
                    return chooseMove(move.first, activeBattlePokemon)
                }
            }

            // Strength Sap
            for (move in availableMoves) {
                if (move.first.id == "strengthsap" && activeTrackerPokemon.currentHpPercent < 0.5
                    && activeTrackerPokemon.species!!.baseStats.getOrDefault(Stats.ATTACK, 0) > 80) {
                    return chooseMove(move.first, activeBattlePokemon)
                }
            }

            // Belly Drum
            for (move in availableMoves) {
                if (move.first.id == "bellydrum"
                    && (activeTrackerPokemon.currentHpPercent > 0.6
                            && activeTrackerPokemon.pokemon!!.heldItem().item == CobblemonItems.SITRUS_BERRY
                            || activeTrackerPokemon.currentHpPercent > 0.8)
                    && activeTrackerPokemon.boosts.getOrDefault(Stats.ATTACK, 0) < 1) {
                    return chooseMove(move.first, activeBattlePokemon)
                }
            }

            // todo have it not do this unless it is actually helpful for the team
            // Weather setup moves
            for (move in availableMoves) {
                AIUtility.weatherSetupMoves[move.first.id]?.let { requiredWeather ->
                    if (activeTracker.currentWeather != requiredWeather.lowercase() &&
                        !(activeTracker.currentWeather == "primordialsea" && requiredWeather == "raindance") &&
                        !(activeTracker.currentWeather == "desolateland" && requiredWeather == "sunnyday")) {
                        return chooseMove(move.first, activeBattlePokemon)
                    }
                }
            }

            // Setup moves
            if (activeTrackerPokemon.currentHpPercent == 1.0 && estimateMatchup(activeBattlePokemon, aiSide, battle) > 0) {
                for (move in availableMoves) {
                    if (AIUtility.setupMoves.contains(move.first.id) &&
                        ((getNonZeroStats(move.first.id).keys.minOfOrNull { activeTrackerPokemon.boosts[it] ?: 0 } ?: 0) < 6)) {
                        if (move.first.id != "curse" || ElementalTypes.GHOST !in activeTrackerPokemon.pokemon!!.types) {
                            return MoveActionResponse(move.first.id)
                        }
                    }
                }
            }

            considerInflictingStatus(activeBattlePokemon, activeTrackerPokemon, opponents, availableMoves, battle, aiSide)

            // Protect style moves
            for (move in availableMoves) {
                if (move.first.id in AIUtility.protectMoves) {
                    // Stall out side conditions
                    if ((activeTracker.opponentSide.screenCondition != null || activeTracker.opponentSide.tailwindCondition != null) &&
                        (activeTracker.alliedSide.screenCondition == null || activeTracker.alliedSide.tailwindCondition == null)  ||
                        opponents.any { it.currentStatus == null } &&
                        activeTrackerPokemon.protectCount == 0 && opponents.none { it.currentAbility?.name == "unseenfist"}) {
                        activeTrackerPokemon.protectCount = 3
                        return chooseMove(move.first, activeBattlePokemon)
                    }
                }
            }

            findAndUseMostDamagingMove(activeBattlePokemon, activeTrackerPokemon, opponents, availableMoves, moveset, battle, aiSide)
        }

        // healing wish (dealing with it here because you'd only use it if you should switch out anyway)
        for (move in availableMoves) {
            if (move.first.id == "healingwish" && activeTrackerPokemon.currentHpPercent < selfKoMoveMatchupThreshold) {
                return chooseMove(move.first, activeBattlePokemon)
            }
        }

        // switch out
        if (shouldSwitchOut(aiSide, battle, activeBattlePokemon, moveset)) {
            val bestEstimation = availableSwitches.maxByOrNull { estimateMatchup(activeBattlePokemon, aiSide, battle, it.first) }
            bestEstimation?.let {
                it.second.willBeSwitchedIn = true
                return SwitchActionResponse(it.second.uuid)
            }
        }
        activeTrackerPokemon.firstTurn = false

        val move = availableMoves
            .filter { it.first.canBeUsed() }
            .filter { it.first.mustBeUsed() || it.first.target.targetList(activeBattlePokemon)?.isEmpty() != true }
            .randomOrNull()
            ?: return MoveActionResponse("struggle")

        return chooseMove(move.first, activeBattlePokemon)
    }

    // estimate mid-battle switch in value
    fun estimateMatchup(activeBattlePokemon: ActiveBattlePokemon, aiSide: BattleSide, battle: PokemonBattle, nonActiveMon: TrackerPokemon? = null): Double {
        updateActiveTracker(aiSide, battle)
        val activeTrackerPokemon = nonActiveMon ?: activeTracker.alliedSide.activePokemon.first { it.id == activeBattlePokemon.battlePokemon!!.uuid }
        val currentAbility = activeTrackerPokemon.pokemon!!.ability
        val speedEstimation = statEstimationActive(activeTrackerPokemon, Stats.SPEED)

        val opponentActiveTracker = activeTracker.opponentSide.activePokemon

        var score = 1.0
        for (opponent in opponentActiveTracker) {
            // todo get count of moves on player side that are PHYSICAL
            // todo get count of moves on player side that are SPECIAL
            // todo Determine if it is a special or physical attacker
            // todo Determine value of matchup based on that attack type against the Defensive stats of the pokemon

            //type comparison
            score += (bestDamageMultiplier(activeTrackerPokemon, opponent) * moveDamageWeightConsideration) + (typeMatchup(activeTrackerPokemon, opponent) * typeMatchupWeightConsideration) // npcPokemon attacking playerPokemon
            score -= (bestDamageMultiplier(opponent, activeTrackerPokemon) * moveDamageWeightConsideration) + (typeMatchup(opponent, activeTrackerPokemon) * typeMatchupWeightConsideration) // playerPokemon attacking npcPokemon

            //Speed comparison
            if (speedEstimation > statEstimationActive(opponent, Stats.SPEED)) {
                score += speedTierCoefficient * trickRoomCoefficient
            } else if (statEstimationActive(opponent, Stats.SPEED) > speedEstimation) {
                score -= speedTierCoefficient * trickRoomCoefficient
            }

            // HP comparison
            score += (activeTrackerPokemon.currentHpPercent * hpFractionCoefficient) * hpWeightConsideration
            score -= (opponent.currentHpPercent * hpFractionCoefficient) * hpWeightConsideration
        }
        // add value to a pokemon with stat boost removal moves/abilities/items
        if ((opponentActiveTracker.any { it.boosts.getOrDefault(Stats.ATTACK, 0) > 1 || it.boosts.getOrDefault(Stats.SPECIAL_ATTACK, 0) > 1 })
            && (activeTrackerPokemon.moves.any { it.name in AIUtility.antiBoostMoves } || currentAbility.name == "unaware")) {
            score += antiBoostWeightConsideration
        }
        return score
    }

    fun hasMajorStatusImmunity(target: TrackerPokemon) : Boolean {
        // TODO: Need to check for Safeguard and Misty Terrain
        val ability = target.pokemon?.ability?.name ?: target.currentAbility ?: return false
        return listOf("comatose", "purifyingsalt").contains(ability) ||
                (activeTracker.currentWeather == "sunny" && ability == "leafguard")
    }

    fun shouldSwitchOut(side: BattleSide, battle: PokemonBattle, activeBattlePokemon: ActiveBattlePokemon, moveset: ShowdownMoveset): Boolean {
        updateActiveTracker(side, battle)
        if (moveset.trapped)
            return false

        val activeTrackerPokemon = activeTracker.alliedSide.activePokemon.first { it.id == activeBattlePokemon.battlePokemon!!.uuid }
        val actorTracker = activeTracker.alliedSide.actors.first { activeTrackerPokemon in it.activePokemon }
        val availableSwitches = actorTracker.party.filter { it.currentHp!! > 0 }
        val currentAbility = activeTrackerPokemon.pokemon!!.ability
        val speedEstimation = statEstimationActive(activeTrackerPokemon, Stats.SPEED)

        val opponentActiveTracker = activeTracker.opponentSide.activePokemon
        val opponentSpeedEstimations = opponentActiveTracker.map { statEstimationActive(it, Stats.SPEED) }
        // todo add some way to keep track of the player's boosting to see if it needs to switch out to something that can stop it

        // if slower speed stat than the opposing pokemon and HP is less than 20% don't switch out
        if (activeTrackerPokemon.currentHpPercent < hpSwitchOutThreshold && opponentSpeedEstimations.any { it > speedEstimation }) {
            return false
        }

        // if the npc pokemon was given Truant then switch it out if it is not it's base ability
        val legalAbilities = activeTrackerPokemon.pokemon?.species?.abilities ?: activeTrackerPokemon.form?.abilities ?: activeTrackerPokemon.species!!.abilities
        val truantAbility = Abilities.get("truant")
        val slowStartAbility = Abilities.get("slowstart")
        if ((currentAbility.template == truantAbility && legalAbilities.none { it.template == truantAbility }) ||
            (currentAbility.template == slowStartAbility && legalAbilities.none { it.template == slowStartAbility }))
            return true

        // if mon is locked in with a certain move/moves that is either a non-damaging move or a move that has no effect then switch
        val availableMoves = moveset.moves.filter { it.canBeUsed() }.mapNotNull { Moves.getByName(it.id) }
        val unavailableMoves = moveset.moves.filter { !it.canBeUsed() }.mapNotNull { Moves.getByName(it.id) }
        if (unavailableMoves.isNotEmpty() &&
            availableMoves.all { move -> opponentActiveTracker.all { moveDamageMultiplier(activeTrackerPokemon, move, it) == 0.0 } || move.power < 40 })
            return true

        // todo add more reasons to switch out
        // If there is a decent switch in and not trapped...
        if (availableSwitches.isEmpty() || availableSwitches.none { estimateMatchup(activeBattlePokemon, side, battle, it) > 0 })
            return false

        // ...and a 'good' reason to switch out
        if (opponentActiveTracker.any { it.boosts.getOrDefault(Stats.ACCURACY, 0) <= accuracySwitchThreshold } ||
            opponentActiveTracker.any { it.boosts.getOrDefault(Stats.DEFENCE, 0) <= -3 } ||
            opponentActiveTracker.any { it.boosts.getOrDefault(Stats.SPECIAL_DEFENCE, 0) <= -3 }
        )
            return true

        val (physicalAttackers, specialAttackers) = opponentActiveTracker
            .partition { (it.form?.baseStats ?: it.species!!.baseStats).getOrDefault(Stats.ATTACK, 0) >
                    (it.form?.baseStats ?: it.species!!.baseStats).getOrDefault(Stats.SPECIAL_ATTACK, 0) }
        if (physicalAttackers.any { it.boosts.getOrDefault(Stats.ATTACK, 0) <= -3 } ||
            specialAttackers.any { it.boosts.getOrDefault(Stats.SPECIAL_ATTACK, 0) <= -3 } )
            return true

        return estimateMatchup(activeBattlePokemon, side, battle) < switchOutMatchupThreshold &&
                activeTrackerPokemon.currentHpPercent > hpSwitchOutThreshold
    }

    fun statEstimationActive(mon: TrackerPokemon, stat: Stat): Double {
        val boost = mon.boosts[stat] ?: 0

        val actualBoost = if (boost > 1) {
            (2 + boost) / 2.0
        } else {
            2 / (2.0 - boost)
        }

        val baseStat = mon.pokemon?.species?.baseStats?.get(stat) ?: mon.species?.baseStats?.get(stat) ?: 0
        return ((2 * baseStat + 31) + 5) * actualBoost
    }

    // move: the move used
    // defender: the activeTracker Pokemon that the move is being used on
    fun moveDamageMultiplier(attacker: TrackerPokemon, move: MoveTemplate, defender: TrackerPokemon): Double {
        val defenderTypes = defender.pokemon?.types ?: defender.form?.types ?: defender.species?.types ?: emptyList()
        var multiplier = 1.0

        for (defenderType in defenderTypes)
            multiplier *= AIUtility.getDamageMultiplier(move.getEffectiveElementalType(attacker.pokemon), defenderType)

        return multiplier
    }

    // returns the best multiplier of an attacking move in the attacking pokemon's move list to deal with the defending pokemon's typing
    fun bestDamageMultiplier(attacker: TrackerPokemon, defender: TrackerPokemon): Double {
        val attackerMoves = attacker.pokemon?.moveSet ?: attacker.moves
        val defenderTypes = defender.pokemon?.types ?: defender.form?.types ?: defender.species?.types ?: emptyList()

        var multiplier = 1.0
        var bestMultiplier = 1.0

        for (attackerMove in attackerMoves) {
            for (defenderType in defenderTypes) {
                multiplier *= AIUtility.getDamageMultiplier(attackerMove.type, defenderType)
            }

            if (multiplier > bestMultiplier) {
                bestMultiplier = multiplier
            }

            multiplier = 1.0
        }

        return bestMultiplier
    }

    fun typeMatchup(attackingPokemon: TrackerPokemon, defendingPokemon: TrackerPokemon): Double {
        val attackerTypes = attackingPokemon.pokemon?.types ?: attackingPokemon.form?.types ?: attackingPokemon.species?.types ?: emptyList()
        val defenderTypes = defendingPokemon.pokemon?.types ?: defendingPokemon.form?.types ?: defendingPokemon.species?.types ?: emptyList()

        var multiplier = 1.0

        for (atkType in attackerTypes) {
            for (defType in defenderTypes) {
                multiplier *= AIUtility.getDamageMultiplier(atkType, defType)
            }
        }

        return multiplier
    }

    // returns an approximate number of hits for a given move for estimation purposes
    fun expectedHits(move: MoveTemplate): Int {
        val minMaxHits = AIUtility.multiHitMoves[move.name]
        if (move.name == "triplekick" || move.name == "tripleaxel") {
            //Triple Kick and Triple Axel have an accuracy check for each hit, and also
            //rise in BP for each hit
            return (1 + 2 * 0.9 + 3 * 0.81).toInt()
        }
        if (move.name == "populationbomb") {
            // population bomb hits until it misses, 90% accuracy
            return 7
        }
        return if (minMaxHits == null)
        // non multihit move
            1
        else if (minMaxHits.first == minMaxHits.second)
            minMaxHits.first
        else {
            // It hits 2-5 times
            (2 + 3) / 3 + (4 + 5) / 6
        }
    }

    private fun getNonZeroStats(name: String): Map<Stat, Int> {
        return AIUtility.boostFromMoves[name] ?: emptyMap()
    }

    private fun updateActiveTracker(aiSide: BattleSide, battle: PokemonBattle) {
        if(!activeTracker.isInitialized) {
            activeTracker.initialize(aiSide)
        }
        activeTracker.currentWeather = battle.contextManager.get(BattleContext.Type.WEATHER)?.firstOrNull()?.id
        activeTracker.currentTerrain = battle.contextManager.get(BattleContext.Type.TERRAIN)?.firstOrNull()?.id
        activeTracker.currentRoom = battle.contextManager.get(BattleContext.Type.ROOM)?.firstOrNull()?.id

        trickRoomCoefficient = if (activeTracker.currentRoom == "trickroom") // todo ALSO consider how many turns of Trick Room are left. If last turn then do not switch out
            -1.0
        else
            1.0

        activeTracker.updateActiveState(aiSide)

        /*val lastMajorBattleMessage = if (battle.majorBattleActions.entries.isNotEmpty()) battle.majorBattleActions?.entries?.last()?.value?.rawMessage else ""
        val lastMinorBattleMessage = if (battle.minorBattleActions.entries.isNotEmpty()) battle.minorBattleActions?.entries?.last()?.value?.rawMessage else ""
        val lastBattleState = battle.battleLog
        var currentType: String? = p1.activePokemon.currentPrimaryType
        // test parsing of the Type change
        val typeChangeIndex = lastMinorBattleMessage?.indexOf("typechange|")

        if (typeChangeIndex != -1) {
            // Add the length of "typechange|" to start from the end of this substring
            val startIndex = typeChangeIndex?.plus("typechange|".length)

            // Find the index of the next "|"
            val endIndex = startIndex?.let { lastMinorBattleMessage?.indexOf('|', it).takeIf { it!! >= 0 } }
                    ?: lastMinorBattleMessage?.length

            if (startIndex != null && endIndex != null ) {
                // Extract the substring
                val result = lastMinorBattleMessage.substring(startIndex, endIndex)

                // grab and store the type change
                currentType = ElementalTypes.get(result.lowercase())?.name
                //p1.activePokemon.currentPrimaryType
            }
        }*/
        // todo parse the battle message and grab the elemental typing after the |
    }

    private fun considerSwitching(
        activeBattlePokemon: ActiveBattlePokemon,
        activeTrackerPokemon: TrackerPokemon,
        opponents: List<TrackerPokemon>,
        availableMoves: List<Pair<InBattleMove, MoveTemplate>>,
        availableSwitches: List<Pair<TrackerPokemon, BattlePokemon>>,
        battle: PokemonBattle,
        aiSide: BattleSide
    ): ShowdownActionResponse? {
        val availableOpponentSwitches = activeTracker.opponentSide.actors.flatMap { it.party }.filter { it.currentHpPercent > 0 }

        // todo try to detect a player switch-in based on if they do that a lot
        // todo if player is in bad matchup against current AI pokemon, and they have switched out before, then they have a chance of switching to a favorable matchup
        // todo make it so that bestEstimation is actually in comparison to that potential pokemon instead and be sure to switch to that instead
        // todo maybe make it random chance to happen the higher the % chance the player likes to switch out AND/OR when the player has revealed more than 3-4 or their party?

        val bestEstimation = if (1 == 1 /* todo if player is in bad matchup and switches out a lot and has a better matchup in revealed party */) {
            // todo make it so that bestEstimation is actually in comparison to that potential pokemon instead and be sure to switch to that instead

            availableSwitches.maxOfOrNull { estimateMatchup(activeBattlePokemon, aiSide, battle, it.first) }
        } else {
            availableSwitches.maxOfOrNull { estimateMatchup(activeBattlePokemon, aiSide, battle, it.first) }
        }

        for (move in availableMoves.filter { it.first.id in AIUtility.pivotMoves }) {
            val singleTargetList = move.first.target.targetList.invoke(activeBattlePokemon)
                ?.map { it as ActiveBattlePokemon }
                ?.filter { it.battlePokemon != null && !it.isAllied(activeBattlePokemon) } // only want to hit enemies, right????
            val multiTargetList = activeBattlePokemon.getMultiTargetList(move.first.target)?.map { it as ActiveBattlePokemon }?.filter { it.battlePokemon != null }

            if (move.first.target == MoveTarget.self) {
                return chooseMove(move.first, activeBattlePokemon, activeBattlePokemon)
            } else if (singleTargetList != null) {
                val potentialTarget =  singleTargetList.associateWith { moveDamageMultiplier(activeTrackerPokemon, move.second, opponents.first {opp -> opp.id == it.battlePokemon!!.uuid}) }
                    .maxByOrNull { it.value }
                if (potentialTarget != null && potentialTarget.value > 0)
                    return chooseMove(move.first, activeBattlePokemon, potentialTarget.key)
            } else if (multiTargetList != null) {
                // multi target move
                // we need to check if our side is affected and if it is, weigh it up against opponents
                val (allies, opponents) = multiTargetList.partition { it.isAllied(activeBattlePokemon) }
                // Only consider allies in below checks if they are targeted
                val alliedTotalDamage = activeTracker.alliedSide.activePokemon.filter { it.id in allies.map { target -> target.battlePokemon!!.uuid } }.sumOf { calculateDamage(move.first, activeTrackerPokemon, it) }
                val opponentTotalDamage = activeTracker.opponentSide.activePokemon.filter { it.id in opponents.map { target -> target.battlePokemon!!.uuid } }.sumOf { calculateDamage(move.first, activeTrackerPokemon, it) }
                if (alliedTotalDamage > opponentTotalDamage)
                    continue // we harm ourselves more than them
                return chooseMove(move.first, activeBattlePokemon)
            }
        }

        availableMoves.firstOrNull { it.first.id == "chillyreception" && activeTracker.currentWeather != "snow"}?.let {
            return chooseMove(it.first, activeBattlePokemon)
        }

        val bestMatchup = availableSwitches.find { estimateMatchup(activeBattlePokemon, aiSide, battle, it.first) == bestEstimation }
        bestMatchup?.let {
            it.second.willBeSwitchedIn = true
            return SwitchActionResponse(it.second.uuid)
        }
        return null
    }

    fun considerAntiBoost(
        activeBattlePokemon: ActiveBattlePokemon,
        activeTrackerPokemon: TrackerPokemon,
        opponents: List<TrackerPokemon>,
        availableMoves: List<Pair<InBattleMove, MoveTemplate>>
    ): ShowdownActionResponse? {
        for (move in availableMoves.filter { it.first.id in AIUtility.antiBoostMoves }) {
            val singleTargetList = move.first.target.targetList.invoke(activeBattlePokemon)
                ?.map { it as ActiveBattlePokemon }
                ?.filter { it.battlePokemon != null && !it.isAllied(activeBattlePokemon) } // only want to clear boosts of enemies, right????
            val multiTargetList = activeBattlePokemon.getMultiTargetList(move.first.target)?.map { it as ActiveBattlePokemon }?.filter { it.battlePokemon != null }

            if (move.first.target == MoveTarget.self && activeTrackerPokemon.boosts.values.filter { it < 0 }.sum() < 3) {
                //TODO perhaps want to tweak this, mainly says if user has 3 debuffs, use move
                return chooseMove(move.first, activeBattlePokemon)
            } else if (singleTargetList != null && singleTargetList.isNotEmpty()) {
                //single target move, focus on mon with most boosts, if any is present
                val potentialTarget =  singleTargetList.associateWith { opponents.first {opp -> opp.id == it.battlePokemon!!.uuid}.boosts.values.filter { num -> num > 0 }.sum() }
                    .maxByOrNull { it.value }
                if (potentialTarget != null && potentialTarget.value > 0)
                    return chooseMove(move.first, activeBattlePokemon, potentialTarget.key)
            } else if (multiTargetList != null && !multiTargetList.isNotEmpty()) {
                // multi target move
                // we need to check if our side is affected and if it is, weigh it up against opponents
                val (allies, opponents) = multiTargetList.partition { it.isAllied(activeBattlePokemon) }
                // Only consider allies in below checks if they are targeted
                val alliedPositiveBoosts = activeTracker.alliedSide.activePokemon.filter { it.id in allies.map { target -> target.battlePokemon!!.uuid } }.sumOf { it.boosts.values.filter { num -> num > 0 }.sum() }
                val alliedNegativeBoosts = activeTracker.alliedSide.activePokemon.filter { it.id in allies.map { target -> target.battlePokemon!!.uuid } }.sumOf { it.boosts.values.filter { num -> num < 0 }.sum() }
                val opponentBoosts = activeTracker.opponentSide.activePokemon.filter { it.id in opponents.map { target -> target.battlePokemon!!.uuid } }.sumOf { it.boosts.values.filter { num -> num > 0 }.sum() }
                if ((allies.isNotEmpty() && opponents.isNotEmpty()) && (alliedPositiveBoosts > opponentBoosts))
                    continue // we harm ourselves more than then
                else if (opponents.isNotEmpty() && opponentBoosts <= 3) //TODO perhaps tweak this, means if the total positive boosts on all opponents are 3 or less, we ignore
                    continue // opponents are weakened already
                else if (allies.isNotEmpty() && alliedPositiveBoosts > alliedNegativeBoosts.absoluteValue) // got more buffs than debuffs right now... not worth it?
                    continue
                return chooseMove(move.first, activeBattlePokemon)
            }
        }

        return null
    }

    fun considerInflictingStatus(
        activeBattlePokemon: ActiveBattlePokemon,
        activeTrackerPokemon: TrackerPokemon,
        opponents: List<TrackerPokemon>,
        availableMoves: List<Pair<InBattleMove, MoveTemplate>>,
        battle: PokemonBattle,
        aiSide: BattleSide
    ): ShowdownActionResponse? {
        if (activeTrackerPokemon.currentHpPercent > 0.5) {
            // Status Inflicting Moves
            // todo calculate the chance of being able to knock out the opponent with one of the moves and if able to then do not do status move
            for (move in availableMoves) {
                val status = AIUtility.statusMoves[move.first.id] ?: continue
                val isPersistent = Statuses.getStatus(status) is PersistentStatus
                if (move.first.target == MoveTarget.self) continue // we couldn't possibly ever want to inflict a status on ourselves, right?????
                val singleTargetList = move.first.target.targetList.invoke(activeBattlePokemon)
                    ?.map { it as ActiveBattlePokemon }
                    ?.filter { it.battlePokemon != null && !it.isAllied(activeBattlePokemon) } // only want to inflict statuses on enemies, right???
                val multiTargetList = activeBattlePokemon.getMultiTargetList(move.first.target)?.map { it as ActiveBattlePokemon }?.filter { it.battlePokemon != null }
                if (singleTargetList != null && singleTargetList.isNotEmpty()) {
                    val sensibleTargets = opponents
                        .filter { it.id in singleTargetList.map { it.battlePokemon!!.uuid } }
                        .filterNot { (calculateDamage(move.first, activeTrackerPokemon, it) >= (it.currentHpPercent * statusDamageConsiderationThreshold)) }
                        .filter {
                            (if (isPersistent) it.currentStatus == null else it.currentVolatile != status) &&
                                    it.currentHpPercent > 0.3 &&
                                    AIUtility.canAffectWithStatus(status, it.species!!.types, it.currentAbility) &&
                                    !hasMajorStatusImmunity(it)
                        }
                    for (potentialTarget in sensibleTargets) {
                        when (status) {
                            Statuses.BURN.showdownName -> {
                                val stats = potentialTarget.species!!.baseStats.getOrDefault(Stats.ATTACK, 0) > 80
                                if (stats) {
                                    return chooseMove(move.first, activeBattlePokemon, singleTargetList.first { it.battlePokemon!!.uuid == potentialTarget.id })
                                }
                            }
                            Statuses.PARALYSIS.showdownName -> {
                                val electricVersusGround = move.second.getEffectiveElementalType(activeTrackerPokemon.pokemon) == ElementalTypes.ELECTRIC &&
                                        potentialTarget.species!!.types.any { type -> type.name == "ground"}
                                val stats = potentialTarget.species!!.baseStats.getOrDefault(Stats.SPEED, 0) >
                                        activeTrackerPokemon.pokemon!!.species.baseStats.getOrDefault(Stats.SPEED, 0)
                                if (!electricVersusGround && stats) {
                                    return chooseMove(move.first, activeBattlePokemon, singleTargetList.first { it.battlePokemon!!.uuid == potentialTarget.id })
                                }
                            }
                            Statuses.SLEEP.showdownName -> {
                                if (move.first.id in listOf("spore", "sleeppoweder")) {
                                    return chooseMove(move.first, activeBattlePokemon, singleTargetList.first { it.battlePokemon!!.uuid == potentialTarget.id })
                                }
                            }
                            Statuses.POISON.showdownName,
                            Statuses.POISON_BADLY.showdownName,
                            "cursed",
                            "leech",
                            Statuses.CONFUSE.showdownName -> {
                                return chooseMove(move.first, activeBattlePokemon, singleTargetList.first { it.battlePokemon!!.uuid == potentialTarget.id })
                            }
                        }
                    }
                } else if (multiTargetList != null && multiTargetList.isNotEmpty()) {
                    val (allies, opponents) = multiTargetList.partition { it.isAllied(activeBattlePokemon) }
                    val possibleAffectedAllies = activeTracker.alliedSide.activePokemon.filter { it.id in allies.map { target -> target.battlePokemon!!.uuid } }.count {
                        (if (isPersistent) it.currentStatus == null else it.currentVolatile != status) &&
                                it.currentHpPercent > 0.3 &&
                                AIUtility.canAffectWithStatus(status, it.species!!.types, it.currentAbility) &&
                                !hasMajorStatusImmunity(it) }
                    val possibleAffectedOpponents = activeTracker.opponentSide.activePokemon.filter { it.id in opponents.map { target -> target.battlePokemon!!.uuid } }.count {
                        (if (isPersistent) it.currentStatus == null else it.currentVolatile != status) &&
                                it.currentHpPercent > 0.3 &&
                                AIUtility.canAffectWithStatus(status, it.species!!.types, it.currentAbility) &&
                                !hasMajorStatusImmunity(it) }
                    if (possibleAffectedAllies > possibleAffectedOpponents)
                        continue

                    return chooseMove(move.first, activeBattlePokemon)
                }
            }
        }

        // Accuracy lowering moves
        for (move in availableMoves) {
            // opponents.any { it.boosts.getOrDefault(Stats.ACCURACY, 0) > boostWeightCoefficient })
            if (move.first.id in AIUtility.accuracyLoweringMoves && activeTrackerPokemon.currentHpPercent == 1.0 &&
                estimateMatchup(activeBattlePokemon, aiSide, battle) > 0) {
                if (move.first.target == MoveTarget.self) continue // we couldn't possibly ever want to inflict a status on ourselves, right?????
                val singleTargetList = move.first.target.targetList.invoke(activeBattlePokemon)
                    ?.map { it as ActiveBattlePokemon }
                    ?.filter { it.battlePokemon != null && !it.isAllied(activeBattlePokemon) } // only want to inflict statuses on enemies, right???
                val multiTargetList = activeBattlePokemon.getMultiTargetList(move.first.target)?.map { it as ActiveBattlePokemon }?.filter { it.battlePokemon != null }
                if (singleTargetList != null && singleTargetList.isNotEmpty()) {
                    val target = singleTargetList.firstOrNull { opp -> opponents.first { it.id == opp.battlePokemon!!.uuid }.boosts.getOrDefault(Stats.ACCURACY, 0) > boostWeightCoefficient }
                    target?.let {
                        return chooseMove(move.first, activeBattlePokemon, it)
                    }
                } else if (multiTargetList != null && multiTargetList.isNotEmpty()) {
                    val (allies, opponents) = multiTargetList.partition { it.isAllied(activeBattlePokemon) }
                    val possibleAffectedAllies = activeTracker.alliedSide.activePokemon.filter { it.id in allies.map { target -> target.battlePokemon!!.uuid } }
                        .count { it.boosts.getOrDefault(Stats.ACCURACY, 0) > boostWeightCoefficient }
                    val possibleAffectedOpponents = activeTracker.opponentSide.activePokemon.filter { it.id in opponents.map { target -> target.battlePokemon!!.uuid } }
                        .count { it.boosts.getOrDefault(Stats.ACCURACY, 0) > boostWeightCoefficient }

                    if (possibleAffectedAllies > possibleAffectedOpponents)
                        continue
                    return chooseMove(move.first, activeBattlePokemon)
                }
            }
        }

        return null
    }

    fun findAndUseMostDamagingMove(
        activeBattlePokemon: ActiveBattlePokemon,
        activeTrackerPokemon: TrackerPokemon,
        opponents: List<TrackerPokemon>,
        availableMoves: List<Pair<InBattleMove, MoveTemplate>>,
        moveset: ShowdownMoveset,
        battle: PokemonBattle,
        aiSide: BattleSide
    ): ShowdownActionResponse? {
        // Damage dealing moves
        val moveValues = mutableMapOf<Pair<InBattleMove, MoveTemplate>, Pair<ActiveBattlePokemon?, Double>>()
        for (move in availableMoves) {
            // calculate initial damage of this move
            var value = calculateDamage(move, activeBattlePokemon, activeTrackerPokemon) // set value to be the output of damage to start with

            // Handle special cases
            if (move.first.id == "fakeout") {
                value = null to 0.0
            }

            if (move.first.id == "synchronoise"
                && !(activeTrackerPokemon.pokemon!!.types.any { ownType -> opponents.any { opp -> opp.species!!.types.contains(ownType) } })) {
                value = null to 0.0
            }

            // if opposing pokemon is steel type or poison type value this higher
            if (move.first.id == "soak" && value.first?.battlePokemon?.effectedPokemon?.species?.types?.none { type -> type.name == "steel" || type.name == "posion"} == true)
                value = value.first to 200.0 // change this to not be so hardcoded but valued for different circumstances


            // todo last resort: only does damage if all other moves have been used at least once (switchout resets this)

            // todo focus punch

            // todo slack off

            // todo stealth rock. Make list of all active hazards to get referenced

            // reduce value of Pivot moves if user doesn't want to switchout anyways todo unless maybe it was the only damaging move and needs to
            if(move.first.id in AIUtility.pivotMoves && opponents.all { moveDamageMultiplier(activeTrackerPokemon, move.second, it) != 0.0 }
                && (!shouldSwitchOut(aiSide, battle, activeBattlePokemon, moveset) && opponents.none { !mostDamagingMove(move.first, moveset, activeTrackerPokemon, it)}))
                value = null to 0.0

            moveValues[move] = value
        }

        val bestMove = moveValues.maxBy { it.value.second }
        val target = if (bestMove.key.first.mustBeUsed()) null else bestMove.value.first
        return MoveActionResponse(bestMove.key.first.id, target?.getPNX())
    }

    private fun chooseMove(move: InBattleMove, activeBattlePokemon: ActiveBattlePokemon, target: ActiveBattlePokemon? = null): MoveActionResponse {
        val targets = if (move.mustBeUsed()) null else move.target.targetList(activeBattlePokemon)
        if (targets == null)
            return MoveActionResponse(move.id)
        else {
            val chosenTarget = target ?: targets.filter { !it.isAllied(activeBattlePokemon) }.randomOrNull() ?: targets.random()
            return MoveActionResponse(move.id, chosenTarget.getPNX())
        }
    }
}