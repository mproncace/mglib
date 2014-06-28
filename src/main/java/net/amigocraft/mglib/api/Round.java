package net.amigocraft.mglib.api;

import static net.amigocraft.mglib.Main.locale;
import static net.amigocraft.mglib.MGUtil.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import com.google.common.collect.Lists;

import net.amigocraft.mglib.MGUtil;
import net.amigocraft.mglib.Main;
import net.amigocraft.mglib.Metadatable;
import net.amigocraft.mglib.RollbackManager;
import net.amigocraft.mglib.UUIDFetcher;
import net.amigocraft.mglib.event.player.PlayerHitArenaBorderEvent;
import net.amigocraft.mglib.event.player.PlayerJoinMinigameRoundEvent;
import net.amigocraft.mglib.event.player.PlayerLeaveMinigameRoundEvent;
import net.amigocraft.mglib.event.round.MinigameRoundEndEvent;
import net.amigocraft.mglib.event.round.MinigameRoundPrepareEvent;
import net.amigocraft.mglib.event.round.MinigameRoundStartEvent;
import net.amigocraft.mglib.event.round.MinigameRoundTickEvent;
import net.amigocraft.mglib.exception.ArenaNotExistsException;
import net.amigocraft.mglib.exception.InvalidLocationException;
import net.amigocraft.mglib.exception.PlayerNotPresentException;
import net.amigocraft.mglib.exception.PlayerOfflineException;
import net.amigocraft.mglib.exception.PlayerPresentException;
import net.amigocraft.mglib.exception.RoundFullException;

/**
 * Represents a round within a minigame.
 * @author Maxim Roncacé
 * @since 0.1.0
 */
public class Round implements Metadatable {

	private int minPlayers;
	private int maxPlayers;
	private int prepareTime;
	private int roundTime;
	private Location exitLocation;

	private String plugin;
	private int time = 0;
	private Stage stage;

	private String world;
	private String arena;
	private List<Location> spawns = new ArrayList<Location>();
	private Location minBound;
	private Location maxBound;

	private HashMap<String, MGPlayer> players = new HashMap<String, MGPlayer>();

	private int timerHandle = -1;

	private boolean damage;
	private boolean pvp;
	private boolean rollback;

	/**
	 * Creates a new {@link Round} with the given parameters.
	 * <br><br>
	 * Please use {@link Minigame#createRound(String)} unless you
	 * understand the implications of using this constructor.
	 * @param plugin the plugin which this round should be associated with.
	 * @param arena the name of the arena in which this round takes place in.
	 * @throws ArenaNotExistsException if the specified arena does not exist.
	 */
	public Round(String plugin, String arena) throws ArenaNotExistsException {
		YamlConfiguration y = loadArenaYaml(plugin);
		if (!y.contains(arena))
			throw new ArenaNotExistsException();
		ConfigurationSection cs = y.getConfigurationSection(arena); // make the code easier to read
		world = cs.getString("world"); // get the name of the world of the arena
		World w = Bukkit.getWorld(world); // convert it to a Bukkit world
		if (w == null){ // but what if world is kill?
			throw new IllegalArgumentException("World " + world + " cannot be loaded!"); // then round is kill
		}
		for (String k : cs.getConfigurationSection("spawns").getKeys(false)){ // load spawns into round object
			Location l = new Location(w, cs.getDouble("spawns." + k + ".x"),
					cs.getDouble("spawns." + k + ".y"),
					cs.getDouble("spawns." + k + ".z"));
			if (cs.isSet(k + ".pitch"))
				l.setPitch((float)cs.getDouble(cs.getCurrentPath() + ".spawns." + k + ".pitch"));
			if (cs.isSet(k + ".yaw"))
				l.setYaw((float)cs.getDouble(cs.getCurrentPath() + ".spawns." + k + ".yaw"));
			spawns.add(l); // register spawn
		}
		if (cs.getBoolean("boundaries")){ // check if arena has boundaries defined
			minBound = new Location(w, cs.getDouble("minX"), cs.getDouble("minY"), cs.getDouble("minZ"));
			maxBound = new Location(w, cs.getDouble("maxX"), cs.getDouble("maxY"), cs.getDouble("maxZ"));
		}
		else {
			minBound = null;
			maxBound = null;
		}
		this.plugin = plugin; // set globals
		this.arena = arena;
		ConfigManager cm = getConfigManager();
		this.prepareTime = cm.getDefaultPreparationTime();
		this.roundTime = cm.getDefaultPlayingTime();
		this.minPlayers = cm.getMinPlayers();
		this.maxPlayers = cm.getMaxPlayers();
		this.exitLocation = cm.getDefaultExitLocation();
		this.damage = cm.isDamageAllowed();
		this.pvp = cm.isPvPAllowed();
		this.rollback = cm.isRollbackEnabled();
		stage = Stage.WAITING; // default to waiting stage
		Minigame.getMinigameInstance(plugin).getRounds().put(arena, this); // register round with minigame instance
	}

