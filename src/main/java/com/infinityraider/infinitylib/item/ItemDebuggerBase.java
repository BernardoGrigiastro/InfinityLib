package com.infinityraider.infinitylib.item;

import com.infinityraider.infinitylib.reference.Names;
import com.infinityraider.infinitylib.utility.debug.DebugMode;
import com.infinityraider.infinitylib.utility.debug.DebugModeFeedback;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
/**
 * Base class for a debug item, allows a single item to have multiple debug modes
 */
public abstract class ItemDebuggerBase extends ItemBase {
    private final List<DebugMode> DEBUG_MODES;

    public ItemDebuggerBase() {
        this(true);
    }

    public ItemDebuggerBase(boolean isVanilla) {
        super("debugger", new Item.Properties());
        this.DEBUG_MODES = new ArrayList<>();
        this.DEBUG_MODES.add(new DebugModeFeedback());
        this.DEBUG_MODES.addAll(getDebugModes());
    }

    protected abstract List<DebugMode> getDebugModes();

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                DebugMode mode = this.changeDebugMode(stack);
                player.sendMessage(new StringTextComponent("Set debug mode to " + mode.debugName()), Util.DUMMY_UUID);
            }
        } else {
            this.getDebugMode(stack).debugActionClicked(stack, world, player, hand);
        }
        return new ActionResult<>(ActionResultType.PASS, stack);
    }


    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        if(!context.getPlayer().isSneaking()) {
            ItemStack stack = context.getItem();
            this.getDebugMode(stack).debugActionBlockClicked(stack, context);
        }
        return ActionResultType.PASS;
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        if(!player.isSneaking()) {
            this.getDebugMode(stack).debugActionEntityClicked(stack, player, target, hand);
        }
        return ActionResultType.PASS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flag) {
        DebugMode mode = getDebugMode(stack);
        tooltip.add(new StringTextComponent("Right Click to use the debugger in its current mode"));
        tooltip.add(new StringTextComponent("Shift + Right Click to cycle debug modes"));
        tooltip.add(new StringTextComponent("Current debug mode: "  + (mode == null ? "null" : mode.debugName())));
    }

    private DebugMode getDebugMode(ItemStack stack) {
        CompoundNBT tag;
        if(!stack.hasTag()) {
            tag = new CompoundNBT();
            stack.setTag(tag);
        } else {
            tag = stack.getTag();
        }
        if(!tag.contains(Names.NBT.COUNT)) {
            tag.putInt(Names.NBT.COUNT, 0);
        }
        return DEBUG_MODES.get(tag.getInt(Names.NBT.COUNT) % DEBUG_MODES.size());
    }

    private DebugMode changeDebugMode(ItemStack stack) {
        CompoundNBT tag;
        if(!stack.hasTag()) {
            tag = new CompoundNBT();
            stack.setTag(tag);
        } else {
            tag = stack.getTag();
        }
        int index;
        if(!tag.contains(Names.NBT.COUNT)) {
            index = 1;
        } else {
            index = (tag.getInt(Names.NBT.COUNT) + 1 ) % DEBUG_MODES.size();
        }
        tag.putInt(Names.NBT.COUNT, index);
        return DEBUG_MODES.get(index);
    }
}
