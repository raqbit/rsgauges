/*
 * @file LinkReceiverSwitchBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Switch-Link receiver switches.
 */
package wile.rsgauges.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import wile.rsgauges.detail.ModResources;
import wile.rsgauges.detail.SwitchLink;
import wile.rsgauges.detail.SwitchLink.RequestResult;

import javax.annotation.Nullable;

public class LinkReceiverSwitchBlock extends SwitchBlock
{
  private final boolean is_analog;

  public LinkReceiverSwitchBlock(long config, Block.Properties properties, AxisAlignedBB unrotatedBBUnpowered, @Nullable AxisAlignedBB unrotatedBBPowered, @Nullable ModResources.BlockSoundEvent powerOnSound, @Nullable ModResources.BlockSoundEvent powerOffSound, boolean analog_device)
  { super(config, properties, unrotatedBBUnpowered, unrotatedBBPowered, powerOnSound, powerOffSound); is_analog = analog_device; }

  @Override
  public boolean switchLinkHasAnalogSupport(World world, BlockPos pos)
  { return is_analog; }

  @Override
  public SwitchLink.RequestResult switchLinkTrigger(SwitchLink link)
  {
    BlockPos pos = link.target_position;
    World world = link.world;
    SwitchTileEntity te = getTe(world, pos);
    BlockState state = world.getBlockState(pos);
    if((te==null) || (!te.verifySwitchLinkTarget(link))) return RequestResult.TARGET_GONE;
    final int p = is_analog ? link.source_analog_power : link.source_digital_power;
    te.on_power(p);
    final boolean powered = (p>0);
    final boolean was_powered = state.get(POWERED);
    if(powered != was_powered) {
      if((config & SWITCH_CONFIG_PULSE)==0) {
        world.setBlockState(pos, state.with(POWERED, powered), 1|2|8|16);
        (powered ? power_off_sound : power_on_sound).play(world, pos);
      } else {
        if(powered) {
          world.setBlockState(pos, state.with(POWERED, true), 1|2|8|16);
          power_on_sound.play(world, pos);
          te.on_timer_reset();
          te.on_timer_extend();
          te.reschedule_block_tick();
        }
      }
    }
    notifyNeighbours(world, pos, state, te, false);
    if(!te.activateSwitchLinks(te.on_power(), powered?15:0, powered != was_powered)) {
      ModResources.BlockSoundEvents.SWITCHLINK_LINK_PEAL_USE_FAILED.play(world, pos);
    }
    return SwitchLink.RequestResult.OK;
  }
}