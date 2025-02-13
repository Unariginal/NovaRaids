package me.unariginal.novaraids.managers;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import me.unariginal.novaraids.NovaRaids;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

import java.util.UUID;

public class EventManager {
    private static final NovaRaids nr = NovaRaids.INSTANCE;

    public static void catch_events() {

    }

//    public static void handle_capture(ServerPlayerEntity player, EmptyPokeBallEntity ball, Raid raid) {
//        PokemonEntity fake_entity = new PokemonEntity(ball.getWorld(), raid.raidBoss_pokemon().clone(true, null), CobblemonEntities.POKEMON);
//        AtomicReference<CaptureContext> captureContext = new AtomicReference<>(Cobblemon.config.getCaptureCalculator().processCapture(player, ball, fake_entity));
//        if (captureContext.get() != null) {
//            PokeBallCaptureCalculatedEvent[] event = {new PokeBallCaptureCalculatedEvent(player, fake_entity, ball, captureContext.get())};
//            CobblemonEvents.POKE_BALL_CAPTURE_CALCULATED.post(event, (e) -> {
//                captureContext.set(e.getCaptureResult());
//                return Unit.INSTANCE;
//            });
//        }
//        if (captureContext.get() != null) {
//            SchedulingFunctionsKt.lerpOnServer(captureContext.get().getNumberOfShakes() * 0.5f, time -> {
//                double ticks = Math.floor(time * 20);
//                if (ticks % 10 == 0) {
//                    SimpleParticleType particleType = ParticleTypes.SMOKE;
//                    int particleCount = 10;
//                    float particleSpeed = 0.1f;
//                    float particleSpread = (ball.getCapturingPokemon() != null) ? (float) ball.getCapturingPokemon().getBoundingBox().getAverageSideLength() : 0.5f;
//
//                    player.networkHandler.sendPacket(
//                            new ParticleS2CPacket(
//                                    particleType,
//                                    true,
//                                    ball.getPos().x,
//                                    ball.getPos().y,
//                                    ball.getPos().z,
//                                    particleSpread,
//                                    particleSpread,
//                                    particleSpread,
//                                    particleSpeed,
//                                    particleCount
//                            )
//                    );
//                    ball.getWorld().playSoundAtBlockCenter(ball.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 2.5F, 0.5F, true);
//                }
//                return Unit.INSTANCE;
//            });
//
//            SchedulingFunctionsKt.afterOnServer(30, player.getWorld(), () -> {
//                if (captureContext.get().isSuccessfulCapture()) {
//                    if (fake_entity.isBattling()) {
//                        Objects.requireNonNull(BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player)).end();
//                    }
//                    if (fake_entity.getPokemon().isWild() && fake_entity.isAlive()) {
//                        fake_entity.discard();
//                        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
//                        fake_entity.getPokemon().setCaughtBall(ball.getPokeBall());
//                        ball.getPokeBall().getEffects().forEach(effect -> effect.apply(player, fake_entity.getPokemon()));
//
//                        party.add(fake_entity.getPokemon());
//                        PokemonCapturedEvent[] event = {new PokemonCapturedEvent(
//                                fake_entity.getPokemon(),
//                                player,
//                                ball
//                        )};
//                        CobblemonEvents.POKEMON_CAPTURED.post(
//                                event,
//                                (e) -> Unit.INSTANCE
//                        );
//                    }
//                    player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(fake_entity.getId()));
//                    ball.getWorld().playSoundAtBlockCenter(ball.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER, 1.0f, 1.65f, true);
//                    ball.getCaptureFuture().complete(true);
//                } else {
//                    fake_entity.getDataTracker().set(PokemonEntity.getBEAM_MODE(), (byte) 0);
//                    fake_entity.setInvisible(false);
//                    ball.getWorld().addParticle(
//                            ParticleTypes.CLOUD,
//                            ball.getPos().x,
//                            ball.getPos().y,
//                            ball.getPos().z,
//                            2,
//                            ball.getPos().normalize().multiply(0.1d).y,
//                            0.0
//                    );
//                    ball.getWorld().playSoundAtBlockCenter(ball.getBlockPos(), SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.MASTER, 1.0f, 2.5F, true);
//                    ball.getCaptureFuture().complete(false);
//                    fake_entity.cry();
//                }
//                return Unit.INSTANCE;
//            });
//            ball.discard();
//        }
//    }

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

