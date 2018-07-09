package EmpiresMod.protection;

import java.util.HashMap;
import java.util.Map;

import EmpiresMod.Empires;
import EmpiresMod.API.Chat.Component.ChatManager;
import EmpiresMod.Configuration.Config;
import EmpiresMod.Datasource.EmpiresUniverse;
import EmpiresMod.Localization.LocalizationManager;
import EmpiresMod.Utilities.EmpireUtils;
import EmpiresMod.Utilities.PlayerUtils;
import EmpiresMod.Utilities.WorldUtils;
import EmpiresMod.entities.Empire.BlockWhitelist;
import EmpiresMod.entities.Empire.Citizen;
import EmpiresMod.entities.Empire.Empire;
import EmpiresMod.entities.Empire.EmpireBlock;
import EmpiresMod.entities.Empire.Plot;
import EmpiresMod.entities.Empire.Wild;
import EmpiresMod.entities.Flags.FlagType;
import EmpiresMod.entities.Misc.Volume;
import EmpiresMod.entities.Position.BlockPosition;
import EmpiresMod.entities.Position.EntityPos;
import EmpiresMod.entities.Signs.SellSign;
import EmpiresMod.protection.JSON.Protection;
import EmpiresMod.protection.Segment.Segment;
import EmpiresMod.protection.Segment.SegmentBlock;
import EmpiresMod.protection.Segment.SegmentEntity;
import EmpiresMod.protection.Segment.SegmentItem;
import EmpiresMod.protection.Segment.SegmentSpecialBlock;
import EmpiresMod.protection.Segment.SegmentTileEntity;
import EmpiresMod.protection.Segment.Enums.EntityType;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Utilities for the protections
 */
public class ProtectionManager {

	public static final Segment.Container<SegmentBlock> segmentsBlock = new Segment.Container<SegmentBlock>();
	public static final Segment.Container<SegmentSpecialBlock> segmentsSpecialBlock = new Segment.Container<SegmentSpecialBlock>();
	public static final Segment.Container<SegmentEntity> segmentsTrackedEntity = new Segment.Container<SegmentEntity>();
	public static final Segment.Container<SegmentEntity> segmentsEntity = new Segment.Container<SegmentEntity>();
	public static final Segment.Container<SegmentItem> segmentsItem = new Segment.Container<SegmentItem>();
	public static final Segment.Container<SegmentTileEntity> segmentsTile = new Segment.Container<SegmentTileEntity>();
	private static final Map<EntityPlayer, EntityPos> lastTickPlayerPos = new HashMap<EntityPlayer, EntityPos>();

	private ProtectionManager() {
	}

	public static void addProtection(Protection protection) {
		for (Segment segment : protection.segments) {
			if (segment instanceof SegmentSpecialBlock) {
				segmentsSpecialBlock.add((SegmentSpecialBlock) segment);
			} else if (segment instanceof SegmentBlock) {
				segmentsBlock.add((SegmentBlock) segment);
			} else if (segment instanceof SegmentEntity) {
				SegmentEntity segmentEntity = (SegmentEntity) segment;
				if (segmentEntity.types.contains(EntityType.TRACKED)) {
					segmentsTrackedEntity.add(segmentEntity);
				} else {
					segmentsEntity.add(segmentEntity);
				}
			} else if (segment instanceof SegmentItem) {
				segmentsItem.add((SegmentItem) segment);
			} else if (segment instanceof SegmentTileEntity) {
				segmentsTile.add((SegmentTileEntity) segment);
			}
		}
	}