	/**
	 * Gets the name of the minigame plugin associated with this {@link Round}.
	 * @return The name of the minigame plugin associated with this {@link Round}. 
	 * @since 0.1.0
	 */
	public String getPlugin(){
		return plugin;
	}

	/**
	 * Gets the instance of the MGLib API registered by the plugin associated with this {@link Round}.
	 * @return The instance of the MGLib API registered by the plugin associated with this {@link Round}.
	 * @since 0.1.0
	 */
	public Minigame getMinigame(){
		return Minigame.getMinigameInstance(plugin);
	}

	/**
	 * Gets the name of the arena associated with this {@link Round}.
	 * @return The name of the arena associated with this {@link Round}.
	 * @since 0.1.0
	 */
	public String getArena(){
		return arena;
	}

	/**
	 * Gets the current {@link Stage} of this {@link Round}.
	 * @return The current {@link Stage} of this {@link Round}.
	 * @since 0.1.0
	 */
	public Stage getStage(){
		return stage;
	}

	/**
	 * Gets the current time in seconds of this {@link Round}, where 0 represents the first second of it.
	 * @return the current time in seconds of this {@link Round}, where 0 represents the first second of it.
	 * @since 0.1.0
	 */
	public int getTime(){
		return time;
	}

	/**
	 * Gets the time remaining in this round.
	 * @return the time remaining in this round, or -1 if there is no time limit or if the {@link Stage stage} is not
	 * {@link Stage#PLAYING PLAYING} or {@link Stage#PREPARING PREPARING}
	 * @since 0.1.0
	 */
	public int getRemainingTime(){
		switch (this.getStage()){
		case PREPARING:
			if (this.getPreparationTime() > 0)
				return this.getPreparationTime() - this.getTime();
			else
				return -1;
		case PLAYING:
			if (this.getPlayingTime() > 0)
				return this.getPlayingTime() - this.getTime();
			else
				return -1;
		default:
			return -1;
		}
	}

	/**
	 * Gets the round's preparation time.
	 * @return The round's preparation time.
	 * @since 0.1.0
	 */
	public int getPreparationTime(){
		return prepareTime;
	}

	/**
	 * Gets the round's playing time.
	 * @return The round's playing time.
	 * @since 0.1.0
	 */
	public int getPlayingTime(){
		return roundTime;
	}

	/**
	 * Gets the round's timer's task's handle, or -1 if a timer is not started.
	 * @return The round's timer's task's handle, or -1 if a timer is not started.
	 * @since 0.1.0
	 */
	public int getTimerHandle(){
		return timerHandle;
	}

	/**
	 * Sets the associated arena of this {@link Round}.
	 * @param arena The arena to associate with this {@link Round}.
	 * @since 0.1.0
	 */
	public void setArena(String arena){
		this.arena = arena;
	}

	/**
	 * Sets the current stage of this {@link Round}.
	 * @param s The stage to set this {@link Round} to.
	 * @since 0.1.0
	 */
	public void setStage(Stage s){
		stage = s;
	}

	/**
	 * Sets the remaining time of this {@link Round}.
	 * @param t The time to set this {@link Round} to.
	 * @since 0.1.0
	 */
	public void setTime(int t){
		time = t;
	}

	/**
	 * Sets the round's preparation time.
	 * @param t The number of seconds to set the preparation time to. Use -1 for no limit, or 0 for
	 * no preparation phase.
	 * @since 0.1.0
	 */
	public void setPreparationTime(int t){
		prepareTime = t;
	}

