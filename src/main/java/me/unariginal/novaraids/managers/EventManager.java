package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.PokeBallItem;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import kotlin.Unit;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.Boss;
import me.unariginal.novaraids.utils.BanHandler;
import me.unariginal.novaraids.utils.TextUtil;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class EventManager {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static final Messages messages = nr.config().getMessages();

    public static void catch_events() {
    }

    public static void battle_events() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGHEST, event -> {
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

            if (player != null) {
                nr.logInfo("[RAIDS] Player is not null");
                if (pokemonEntity != null) {
                    nr.logInfo("[RAIDS] Pokemon Entity is not null");
                    UUID entity_uuid = pokemonEntity.getUuid();
                    for (Raid raid : nr.active_raids().values()) {
                        nr.logInfo("[RAIDS] Checking Clones..");
                        for (PokemonEntity clone : raid.get_clones().keySet()) {
                            if (clone.getUuid().equals(entity_uuid)) {
                                if (!raid.get_clones().get(clone).equals(player.getUuid())) {
                                    player.sendMessage(TextUtil.format(messages.parse(messages.message("warning_not_your_encounter"), raid)));
                                    event.setReason(null);
                                    event.cancel();
                                    return Unit.INSTANCE;
                                } else {
                                    nr.logInfo("[RAIDS] This is the right player and clone thingy");
                                    return Unit.INSTANCE;
                                }
                            }
                        }

                        nr.logInfo("[RAIDS] Checking if raid uuid matches the pokemon entity uuid");
                        if (raid.uuid().equals(entity_uuid)) {
                            nr.logInfo("[RAIDS] Checking if player is in this raid");
                            if (raid.get_player_index(player) == -1) {
                                event.setReason(null);
                                event.cancel();
                                return Unit.INSTANCE;
                            }

                            nr.logInfo("[RAIDS] Checking if this is the fight phase");
                            if (raid.stage() == 2) {
                                if (!BanHandler.hasContraband(player)) {
                                    BattleManager.invoke_battle(raid, player);
                                }
                            }

                            nr.logInfo("[RAIDS] Cancelling Event");
                            event.setReason(null);
                            event.cancel();
                            return Unit.INSTANCE;
                        }
                        nr.logInfo("[RAIDS] No Match");
                    }
                }

                for (Raid raid : nr.active_raids().values()) {
                    if (raid.participating_players().contains(player)) {
                        player.sendMessage(TextUtil.format(messages.parse(messages.message("warning_battle_during_raid"), raid)));
                        event.setReason(null);
                        event.cancel();
                        return Unit.INSTANCE;
                    }
                }
            }

            return Unit.INSTANCE;
        });

        CobblemonEvents.BATTLE_FLED.subscribe(Priority.HIGHEST, event -> {
            PokemonBattle battle = event.getBattle();
            for (BattleActor actor : battle.getActors()) {
                if (actor instanceof PlayerBattleActor playerBattleActor) {
                    ServerPlayerEntity player = playerBattleActor.getEntity();
                    if (player != null) {
                        for (Raid raid : nr.active_raids().values()) {
                            if (raid.participating_players().contains(player)) {
                                List<PokemonEntity> toRemove = new ArrayList<>();
                                for (PokemonEntity cloneEntity : raid.get_clones().keySet()) {
                                    if (raid.get_clones().get(cloneEntity).equals(player.getUuid())) {
                                        toRemove.add(cloneEntity);
                                    }
                                }
                                for (PokemonEntity cloneEntity : toRemove) {
                                    raid.remove_clone(cloneEntity);
                                }
                            }
                        }
                    }
                }
            }
            return Unit.INSTANCE;
        });

        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGHEST, event -> {
            ServerPlayerEntity player = null;
            PokemonBattle battle = event.getBattle();
            int damage = 0;
            for (BattleActor actor : battle.getActors()) {
                if (actor instanceof PokemonBattleActor pokemonBattleActor) {
                    Pokemon pokemon = pokemonBattleActor.getPokemon().getOriginalPokemon();
                    if (pokemon.getPersistentData().contains("boss_clone") && pokemon.getPersistentData().contains("catch_encounter")) {
                        if (pokemon.getPersistentData().getBoolean("boss_clone") && !pokemon.getPersistentData().getBoolean("catch_encounter")) {
                            damage = Math.abs(pokemon.getCurrentHealth() - pokemon.getMaxHealth());
                        } else {
                            return Unit.INSTANCE;
                        }
                    }
                } else if (actor instanceof PlayerBattleActor playerBattleActor) {
                    player = playerBattleActor.getEntity();
                    if (!playerBattleActor.getActivePokemon().isEmpty()) {
                        for (ActiveBattlePokemon activeBattlePokemon : playerBattleActor.getActivePokemon()) {
                            BattlePokemon battlePokemon = activeBattlePokemon.getBattlePokemon();
                            if (battlePokemon != null) {
                                battlePokemon.getOriginalPokemon().recall();
                            }
                        }
                    }
                }
            }

            if (player != null) {
                for (Raid raid : nr.active_raids().values()) {
                    if (raid.participating_players().contains(player)) {
                        raid.apply_damage(damage);
                        raid.update_player_damage(player, damage);
                        raid.participating_broadcast(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("player_damage_report"), raid, player, damage, -1)));
                        break;
                    }
                }
            }

            return Unit.INSTANCE;
        });

        CobblemonEvents.LOOT_DROPPED.subscribe(Priority.HIGHEST, event -> {
            LivingEntity entity = event.getEntity();
            if (entity != null) {
                if (entity instanceof PokemonEntity pokemonEntity) {
                    Pokemon pokemon = pokemonEntity.getPokemon();
                    for (Raid raid : nr.active_raids().values()) {
                        if (raid.uuid().equals(pokemonEntity.getUuid())) {
                            event.cancel();
                            return Unit.INSTANCE;
                        }
                    }
                    if (pokemon.getPersistentData().contains("boss_clone")) {
                        if (pokemon.getPersistentData().getBoolean("boss_clone")) {
                            event.cancel();
                            return Unit.INSTANCE;
                        }
                    }
                }
            }
            return Unit.INSTANCE;
        });
    }

    public static void right_click_events() {
        UseItemCallback.EVENT.register((playerEntity, world, hand) -> {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerEntity.getUuid());
            if (player != null) {
                ItemStack held_item = player.getStackInHand(hand);
                NbtComponent custom_data = held_item.getComponents().get(DataComponentTypes.CUSTOM_DATA);

                NbtCompound passNBT = new NbtCompound();
                passNBT.putString("raid_item", "raid_pass");
                NbtCompound voucherNBT = new NbtCompound();
                voucherNBT.putString("raid_item", "raid_voucher");

                if (custom_data != null) {
                    if (hand.name().contains("MAIN_HAND") && custom_data.contains("raid_item")) {
                        if (custom_data.copyNbt().getString("raid_item").equals("raid_pass")) {
                            String boss_name = custom_data.copyNbt().getString("raid_boss");
                            String category = custom_data.copyNbt().getString("raid_category");
                            if (boss_name.equalsIgnoreCase("*")) {
                                List<Raid> joinable_raids = new ArrayList<>();
                                if (category.equalsIgnoreCase("*")) {
                                    joinable_raids.addAll(nr.active_raids().values().stream().filter(raid -> raid.stage() == 1).toList());
                                } else {
                                    for (Raid raid : nr.active_raids().values()) {
                                        if (raid.boss_info().category().equalsIgnoreCase(category)) {
                                            joinable_raids.add(raid);
                                        }
                                    }
                                }

                                SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
                                gui.setTitle(Text.literal("Pick A Raid!"));
                                for (int i = 0; i < joinable_raids.size(); i++) {
                                    Raid raid = joinable_raids.get(i);
                                    GuiElement element = new GuiElementBuilder(PokemonItem.from(joinable_raids.get(i).raidBoss_pokemon())).setCallback((slot, clickType, slotActionType) -> {
                                        if (raid.raidBoss_category().require_pass()) {
                                            if (nr.active_raids().get(nr.get_raid_id(raid)).stage() == 1) {
                                                if (nr.active_raids().get(nr.get_raid_id(raid)).participating_players().size() < nr.active_raids().get(nr.get_raid_id(raid)).max_players() || nr.active_raids().get(nr.get_raid_id(raid)).max_players() == -1 || Permissions.check(player, "novaraids.override")) {
                                                    if (raid.addPlayer(player, true)) {
                                                        held_item.decrement(1);
                                                        player.setStackInHand(hand, held_item);

                                                        player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("joined_raid"), raid)));

                                                        gui.close();
                                                    }
                                                } else {
                                                    player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_max_players"), raid)));
                                                }
                                            } else {
                                                player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_not_joinable"), raid)));
                                            }
                                        } else {
                                            player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_no_pass_needed"), raid)));
                                        }
                                    }).build();
                                    gui.setSlot(i, element);
                                }
                                gui.open();
                            } else {
                                for (Raid raid : nr.active_raids().values()) {
                                    if (raid.participating_players().contains(player)) {
                                        return TypedActionResult.fail(held_item);
                                    }

                                    if (raid.boss_info().name().equalsIgnoreCase(boss_name)) {
                                        if (raid.raidBoss_category().require_pass()) {
                                            if (raid.stage() == 1) {
                                                if (raid.participating_players().size() < raid.max_players() || raid.max_players() == -1 || Permissions.check(player, "novaraids.override")) {
                                                    if (raid.addPlayer(player, true)) {
                                                        held_item.decrement(1);
                                                        player.setStackInHand(hand, held_item);

                                                        player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("joined_raid"), raid)));
                                                    }
                                                } else {
                                                    player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_max_players"), raid)));
                                                }
                                            } else {
                                                player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_not_joinable"), raid)));
                                            }
                                        } else {
                                            player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_no_pass_needed"), raid)));
                                        }
                                    }
                                }
                            }
                        } else if (custom_data.copyNbt().getString("raid_item").equals("raid_voucher")) {
                            String boss_name = custom_data.copyNbt().getString("raid_boss");
                            String category = custom_data.copyNbt().getString("raid_category");
                            if (boss_name.equalsIgnoreCase("*")) {
                                List<Boss> available_raids = new ArrayList<>();
                                if (category.equalsIgnoreCase("*")) {
                                    available_raids.addAll(nr.config().getBosses());
                                } else {
                                    for (Boss boss : nr.config().getBosses()) {
                                        if (boss.category().equalsIgnoreCase(category)) {
                                            available_raids.add(boss);
                                        }
                                    }
                                }

                                SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
                                gui.setTitle(Text.literal("Pick A Raid!"));
                                for (int i = 0; i < available_raids.size(); i++) {
                                    int index = i;
                                    GuiElement element = new GuiElementBuilder(PokemonItem.from(available_raids.get(i).species())).setCallback((slot, clickType, slotActionType) -> {
                                        nr.raidCommands().start(available_raids.get(index), player, held_item);

                                        held_item.decrement(1);
                                        player.setStackInHand(hand, held_item);

                                        player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("used_voucher"), available_raids.get(index))));

                                        gui.close();
                                    }).build();
                                    gui.setSlot(i, element);
                                }
                                gui.open();
                            } else if (boss_name.equalsIgnoreCase("random")) {
                                Random rand = new Random();
                                Boss boss_info;
                                if (category.equalsIgnoreCase("null")) {
                                    boss_info = nr.config().getBosses().get(rand.nextInt(nr.config().getBosses().size()));
                                } else {
                                    List<Boss> available_raids = new ArrayList<>();
                                    for (Boss boss : nr.config().getBosses()) {
                                        if (boss.category().equalsIgnoreCase(category)) {
                                            available_raids.add(boss);
                                        }
                                    }
                                    boss_info = available_raids.get(rand.nextInt(available_raids.size()));
                                }

                                held_item.decrement(1);
                                player.setStackInHand(hand, held_item);

                                nr.raidCommands().start(boss_info, player, held_item);
                                player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("used_voucher"), boss_info)));
                            } else {
                                for (Boss boss : nr.config().getBosses()) {
                                    if (boss.name().equalsIgnoreCase(boss_name)) {
                                        nr.raidCommands().start(boss, player, held_item);

                                        held_item.decrement(1);
                                        player.setStackInHand(hand, held_item);

                                        player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("used_voucher"), boss)));

                                        break;
                                    }
                                }
                            }
                        } else if (custom_data.copyNbt().getString("raid_item").equals("raid_ball") && nr.config().getSettings().use_raid_pokeballs()) {
                            boolean can_throw = false;

                            if (custom_data.contains("owner_uuid")) {
                                if (!custom_data.copyNbt().getUuid("owner_uuid").equals(player.getUuid())) {
                                    player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_not_your_raid_pokeball"))));
                                    return TypedActionResult.fail(held_item);
                                }
                            }

                            for (Raid raid : nr.active_raids().values()) {
                                if (raid.participating_players().contains(player)) {
                                    can_throw = true;
                                }
                            }

                            if (can_throw) {
                                for (Raid raid : nr.active_raids().values()) {
                                    if (raid.participating_players().contains(player)) {
                                        if (raid.stage() == 4) {
                                            can_throw = true;
                                            break;
                                        } else {
                                            can_throw = false;
                                        }
                                    }
                                }
                            } else {
                                player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_raid_pokeball_outside_raid"))));
                                return TypedActionResult.fail(held_item);
                            }

                            if (!can_throw) {
                                player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_not_catch_phase"))));
                                return TypedActionResult.fail(held_item);
                            } else {
                                return TypedActionResult.pass(held_item);
                            }
                        }
                    }
                }

                if (isPokeball(held_item) && nr.config().getSettings().use_raid_pokeballs()) {
                    for (Raid raid : nr.active_raids().values()) {
                        if (raid.participating_players().contains(player)) {
                            player.sendMessage(TextUtil.format(nr.config().getMessages().parse(nr.config().getMessages().message("warning_deny_normal_pokeball"))));
                            return TypedActionResult.fail(held_item);
                        }
                    }
                }

                return TypedActionResult.pass(held_item);
            }
            return null;
        });
    }

    public static boolean isPokeball(ItemStack itemstack) {
        for (PokeBallItem item : CobblemonItems.pokeBalls) {
            if (item.equals(itemstack.getItem())) {
                return true;
            }
        }
        return false;
    }

    public static void player_events() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            for (Raid raid : nr.active_raids().values()) {
                raid.removePlayer(handler.getPlayer());
            }
        });
    }
}
