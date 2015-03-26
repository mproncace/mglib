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
package net.amigocraft.mglib.api;

import java.util.UUID;

/**
 * Represents a message which may be retrieved in multiple locales.
 *
 * @since 0.5.0
 */
public interface Localizable {

	/**
	 * Gets the key associated with this {@link Localizable}.
	 *
	 * @return The key associated with this {@link Localizable}
	 * @since 0.5.0
	 */
	String getKey();

	/**
	 * Gets the parent {@link Locale} of this message.
	 *
	 * @return the parent {@link Locale} of this message
	 * @since 0.5.0
	 */
	Locale getParent();

	/**
	 * Returns an array of objects which will be used to replace wildcard
	 * sequences, in respective order.
	 * @return an array of objects which will be used to replace wildcard
	 * sequences, in respective order
	 * @since 0.5.0
	 */
	Object[] getReplacementSequences();

	/**
	 * Attempts to localize this message based on the server's defined locale.
	 *
	 * @return The message localized as requested if possible; otherwise the
	 *         message localized in the default locale if possible; otherwise
	 *         the raw localization key
	 * @since 0.5.0
	 */
	String localize();

	/**
	 * Attempts to localize this message in the given locale.
	 *
	 * @param locale The name of the locale to localize in. This should follow
	 *               the ISO 639-1 and ISO 3166-1 standards, respectively (e.g.
	 *               en_US or enUS).
	 * @return The message localized as requested if possible; otherwise the
	 *         message localized in the default locale if possible; otherwise
	 *         the raw localization key
	 * @since 0.5.0
	 */
	String localize(String locale);

	/**
	 * Attempts to localize this message for the player with the given username.
	 *
	 * @param playerName The username of the player to localize this message
	 *                   for.
	 * @return The message localized as requested if possible; otherwise the
	 *         message localized in the default locale if possible; otherwise
	 *         the raw localization key
	 * @throws IllegalArgumentException If a player with the given username
	 *                                  cannot be found
	 * @since 0.5.0
	 */
	String localizeFor(String playerName) throws IllegalArgumentException;

	/**
	 * Attempts to localize this message for the player with the given UUID.
	 *
	 * @param playerUuid The UUID of the player to localize this message for.
	 * @return The message localized as requested if possible; otherwise the
	 *         message localized in the default locale if possible; otherwise
	 *         the raw localization key
	 * @throws IllegalArgumentException If a player with the given UUID cannot
	 *                                  be found
	 * @since 0.5.0
	 */
	String localizeFor(UUID playerUuid) throws IllegalArgumentException;

	/**
	 * Attempts to localize this message for the given {@link MGPlayer}.
	 *
	 * @param player The {@link MGPlayer} to localize this message for
	 * @return The message localized as requested if possible; otherwise the
	 *         message localized in the default locale if possible; otherwise
	 *         the raw localization key
	 * @throws IllegalArgumentException If a player with the given username
	 *                                  cannot be found
	 * @since 0.5.0
	 */
	String localizeFor(MGPlayer player) throws IllegalArgumentException;

	/**
	 * Attempts to localize this message for the player with the given username
	 * and send it to them.
	 *
	 * @param playerName The username of the player to send the localized
	 *                   message to
	 * @throws IllegalArgumentException If a player with the given username
	 *                                  cannot be found
	 * @since 0.5.0
	 */
	void sendTo(String playerName) throws IllegalArgumentException;

	/**
	 * Attempts to localize this message for the player with the given username
	 * and send it to them prefixed by the specified chat color.
	 *
	 * @param playerName The username of the player to send the localized
	 *                   message to
	 * @param color The color to prefix the localized string with
	 * @throws IllegalArgumentException If a player with the given username
	 *                                  cannot be found
	 * @since 0.5.0
	 */
	void sendTo(String playerName, Color color) throws IllegalArgumentException;

	/**
	 * Attempts to localize this message for the player with the given username
	 * and send it to them.
	 *
	 * @param playerUuid The username of the player to send the localized
	 *                   message to
	 * @throws IllegalArgumentException If a player with the given username
	 *                                  cannot be found
	 * @since 0.5.0
	 */
	void sendTo(UUID playerUuid) throws IllegalArgumentException;

	/**
	 * Attempts to localize this message for the player with the given UUID and
	 * send it to them prefixed by the specified chat color.
	 *
	 * @param playerUuid The UUID of the player to send the localized message to
	 * @param color The color to prefix the localized string with
	 * @throws IllegalArgumentException If a player with the given username
	 *                                  cannot be found
	 * @since 0.5.0
	 */
	void sendTo(UUID playerUuid, Color color) throws IllegalArgumentException;
}