	/**
	 * Sets the round's playing time.
	 * @param t The number of seconds to set the preparation time to. Use -1 for no limit.
	 * @since 0.1.0
	 */
	public void setPlayingTime(int t){
		roundTime = t;
	}

	/**
	 * Decrements the time remaining in the round by 1.
	 * <br><br>
	 * Please do not call this method from your plugin unless you understand the implications. Let MGLib handle
	 * the timer.
	 * @since 0.1.0
	 */
	public void tick(){
		time += 1;
	}

	/**
	 * Subtracts <b>t</b> seconds from the remaining time in the round.
	 * @param t The number of seconds to subtract.
	 * @since 0.1.0
	 */
	public void subtractTime(int t){
		time -= t;
	}

	/**
	 * Adds <b>t</b> seconds from the remaining time in the round.
	 * @param t The number of seconds to add.
	 * @since 0.1.0
	 */
	public void addTime(int t){
		time += t;
	}

	/**
	 * Destroys this {@link Round}.
	 * <br><br>
	 * <b>Please do not call this method from your plugin unless you understand the implications.</b>
	 * @since 0.1.0
	 */
	public void destroy(){
		Minigame.getMinigameInstance(plugin).getRounds().remove(this);
	}

	/**
	 * Retrieves a list of {@link MGPlayer MGPlayers} in this round.
	 * @return A list of {@link MGPlayer MGPlayers} in this round.
	 * @since 0.1.0
	 */
	public List<MGPlayer> getPlayerList(){
		return Lists.newArrayList(players.values());
	}

	/**
	 * Retrieves a {@link HashMap} of players in this round.
	 * @return a {@link HashMap} mapping the names of players in the round to their respective {@link MGPlayer} objects.
	 * @since 0.1.0
	 */
	public HashMap<String, MGPlayer> getPlayers(){
		return players;
	}
	
	/**
	 * Retrieves a {@link HashMap} of all players on a given team.
	 * @param team the team to retrieve players from.
	 * @return a {@link HashMap} mapping the names of players on a given team to their respective {@link MGPlayer} objects.
	 * @since 0.3.0
	 */
	public HashMap<String, MGPlayer> getTeam(String team){
		HashMap<String, MGPlayer> t = new HashMap<String, MGPlayer>();
		for (MGPlayer p : getPlayerList()){
			if (p.getTeam().equals(team))
				t.put(p.getName(), p);
		}
		return t;
	}

	/**
	 * Retrieves a list of non-spectating {@link MGPlayer MGPlayers} in this round.
	 * @return a list of non-spectating {@link MGPlayer MGPlayers} in this round.
	 * @since 0.2.0
	 */
	public List<MGPlayer> getAlivePlayerList(){
		List<MGPlayer> list = new ArrayList<MGPlayer>();
		for (MGPlayer p : players.values())
			if (!p.isSpectating())
				list.add(p);
		return list;
	}

	/**
	 * Retrieves a list of spectating {@link MGPlayer MGPlayers} in this {@link Round}.
	 * @return a list of spectating {@link MGPlayer MGPlayers} in this {@link Round}.
	 * @since 0.2.0
	 */
	public List<MGPlayer> getSpectatingPlayerList(){
		List<MGPlayer> list = new ArrayList<MGPlayer>();
		for (MGPlayer p : players.values())
			if (p.isSpectating())
				list.add(p);
		return list;
	}

	/**
	 * Retrieves the number of {@link MGPlayer MGPlayers} in this {@link Round}.
	 * @return the number of {@link MGPlayer MGPlayers} in this {@link Round}.
	 * @since 0.2.0
	 */
	public int getPlayerCount(){
		return players.size();
	}

	/**
	 * Retrieves the number of in-game (non-spectating) {@link MGPlayer MGPlayers} in this {@link Round}.
	 * @return the number of in-game (non-spectating) {@link MGPlayer MGPlayers} in this {@link Round}.
	 * @since 0.2.0
	 */
	public int getAlivePlayerCount(){
		int count = 0;
		for (MGPlayer p : players.values())
			if (!p.isSpectating())
				count += 1;
		return count;
	}

