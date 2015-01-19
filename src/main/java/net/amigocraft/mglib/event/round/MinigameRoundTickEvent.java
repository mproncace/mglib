/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2015 Maxim Roncacé
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.amigocraft.mglib.event.round;

import net.amigocraft.mglib.api.Round;
import net.amigocraft.mglib.api.Stage;

/**
 * Called once per second or 20 ticks, when a round "ticks"
 */
public class MinigameRoundTickEvent extends MGRoundEvent {

	private int oldTime;
	private boolean stageChange;

	/**
	 * You probably shouldn't call this from your plugin. Let MGLib handle that.
	 *
	 * @param round       the round which has ticked
	 * @param oldTime     the round time before the tick.
	 * @param stageChange whether the tick resulted in a stage change (e.g. from {@link Stage#PREPARING} to {@link
	 *                    Stage#PLAYING}.
	 * @since 0.1.0
	 */
	public MinigameRoundTickEvent(Round round, int oldTime, boolean stageChange) {
		super(round);
		this.oldTime = oldTime;
		this.stageChange = stageChange;
	}

	/**
	 * Returns the round time before the tick.
	 *
	 * @return the time remaining in the round before the tick.
	 * @since 0.1.0
	 */
	public int getTimeBefore() {
		return oldTime;
	}

	/**
	 * Returns whether the tick resulted in a stage change for the round (e.g. from "PREPARING" to "PLAYING").
	 *
	 * @return whether the tick resulted in a stage change for the round.
	 * @since 0.1.0
	 */
	public boolean isStageChange() {
		return stageChange;
	}

}
