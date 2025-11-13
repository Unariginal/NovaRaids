package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
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
import me.unariginal.novaraids.data.bosssettings.Boss;
import me.unariginal.novaraids.utils.BanHandler;
import me.unariginal.novaraids.utils.GuiUtils;
import me.unariginal.novaraids.utils.TextUtils;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;

public class EventManager {
    private static final NovaRaids nr = NovaRaids.INSTANCE;
    private static final MessagesConfig messages = nr.messagesConfig();

    public static void captureEvent() {
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.HIGHEST, event -> {
            PokemonEntity pokemonEntity = event.getPokemon();
            Pokemon pokemon = pokemonEntity.getPokemon();
            if (pokemon.getPersistentData().contains("raid_entity")) {
                for (Raid raid : nr.activeRaids().values()) {
                    for (PokemonEntity clone : raid.getClones().keySet()) {
                        if (clone.getUuid().equals(pokemonEntity.getUuid())) {
                            raid.addPokeballsCapturing(event.getPokeBall());
                            return Unit.INSTANCE;
                        }
                    }
                }
                pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
                event.cancel();
            }
            return Unit.INSTANCE;
        });
    }

    public static void battleEvents() {
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
                    UUID entityUUID = pokemonEntity.getUuid();
                    for (Raid raid : nr.activeRaids().values()) {
                        for (PokemonEntity clone : raid.getClones().keySet()) {
                            if (clone.getUuid().equals(entityUUID)) {
                                if (!raid.getClones().get(clone).equals(player.getUuid())) {
                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_your_encounter"), raid)));
                                    event.setReason(null);
                                    event.cancel();
                                }
                                return Unit.INSTANCE;
                            }
                        }

                        if (raid.uuid().equals(entityUUID)) {
                            if (raid.getPlayerIndex(player.getUuid()) == -1) {
                                event.setReason(null);
                                event.cancel();
                                return Unit.INSTANCE;
                            }

                            for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                                if (pokemon != null) {
                                    if (pokemon.getLevel() < raid.bossInfo().raidDetails().minimumLevel()) {
                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_minimum_level"), raid)));
                                        event.setReason(null);
                                        event.cancel();
                                        return Unit.INSTANCE;
                                    }
                                    if (pokemon.getLevel() > raid.bossInfo().raidDetails().maximumLevel()) {
                                        player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_maximum_level"), raid)));
                                        event.setReason(null);
                                        event.cancel();
                                        return Unit.INSTANCE;
                                    }
                                }
                            }

                            if (raid.stage() == 2) {
                                if (!BanHandler.hasContraband(player, raid.bossInfo())) {
                                    BattleManager.invokeBattle(raid, player);
                                }
                            }

                            event.setReason(null);
                            event.cancel();
                            return Unit.INSTANCE;
                        }
                    }
                }

                for (Raid raid : nr.activeRaids().values()) {
                    if (raid.participatingPlayers().contains(player.getUuid())) {
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
                        for (Raid raid : nr.activeRaids().values()) {
                            if (raid.participatingPlayers().contains(player.getUuid())) {
                                raid.addFleeingPlayer(player.getUuid());
                                List<PokemonEntity> toRemove = new ArrayList<>();
                                for (PokemonEntity cloneEntity : raid.getClones().keySet()) {
                                    if (raid.getClones().get(cloneEntity).equals(player.getUuid())) {
                                        toRemove.add(cloneEntity);
                                    }
                                }
                                for (PokemonEntity cloneEntity : toRemove) {
                                    raid.removeClone(cloneEntity, true);
                                }
                                break;
                            }
                        }
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
                    for (Raid raid : nr.activeRaids().values()) {
                        if (raid.uuid().equals(pokemonEntity.getUuid())) {
                            event.cancel();
                            return Unit.INSTANCE;
                        }
                    }
                    if (!pokemon.isPlayerOwned()) {
                        if (pokemon.getPersistentData().contains("boss_clone")) {
                            if (pokemon.getPersistentData().getBoolean("boss_clone")) {
                                event.cancel();
                                return Unit.INSTANCE;
                            }
                        }
                    }
                }
            }
            return Unit.INSTANCE;
        });

        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(event -> {
            Pokemon pokemon = event.getPokemon();
            if (pokemon.isPlayerOwned()) {
                ServerPlayerEntity player = pokemon.getOwnerPlayer();
                if (player != null) {
                    for (Raid raid : nr.activeRaids().values()) {
                        if (raid.participatingPlayers().contains(player.getUuid())) {
                            if ((!nr.config().allowExperienceGain || raid.isPlayerFleeing(player.getUuid())) && event.getSource().isBattle()) {
                                event.cancel();
                            }
                        }
                    }
                }
            }
        });
    }

    public static void rightClickEvents() {
        UseItemCallback.EVENT.register((playerEntity, world, hand) -> {
            ServerPlayerEntity player = nr.server().getPlayerManager().getPlayer(playerEntity.getUuid());
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
                                    joinableRaids.addAll(nr.activeRaids().values().stream().filter(raid -> raid.stage() == 1).toList());
                                } else {
                                    for (Raid raid : nr.activeRaids().values()) {
                                        if (raid.bossInfo().categoryId().equalsIgnoreCase(category)) {
                                            joinableRaids.add(raid);
                                        }
                                    }
                                }

                                Map<Integer, SimpleGui> pages = new HashMap<>();
                                int page_total = GuiUtils.getPageTotal(joinableRaids.size(), nr.guisConfig().passGui.displaySlotTotal());
                                for (int i = 1; i <= page_total; i++) {
                                    SimpleGui gui = new SimpleGui(GuiUtils.getScreenSize(nr.guisConfig().passGui.rows), player, false);
                                    gui.setTitle(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().passGui.title)));
                                    pages.put(i, gui);
                                }

                                int index = 0;
                                for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
                                    for (Integer slot : nr.guisConfig().passGui.displaySlots()) {
                                        if (index < joinableRaids.size()) {
                                            Raid raid = joinableRaids.get(index);

                                            List<Text> lore = new ArrayList<>();
                                            for (String line : nr.guisConfig().passGui.displayButton.itemLore()) {
                                                lore.add(TextUtils.deserialize(TextUtils.parse(line, raid)));
                                            }

                                            ItemStack item = PokemonItem.from(raid.raidBossPokemon());
                                            item.applyChanges(nr.guisConfig().passGui.displayButton.itemData());
                                            GuiElement element = new GuiElementBuilder(item)
                                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().passGui.displayButton.itemName(), raid)))
                                                    .setLore(lore)
                                                    .setCallback((num, clickType, slotActionType) -> {
                                                        if (clickType.isLeft) {
                                                            if (raid.raidBossCategory().requirePass()) {
                                                                if (nr.activeRaids().get(nr.getRaidId(raid)).stage() == 1) {
                                                                    if (nr.activeRaids().get(nr.getRaidId(raid)).participatingPlayers().size() < nr.activeRaids().get(nr.getRaidId(raid)).maxPlayers() || nr.activeRaids().get(nr.getRaidId(raid)).maxPlayers() == -1 || Permissions.check(player, "novaraids.override")) {
                                                                        if (raid.addPlayer(player.getUuid(), true)) {
                                                                            itemStack.decrement(1);
                                                                            player.setStackInHand(hand, itemStack);

                                                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("joined_raid"), raid)));

                                                                            pageEntry.getValue().close();
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
                                                    }).build();
                                            pageEntry.getValue().setSlot(slot, element);
                                            index++;
                                        } else {
                                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().passGui.backgroundButton.item())));
                                            item.applyChanges(nr.guisConfig().passGui.backgroundButton.itemData());
                                            List<Text> lore = new ArrayList<>();
                                            for (String line : nr.guisConfig().passGui.backgroundButton.itemLore()) {
                                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                            }
                                            GuiElement element = new GuiElementBuilder(item)
                                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().passGui.backgroundButton.itemName())))
                                                    .setLore(lore)
                                                    .build();
                                            pageEntry.getValue().setSlot(slot, element);
                                        }
                                    }

                                    if (pageEntry.getKey() < page_total) {
                                        for (Integer slot : nr.guisConfig().passGui.nextButtonSlots()) {
                                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().passGui.nextButton.item())));
                                            item.applyChanges(nr.guisConfig().passGui.nextButton.itemData());
                                            List<Text> lore = new ArrayList<>();
                                            for (String line : nr.guisConfig().passGui.nextButton.itemLore()) {
                                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                            }
                                            GuiElement element = new GuiElementBuilder(item)
                                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().passGui.nextButton.itemName())))
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
                                        for (Integer slot : nr.guisConfig().passGui.previousButtonSlots()) {
                                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().passGui.previousButton.item())));
                                            item.applyChanges(nr.guisConfig().passGui.previousButton.itemData());
                                            List<Text> lore = new ArrayList<>();
                                            for (String line : nr.guisConfig().passGui.previousButton.itemLore()) {
                                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                            }
                                            GuiElement element = new GuiElementBuilder(item)
                                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().passGui.previousButton.itemName())))
                                                    .setLore(lore)
                                                    .setCallback(clickType -> {
                                                        pageEntry.getValue().close();
                                                        pages.get(pageEntry.getKey() - 1).open();
                                                    })
                                                    .build();
                                            pageEntry.getValue().setSlot(slot, element);
                                        }
                                    }

                                    for (Integer slot : nr.guisConfig().passGui.closeButtonSlots()) {
                                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().passGui.closeButton.item())));
                                        item.applyChanges(nr.guisConfig().passGui.closeButton.itemData());
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : nr.guisConfig().passGui.closeButton.itemLore()) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().passGui.closeButton.itemName())))
                                                .setLore(lore)
                                                .setCallback(clickType -> pageEntry.getValue().close())
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }

                                    for (Integer slot : nr.guisConfig().passGui.backgroundButtonSlots()) {
                                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().passGui.backgroundButton.item())));
                                        item.applyChanges(nr.guisConfig().passGui.backgroundButton.itemData());
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : nr.guisConfig().passGui.backgroundButton.itemLore()) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().passGui.backgroundButton.itemName())))
                                                .setLore(lore)
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }
                                }
                                if (!pages.isEmpty()) {
                                    pages.get(1).open();
                                }
                            } else {
                                for (Raid raid : nr.activeRaids().values()) {
                                    if (raid.participatingPlayers().contains(player.getUuid())) {
                                        return TypedActionResult.fail(itemStack);
                                    }

                                    if (raid.bossInfo().bossId().equalsIgnoreCase(bossName)) {
                                        if (raid.raidBossCategory().requirePass()) {
                                            if (raid.stage() == 1) {
                                                if (raid.participatingPlayers().size() < raid.maxPlayers() || raid.maxPlayers() == -1 || Permissions.check(player, "novaraids.override")) {
                                                    if (raid.addPlayer(player.getUuid(), true)) {
                                                        itemStack.decrement(1);
                                                        player.setStackInHand(hand, itemStack);

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
                        } else if (customData.copyNbt().getString("raid_item").equals("raid_voucher") && nr.config().vouchersEnabled) {
                            String bossName = customData.copyNbt().getString("raid_boss");
                            String category = customData.copyNbt().getString("raid_category");
                            if (bossName.equalsIgnoreCase("*")) {
                                List<Boss> availableRaids = new ArrayList<>();
                                if (category.equalsIgnoreCase("*")) {
                                    availableRaids.addAll(nr.bossesConfig().bosses);
                                } else {
                                    for (Boss boss : nr.bossesConfig().bosses) {
                                        if (boss.categoryId().equalsIgnoreCase(category)) {
                                            availableRaids.add(boss);
                                        }
                                    }
                                }

                                Map<Integer, SimpleGui> pages = new HashMap<>();
                                int page_total = GuiUtils.getPageTotal(availableRaids.size(), nr.guisConfig().voucherGui.displaySlotTotal());
                                for (int i = 1; i <= page_total; i++) {
                                    SimpleGui gui = new SimpleGui(GuiUtils.getScreenSize(nr.guisConfig().voucherGui.rows), player, false);
                                    gui.setTitle(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().voucherGui.title)));
                                    pages.put(i, gui);
                                }

                                int index = 0;
                                for (Map.Entry<Integer, SimpleGui> pageEntry : pages.entrySet()) {
                                    for (Integer slot : nr.guisConfig().voucherGui.displaySlots()) {
                                        if (index < availableRaids.size()) {
                                            Boss boss = availableRaids.get(index);

                                            List<Text> lore = new ArrayList<>();
                                            for (String line : nr.guisConfig().voucherGui.displayButton.itemLore()) {
                                                lore.add(TextUtils.deserialize(TextUtils.parse(line, boss)));
                                            }

                                            ItemStack item = PokemonItem.from(boss.pokemonDetails().createPokemon(false));
                                            item.applyChanges(nr.guisConfig().voucherGui.displayButton.itemData());
                                            GuiElement element = new GuiElementBuilder(item)
                                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().voucherGui.displayButton.itemName(), boss)))
                                                    .setLore(lore)
                                                    .setCallback((num, clickType, slotActionType) -> {
                                                        if (clickType.isLeft) {
                                                            if (nr.raidCommands().start(boss, player, itemStack) != 0) {
                                                                itemStack.decrement(1);
                                                                player.setStackInHand(hand, itemStack);

                                                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("used_voucher"), boss)));

                                                                pageEntry.getValue().close();
                                                            }
                                                        }
                                                    }).build();
                                            pageEntry.getValue().setSlot(slot, element);
                                            index++;
                                        } else {
                                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().voucherGui.backgroundButton.item())));
                                            item.applyChanges(nr.guisConfig().voucherGui.backgroundButton.itemData());
                                            List<Text> lore = new ArrayList<>();
                                            for (String line : nr.guisConfig().voucherGui.backgroundButton.itemLore()) {
                                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                            }
                                            GuiElement element = new GuiElementBuilder(item)
                                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().voucherGui.backgroundButton.itemName())))
                                                    .setLore(lore)
                                                    .build();
                                            pageEntry.getValue().setSlot(slot, element);
                                        }
                                    }

                                    if (pageEntry.getKey() < page_total) {
                                        for (Integer slot : nr.guisConfig().voucherGui.nextButtonSlots()) {
                                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().voucherGui.nextButton.item())));
                                            item.applyChanges(nr.guisConfig().voucherGui.nextButton.itemData());
                                            List<Text> lore = new ArrayList<>();
                                            for (String line : nr.guisConfig().voucherGui.nextButton.itemLore()) {
                                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                            }
                                            GuiElement element = new GuiElementBuilder(item)
                                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().voucherGui.nextButton.itemName())))
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
                                        for (Integer slot : nr.guisConfig().voucherGui.previousButtonSlots()) {
                                            ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().voucherGui.previousButton.item())));
                                            item.applyChanges(nr.guisConfig().voucherGui.previousButton.itemData());
                                            List<Text> lore = new ArrayList<>();
                                            for (String line : nr.guisConfig().voucherGui.previousButton.itemLore()) {
                                                lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                            }
                                            GuiElement element = new GuiElementBuilder(item)
                                                    .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().voucherGui.previousButton.itemName())))
                                                    .setLore(lore)
                                                    .setCallback(clickType -> {
                                                        pageEntry.getValue().close();
                                                        pages.get(pageEntry.getKey() - 1).open();
                                                    })
                                                    .build();
                                            pageEntry.getValue().setSlot(slot, element);
                                        }
                                    }

                                    for (Integer slot : nr.guisConfig().voucherGui.closeButtonSlots()) {
                                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().voucherGui.closeButton.item())));
                                        item.applyChanges(nr.guisConfig().voucherGui.closeButton.itemData());
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : nr.guisConfig().voucherGui.closeButton.itemLore()) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().voucherGui.closeButton.itemName())))
                                                .setLore(lore)
                                                .setCallback(clickType -> pageEntry.getValue().close())
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }

                                    for (Integer slot : nr.guisConfig().voucherGui.backgroundButtonSlots()) {
                                        ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(nr.guisConfig().voucherGui.backgroundButton.item())));
                                        item.applyChanges(nr.guisConfig().voucherGui.backgroundButton.itemData());
                                        List<Text> lore = new ArrayList<>();
                                        for (String line : nr.guisConfig().voucherGui.backgroundButton.itemLore()) {
                                            lore.add(TextUtils.deserialize(TextUtils.parse(line)));
                                        }
                                        GuiElement element = new GuiElementBuilder(item)
                                                .setName(TextUtils.deserialize(TextUtils.parse(nr.guisConfig().voucherGui.backgroundButton.itemName())))
                                                .setLore(lore)
                                                .build();
                                        pageEntry.getValue().setSlot(slot, element);
                                    }
                                }
                                if (!pages.isEmpty()) {
                                    pages.get(1).open();
                                }
                            } else if (bossName.equalsIgnoreCase("random")) {
                                Boss bossInfo;
                                if (category.equalsIgnoreCase("null")) {
                                    bossInfo = nr.bossesConfig().getRandomBoss();
                                } else {
                                    bossInfo = nr.bossesConfig().getRandomBoss(category);
                                }

                                if (nr.raidCommands().start(bossInfo, player, itemStack) != 0) {
                                    itemStack.decrement(1);
                                    player.setStackInHand(hand, itemStack);

                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("used_voucher"), bossInfo)));
                                }
                            } else {
                                Boss boss = nr.bossesConfig().getBoss(bossName);
                                if (nr.raidCommands().start(boss, player, itemStack) != 0) {
                                    itemStack.decrement(1);
                                    player.setStackInHand(hand, itemStack);

                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("used_voucher"), boss)));
                                }
                            }
                        } else if (customData.copyNbt().getString("raid_item").equals("raid_ball") && nr.config().raidBallsEnabled) {
                            boolean canThrow = false;

                            if (nr.config().playerLinkedRaidBalls && customData.contains("owner_uuid")) {
                                if (!customData.copyNbt().getUuid("owner_uuid").equals(player.getUuid())) {
                                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_your_raid_pokeball"))));
                                    return TypedActionResult.fail(itemStack);
                                }
                            }

                            for (Raid raid : nr.activeRaids().values()) {
                                if (raid.participatingPlayers().contains(player.getUuid())) {
                                    canThrow = true;
                                    break;
                                }
                            }

                            if (canThrow) {
                                canThrow = false;
                                for (Raid raid : nr.activeRaids().values()) {
                                    if (raid.participatingPlayers().contains(player.getUuid())) {
                                        if (raid.stage() == 4) {
                                            // Old Data
                                            if (customData.contains("raid_categories")) {
                                                break;
                                            } else if (customData.contains("raid_bosses")) {
                                                break;
                                            }

                                            if (customData.contains("raid_boss") && customData.contains("raid_category")) {
                                                String boss = customData.copyNbt().getString("raid_boss");
                                                String category = customData.copyNbt().getString("raid_category");
                                                if (boss.equalsIgnoreCase("*") && category.equalsIgnoreCase("*")) {
                                                    if (raid.bossInfo().itemSettings().allowGlobalPokeballs()) {
                                                        canThrow = true;
                                                        break;
                                                    }
                                                } else if (boss.equalsIgnoreCase("*")) {
                                                    if (raid.bossInfo().categoryId().equalsIgnoreCase(category)) {
                                                        if (raid.bossInfo().itemSettings().allowCategoryPokeballs()) {
                                                            canThrow = true;
                                                            break;
                                                        }
                                                    }
                                                } else {
                                                    if (raid.bossInfo().bossId().equalsIgnoreCase(boss)) {
                                                        canThrow = true;
                                                        break;
                                                    }
                                                }
                                            } else {
                                                canThrow = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else {
                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_raid_pokeball_outside_raid"))));
                                return TypedActionResult.fail(itemStack);
                            }

                            if (!canThrow) {
                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_not_catch_phase"))));
                                return TypedActionResult.fail(itemStack);
                            } else {
                                return TypedActionResult.pass(itemStack);
                            }
                        }
                    }
                }

                if (isPokeball(itemStack) && nr.config().raidBallsEnabled) {
                    for (Raid raid : nr.activeRaids().values()) {
                        if (raid.participatingPlayers().contains(player.getUuid())) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_deny_normal_pokeball"))));
                            return TypedActionResult.fail(itemStack);
                        }
                    }
                }

                for (Raid raid : nr.activeRaids().values()) {
                    if (raid.participatingPlayers().contains(player.getUuid())) {
                        List<Item> bannedBagItems = nr.config().globalContraband.bannedBagItems();
                        bannedBagItems.addAll(raid.bossInfo().raidDetails().contraband().bannedBagItems());
                        bannedBagItems.addAll(raid.raidBossCategory().contraband().bannedBagItems());
                        if (bannedBagItems.contains(itemStack.getItem())) {
                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(messages.getMessage("warning_banned_bag_item").replaceAll("%banned.bag_item%", itemStack.getItem().getName().getString()), raid)));
                            return TypedActionResult.fail(itemStack);
                        }
                    }
                }

                return TypedActionResult.pass(itemStack);
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

    public static void playerEvents() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            for (Raid raid : nr.activeRaids().values()) {
                raid.removePlayer(handler.getPlayer().getUuid());
                if (raid.stage() > 1 && raid.participatingPlayers().isEmpty()) {
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

    public static void cobblemonEvents() {
        CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.subscribe(Priority.HIGHEST, event -> {
            PokemonEntity pokemonEntity = event.getPokemonEntity();
            Pokemon pokemon = pokemonEntity.getPokemon();
            if (pokemon.getPersistentData().contains("raid_entity")) {
                event.cancel();
            }
            return Unit.INSTANCE;
        });

        CobblemonEvents.POKEMON_SENT_POST.subscribe(event -> {
            if (nr.config().reduceLargePokemonSize) {
                Pokemon pokemon = event.getPokemon();
                PokemonEntity pokemonEntity = event.getPokemonEntity();
                if (pokemon.isPlayerOwned()) {
                    ServerPlayerEntity player = pokemon.getOwnerPlayer();
                    if (player != null) {
                        for (Raid raid : nr.activeRaids().values()) {
                            if (raid.participatingPlayers().contains(player.getUuid())) {
                                Box hitbox = pokemonEntity.getBoundingBox();
                                double maxLength = Math.max(Math.max(hitbox.getLengthX(), hitbox.getLengthY()), hitbox.getLengthZ());
                                if (maxLength > 1) {
                                    double scale = 1 / maxLength;
                                    EntityAttributeInstance scaleAttribute = pokemonEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
                                    if (scaleAttribute != null) scaleAttribute.setBaseValue(scale);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });

        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(event -> {
            if (nr.config().disableSpawnsInArena) {
                BlockPos pos = event.getSpawnablePosition().getPosition();
                for (Raid raid : nr.activeRaids().values()) {
                    if (raid.raidBossLocation().isPointInLocation(pos.getX(), pos.getZ())) {
                        event.cancel();
                        break;
                    }
                }
            }
        });
    }
}