	public static void check(EntityPlayerMP player) {
		Empire empire = EmpireUtils.getEmpireAtPosition(player.dimension, (int) Math.floor(player.posX) >> 4,
				(int) Math.floor(player.posZ) >> 4);
		Citizen res = EmpiresUniverse.instance.getOrMakeCitizen(player);
		EntityPos lastTickPos = lastTickPlayerPos.get(player);

		if (res == null) {
			return;
		}

		if (!ProtectionManager.hasPermission(res, FlagType.ENTER, player.dimension, (int) Math.floor(player.posX),
				(int) Math.floor(player.posY), (int) Math.floor(player.posZ))) {
			if (lastTickPos == null) {
				res.knockbackPlayerToBorder(empire);
			} else if (lastTickPos.getX() != player.posX || lastTickPos.getY() != player.posY
					|| lastTickPos.getZ() != player.posZ || lastTickPos.getDim() != player.dimension) {
				PlayerUtils.teleport(player, lastTickPos.getDim(), lastTickPos.getX(), lastTickPos.getY(),
						lastTickPos.getZ());
			}
		} else {
			// TODO: Refactor so that it's understandable
			if (lastTickPos != null
					&& (((int) Math.floor(lastTickPos.getX())) >> 4 != (int) (Math.floor(player.posX)) >> 4
							|| ((int) Math.floor(lastTickPos.getZ())) >> 4 != (int) (Math.floor(player.posZ)) >> 4)) {
				if (lastTickPos.getDim() == player.dimension) {
					res.checkLocation(((int) Math.floor(lastTickPos.getX())) >> 4,
							((int) Math.floor(lastTickPos.getZ())) >> 4, ((int) Math.floor(player.posX)) >> 4,
							((int) (Math.floor(player.posZ))) >> 4, player.dimension);
				} else {
					res.checkLocationOnDimensionChanged((int) (Math.floor(player.posX)),
							(int) (Math.floor(player.posZ)), player.dimension);
				}
			}

			if (lastTickPos != null && empire != null) {
				Plot currentPlot = empire.plotsContainer.get(player.dimension, (int) Math.floor(player.posX),
						(int) Math.floor(player.posY), (int) Math.floor(player.posZ));
				Plot lastTickPlot = empire.plotsContainer.get(lastTickPos.getDim(),
						(int) Math.floor(lastTickPos.getX()), (int) Math.floor(lastTickPos.getY()),
						(int) Math.floor(lastTickPos.getZ()));

				if (currentPlot != null && (lastTickPlot == null || currentPlot != lastTickPlot)) {
					ChatManager.send(player, "Empires.notification.plot.enter", currentPlot);
				} else if (currentPlot == null && lastTickPlot != null) {
					ChatManager.send(player, "Empires.notification.plot.enter",
							LocalizationManager.get("Empires.notification.plot.enter.unassigned"));
				}
			}
			lastTickPlayerPos.put(player, new EntityPos(player.posX, player.posY, player.posZ, player.dimension));
		}
	}

	public static boolean checkExist(Entity entity, boolean spawn) {
		if (entity instanceof EntityLiving) {
			if (Config.instance.mobTravelInEmpires.get() && !spawn) {
				return false;
			}
			if (!getFlagValueAtLocation(FlagType.ENTITIES, entity.dimension, (int) Math.floor(entity.posX),
					(int) Math.floor(entity.posY), (int) Math.floor(entity.posZ))) {
				entity.isDead = true;
				entity.setDead();
				return true;
			}
		}

		for (SegmentEntity segment : segmentsTrackedEntity.get(entity.getClass())) {
			if (!segment.shouldExist(entity)) {
				entity.isDead = true;
				entity.setDead();
				return true;
			}
		}

		return false;
	}

	public static void checkImpact(Entity entity, Citizen owner, MovingObjectPosition mop, Event event) {
		for (SegmentEntity segment : segmentsEntity.get(entity.getClass())) {
			if (!segment.shouldImpact(entity, owner, mop)) {
				event.setCanceled(true);
				entity.isDead = true;
				entity.setDead();
			}
		}
	}

	public static void check(TileEntity te) {
		for (SegmentTileEntity segment : segmentsTile.get(te.getClass())) {
			if (!segment.shouldExist(te)) {
				ItemStack itemStack = new ItemStack(te.getBlockType(), 1, te.getBlockMetadata());
				NBTTagCompound nbt = new NBTTagCompound();
				te.writeToNBT(nbt);
				itemStack.setTagCompound(nbt);
				WorldUtils.dropAsEntity(te.getWorld(), te.getPos().getX(), te.getPos().getY(), te.getPos().getZ(), itemStack);
				te.getWorld().setBlockState(new BlockPos(te.getPos().getX(), te.getPos().getY(), te.getPos().getZ()), Blocks.air.getDefaultState());
				te.invalidate();
				Empires.instance.LOG.info("TileEntity {} was ATOMICALLY DISINTEGRATED!", te.toString());
				return;
			}
		}
	}

