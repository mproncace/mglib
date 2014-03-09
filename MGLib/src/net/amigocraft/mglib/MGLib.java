package net.amigocraft.mglib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * MGLib's primary (central) class.
 * @author Maxim Roncacé
 * @version 0.1-dev11
 * @since 0.1
 */
public class MGLib extends JavaPlugin {

	/**
	 * A list containing compatible versions of MGLib.
	 * @since 0.1
	 */
	public static List<String> approved = new ArrayList<String>();

	/**
	 * The current (or last, if the current version is a release) development version of MGLib.
	 * @since 0.1
	 */
	public static final int lastDev = 11;

	/**
	 * The current instance of the plugin.
	 * <br><br>
	 * This is for use within the library; please do not modify this in your plugin or everything will break.
	 * @since 0.1
	 */
	public static MGLib plugin;

	/**
	 * MGLib's logger.
	 * <br><br>
	 * This is for use within the library; please do not use this in your plugin or you'll confuse the server owner.
	 * @since 0.1
	 */
	public static Logger log;

	/**
	 * Standard {@link JavaPlugin#onEnable()} override.
	 * @since 0.1
	 */
	public void onEnable(){
		plugin = this;
		log = getLogger();
		initialize();
		saveDefaultConfig();
		// updater
		if (getConfig().getBoolean("enable-updater")){
			new Updater(this, 74979, this.getFile(), Updater.UpdateType.DEFAULT, true);
		}

		// submit metrics
		if (getConfig().getBoolean("enable-metrics")){
			try {
				Metrics metrics = new Metrics(this);
				metrics.start();
			}
			catch (IOException ex){
				log.warning("Failed to enable plugin metrics!");
			}
		}
		if (this.getDescription().getVersion().contains("dev"))
			log.warning("You are running a development build of MGLib. As such, plugins using the library may not " +
					"work correctly. If you're a developer, we strongly recommend building against a alpha/beta/release" +
					"build of the library.");
		log.info(this + " is now ready!");
	}

	/**
	 * Standard {@link JavaPlugin#onDisable()} override.
	 * @since 0.1
	 */
	public void onDisable(){
		plugin = null;
		log.info(this + " has been disabled!");
		log = null;
	}

	private static void initialize(){
		approved.add("0.1");
	}

}