	/**
	 * Retrieves the number of spectating {@link MGPlayer MGPlayers} in this {@link Round}.
	 * @return the number of spectating {@link MGPlayer MGPlayers} in this {@link Round}.
	 * @since 0.2.0
	 */
	public int getSpectatingPlayerCount(){
		int count = 0;
		for (MGPlayer p : players.values())
			if (p.isSpectating())
				count += 1;
		return count;
	}



	/**
	 * Begin the round and start its timer. If the round's current stage is {@link Stage#PREPARING}, it will
	 * be set to {@link Stage#PLAYING} and the timer will be reset when it reaches 0. Otherwise, its stage
	 * will be set to {@link Stage#PREPARING} and it will begins its preparation stage.
	 * <br><br>
	 * After it finishes its preparation, it will begin as it would if this method were called again (don't
	 * actually call it again though, or you'll trigger an exception).
	 * @throws IllegalStateException if the stage is already {@link Stage#PLAYING}.
	 * @since 0.1.0
	 */
	public void start(){
		if (stage == Stage.WAITING){ // make sure the round isn't already started
			final Round r = this;
			if (r.getPreparationTime() > 0){
				r.setTime(0); // reset time
				r.setStage(Stage.PREPARING); // set stage to preparing
				MGUtil.callEvent(new MinigameRoundPrepareEvent(r)); // call an event for anyone who cares
			}
			else {
				r.setTime(0); // reset timer
				r.setStage(Stage.PLAYING);
				MGUtil.callEvent(new MinigameRoundStartEvent(r));
			}
			if (time != -1){ // I'm pretty sure this is wrong, but I'm also pretty tired
				timerHandle = Bukkit.getScheduler().runTaskTimer(Main.plugin, new Runnable(){
					public void run(){
						int oldTime = r.getTime();
						boolean stageChange = false;
						int limit = r.getStage() == Stage.PLAYING ? r.getPlayingTime() : r.getPreparationTime();
						if (r.getTime() >= limit && limit > 0){ // timer reached its limit
							if (r.getStage() == Stage.PREPARING){ // if we're still preparing...
								r.setStage(Stage.PLAYING); // ...set stage to playing
								stageChange = true;
								r.setTime(0); // reset timer
								MGUtil.callEvent(new MinigameRoundStartEvent(r));
							}
							else { // we're playing and the round just ended
								end(true);
								stageChange = true;
							}
						}
						if (!stageChange)
							r.tick();
						//TODO: Allow for a grace period upon player disconnect
						if (r.getMinBound() != null){
							// this whole bit handles keeping player inside the arena
							//TODO: Possibly make an event for when a player wanders out of an arena
							for (MGPlayer p : r.getPlayerList()){
								Player pl = p.getBukkitPlayer();
								Location l = pl.getLocation();
								boolean event = true;
								if (l.getX() < r.getMinBound().getX())
									pl.teleport(new Location(l.getWorld(), r.getMinBound().getX(), l.getY(), l.getZ()), TeleportCause.PLUGIN);
								else if (l.getX() > r.getMaxBound().getX())
									pl.teleport(new Location(l.getWorld(), r.getMaxBound().getX(), l.getY(), l.getZ()), TeleportCause.PLUGIN);
								else if (l.getY() < r.getMinBound().getY())
									pl.teleport(new Location(l.getWorld(), l.getX(), r.getMinBound().getY(), l.getZ()), TeleportCause.PLUGIN);
								else if (l.getY() > r.getMaxBound().getY())
									pl.teleport(new Location(l.getWorld(), l.getX(), r.getMinBound().getY(), l.getZ()), TeleportCause.PLUGIN);
								else if (l.getZ() < r.getMinBound().getZ())
									pl.teleport(new Location(l.getWorld(), l.getX(), l.getY(), r.getMinBound().getZ()), TeleportCause.PLUGIN);
								else if (l.getZ() > r.getMaxBound().getZ())
									pl.teleport(new Location(l.getWorld(), l.getX(), l.getY(), r.getMinBound().getZ()), TeleportCause.PLUGIN);
								else
									event = false;
								if (event)
									MGUtil.callEvent(new PlayerHitArenaBorderEvent(p));
							}
						}
						if (r.getStage() == Stage.PLAYING || r.getStage() == Stage.PREPARING)
							MGUtil.callEvent(new MinigameRoundTickEvent(r, oldTime, stageChange));
					}
				}, 0L, 20L).getTaskId(); // iterates once per second
			}
		}
		else
			throw new IllegalStateException(Bukkit.getPluginManager().getPlugin(plugin) +
					" attempted to start a round which had already been started.");
	}