	public static void checkInteraction(Entity entity, Citizen res, Event event) {
		if (!event.isCancelable()) {
			return;
		}

		for (SegmentEntity segment : segmentsEntity.get(entity.getClass())) {
			if (!segment.shouldInteract(entity, res)) {
				event.setCanceled(true);
			}
		}
	}

	public static void checkPVP(Entity entity, Citizen res, Event event) {
		if (!event.isCancelable()) {
			return;
		}

		for (SegmentEntity segment : segmentsEntity.get(entity.getClass())) {
			if (!segment.shouldAttack(entity, res)) {
				event.setCanceled(true);
			}
		}
	}

	public static void checkUsage(ItemStack stack, Citizen res, PlayerInteractEvent.Action action, BlockPosition bp,
			EnumFacing face, Event ev) {
		if (!ev.isCancelable()) {
			return;
		}

		for (SegmentItem segment : segmentsItem.get(stack.getItem().getClass())) {
			if (!segment.shouldInteract(stack, res, action, bp, face)) {
				ev.setCanceled(true);
			}
		}
	}

	public static void checkBreakWithItem(ItemStack stack, Citizen res, BlockPosition bp, Event ev) {
		if (!ev.isCancelable()) {
			return;
		}

		for (SegmentItem segment : segmentsItem.get(stack.getItem().getClass())) {
			if (!segment.shouldBreakBlock(stack, res, bp)) {
				ev.setCanceled(true);
			}
		}
	}

	public static void checkBlockInteraction(Citizen res, BlockPosition bp, PlayerInteractEvent.Action action, Event ev) {
		if (!ev.isCancelable()) {
			return;
		}

		World world = MinecraftServer.getServer().worldServerForDimension(bp.getDim());
		Block block = world.getBlockState(new BlockPos(bp.getX(), bp.getY(), bp.getZ())).getBlock();

		// Bypass for SellSign
		if (block instanceof BlockSign) {
			TileEntity te = world.getTileEntity(new BlockPos(bp.getX(), bp.getY(), bp.getZ()));
			if (te instanceof TileEntitySign && SellSign.SellSignType.instance.isTileValid((TileEntitySign) te)) {
				return;
			}
		}

		for (SegmentBlock segment : segmentsBlock.get(block.getClass())) {
			if (!segment.shouldInteract(res, bp, action)) {
				ev.setCanceled(true);
			}
		}
	}

