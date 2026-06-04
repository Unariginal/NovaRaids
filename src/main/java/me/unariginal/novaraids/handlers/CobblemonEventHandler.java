package me.unariginal.novaraids.handlers;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.events.drops.LootDroppedEvent;
import com.cobblemon.mod.common.api.events.entity.PokemonEntitySaveToWorldEvent;
import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.api.events.pokeball.ThrownPokeballHitEvent;
import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonSentEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.PokeBallItem;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.platform.events.PlatformEvents;
import com.cobblemon.mod.common.platform.events.ServerPlayerEvent;
import com.cobblemon.mod.common.pokemon.Pokemon;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import kotlin.Unit;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.cache.PlayerRaidCache;
import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.data.categories.bosses.Boss;
import me.unariginal.novaraids.data.categories.Category;
import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.handlers.custom.*;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;
import me.unariginal.novaraids.raid.RaidPhase;
import me.unariginal.novaraids.utils.BanHandler;
import me.unariginal.novaraids.utils.TextUtils;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.*;

import static me.unariginal.novaraids.config.ConfigManager.*;
import static me.unariginal.novaraids.raid.RaidManager.activeRaids;

@SuppressWarnings("UnusedReturnValue")
public class CobblemonEventHandler {
    private static Unit unit() {
        return Unit.INSTANCE;
    }

    private static final NovaRaids nr = NovaRaids.INSTANCE;