	/**
	 * Ends the round and resets its timer. The stage will also be set to {@link Stage#WAITING}.
	 * @param timeUp Whether the round was ended due to its timer expiring. This will default to false if omitted.
	 * @throws IllegalStateException if the timer has not been started.
	 * @since 0.1.0
	 */
	public void end(boolean timeUp){
		setTime(-1);
		if (timerHandle != -1)
			Bukkit.getScheduler().cancelTask(timerHandle); // cancel the round's timer task
		stage = Stage.WAITING; // set stage back to waiting
		timerHandle = -1; // reset timer handle since the task no longer exists
		for (MGPlayer mp : getPlayerList()){ // iterate and remove players
			try {
				removePlayer(mp.getName());
			}
			catch (Exception ex){} // I don't care if this happens
		}
		MGUtil.callEvent(new MinigameRoundEndEvent(this, timeUp));
		if (getConfigManager().isRollbackEnabled()) // check if rollbacks are enabled
			getRollbackManager().rollback(getArena()); // roll back arena
	}

	/**
	 * Ends the round and resets its timer. The stage will also be set to {@link Stage#WAITING}.
	 * @throws IllegalStateException if the timer has not been started.
	 * @since 0.1.0
	 */
	public void end(){
		end(false);
	}

	/**
	 * Retrieves the location representing the minimum boundary on all three axes of the arena this round takes place in.
	 * @return the location representing the minimum boundary on all three axes of the arena this round takes place in, or
	 * null if the arena does not have boundaries.
	 * @since 0.1.0
	 */
	public Location getMinBound(){
		return minBound;
	}

	/**
	 * Retrieves the location representing the maximum boundary on all three axes of the arena this round takes place in.
	 * @return the location representing the maximum boundary on all three axes of the arena this round takes place in, or
	 * null if the arena does not have boundaries.
	 * @since 0.1.0
	 */
	public Location getMaxBound(){
		return maxBound;
	}

	/**
	 * Sets the minimum boundary on all three axes of this round object.
	 * @param x The minimum x-value.
	 * @param y The minimum y-value.
	 * @param z The minimum z-value.
	 * @since 0.1.0
	 */
	public void setMinBound(double x, double y, double z){
		this.minBound = new Location(this.minBound.getWorld(), x, y, z);
	}

	/**
	 * Sets the maximum boundary on all three axes of this round object.
	 * @param x The maximum x-value.
	 * @param y The maximum y-value.
	 * @param z The maximum z-value.
	 * @since 0.1.0
	 */
	public void setMaxBound(double x, double y, double z){
		this.minBound = new Location(this.minBound.getWorld(), x, y, z);
	}

	/**
	 * Retrieves a list of possible spawns for this round's arena.
	 * @return a list of possible spawns for this round's arena.
	 * @since 0.1.0
	 */
	public List<Location> getSpawns(){
		return spawns;
	}

	/**
	 * Returns the {@link MGPlayer} in this round associated with the given username.
	 * @param player The username to search for.
	 * @return The {@link MGPlayer} in this round associated with the given username, or <b>null</b> if none is found.
	 * @since 0.1.0
	 */
	public MGPlayer getMGPlayer(String player){
		return players.get(player);
	}

	/**
	 * Retrieves the world of this arena.
	 * @return The name of the world containing this arena.
	 * @since 0.1.0
	 */
	public String getWorld(){
		return world;
	}
	
	/**
	 * Adds a player by the given name to this {@link Round round}.
	 * @param name the player to add to this {@link Round round}.
	 * (will default to random/sequential (depending on configuration) if out of bounds). 
	 * @throws PlayerOfflineException if the player is not online.
	 * @throws PlayerPresentException if the player is already in a round.
	 * @throws RoundFullException if the round is full.
	 * @since 0.1.0
	 */
	public void addPlayer(String name) throws PlayerOfflineException, PlayerPresentException, RoundFullException {
		addPlayer(name, -1);
	}