	public static boolean checkBlockBreak(Block block) {
		for (SegmentSpecialBlock segment : segmentsSpecialBlock.get(block.getClass())) {
			if (segment.isAlwaysBreakable()) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasPermission(Citizen res, FlagType<Boolean> flagType, int dim, int x, int y, int z) {
		if (EmpiresUniverse.instance.blocks.contains(dim, x >> 4, z >> 4)) {
			Empire empire = EmpiresUniverse.instance.blocks.get(dim, x >> 4, z >> 4).getEmpire();
			return empire.hasPermission(res, flagType, dim, x, y, z);
		} else {
			return !flagType.isWildPerm || Wild.instance.hasPermission(res, flagType);
		}
	}

	public static <T> T getFlagValueAtLocation(FlagType<T> flagType, int dim, int x, int y, int z) {
		if (EmpiresUniverse.instance.blocks.contains(dim, x >> 4, z >> 4)) {
			Empire empire = EmpiresUniverse.instance.blocks.get(dim, x >> 4, z >> 4).getEmpire();
			return empire.getValueAtCoords(dim, x, y, z, flagType);
		} else {
			return flagType.isWildPerm ? Wild.instance.flagsContainer.get(flagType).value : null;
		}
	}

	public static boolean hasPermission(Citizen res, FlagType<Boolean> flagType, int dim, Volume volume) {
		boolean inWild = false;

		for (int empireBlockX = volume.getMinX() >> 4; empireBlockX <= volume.getMaxX() >> 4; empireBlockX++) {
			for (int empireBlockZ = volume.getMinZ() >> 4; empireBlockZ <= volume.getMaxZ() >> 4; empireBlockZ++) {
				EmpireBlock empireBlock = EmpiresUniverse.instance.blocks.get(dim, empireBlockX, empireBlockZ);

				if (empireBlock == null) {
					inWild = true;
					continue;
				}

				Empire empire = empireBlock.getEmpire();
				Volume rangeBox = volume.intersect(empireBlock.toVolume());

				// If the range volume intersects the EmpireBlock, check
				// Empire/Plot permissions
				if (rangeBox != null) {
					int totalIntersectArea = 0;

					// Check every plot in the current EmpireBlock and sum all
					// plot areas
					for (Plot plot : empireBlock.plotsContainer) {
						Volume plotIntersection = rangeBox.intersect(plot.toVolume());
						if (plotIntersection != null) {
							if (!plot.hasPermission(res, flagType)) {
								return false;
							}
							totalIntersectArea += plotIntersection.getVolumeAmount();
						}
					}

					// If plot area sum is not equal to range area, check empire
					// permission
					if (totalIntersectArea != rangeBox.getVolumeAmount()) {
						if (!empire.hasPermission(res, flagType)) {
							return false;
						}
					}
				}
			}
		}

		if (inWild) {
			return Wild.instance.hasPermission(res, flagType);
		}

		return true;
	}

	public static Citizen getOwner(Entity entity) {
		for (SegmentEntity segment : segmentsEntity.get(entity.getClass())) {
			return segment.getOwner(entity);
		}
		return null;
	}

	public static boolean isOwnable(Class<? extends TileEntity> clazz) {
		for (SegmentTileEntity segment : segmentsTile.get(clazz)) {
			if (segment.retainsOwner()) {
				return true;
			}
		}

		return false;
	}

	public static boolean isBlockWhitelistValid(BlockWhitelist bw) {
		// Delete if the empire is gone
		if (EmpireUtils.getEmpireAtPosition(bw.getDim(), bw.getX() >> 4, bw.getZ() >> 4) == null) {
			return false;
		}

		if (!bw.getFlagType().isWhitelistable) {
			return false;
		}

		/*
		 * if (bw.getFlagType() == FlagType.ACTIVATE &&
		 * !checkActivatedBlocks(MinecraftServer.getServer().
		 * worldServerForDimension(bw.getDim()).getBlock(bw.getX(), bw.getY(),
		 * bw.getZ()),
		 * MinecraftServer.getServer().worldServerForDimension(bw.getDim()).
		 * getBlockMetadata(bw.getX(), bw.getY(), bw.getZ()))) return false; if
		 * (bw.getFlagType() == FlagType.MODIFY || bw.getFlagType() ==
		 * FlagType.ACTIVATE || bw.getFlagType() == FlagType.USAGE) { TileEntity
		 * te =
		 * MinecraftServer.getServer().worldServerForDimension(bw.getDim()).
		 * getTileEntity(bw.getX(), bw.getY(), bw.getZ()); if (te == null)
		 * return false; return
		 * getFlagsForTile(te.getClass()).contains(bw.getFlagType()); }
		 */
		return true;
	}

	public static void saveBlockOwnersToDB() {
		for (Map.Entry<TileEntity, Citizen> set : ProtectionHandlers.instance.ownedTileEntities.entrySet()) {
			Empires.instance.datasource.saveBlockOwner(set.getValue(), set.getKey().getWorld().provider.getDimensionId(),
					set.getKey().getPos().getX(), set.getKey().getPos().getY(), set.getKey().getPos().getZ());
		}
	}

	/**
	 * Method called by the ThreadPlacementCheck after it found a TileEntity
	 */
	public static synchronized void addTileEntity(TileEntity te, Citizen res) {
		ProtectionHandlers.instance.ownedTileEntities.put(te, res);
		if (ProtectionHandlers.instance.activePlacementThreads != 0)
			ProtectionHandlers.instance.activePlacementThreads--;
	}

	public static synchronized void placementThreadTimeout() {
		ProtectionHandlers.instance.activePlacementThreads--;
	}

	private EmpiresUniverse getUniverse() {
		return EmpiresUniverse.instance;
	}
}