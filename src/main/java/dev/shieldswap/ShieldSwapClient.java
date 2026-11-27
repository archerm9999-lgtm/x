package dev.shieldswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ShieldSwapClient implements ClientModInitializer {

    // settings
    private static final boolean DEBUG = true;              // shows [ShieldSwap] messages in chat; set false once it works
    private static final boolean REQUIRE_FACING = true;     // only if their shield faces you
    private static final boolean AUTO_FOLLOWUP_HIT = true;  // false = swap only, you click yourself
    private static final int FOLLOWUP_DELAY_TICKS = 2;      // 1 = same tick, 2 = next tick
    private static final double MAX_REACH = 3.0;

    private static boolean pending = false;
    private static boolean ourOwnAttack = false;
    private static boolean announced = false;
    private static int targetId = -1;
    private static int ticksLeft = 0;

    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (player != Minecraft.getInstance().player) return InteractionResult.PASS;
            if (pending || ourOwnAttack) return InteractionResult.PASS;

            if (!(entity instanceof Player target) || target == player) {
                debug(player, "hit something that isn't a player");
                return InteractionResult.PASS;
            }

            boolean blocking = target.isBlocking();
            boolean facing = shieldFacesAttacker(player, target);
            debug(player, "hit " + target.getName().getString() + " | blocking=" + blocking + " facing=" + facing);
            if (!blocking) return InteractionResult.PASS;
            if (REQUIRE_FACING && !facing) return InteractionResult.PASS;

            if (player.getMainHandItem().is(ItemTags.AXES)) {
                debug(player, "already holding an axe");
            } else {
                int axeSlot = bestHotbarSlot(player, ItemTags.AXES);
                if (axeSlot == -1) {
                    debug(player, "no axe on your hotbar");
                    return InteractionResult.PASS;
                }
                player.getInventory().setSelectedSlot(axeSlot);
                debug(player, "swapped to axe (slot " + (axeSlot + 1) + ")");
            }

            pending = true;
            targetId = target.getId();
            ticksLeft = FOLLOWUP_DELAY_TICKS;
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(ShieldSwapClient::onTick);
    }

    private static void onTick(Minecraft mc) {
        if (!announced && mc.player != null) {
            announced = true;
            debug(mc.player, "mod loaded");
        }
        if (!pending) return;

        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) {
            pending = false;
            return;
        }
        if (--ticksLeft > 0) return;
        pending = false;

        int slot = firstHotbarSlot(player, Items.MACE);
        boolean mace = slot != -1;
        if (!mace) slot = bestHotbarSlot(player, ItemTags.SWORDS);
        if (slot == -1) {
            debug(player, "no mace or sword on your hotbar");
            return;
        }

        player.getInventory().setSelectedSlot(slot);
        debug(player, "swapped to " + (mace ? "mace" : "sword") + " (slot " + (slot + 1) + ")");
        if (!AUTO_FOLLOWUP_HIT) return;

        Entity target = mc.level.getEntity(targetId);
        if (target instanceof LivingEntity living
                && living.isAlive()
                && living.getBoundingBox().distanceToSqr(player.getEyePosition()) <= MAX_REACH * MAX_REACH) {
            ourOwnAttack = true;
            try {
                mc.gameMode.attack(player, living);
                player.swing(InteractionHand.MAIN_HAND);
                debug(player, "follow-up hit sent");
            } finally {
                ourOwnAttack = false;
            }
        } else {
            debug(player, "no follow-up hit (target dead or out of reach)");
        }
    }

    private static void debug(Player player, String message) {
          if (DEBUG) player.sendSystemMessage(Component.literal("[ShieldSwap] " + message));
    }

    private static boolean shieldFacesAttacker(Player attacker, Player target) {
        double yaw = Math.toRadians(target.getYHeadRot());
        double dx = attacker.getX() - target.getX();
        double dz = attacker.getZ() - target.getZ();
        return dx * -Math.sin(yaw) + dz * Math.cos(yaw) > 0;
    }

    private static int firstHotbarSlot(Player player, Item item) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    private static int bestHotbarSlot(Player player, TagKey<Item> tag) {
        int best = -1;
        int bestScore = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(tag)) continue;
            int score = tierScore(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
            if (score > bestScore) {
                best = i;
                bestScore = score;
            }
        }
        return best;
    }

    private static int tierScore(String id) {
        if (id.startsWith("netherite")) return 6;
        if (id.startsWith("diamond")) return 5;
        if (id.startsWith("iron")) return 4;
        if (id.startsWith("copper")) return 3;
        if (id.startsWith("stone")) return 2;
        if (id.startsWith("golden")) return 1;
        return 0;
    }
}