            if (player != null && pokemonEntity != null) {
                UUID entity_uuid = pokemonEntity.getUuid();
                for (Raid raid : nr.active_raids().values()) {
                    if (raid.uuid().equals(entity_uuid)) {
                        if (raid.get_player_index(player) == -1) {
                            event.cancel();
                            return Unit.INSTANCE;
                        }
                        if (raid.stage() == 2) {
                            BattleManager.invoke_battle(raid, player);
                        }
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
                if (actor instanceof PokemonBattleActor pokemonBattleActor) {
                    PokemonEntity pokemonEntity = pokemonBattleActor.getEntity();
                    if (pokemonEntity != null) {
                        Pokemon pokemon = pokemonEntity.getPokemon();
                        if (pokemon.getPersistentData().contains("boss_clone")) {
                            if (pokemon.getPersistentData().getBoolean("boss_clone")) {
                                pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
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
                    if (pokemon.getPersistentData().contains("boss_clone")) {
                        if (pokemon.getPersistentData().getBoolean("boss_clone")) {
                            damage = Math.abs(pokemon.getCurrentHealth() - pokemon.getMaxHealth());
                        }
                    }
                } else if (actor instanceof PlayerBattleActor playerBattleActor) {
                    player = playerBattleActor.getEntity();
                }
            }

            if (player != null) {
                for (Raid raid : nr.active_raids().values()) {
                    if (raid.participating_players().contains(player)) {
                        raid.apply_damage(damage);
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

//        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.HIGHEST, event -> {
//            EmptyPokeBallEntity pokeball = event.getPokeBall();
//            PokemonEntity pokemon_entity = event.getPokemon();
//            Pokemon pokemon = pokemon_entity.getPokemon();
//            PokemonEntity clone_pokemon_entity = new PokemonEntity(pokemon_entity.getWorld(), pokemon.clone(true, null), CobblemonEntities.POKEMON);
//            for (Raid raid : cr.active_raids().values()) {
//                if (raid.uuid().equals(pokemon_entity.getUuid())) {
//                    if (raid.stage() == 4) {
//                        CobblemonEvents.THROWN_POKEBALL_HIT.postThen(
//                                new ThrownPokeballHitEvent(pokeball, clone_pokemon_entity),
//                                (Function1<? super ThrownPokeballHitEvent, Unit>) (e) -> {
//                                    EmptyPokeBallEntity e_pokeball = e.getPokeBall();
//                                    try {
//                                        Method m = e_pokeball.getClass().getDeclaredMethod("drop");
//                                        m.setAccessible(true);
//                                        m.invoke(e_pokeball);
//                                    } catch (NoSuchMethodException | InvocationTargetException |
//                                             IllegalAccessException error) {
//                                        error.printStackTrace();
//                                    }
//                                    return Unit.INSTANCE;
//                                },
//                                (Function1<? super ThrownPokeballHitEvent, Unit>) (e) -> {
//                                    EmptyPokeBallEntity e_pokeball = e.getPokeBall();
//                                    try {
//                                        Method m = e_pokeball.getClass().getDeclaredMethod("attemptCatch", PokemonEntity.class);
//                                        m.setAccessible(true);
//                                        m.invoke(e_pokeball, clone_pokemon_entity);
//                                    } catch (NoSuchMethodException | InvocationTargetException |
//                                             IllegalAccessException error) {
//                                        error.printStackTrace();
//                                    }
//                                    return Unit.INSTANCE;
//                                }
//                        );
//                    }
//                    event.cancel();
//                }
//            }
//            return Unit.INSTANCE;
//        });
    }

    public static void right_click_events() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ServerPlayerEntity playerEntity = nr.server().getPlayerManager().getPlayer(player.getUuid());
            ItemStack held_item = player.getMainHandStack();
            NbtComponent custom_data = held_item.getComponents().get(DataComponentTypes.CUSTOM_DATA);

            NbtCompound passNBT = new NbtCompound();
            passNBT.putString("raid_item", "raid_pass");
            NbtCompound voucherNBT = new NbtCompound();
            voucherNBT.putString("raid_item", "raid_voucher");

            if (playerEntity != null && custom_data != null) {
                if (hand.name().contains("MAIN_HAND") && custom_data.contains("raid_item")) {
                    if (custom_data.copyNbt().getString("raid_item").equals("raid_pass")) {
                        String boss_name = custom_data.copyNbt().getString("raid_boss");
                        String category = custom_data.copyNbt().getString("raid_category");
                        if (boss_name.equalsIgnoreCase("*")) {
                            // gui to pick raid
                        }
                        for (Raid raid : nr.active_raids().values()) {
                            if (raid.boss_info().name().equalsIgnoreCase(boss_name)) {
                                if (raid.addPlayer(playerEntity)) {
                                    nr.logger().info("[RAIDS] {} has joined the {} raid!", playerEntity.getName(), raid.boss_info().name());
                                    playerEntity.sendMessage(Text.of("You have joined the " + raid.boss_info().name() + " raid!"));

                                    int newCount = held_item.getCount() - 1;
                                    if (newCount == 0) {
                                        player.setStackInHand(hand, ItemStack.EMPTY);
                                    } else {
                                        held_item.setCount(newCount);
                                        player.setStackInHand(hand, held_item);
                                    }
                                } else {
                                    nr.logger().info("[RAIDS] {} is already in this raid!", playerEntity.getName());
                                    playerEntity.sendMessage(Text.of("You are already in this raid!"));
                                }
                            }
                        }
                    }
                }
            }

            return TypedActionResult.pass(held_item);
        });
    }
}
