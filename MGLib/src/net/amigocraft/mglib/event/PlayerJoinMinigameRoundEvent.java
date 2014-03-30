package net.amigocraft.mglib.event;

import net.amigocraft.mglib.api.MGPlayer;
import net.amigocraft.mglib.api.Round;

/**
 * Thrown when a {@link MGPlayer player} joins an {@link Round MGLib round}.
 * @since 0.1
 */
public class PlayerJoinMinigameRoundEvent extends MGPlayerEvent {

	protected Round round;
	
	/**
	 * Creates a new instance of this {@link Event event}.
	 * @param round
	 * @param player
	 */
	public PlayerJoinMinigameRoundEvent(Round round, MGPlayer player){
		super(player);
		this.round = round;
	}
	
	/**
	 * Returns the {@link Round round} involved in this {@link PlayerJoinMinigameRoundEvent event}.
	 * @return the {@link Round round} involved in this {@link PlayerJoinMinigameRoundEvent event}.
	 */
	public Round getRound(){
		return round;
	}
	
}