	/**
	 * Adds a player by the given name to this {@link Round round}.
	 * @param name the player to add to this {@link Round round}.
	 * @param spawn the spawn number to teleport the player to
	 * (will default to random/sequential (depending on configuration) if out of bounds). 
	 * @throws PlayerOfflineException if the player is not online.
	 * @throws PlayerPresentException if the player is already in a round.
	 * @throws RoundFullException if the round is full.
	 * @since 0.3.0
	 */
	@SuppressWarnings("deprecation")
	public void addPlayer(String name, int spawn) throws PlayerOfflineException, PlayerPresentException, RoundFullException {
		final Player p = Bukkit.getPlayer(name);
		if (p == null) // check that the specified player is online
			throw new PlayerOfflineException();
		if (getPlayerCount() >= getMaxPlayers() && getMaxPlayers() > 0)
			throw new RoundFullException();
		if (getStage() == Stage.PREPARING){
			if (!getConfigManager().getAllowJoinRoundWhilePreparing()){
				p.sendMessage(ChatColor.RED + locale.getMessage("no-join-prepare"));
				return;
			}
		}
		else if (getStage() == Stage.PLAYING){
			if (!getConfigManager().getAllowJoinRoundInProgress()){
				p.sendMessage(ChatColor.RED + locale.getMessage("no-join-progress"));
				return;
			}
		}
		MGPlayer mp = Minigame.getMinigameInstance(plugin).getMGPlayer(name);
		if (mp == null){
			try {
				mp = (MGPlayer)getConfigManager().getPlayerClass().getDeclaredConstructors()[0]
						.newInstance(plugin, name, arena);
			}
			catch (InvocationTargetException ex){ // any error thrown from the called constructor
				ex.getTargetException().printStackTrace();
			}
			catch (IllegalArgumentException ex){ // thrown when the overriding constructor doesn't match what's expected
				Main.log.severe("The constructor overriding MGLib's default MGPlayer for plugin " + plugin + " is malformed");
				ex.printStackTrace();
			}
			catch (SecurityException ex){ // I have no idea why this would happen.
				ex.printStackTrace();
			}
			catch (InstantiationException ex){ // if this happens then the overriding plugin seriously screwed something up
				Main.log.severe("The constructor overriding MGLib's default MGPlayer for plugin " + plugin + " is seriously wack. Fix it, developer.");
				ex.printStackTrace();
			}
			catch (IllegalAccessException ex){ // thrown if the called method from the overriding class is not public
				Main.log.severe("The constructor overriding MGLib's default MGPlayer for plugin " + plugin + " is not visible");
				ex.printStackTrace();
			}
		}
		else if (mp.getArena() == null)
			mp.setArena(arena);
		else
			throw new PlayerPresentException();
		ItemStack[] contents = p.getInventory().getContents();
		PlayerInventory pInv = (PlayerInventory)p.getInventory();
		ItemStack helmet = pInv.getHelmet(), chestplate = pInv.getChestplate(), leggings = pInv.getLeggings(), boots = pInv.getBoots();
		try {
			File invDir = new File(Main.plugin.getDataFolder(), "inventories");
			File invF = new File(Main.plugin.getDataFolder() + File.separator +
					"inventories" + File.separator +
					UUIDFetcher.getUUIDOf(p.getName()) + ".dat");
			if (!invF.exists()){
				invDir.mkdirs();
				invF.createNewFile();
			}
			YamlConfiguration invY = new YamlConfiguration();
			invY.load(invF);
			for (int i = 0; i < contents.length; i++)
				invY.set(Integer.toString(i), contents[i]);
			if (helmet != null)
				invY.set("h", helmet);
			if (chestplate != null)
				invY.set("c", chestplate);
			if (leggings != null)
				invY.set("l", leggings);
			if (boots != null)
				invY.set("b", boots);
			invY.save(invF);
		}
		catch (Exception ex){
			ex.printStackTrace();
			p.sendMessage(ChatColor.RED + locale.getMessage("inv-save-fail"));
			return;
		}
		((PlayerInventory)p.getInventory()).clear();
		((PlayerInventory)p.getInventory()).setArmorContents(new ItemStack[]{null, null, null, null});
		p.updateInventory();
		for (PotionEffect pe : p.getActivePotionEffects())
			p.removePotionEffect(pe.getType()); // remove any potion effects before adding the player
		if ((getStage() == Stage.PREPARING || getStage() == Stage.PLAYING) &&
				getConfigManager().getSpectateOnJoin())
			mp.setSpectating(true);
		else
			mp.setSpectating(false);
		mp.setPrevGameMode(p.getGameMode());
		p.setGameMode(getConfigManager().getDefaultGameMode());
		players.put(name, mp); // register player with round object
		Location sp = (spawn >= 0 && spawns.size() > spawn) ? spawns.get(spawn) :
			getConfigManager().isRandomSpawning() ?
					spawns.get(new Random().nextInt(spawns.size())) :
						spawns.get(players.size() % spawns.size());
					p.teleport(sp, TeleportCause.PLUGIN); // teleport the player to it
					MGUtil.callEvent(new PlayerJoinMinigameRoundEvent(this, mp));
					if (getStage() == Stage.WAITING && getPlayerCount() >= getConfigManager().getMinPlayers() && getPlayerCount() > 0)
						start();
	}

