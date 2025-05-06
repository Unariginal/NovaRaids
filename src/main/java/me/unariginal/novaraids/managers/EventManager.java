package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.Cobblemon;
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
import me.unariginal.novaraids.config.MessagesConfig;
import me.unariginal.novaraids.data.Category;
import me.unariginal.novaraids.data.bosssettings.Boss;
import me.unariginal.novaraids.utils.BanHandler;
import me.unariginal.novaraids.utils.TextUtils;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class EventManager {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static final MessagesConfig messages = nr.messagesConfig();

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
                if (pokemonEntity != null) {
                    UUID entity_uuid = pokemonEntity.getUuid();
                    for (Raid raid : nr.active_raids().values()) {
                        for (PokemonEntity clone : raid.get_clones().keySet()) {
                            if (clone.getUuid().equals(entity_uuid)) {
                                if (!raid.get_clones().get(clone).equals(player.getUuid())) {
                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_your_encounter"), raid)));
                                    event.setReason(null);
                                    event.cancel();
                                }
                                return Unit.INSTANCE;
                            }
                        }

                        if (raid.uuid().equals(entity_uuid)) {
                            if (raid.get_player_index(player.getUuid()) == -1) {
                                event.setReason(null);
                                event.cancel();
                                return Unit.INSTANCE;
                            }

                            for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                                if (pokemon != null) {
                                    if (pokemon.getLevel() < raid.boss_info().raid_details().minimum_level()) {
                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_minimum_level"), raid)));
                                        event.setReason(null);
                                        event.cancel();
                                        return Unit.INSTANCE;
                                    }
                                }
                            }

                            if (raid.stage() == 2) {
                                if (!BanHandler.hasContraband(player, raid.boss_info())) {
                                    BattleManager.invoke_battle(raid, player);
                                }
                            }

                            event.setReason(null);
                            event.cancel();
                            return Unit.INSTANCE;
                        }
                    }
                }

                for (Raid raid : nr.active_raids().values()) {
                    if (raid.participating_players().contains(player.getUuid())) {
                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_battle_during_raid"), raid)));
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
                            if (raid.participating_players().contains(player.getUuid())) {
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
                    if (raid.participating_players().contains(player.getUuid())) {
                        if (damage > raid.current_health()) {
                            damage = raid.current_health();
                        }

                        raid.apply_damage(damage);
                        raid.update_player_damage(player.getUuid(), damage);
                        raid.participating_broadcast(TextUtils.deserialize(TextUtils.parse(messages.getMessage("player_damage_report"), raid, player, damage, -1)));
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
                                        if (raid.boss_info().category_id().equalsIgnoreCase(category)) {
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
                                                    if (raid.addPlayer(player.getUuid(), true)) {
                                                        held_item.decrement(1);
                                                        player.setStackInHand(hand, held_item);

                                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("joined_raid"), raid)));

                                                        gui.close();
                                                    }
                                                } else {
                                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_max_players"), raid)));
                                                }
                                            } else {
                                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_joinable"), raid)));
                                            }
                                        } else {
                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_no_pass_needed"), raid)));
                                        }
                                    }).build();
                                    gui.setSlot(i, element);
                                }
                                gui.open();
                            } else {
                                for (Raid raid : nr.active_raids().values()) {
                                    if (raid.participating_players().contains(player.getUuid())) {
                                        return TypedActionResult.fail(held_item);
                                    }

                                    if (raid.boss_info().boss_id().equalsIgnoreCase(boss_name)) {
                                        if (raid.raidBoss_category().require_pass()) {
                                            if (raid.stage() == 1) {
                                                if (raid.participating_players().size() < raid.max_players() || raid.max_players() == -1 || Permissions.check(player, "novaraids.override")) {
                                                    if (raid.addPlayer(player.getUuid(), true)) {
                                                        held_item.decrement(1);
                                                        player.setStackInHand(hand, held_item);

                                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("joined_raid"), raid)));
                                                    }
                                                } else {
                                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_max_players"), raid)));
                                                }
                                            } else {
                                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_joinable"), raid)));
                                            }
                                        } else {
                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_no_pass_needed"), raid)));
                                        }
                                    }
                                }
                            }
                        } else if (custom_data.copyNbt().getString("raid_item").equals("raid_voucher") && nr.config().vouchers_enabled) {
                            String boss_name = custom_data.copyNbt().getString("raid_boss");
                            String category = custom_data.copyNbt().getString("raid_category");
                            if (boss_name.equalsIgnoreCase("*")) {
                                List<Boss> available_raids = new ArrayList<>();
                                if (category.equalsIgnoreCase("*")) {
                                    available_raids.addAll(nr.bossesConfig().bosses);
                                } else {
                                    for (Boss boss : nr.bossesConfig().bosses) {
                                        if (boss.category_id().equalsIgnoreCase(category)) {
                                            available_raids.add(boss);
                                        }
                                    }
                                }

                                SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, false);
                                gui.setTitle(Text.literal("Pick A Raid!"));
                                for (int i = 0; i < available_raids.size(); i++) {
                                    int index = i;
                                    GuiElement element = new GuiElementBuilder(PokemonItem.from(available_raids.get(i).pokemonDetails().species())).setCallback((slot, clickType, slotActionType) -> {
                                        if (nr.raidCommands().start(available_raids.get(index), player, held_item) != 0) {

                                            held_item.decrement(1);
                                            player.setStackInHand(hand, held_item);

                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("used_voucher"), available_raids.get(index))));

                                            gui.close();
                                        }
                                    }).build();
                                    gui.setSlot(i, element);
                                }
                                gui.open();
                            } else if (boss_name.equalsIgnoreCase("random")) {
                                Boss boss_info;
                                if (category.equalsIgnoreCase("null")) {
                                    boss_info = nr.bossesConfig().getRandomBoss();
                                } else {
                                    boss_info = nr.bossesConfig().getRandomBoss(category);
                                }

                                if (nr.raidCommands().start(boss_info, player, held_item) != 0) {
                                    held_item.decrement(1);
                                    player.setStackInHand(hand, held_item);

                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("used_voucher"), boss_info)));
                                }
                            } else {
                                Boss boss = nr.bossesConfig().getBoss(boss_name);
                                if (nr.raidCommands().start(boss, player, held_item) != 0) {
                                    held_item.decrement(1);
                                    player.setStackInHand(hand, held_item);

                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("used_voucher"), boss)));
                                }
                            }
                        } else if (custom_data.copyNbt().getString("raid_item").equals("raid_ball") && nr.config().raid_balls_enabled) {
                            boolean can_throw = false;

                            if (custom_data.contains("owner_uuid")) {
                                if (!custom_data.copyNbt().getUuid("owner_uuid").equals(player.getUuid())) {
                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_your_raid_pokeball"))));
                                    return TypedActionResult.fail(held_item);
                                }
                            }

                            for (Raid raid : nr.active_raids().values()) {
                                if (raid.participating_players().contains(player.getUuid())) {
                                    can_throw = true;
                                    break;
                                }
                            }

                            if (can_throw) {
                                can_throw = false;
                                outer:
                                for (Raid raid : nr.active_raids().values()) {
                                    if (raid.participating_players().contains(player.getUuid())) {
                                        if (raid.stage() == 4) {
                                            // Old Data
                                            if (custom_data.contains("raid_categories")) {
                                                NbtCompound categories = custom_data.copyNbt().getCompound("raid_categories");
                                                for (String key : categories.getKeys()) {
                                                    if (raid.raidBoss_category().name().equalsIgnoreCase(key)) {
                                                        can_throw = true;
                                                        break outer;
                                                    }
                                                }
                                            } else if (custom_data.contains("raid_bosses")) {
                                                NbtCompound bosses = custom_data.copyNbt().getCompound("raid_bosses");
                                                for (String key : bosses.getKeys()) {
                                                    if (raid.boss_info().boss_id().equalsIgnoreCase(key)) {
                                                        can_throw = true;
                                                        break outer;
                                                    }
                                                }
                                            } else {
                                                if (custom_data.contains("raid_boss") && custom_data.contains("raid_category")) {
                                                    String boss = custom_data.copyNbt().getString("raid_boss");
                                                    String category = custom_data.copyNbt().getString("raid_category");
                                                    if (boss.equalsIgnoreCase("*") && category.equalsIgnoreCase("*")) {
                                                        if (raid.boss_info().item_settings().allow_global_pokeballs()) {
                                                            can_throw = true;
                                                            break;
                                                        }
                                                    } else if (boss.equalsIgnoreCase("*")) {
                                                        if (raid.boss_info().category_id().equalsIgnoreCase(category)) {
                                                            if (raid.boss_info().item_settings().allow_category_pokeballs()) {
                                                                can_throw = true;
                                                                break;
                                                            }
                                                        }
                                                    } else {
                                                        if (raid.boss_info().boss_id().equalsIgnoreCase(boss)) {
                                                            can_throw = true;
                                                            break;
                                                        }
                                                    }
                                                } else {
                                                    can_throw = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_raid_pokeball_outside_raid"))));
                                return TypedActionResult.fail(held_item);
                            }

                            if (!can_throw) {
                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_catch_phase"))));
                                return TypedActionResult.fail(held_item);
                            } else {
                                return TypedActionResult.pass(held_item);
                            }
                        }
                    }
                }

                if (isPokeball(held_item) && nr.config().raid_balls_enabled) {
                    for (Raid raid : nr.active_raids().values()) {
                        if (raid.participating_players().contains(player.getUuid())) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_deny_normal_pokeball"))));
                            return TypedActionResult.fail(held_item);
                        }
                    }
                }

                // TODO: <!>TEST<!> Other banned shit
                for (Raid raid : nr.active_raids().values()) {
                    if (raid.participating_players().contains(player.getUuid())) {
                        List<Item> banned_bag_items = nr.config().global_banned_bag_items;
                        banned_bag_items.addAll(raid.boss_info().raid_details().banned_bag_items());
                        if (banned_bag_items.contains(held_item.getItem())) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_bag_item").replaceAll("%banned.bag_item%", held_item.getItem().getName().getString()), raid)));
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
                raid.removePlayer(handler.getPlayer().getUuid());
                if (raid.stage() > 1 && raid.participating_players().isEmpty()) {
                    raid.stop();
                }
            }
        });

        AttackEntityCallback.EVENT.register(((playerEntity, world, hand, entity, entityHitResult) -> {
            if (entity instanceof PokemonEntity pokemonEntity) {
                Pokemon pokemon = pokemonEntity.getPokemon();
                if (pokemon.getPersistentData().contains("raid_entity")) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        }));
    }

    public static void cobblemon_events() {
        CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.subscribe(Priority.HIGHEST, event -> {
            PokemonEntity pokemonEntity = event.getPokemonEntity();
            Pokemon pokemon = pokemonEntity.getPokemon();
            if (pokemon.getPersistentData().contains("raid_entity")) {
                event.cancel();
            }
            return Unit.INSTANCE;
        });
    }
}
