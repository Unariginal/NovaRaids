package me.unariginal.novaraids.config;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Species;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.items.RaidBall;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private final NovaRaids nr = NovaRaids.INSTANCE;

    boolean debug = false;

    // Raid Settings
    boolean use_queue_system = false;
    boolean run_raids_with_no_players = false;
    List<Species> global_banned_pokemon = new ArrayList<>();
    List<Move> global_banned_moves = new ArrayList<>();
    List<Ability> global_banned_abilities = new ArrayList<>();
    List<Item> global_banned_held_items = new ArrayList<>();
    List<Item> global_banned_bag_items = new ArrayList<>();

    // Item Settings
    boolean vouchers_enabled = true;
    Item default_voucher_item = Items.FEATHER;
    Text default_voucher_name = Text.empty();
    List<Text> default_voucher_lore = new ArrayList<>();
    ComponentChanges default_voucher_data = ComponentChanges.EMPTY;

    boolean passes_enabled = true;
    Item default_pass_item = Items.PAPER;
    Text default_pass_name = Text.empty();
    List<Text> default_pass_lore = new ArrayList<>();
    ComponentChanges default_pass_data = ComponentChanges.EMPTY;

    boolean raid_balls_enabled = true;
    List<RaidBall> raid_balls = new ArrayList<>();

    public Config() {

    }
}
