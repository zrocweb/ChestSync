package net.krinsoft.chestsync;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

public class ChestSyncBlockListener extends BlockListener{
	private final ChestSync plugin;

	ChestSyncBlockListener(ChestSync aThis) {
		plugin = aThis;
	}

	@Override
	public void onSignChange(SignChangeEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!(event.getBlock().getState() instanceof Sign)) {
			return;
		}
		if (event.getLine(0).toLowerCase().contains("synced chest")) {
			Sign sign = (Sign)event.getBlock().getState();
			org.bukkit.material.Sign data = (org.bukkit.material.Sign)sign.getData();
			Block behind = event.getBlock().getFace(data.getFacing().getOppositeFace());
			if (behind.getState() instanceof Chest) {
				//check for valid name
				String name = event.getLine(1);
				boolean perm = false;
				if (name.trim().isEmpty()) {
					event.setCancelled(true);
					event.getPlayer().sendMessage(ChatColor.RED + "Synced Chests require a name on line 2");
					return;
				}
				if (!checkPermission(event.getPlayer(), "make", name)) {
					event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to make Synced Chests");
					sign.setLine(0, "&C[error]".replaceAll("&([a-fA-F0-9])", "\u00A7$1"));
					sign.update();
					event.setCancelled(true);
					return;
				}
				if (SyncedChest.getSyncedChest(behind.getLocation()) != null) {
					event.getPlayer().sendMessage(ChatColor.RED + "A Synced Chest already exists here.");
					event.setCancelled(true);
					sign.setLine(0, "&C[error]".replaceAll("&([a-fA-F0-9])", "\u00A7$1"));
					sign.update();
					return;
				}
				SyncedChest.createSyncedChest(event.getBlock().getLocation(), behind.getLocation(), name);
				event.getPlayer().sendMessage(ChatColor.GREEN + "Successfully created a linked chest!");
				String line0 = "", line2 = "";
				if (SyncedChest.syncedChests.get(name).size() == 1) {
					line0 = "&C[Synced]";
					line2 = "&C[no link]";
				} else {
					line0 = "&A[Synced]";
				}
				line0 = line0.replaceAll("&([a-fA-F0-9])", "\u00A7$1");
				line2 = line2.replaceAll("&([a-fA-F0-9])", "\u00A7$1");
				event.setLine(0, line0);
				event.setLine(2, line2);
				event.setLine(3, "[" + SyncedChest.syncedChests.get(name).size() + "]");
				return;
			} else {
				event.getPlayer().sendMessage(ChatColor.RED + "There is no chest behind this sign!");
				return;
			}
		}
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()){
			return;
		}
		Block block = event.getBlock();
		if (block.getState() instanceof Sign) {
			final Sign sign = (Sign)block.getState();
			if (sign.getLine(0).toLowerCase().contains("[synced]")) {
				if (!checkPermission(event.getPlayer(), "destroy", sign.getLine(1))) {
					event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to destroy this chest.");
					event.setCancelled(true);
					event.getPlayer().getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
						@Override
						public void run() {
							sign.update();
						}
					}, 2L);
					return;
				}
				org.bukkit.material.Sign data = (org.bukkit.material.Sign)sign.getData();
				Block behind = block.getFace(data.getFacing().getOppositeFace());
				SyncedChest chest = SyncedChest.getSyncedChest(behind.getLocation());
				if (chest != null) {
					SyncedChest.removeSyncedChest(behind.getLocation());
					event.getPlayer().sendMessage(ChatColor.YELLOW + "Synced Chest removed");
				}
			}
		} else if (block.getState() instanceof Chest) {
			SyncedChest chest = SyncedChest.getSyncedChest(block.getLocation());
			if (chest != null) {
				if (!checkPermission(event.getPlayer(), "destroy", chest.getName())) {
					event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to destroy this chest.");
					event.setCancelled(true);
					return;
				}
				SyncedChest.removeSyncedChest(block.getLocation());
				event.getPlayer().sendMessage(ChatColor.YELLOW + "Synced Chest removed");
			}
		}
	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (block.getState() instanceof Chest) {
			if (SyncedChest.chests.containsKey(block.getRelative(BlockFace.NORTH).getLocation())) {
				event.getPlayer().sendMessage(ChatColor.RED + "A Synced Chest can only be single");
				event.setCancelled(true);
			}
			if (SyncedChest.chests.containsKey(block.getRelative(BlockFace.EAST).getLocation())) {
				event.getPlayer().sendMessage(ChatColor.RED + "A Synced Chest can only be single");
				event.setCancelled(true);
			}
			if (SyncedChest.chests.containsKey(block.getRelative(BlockFace.SOUTH).getLocation())) {
				event.getPlayer().sendMessage(ChatColor.RED + "A Synced Chest can only be single");
				event.setCancelled(true);
			}
			if (SyncedChest.chests.containsKey(block.getRelative(BlockFace.WEST).getLocation())) {
				event.getPlayer().sendMessage(ChatColor.RED + "A Synced Chest can only be single");
				event.setCancelled(true);
			}
		}
	}

	private boolean checkPermission(Player player, String field, String name) {
		boolean perm = false;
		if (player.hasPermission("chestsync." + field)) {
			if (player.getName().equalsIgnoreCase(name) && player.hasPermission("chestsync." + field + ".self")) {
				perm = true;
			} else if (player.hasPermission("chestsync." + field + "." + name)) {
				perm = true;
			} else if(player.hasPermission("chestsync." + field + ".*")) {
				perm = true;
			} else {
				perm = false;
			}
		}
		return perm;
	}

}