    public static void initialiseEvents() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGHEST, event -> {
            return onBattleStartedPre(event);
        });

        CobblemonEvents.BATTLE_FLED.subscribe(CobblemonEventHandler::onBattleFled);
        CobblemonEvents.LOOT_DROPPED.subscribe(CobblemonEventHandler::onLootDropped);
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(CobblemonEventHandler::onThrownPokeballHit);
        CobblemonEvents.POKEMON_CAPTURED.subscribe(CobblemonEventHandler::onPokemonCaptured);
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(CobblemonEventHandler::onExperienceGainedPre);
        CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.subscribe(CobblemonEventHandler::onSavePokemonEntity);
        CobblemonEvents.POKEMON_SENT_POST.subscribe(CobblemonEventHandler::onSendOutPokemon);
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(CobblemonEventHandler::onPokemonEntitySpawn);
        PlatformEvents.SERVER_PLAYER_LOGOUT.subscribe(CobblemonEventHandler::onPlayerLogout);

        UseItemCallback.EVENT.register(CobblemonEventHandler::onRightClick);
        AttackEntityCallback.EVENT.register(CobblemonEventHandler::onAttackEntity);

        RaidStartEventHandler.register();
        RaidEndEventHandler.register();
        RaidLostEventHandler.register();
        BossDamagedEventHandler.register();
        BossDefeatedEventHandler.register();
        SetupPhaseEventHandler.register();
        FightPhaseEventHandler.register();
        CatchWarningPhaseEventHandler.register();
        CatchPhaseEventHandler.register();
    }

    private static Unit onThrownPokeballHit(ThrownPokeballHitEvent event) {
        PokemonEntity pokemonEntity = event.getPokemon();
        Pokemon pokemon = pokemonEntity.getPokemon();
        if (pokemon.getPersistentData().contains("raid_entity")) {
            for (Raid raid : activeRaids.values()) {
                for (PokemonEntity clone : raid.clones.keySet()) {
                    if (clone.getUuid().equals(pokemonEntity.getUuid())) {
                        raid.addPokeballsCapturing(event.getPokeBall());
                        return unit();
                    }
                }
            }
            pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
            event.cancel();
        }
        return unit();
    }

    private static Unit onPokemonCaptured(PokemonCapturedEvent event) {
        ServerPlayerEntity player = event.getPlayer();
        Pokemon pokemon = event.getPokemon();
        // If a player is in a raid they could only be catching a catch phase pokemon. No further checks are needed.
        Raid raid = PlayerRaidCache.currentRaid(player);
        if (raid != null) {
            raid.catchPhaseResults.put(player.getUuidAsString(), new RaidHistory.CatchDetails(
                    player.getUuidAsString(),
                    player.getNameForScoreboard(),
                    pokemon.getSpecies().getName(),
                    pokemon.getForm().formOnlyShowdownId(),
                    pokemon.getIvs(),
                    pokemon.getEvs(),
                    pokemon.getShiny()
            ));
        }
        return unit();
    }

    private static Unit onBattleStartedPre(BattleStartedEvent.Pre event) {
        PokemonBattle battle = event.getBattle();
        ServerPlayerEntity player = null;
        PokemonEntity pokemonEntity = null;

        for (BattleActor actor : battle.getActors()) {
            if (actor instanceof PlayerBattleActor playerBattleActor) {
                player = playerBattleActor.getEntity();
            } else if (actor instanceof PokemonBattleActor pokemonBattleActor) {
                pokemonEntity = pokemonBattleActor.getEntity();
            }
        }

        if (player != null && pokemonEntity != null) {
            UUID entityUUID = pokemonEntity.getUuid();
            for (Raid raid : activeRaids.values()) {
                for (PokemonEntity clone : raid.clones.keySet()) {
                    if (clone.getUuid().equals(entityUUID)) {
                        if (!raid.clones.get(clone).equals(player.getUuid())) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.notYourEncounter, raid)));
                            event.cancel();
                        }
                        return unit();
                    }
                }

                if (!raid.bossEntity.getUuid().equals(entityUUID)) continue;

                if (!raid.isParticipating(player)) {
                    event.cancel();
                    return unit();
                }

                for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                    if (pokemon != null) {
                        if (pokemon.getLevel() < raid.boss.raidDetails.minimumLevel) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.minimumLevel, raid)));
                            event.cancel();
                            return unit();
                        }
                        if (pokemon.getLevel() > raid.boss.raidDetails.maximumLevel) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.maximumLevel, raid)));
                            event.cancel();
                            return unit();
                        }
                    }
                }

                if (raid.phase == RaidPhase.FIGHT) {
                    if (!BanHandler.hasContraband(player, raid.boss)) {
                        BattleHandler.invokeBattle(raid, player);
                    }
                }

                event.cancel();
                return unit();
            }
        }

        if (player != null) {
            Raid raid = PlayerRaidCache.currentRaid(player);
            if (raid != null) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.battleDuringRaid, raid)));
                event.cancel();
            }
        }

        return unit();
    }

    private static Unit onBattleFled(BattleFledEvent event) {
        PokemonBattle battle = event.getBattle();
        for (BattleActor actor : battle.getActors()) {
            if (!(actor instanceof PlayerBattleActor playerBattleActor) || playerBattleActor.getEntity() == null) {
                return unit();
            }

            ServerPlayerEntity player = playerBattleActor.getEntity();
            Raid raid = PlayerRaidCache.currentRaid(player);

            if (raid == null) return unit();

            raid.addFleeingPlayer(player.getUuid());
            List<PokemonEntity> toRemove = new ArrayList<>();
            for (PokemonEntity cloneEntity : raid.clones.keySet()) {
                if (raid.clones.get(cloneEntity).equals(player.getUuid())) {
                    toRemove.add(cloneEntity);
                }
            }
            for (PokemonEntity cloneEntity : toRemove) {
                raid.removeClone(cloneEntity, true);
            }
        }

        return unit();
    }

    private static Unit onLootDropped(LootDroppedEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof PokemonEntity pokemonEntity) {
            Pokemon pokemon = pokemonEntity.getPokemon();
            if (pokemon.getPersistentData().contains("raid_entity")) event.cancel();
        }
        return unit();
    }

    private static Unit onExperienceGainedPre(ExperienceGainedEvent.Pre event) {
        Pokemon pokemon = event.getPokemon();
        if (!pokemon.isPlayerOwned()) return unit();

        ServerPlayerEntity player = pokemon.getOwnerPlayer();
        if (player == null) return unit();

        Raid raid = PlayerRaidCache.currentRaid(player);
        if (raid == null) return unit();

        if ((!CONFIG.raidSettings.allowExperienceGain || raid.isPlayerFleeing(player.getUuid())) && event.getSource().isBattle()) {
            event.cancel();
        }

        return unit();
    }

    private static Unit onPlayerLogout(ServerPlayerEvent.Logout event) {
        nr.server.execute(() -> {
            ServerPlayerEntity player = event.getPlayer();
            Raid raid = PlayerRaidCache.currentRaid(player);
            if (raid == null) return;

            raid.removePlayer(player);
            if (raid.phase != RaidPhase.INIT && raid.phase != RaidPhase.SETUP && raid.phase != RaidPhase.STOPPING && raid.participatingPlayers.isEmpty()) {
                raid.stop();
            }
        });

        return unit();
    }

    private static Unit onSavePokemonEntity(PokemonEntitySaveToWorldEvent event) {
        PokemonEntity pokemonEntity = event.getPokemonEntity();
        Pokemon pokemon = pokemonEntity.getPokemon();
        if (pokemon.getPersistentData().contains("raid_entity")) {
            event.cancel();
        }
        return unit();
    }

    private static Unit onSendOutPokemon(PokemonSentEvent.Post event) {
        if (!CONFIG.raidSettings.reduceLargePokemonSize) return unit();

        Pokemon pokemon = event.getPokemon();
        if (!pokemon.isPlayerOwned()) return unit();

        PokemonEntity pokemonEntity = event.getPokemonEntity();

        ServerPlayerEntity player = pokemon.getOwnerPlayer();
        if (player == null) return unit();

        Raid raid = PlayerRaidCache.currentRaid(player);
        if (raid == null) return unit();

        Box hitbox = pokemonEntity.getBoundingBox();
        double maxLength = Math.max(Math.max(hitbox.getLengthX(), hitbox.getLengthY()), hitbox.getLengthZ());
        if (maxLength > 1) {
            double scale = 1 / maxLength;
            EntityAttributeInstance scaleAttribute = pokemonEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
            if (scaleAttribute != null) scaleAttribute.setBaseValue(scale);
        }
        return unit();
    }

    private static Unit onPokemonEntitySpawn(SpawnEvent<PokemonEntity> event) {
        if (CONFIG.raidSettings.disableSpawnsInArena) {
            BlockPos pos = event.getSpawnablePosition().getPosition();
            for (Raid raid : activeRaids.values()) {
                if (raid.location.isPointInLocation(pos.getX(), pos.getZ())) {
                    event.cancel();
                    break;
                }
            }
        }
        return unit();
    }

    private static ActionResult onAttackEntity(PlayerEntity playerEntity, World world, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            Pokemon pokemon = pokemonEntity.getPokemon();
            if (pokemon.getPersistentData().contains("raid_entity")) {
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }

    private static TypedActionResult<ItemStack> onRightClick(PlayerEntity playerEntity, World world, Hand hand) {
        ServerPlayerEntity player = nr.server.getPlayerManager().getPlayer(playerEntity.getUuid());
        if (player != null) {
            ItemStack itemStack = player.getStackInHand(hand);
            NbtComponent customData = itemStack.getComponents().get(DataComponentTypes.CUSTOM_DATA);

            NbtCompound passNBT = new NbtCompound();
            passNBT.putString("raid_item", "raid_pass");
            NbtCompound voucherNBT = new NbtCompound();
            voucherNBT.putString("raid_item", "raid_voucher");

            if (customData != null) {
                if (hand.name().contains("MAIN_HAND") && customData.contains("raid_item")) {
                    if (customData.copyNbt().getString("raid_item").equals("raid_pass")) {
                        String bossName = customData.copyNbt().getString("raid_boss");
                        String category = customData.copyNbt().getString("raid_category");
                        if (bossName.equalsIgnoreCase("*")) {
                            List<Raid> joinableRaids = new ArrayList<>();
                            if (category.equalsIgnoreCase("*")) {
                                joinableRaids.addAll(activeRaids.values().stream().filter(raid -> raid.phase == RaidPhase.SETUP).toList());
                            } else {
                                for (Raid raid : activeRaids.values()) {
                                    if (raid.boss.categoryId.equalsIgnoreCase(category)) {
                                        joinableRaids.add(raid);
                                    }
                                }
                            }

                            Map<Integer, SimpleGui> pages = new HashMap<>();
                            int pageTotal = BaseGUI.getPageTotal(joinableRaids.size(), RAID_PASS_GUI.getTotalSlotsBySymbol(RAID_PASS_GUI.raidDisplayItem.symbol));
                            for (int i = 1; i <= pageTotal; i++) {
                                SimpleGui gui = new SimpleGui(RAID_PASS_GUI.getScreenHandler(), player, false);
                                gui.setTitle(TextUtils.deserialize(TextUtils.parse(RAID_PASS_GUI.guiTitle)));
                                pages.put(i, gui);
                            }

                            int index = 0;
                            for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
                                for (Integer slot : RAID_PASS_GUI.getSlotsBySymbol(RAID_PASS_GUI.raidDisplayItem.symbol)) {
                                    if (index < joinableRaids.size()) {
                                        Raid raid = joinableRaids.get(index);

                                        List<Text> lore = new ArrayList<>();
                                        for (String line : RAID_PASS_GUI.raidDisplayItem.itemLore) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                                        }

                                        ItemStack item = RAID_PASS_GUI.raidDisplayItem.item.copyComponentsToNewStack(PokemonItem.from(raid.bossPokemon).getItem(), 1);
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_PASS_GUI.raidDisplayItem.itemName, raid)))
                                                .setLore(lore)
                                                .setCallback((num, clickType, slotActionType) -> {

                                                    if (!clickType.isLeft) return;

                                                    if (!raid.requiresPass) {
                                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.noPassNeeded, raid)));
                                                        return;
                                                    }

                                                    if (raid.phase != RaidPhase.SETUP) {
                                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.notJoinable, raid)));
                                                        return;
                                                    }

                                                    boolean hasSpace = raid.participatingPlayers.size() < raid.maxPlayers ||
                                                            raid.maxPlayers == -1 ||
                                                            Permissions.check(player, "novaraids.override");

                                                    if (!hasSpace) {
                                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.maxPlayers, raid)));
                                                        return;
                                                    }

                                                    if (raid.addPlayer(player.getUuid(), true)) {
                                                        itemStack.decrement(1);
                                                        player.setStackInHand(hand, itemStack);

                                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.joinedRaid, raid)));

                                                        pageEntry.getValue().close();
                                                    }

                                                }).build();
                                        pageEntry.getValue().setSlot(slot, element);
                                        index++;
                                    } else {
                                        ItemStack item = RAID_PASS_GUI.backgroundItem.item.copy();
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : RAID_PASS_GUI.backgroundItem.itemLore) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_PASS_GUI.backgroundItem.itemName)))
                                                .setLore(lore)
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }
                                }

                                if (pageEntry.getKey() < pageTotal) {
                                    for (Integer slot : RAID_PASS_GUI.getSlotsBySymbol(RAID_PASS_GUI.nextItem.symbol)) {
                                        ItemStack item = RAID_PASS_GUI.nextItem.item.copy();
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : RAID_PASS_GUI.nextItem.itemLore) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_PASS_GUI.nextItem.itemName)))
                                                .setLore(lore)
                                                .setCallback(clickType -> {
                                                    pageEntry.getValue().close();
                                                    pages.get(pageEntry.getKey() + 1).open();
                                                })
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }
                                }

                                if (pageEntry.getKey() > 1) {
                                    for (Integer slot : RAID_PASS_GUI.getSlotsBySymbol(RAID_PASS_GUI.previousItem.symbol)) {
                                        ItemStack item = RAID_PASS_GUI.previousItem.item.copy();
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : RAID_PASS_GUI.previousItem.itemLore) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_PASS_GUI.previousItem.itemName)))
                                                .setLore(lore)
                                                .setCallback(clickType -> {
                                                    pageEntry.getValue().close();
                                                    pages.get(pageEntry.getKey() - 1).open();
                                                })
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }
                                }

                                for (Integer slot : RAID_PASS_GUI.getSlotsBySymbol(RAID_PASS_GUI.closeItem.symbol)) {
                                    ItemStack item = RAID_PASS_GUI.closeItem.item.copy();
                                    List<Text> lore = new ArrayList<>();
                                    for (String line : RAID_PASS_GUI.closeItem.itemLore) {
                                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                    }
                                    GuiElement element = new GuiElementBuilder(item)
                                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_PASS_GUI.closeItem.itemName)))
                                            .setLore(lore)
                                            .setCallback(clickType -> pageEntry.getValue().close())
                                            .build();
                                    pageEntry.getValue().setSlot(slot, element);
                                }

                                for (Integer slot : RAID_PASS_GUI.getSlotsBySymbol(RAID_PASS_GUI.backgroundItem.symbol)) {
                                    ItemStack item = RAID_PASS_GUI.backgroundItem.item.copy();
                                    List<Text> lore = new ArrayList<>();
                                    for (String line : RAID_PASS_GUI.backgroundItem.itemLore) {
                                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                    }
                                    GuiElement element = new GuiElementBuilder(item)
                                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_PASS_GUI.backgroundItem.itemName)))
                                            .setLore(lore)
                                            .build();
                                    pageEntry.getValue().setSlot(slot, element);
                                }
                            }
                            if (!pages.isEmpty()) {
                                pages.get(1).open();
                            }
                        } else {

                            if (PlayerRaidCache.isInRaid(player)) {
                                return TypedActionResult.fail(itemStack);
                            }

                            for (Raid raid : activeRaids.values()) {
                                if (raid.boss.bossId.equalsIgnoreCase(bossName)) {
                                    if (raid.requiresPass) {
                                        if (raid.phase == RaidPhase.SETUP) {
                                            if (raid.participatingPlayers.size() < raid.maxPlayers || raid.maxPlayers == -1 || Permissions.check(player, "novaraids.override")) {
                                                if (raid.addPlayer(player.getUuid(), true)) {
                                                    itemStack.decrement(1);
                                                    player.setStackInHand(hand, itemStack);

                                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.joinedRaid, raid)));
                                                }
                                            } else {
                                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.maxPlayers, raid)));
                                            }
                                        } else {
                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.notJoinable, raid)));
                                        }
                                    } else {
                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.noPassNeeded, raid)));
                                    }
                                }
                            }
                        }
                    } else if (customData.copyNbt().getString("raid_item").equals("raid_voucher") && CONFIG.itemSettings.voucherSettings.vouchersEnabled) {
                        String bossId = customData.copyNbt().getString("raid_boss");
                        String categoryId = customData.copyNbt().getString("raid_category");
                        if (bossId.equalsIgnoreCase("*")) {
                            List<Boss> availableRaids = new ArrayList<>();
                            if (categoryId.equalsIgnoreCase("*")) {
                                availableRaids.addAll(BOSSES.values());
                            } else {
                                Category category = Category.getCategory(categoryId);
                                if (category != null) availableRaids.addAll(category.bosses.values());
                            }

                            Map<Integer, SimpleGui> pages = new HashMap<>();
                            int pageTotal = BaseGUI.getPageTotal(availableRaids.size(), RAID_VOUCHER_GUI.getTotalSlotsBySymbol(RAID_VOUCHER_GUI.raidDisplayItem.symbol));
                            for (int i = 1; i <= pageTotal; i++) {
                                SimpleGui gui = new SimpleGui(RAID_VOUCHER_GUI.getScreenHandler(), player, false);
                                gui.setTitle(TextUtils.deserialize(TextUtils.parse(RAID_VOUCHER_GUI.guiTitle)));
                                pages.put(i, gui);
                            }

                            int index = 0;
                            for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
                                for (Integer slot : RAID_VOUCHER_GUI.getSlotsBySymbol(RAID_VOUCHER_GUI.raidDisplayItem.symbol)) {
                                    if (index < availableRaids.size()) {
                                        Boss boss = availableRaids.get(index);

                                        List<Text> lore = new ArrayList<>();
                                        for (String line : RAID_VOUCHER_GUI.raidDisplayItem.itemLore) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line, boss)));
                                        }

                                        // TODO: Check performance of calling createPokemon() each time!
                                        ItemStack item = RAID_VOUCHER_GUI.raidDisplayItem.item.copyComponentsToNewStack(PokemonItem.from(boss.pokemonDetails.createPokemon()).getItem(), 1);
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_VOUCHER_GUI.raidDisplayItem.itemName, boss)))
                                                .setLore(lore)
                                                .setCallback((num, clickType, slotActionType) -> {
                                                    if (clickType.isLeft) {
                                                        RaidManager.queueRaid(boss, player, itemStack, null);
                                                        itemStack.decrement(1);
                                                        player.setStackInHand(hand, itemStack);

                                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.usedVoucher, boss)));

                                                        pageEntry.getValue().close();
                                                    }
                                                }).build();
                                        pageEntry.getValue().setSlot(slot, element);
                                        index++;
                                    } else {
                                        ItemStack item = RAID_VOUCHER_GUI.backgroundItem.item.copy();
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : RAID_VOUCHER_GUI.backgroundItem.itemLore) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_VOUCHER_GUI.backgroundItem.itemName)))
                                                .setLore(lore)
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }
                                }

                                if (pageEntry.getKey() < pageTotal) {
                                    for (Integer slot : RAID_VOUCHER_GUI.getSlotsBySymbol(RAID_VOUCHER_GUI.nextItem.symbol)) {
                                        ItemStack item = RAID_VOUCHER_GUI.nextItem.item.copy();
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : RAID_VOUCHER_GUI.nextItem.itemLore) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_VOUCHER_GUI.nextItem.itemName)))
                                                .setLore(lore)
                                                .setCallback(clickType -> {
                                                    pageEntry.getValue().close();
                                                    pages.get(pageEntry.getKey() + 1).open();
                                                })
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }
                                }

                                if (pageEntry.getKey() > 1) {
                                    for (Integer slot : RAID_VOUCHER_GUI.getSlotsBySymbol(RAID_VOUCHER_GUI.previousItem.symbol)) {
                                        ItemStack item = RAID_VOUCHER_GUI.previousItem.item.copy();
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : RAID_VOUCHER_GUI.previousItem.itemLore) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(RAID_VOUCHER_GUI.previousItem.itemName)))
                                                .setLore(lore)
                                                .setCallback(clickType -> {
                                                    pageEntry.getValue().close();
                                                    pages.get(pageEntry.getKey() - 1).open();
                                                })
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }
                                }

                                for (Integer slot : RAID_VOUCHER_GUI.getSlotsBySymbol(RAID_VOUCHER_GUI.closeItem.symbol)) {
                                    ItemStack item = RAID_VOUCHER_GUI.closeItem.item.copy();
                                    List<Text> lore = new ArrayList<>();
                                    for (String line : RAID_VOUCHER_GUI.closeItem.itemLore) {
                                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                    }
                                    GuiElement element = new GuiElementBuilder(item)
                                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_VOUCHER_GUI.closeItem.itemName)))
                                            .setLore(lore)
                                            .setCallback(clickType -> pageEntry.getValue().close())
                                            .build();
                                    pageEntry.getValue().setSlot(slot, element);
                                }

                                for (Integer slot : RAID_VOUCHER_GUI.getSlotsBySymbol(RAID_VOUCHER_GUI.backgroundItem.symbol)) {
                                    ItemStack item = RAID_VOUCHER_GUI.backgroundItem.item.copy();
                                    List<Text> lore = new ArrayList<>();
                                    for (String line : RAID_VOUCHER_GUI.backgroundItem.itemLore) {
                                        lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                    }
                                    GuiElement element = new GuiElementBuilder(item)
                                            .setName(TextUtils.deserialize(TextUtils.parse(RAID_VOUCHER_GUI.backgroundItem.itemName)))
                                            .setLore(lore)
                                            .build();
                                    pageEntry.getValue().setSlot(slot, element);
                                }
                            }
                            if (!pages.isEmpty()) {
                                pages.get(1).open();
                            }
                        } else if (bossId.equalsIgnoreCase("random")) {
                            Boss bossInfo;
                            if (categoryId.equalsIgnoreCase("null")) {
                                bossInfo = Boss.getRandomBoss(null);
                            } else {
                                bossInfo = Boss.getRandomBoss(categoryId, null);
                            }

                            if (bossInfo == null) return TypedActionResult.fail(itemStack);

                            RaidManager.queueRaid(bossInfo, player, itemStack, null);
                            itemStack.decrement(1);
                            player.setStackInHand(hand, itemStack);

                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.usedVoucher, bossInfo)));
                        } else {
                            Boss boss = Boss.getBoss(bossId);
                            RaidManager.queueRaid(boss, player, itemStack, null);
                            itemStack.decrement(1);
                            player.setStackInHand(hand, itemStack);

                            if (boss != null) player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.usedVoucher, boss)));
                        }
                    } else if (customData.copyNbt().getString("raid_item").equals("raid_ball") && CONFIG.itemSettings.raidBallSettings.raidBallsEnabled) {
                        boolean canThrow;

                        if (CONFIG.itemSettings.raidBallSettings.playerLinkedRaidBalls && customData.contains("owner_uuid")) {
                            if (!customData.copyNbt().getUuid("owner_uuid").equals(player.getUuid())) {
                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.notYourRaidPokeball)));
                                return TypedActionResult.fail(itemStack);
                            }
                        }

                        canThrow = PlayerRaidCache.isInRaid(player.getUuid());

                        if (canThrow) {
                            canThrow = false;

                            Raid raid = PlayerRaidCache.currentRaid(player);
                            if (raid != null && raid.phase == RaidPhase.CATCH) {

                                if (customData.contains("raid_boss") && customData.contains("raid_category")) {
                                    String boss = customData.copyNbt().getString("raid_boss");
                                    String category = customData.copyNbt().getString("raid_category");
                                    if (boss.equalsIgnoreCase("*") && category.equalsIgnoreCase("*")) {
                                        if (raid.boss.itemSettings.allowGlobalPokeballs) {
                                            canThrow = true;
                                        }
                                    } else if (boss.equalsIgnoreCase("*")) {
                                        if (raid.boss.categoryId.equalsIgnoreCase(category)) {
                                            if (raid.boss.itemSettings.allowCategoryPokeballs) {
                                                canThrow = true;
                                            }
                                        }
                                    } else {
                                        if (raid.boss.bossId.equalsIgnoreCase(boss)) {
                                            canThrow = true;
                                        }
                                    }
                                } else {
                                    canThrow = true;
                                }

                            }

                        } else {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.raidPokeballOutsideRaid)));
                            return TypedActionResult.fail(itemStack);
                        }

                        if (!canThrow) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.notCatchPhase)));
                            return TypedActionResult.fail(itemStack);
                        } else {
                            return TypedActionResult.pass(itemStack);
                        }
                    }
                }
            }

            Raid raid = PlayerRaidCache.currentRaid(player);
            if (raid == null) return TypedActionResult.pass(itemStack);

            if (isPokeball(itemStack) && CONFIG.itemSettings.raidBallSettings.raidBallsEnabled) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.normalPokeball)));
                return TypedActionResult.fail(itemStack);
            }

            List<String> bannedBagItemIDs = CONFIG.raidSettings.globalContraband.bannedBagItems;
            bannedBagItemIDs.addAll(raid.boss.raidDetails.contraband.bannedBagItems);
            bannedBagItemIDs.addAll(raid.category.raidDetails.contraband.bannedBagItems);

            List<Item> bannedBagItems = new ArrayList<>();
            for (String itemID : bannedBagItemIDs) {
                if (Registries.ITEM.containsId(Identifier.of(itemID))) {
                    bannedBagItems.add(Registries.ITEM.get(Identifier.of(itemID)));
                }
            }

            if (bannedBagItems.contains(itemStack.getItem())) {
                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MESSAGES.feedback.warnings.bannedBagItem.replaceAll("%banned%", itemStack.getItem().getName().getString()), raid)));
                return TypedActionResult.fail(itemStack);
            }

            return TypedActionResult.pass(itemStack);
        }
        return TypedActionResult.pass(playerEntity.getStackInHand(hand));
    }

    private static boolean isPokeball(ItemStack itemstack) {
        for (PokeBallItem item : CobblemonItems.pokeBalls) {
            if (item.equals(itemstack.getItem())) {
                return true;
            }
        }
        return false;
    }
}
