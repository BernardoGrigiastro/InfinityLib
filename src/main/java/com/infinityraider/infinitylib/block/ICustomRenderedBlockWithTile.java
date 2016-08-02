package com.infinityraider.infinitylib.block;

import com.infinityraider.infinitylib.block.blockstate.IBlockStateSpecial;
import com.infinityraider.infinitylib.block.tile.TileEntityBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.infinityraider.infinitylib.render.block.RenderBlock;

/**
 * Implemented in a Block class to have special rendering handling for the block
 * @param <T> TileEntity class for this block, can be simple TileEntity if this block doesn't have a tile entity
 */
public interface ICustomRenderedBlockWithTile<T extends TileEntityBase> extends ICustomRenderedBlock {
    /**
     * This is here to make sure a block state containing the tile entity and block position of the block are passed in the block's getExtendedState method
     * @param state the block's in world state (can be an IExtendedState)
     * @param world the world
     * @param pos the block's position in the world
     * @return a special block state containing the tile entity and the position
     */
    @SuppressWarnings("unused")
    IBlockStateSpecial<T, ? extends IBlockState> getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos);

    /**
     * Gets called to create the IBlockRenderingHandler instance to render this block with
     * @return a new IBlockRenderingHandler object for this block
     */
    @Override
    @SideOnly(Side.CLIENT)
    RenderBlock<? extends BlockBase> getRenderer();
}