	/**
	 * Removes a given player from this {@link Round round} and teleports them to the given location.
	 * @param name The player to remove from this {@link Round round}.
	 * @param location The location to teleport the player to.
	 * @throws PlayerOfflineException if the player is not online.
	 * @throws PlayerNotPresentException if the player are not in this round.
	 * @since 0.1.0
	 */
	public void removePlayer(String name, Location location) throws PlayerOfflineException, PlayerNotPresentException {
		Player p = Bukkit.getPlayer(name);
		MGPlayer mp = players.get(name);
		if (mp == null)
			throw new PlayerNotPresentException();
		if (p != null){
			mp.setArena(null); // they're not in an arena anymore
			mp.setSpectating(false); // make sure they're not dead when they join a new round
			players.remove(name); // remove player from round
			p.setGameMode(mp.getPrevGameMode()); // restore the player's gamemode
			mp.reset(location); // reset the object and send the player to the exit point
		}
		PlayerLeaveMinigameRoundEvent event = new PlayerLeaveMinigameRoundEvent(this, mp);
		MGUtil.callEvent(event);
	}

	/**
	 * Removes a given player from this {@link Round round} and teleports them to the round or plugin's default exit location
	 * (defaults to the main world's spawn point).
	 * @param name The player to remove from this {@link Round round}. 
	 * @throws PlayerNotPresentException if the given player is not in this round.
	 * @throws PlayerOfflineException if the given player is offline.
	 * @since 0.1.0
	 */
	public void removePlayer(String name) throws PlayerOfflineException, PlayerNotPresentException{
		removePlayer(name, getConfigManager().getDefaultExitLocation());
	}

	/**
	 * Retrieves the minimum number of players required to automatically start the round.
	 * @return the minimum number of players required to automatically start the round.
	 * @since 0.2.0
	 */
	public int getMinPlayers(){
		return minPlayers;
	}

	/**
	 * Sets the minimum number of players required to automatically start the round.
	 * @param players the minimum number of players required to automatically start the round.
	 * @since 0.2.0
	 */
	public void setMinPlayers(int players){
		this.minPlayers = players;
	}

	/**
	 * Retrieves the maximum number of players allowed in a round at once.
	 * @return the maximum number of players allowed in a round at once.
	 * @since 0.1.0
	 */
	public int getMaxPlayers(){
		return maxPlayers;
	}

	/**
	 * Sets the maximum number of players allowed in a round at once.
	 * @param players the maximum number of players allowed in a round at once.
	 * @since 0.1.0
	 */
	public void setMaxPlayers(int players){
		this.maxPlayers = players;
	}

	/**
	 * Creates a new LobbySign to be managed
	 * @param location The location to create the sign at.
	 * @param type The type of the sign ("status" or "players")
	 * @param index The number of the sign (applicable only for "players" signs)
	 * @throws ArenaNotExistsException  if the specified arena does not exist.
	 * @throws InvalidLocationException if the specified location does not contain a sign.
	 * @throws IndexOutOfBoundsException if the specified index for a player sign is less than 1. 
	 * @since 0.1.0
	 */
	public void addSign(Location location, LobbyType type, int index)
			throws ArenaNotExistsException, InvalidLocationException, IndexOutOfBoundsException {
		this.getMinigame().getLobbyManager().add(location, this.getArena(), type, index);
	}

