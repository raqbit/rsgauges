/*
 * @file RsBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Base class for blocks used in this mod. It sets the
 * defaults for block properties, registration categories,
 * and provides default overloads for further blocks.
 * As rsgauges blocks work directional (placed with a
 * defined facing), the basics for `facing` dependent
 * data are implemented here, too.
 */
package wile.rsgauges.blocks;

import net.minecraft.world.server.ServerWorld;
import wile.rsgauges.detail.ModAuxiliaries;
import wile.rsgauges.detail.ModColors;
import wile.rsgauges.detail.ModConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.*;
import net.minecraft.entity.EntityType;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public abstract class RsBlock extends Block implements ModColors.ColorTintSupport
{
  public static final long RSBLOCK_CONFIG_SOLID              = 0x0000000000000000l;
  public static final long RSBLOCK_CONFIG_CUTOUT             = 0x1000000000000000l;
  public static final long RSBLOCK_CONFIG_CUTOUT_MIPPED      = 0x2000000000000000l;
  public static final long RSBLOCK_CONFIG_TRANSLUCENT        = 0x3000000000000000l;
  public static final long RSBLOCK_CONFIG_COLOR_TINT_SUPPORT = 0x0004000000000000l;   /// TODO: REMOVE FROM SWITCH/GAUGE CONF
  public enum RenderTypeHint { SOLID,CUTOUT,CUTOUT_MIPPED,TRANSLUCENT }

  private static final RenderTypeHint render_layer_map_[] = { RenderTypeHint.SOLID, RenderTypeHint.CUTOUT, RenderTypeHint.CUTOUT_MIPPED, RenderTypeHint.TRANSLUCENT };
  public final long config;

  // -------------------------------------------------------------------------------------------------------------------

  public RsBlock(long config, Block.Properties properties)
  { this(config, properties, ModAuxiliaries.getPixeledAABB(0, 0, 0, 16, 16,16 )); }

  public RsBlock(long config, Block.Properties properties, final AxisAlignedBB aabb)
  { this(config, properties, VoxelShapes.create(aabb)); }

  public RsBlock(long config, Block.Properties properties, final VoxelShape vshape)
  {
    super(properties);
    this.config = config;
  }

  public RenderTypeHint getRenderTypeHint()
  { return render_layer_map_[(int)((config>>60)&0x3)]; }

  // -------------------------------------------------------------------------------------------------------------------
  // Block overrides
  // -------------------------------------------------------------------------------------------------------------------

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public boolean addHitEffects(BlockState state, World worldObj, RayTraceResult target, net.minecraft.client.particle.ParticleManager manager)
  { return true; } // no hit particles

  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("deprecation")
  public boolean hasCustomBreakingProgress(BlockState state)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return VoxelShapes.fullCube(); }

  @Override
  @SuppressWarnings("deprecation")
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }
  // was { return (!isCube()) ? (NULL_AABB) : (getBoundingBox(state, world, pos)); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(BlockState state, IBlockReader worldIn, BlockPos pos)
  { return false; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canEntitySpawn(BlockState state, IBlockReader worldIn, BlockPos pos, EntityType<?> type)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public PushReaction getPushReaction(BlockState state)
  { return PushReaction.DESTROY; } // prevent tile entities getting moved around

  @Override
  @SuppressWarnings("deprecation")
  public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
  {
    if(state.hasTileEntity() && (state.getBlock() != newState.getBlock())) {
      if((state.getBlock() instanceof RsBlock)) ((RsBlock)state.getBlock()).onRsBlockDestroyed(state, world, pos, false);
      world.removeTileEntity(pos);
      world.updateComparatorOutputLevel(pos, this);
      world.notifyNeighbors(pos, state.getBlock());
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
  { return Collections.singletonList(new ItemStack(state.getBlock().asItem())); }

  @Override
  public boolean hasTileEntity(BlockState state)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public void onBlockClicked(BlockState state, World world, BlockPos pos, PlayerEntity player)
  {}

  ///////////// --------------------------------------------------------------------------------------------------------
  // 1.15 transition

  @Deprecated
  public ActionResultType func_225533_a_(BlockState p_225533_1_, World p_225533_2_, BlockPos p_225533_3_, PlayerEntity p_225533_4_, Hand p_225533_5_, BlockRayTraceResult p_225533_6_)
  { return onBlockActivated(p_225533_1_,p_225533_2_,p_225533_3_,p_225533_4_,p_225533_5_,p_225533_6_) ? ActionResultType.SUCCESS : ActionResultType.PASS; }

  public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  { return true; }

  @Deprecated
  public void func_225534_a_(BlockState p_225534_1_, ServerWorld p_225534_2_, BlockPos p_225534_3_, Random p_225534_4_)
  { tick(p_225534_1_,p_225534_2_,p_225534_3_,p_225534_4_); }

  public void tick(BlockState state, World world, BlockPos pos, Random rnd)
  {}

  // 1.15 /transition
  ///////////// --------------------------------------------------------------------------------------------------------

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(BlockState state)
  { return super.getLightValue(state); }

  // @Override public int getLightValue(BlockState state, IEnviromentBlockReader world, BlockPos pos) { return getLightValue(state); }

  // -------------------------------------------------------------------------------------------------------------------

  @Override
  public boolean hasColorMultiplierRGBA()
  { return (!ModConfig.without_color_tinting) && ((config & RSBLOCK_CONFIG_COLOR_TINT_SUPPORT)!=0); }

  @Override
  public int getColorMultiplierRGBA(@Nullable BlockState state, @Nullable ILightReader world, @Nullable BlockPos pos)
  {
    if(!(world instanceof IWorldReader)) return 0xffffffff;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof RsTileEntity)) return 0xffffffff;
    return ModAuxiliaries.DyeColorFilters.lightTintByIndex(((RsTileEntity)te).color_tint());
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * RsBlock handler before a block gets dropped as item in the world.
   * Allows actions in the tile entity to happen before the forge/MC
   * block dropping actions are invoked.
   */
  protected void onRsBlockDestroyed(BlockState state, World world, BlockPos pos, boolean isUpdateEvent)
  {} // @todo check if still needed in 1.14

  /**
   * Checks if these blocks can be placed at a given position with a given facing. The client does not send a
   * placing request if not, the server will drop the item if it was placed somehow.
   */
  protected boolean onBlockPlacedByCheck(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos)
  { return state; }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
  { neighborUpdated(state, world, pos, fromPos); }

  // using internal method because the neighborChanged() implementation changes twice a day.
  protected void neighborUpdated(BlockState state, World world, BlockPos pos, BlockPos facingPos)
  {}

  // -------------------------------------------------------------------------------------------------------------------
  // Basic tile entity
  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Main RsBlock derivate tile entity base
   */
  public static abstract class RsTileEntity extends TileEntity
  {
    private static final int NBT_ENTITY_TYPE = 1; // forge-doc: use 1, does not matter, only used by vanilla.

    public RsTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public void writeNbt(CompoundNBT nbt, boolean updatePacket)
    {}

    public void readNbt(CompoundNBT nbt, boolean updatePacket)
    {}

    public int color_tint()
    { return 0xffffffff; }

    // --------------------------------------------------------------------------------------------------------
    // TileEntity
    // --------------------------------------------------------------------------------------------------------

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writeNbt(nbt, false); return nbt; }

    @Override
    public void read(CompoundNBT nbt)
    { super.read(nbt); readNbt(nbt, false); }

  }

}