	/**
	 * Updates all lobby signs linked to this round's arena.
	 * @since 0.1.0
	 */
	public void updateSigns(){
		this.getMinigame().getLobbyManager().update(arena);
	}

	/**
	 * Retrieves this round's exit location.
	 * @return this round's exit location.
	 * @since 0.1.0
	 */
	public Location getExitLocation(){
		return exitLocation;
	}

	/**
	 * Sets this round's exit location.
	 * @param location the new exit location for this round.
	 * @since 0.1.0
	 */
	public void setExitLocation(Location location){
		this.exitLocation = location;
	}

	/**
	 * Retrieves whether PvP is allowed.
	 * @return whether PvP is allowed.
	 * @since 0.1.0
	 */
	public boolean isPvPAllowed(){
		return pvp;
	}

	/**
	 * Sets whether PvP is allowed.
	 * @param allowed whether PvP is allowed.
	 * @since 0.1.0
	 */
	public void setPvPAllowed(boolean allowed){
		this.pvp = allowed;
	}

	/**
	 * Retrieves whether players in rounds may receive damage. (default: true)
	 * @return whether players in rounds may receive damage.
	 * @since 0.1.0
	 */
	public boolean isDamageAllowed(){
		return damage;
	}

	/**
	 * Sets whether players in rounds may receive damage. (default: false)
	 * @param allowed whether players in rounds may receive damage.
	 * @since 0.1.0 
	 */
	public void setDamageAllowed(boolean allowed){
		this.damage = allowed;
	}

	/**
	 * Retrieves whether rollback is enabled in this round.
	 * @return whether rollback is enabled in this round.
	 * @since 0.2.0
	 */
	public boolean isRollbackEnabled(){
		return rollback;
	}

	/**
	 * Sets whether rollback is enabled by default.
	 * @param enabled whether rollback is enabled by default.
	 * @since 0.2.0
	 */
	public void setRollbackEnabled(boolean enabled){
		this.rollback = enabled;
	}

	/**
	 * Retrieves the {@link ConfigManager} of the plugin owning this round.
	 * @return the {@link ConfigManager} of the plugin owning this round.
	 * @since 0.2.0
	 */
	public ConfigManager getConfigManager(){
		return getMinigame().getConfigManager();
	}

	/**
	 * Retrieves the {@link RollbackManager} of the plugin owning this round.
	 * @return the {@link RollbackManager} of the plugin owning this round.
	 * @since 0.2.0
	 */
	public RollbackManager getRollbackManager(){
		return getMinigame().getRollbackManager();
	}

	/**
	 * Broadcasts a message to all players in this round.
	 * @param message the message to broadcast.
	 * @param broadcastToSpectators whether the message should be broadcast to spectators.
	 * @since 0.2.0
	 */
	public void broadcast(String message, boolean broadcastToSpectators){
		for (MGPlayer p : players.values())
			if (!p.isSpectating() || broadcastToSpectators)
				p.getBukkitPlayer().sendMessage(message);
	}

	/**
	 * Broadcasts a message to all players in this round.
	 * @param message the message to broadcast.
	 * @since 0.2.0
	 */
	public void broadcast(String message){
		broadcast(message, true);
	}

	@Override
	public Object getMetadata(String key){
		return metadata.get(key);
	}

	@Override
	public void setMetadata(String key, Object value){
		metadata.put(key, value);
	}

	@Override
	public void removeMetadata(String key){
		metadata.remove(key);
	}

	@Override
	public boolean hasMetadata(String key){
		return metadata.containsKey(key);
	}

	@Override
	public HashMap<String, Object> getAllMetadata(){
		return metadata;
	}

	public boolean equals(Object p){
		Round r = (Round)p;
		return arena.equals(r.getArena());
	}

	public int hashCode(){
		return 41 * (plugin.hashCode() + arena.hashCode() + 41);
	}
}
