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
package net.amigocraft.mglib.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.google.common.collect.Maps;
import net.amigocraft.mglib.Main;
import net.amigocraft.mglib.UUIDFetcher;
import net.amigocraft.mglib.api.Color;
import net.amigocraft.mglib.api.Locale;
import net.amigocraft.mglib.api.Localizable;
import net.amigocraft.mglib.api.MGPlayer;
import net.amigocraft.mglib.util.NmsUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.json.simple.parser.ParseException;

public class BukkitLocalizable implements Localizable {

	private static final String FALLBACK_LOCALE = "enus";

	private final BukkitLocale parent;
	private final BukkitLocalizable subParent;
	private final String key;
	private final Object[] replacements;
	private final Map<String, String> locales;

	BukkitLocalizable(BukkitLocale parent, String key, Object... replacements) {
		this.parent = parent;
		this.subParent = null;
		this.key = key;
		this.replacements = replacements;
		this.locales = Maps.newHashMap();
	}

	private BukkitLocalizable(BukkitLocalizable parent, String key, Object... replacements) {
		this.parent = null;
		this.subParent = parent;
		this.key = key;
		this.replacements = replacements;
		this.locales = null;
	}

	/**
	 * Adds a translation to this Localizable.
	 * @param locale The locale code of the new translation
	 * @param message The translation itself
	 */
	void addLocale(String locale, String message) {
		locale = locale.replace("_", "").replace("-", ""); // normalize locale code
		getLocales().put(locale, message);
	}

	/**
	 * Creates a child with the given replacement sequences.
	 * @param replacements The replacement sequences to assign to the child
	 * @return The new child
	 */
	BukkitLocalizable createChild(Object... replacements) {
		return new BukkitLocalizable(this, this.getKey(), replacements);
	}

	Map<String, String> getLocales() {
		return this.subParent == null ? locales : this.subParent.getLocales();
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public Locale getParent() {
		return this.subParent == null ? parent : subParent.getParent();
	}

	@Override
	public Object[] getReplacementSequences() {
		return this.replacements;
	}

	@Override
	public String localize() {
		return this.localize(Main.getServerLocale());
	}

	@Override
	public String localize(String locale) {
		locale = locale.replace("_", "").replace("-", "").toLowerCase(); // normalize locale code
		if (getLocales().containsKey(locale)) {
			String message = getLocales().get(locale);
			for (int i = 0; i < getReplacementSequences().length; i++) {
				String repl = getReplacementSequences()[i] instanceof Localizable
				              ? ((Localizable)getReplacementSequences()[i]).localize(locale)
				              : getReplacementSequences()[i].toString();
				message = message.replaceAll("%" + (i + 1), repl);
			}
			return message;
		}
		else if (locale.equals(FALLBACK_LOCALE)) {
			return this.getKey();
		}
		else if (locale.equals(Main.getServerLocale())) {
			return this.localize(FALLBACK_LOCALE);
		}
		else {
			return this.localize(Main.getServerLocale());
		}
	}

	@Override
	public String localizeFor(String playerName) throws IllegalArgumentException {
		try {
			return this.localizeFor(UUIDFetcher.getUUIDOf(playerName));
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		catch (ParseException ex) {
			ex.printStackTrace();
		}
		return this.getKey();
	}

	@Override
	public String localizeFor(UUID playerUuid) throws IllegalArgumentException {
		Player player = Bukkit.getPlayer(playerUuid);
		String locale = null;
		if (player != null) {
			locale = NmsUtil.getLocale(player);
		}
		return this.localize(locale != null ? locale : Main.getServerLocale());
	}

	@Override
	public String localizeFor(MGPlayer player) throws IllegalArgumentException {
		return this.localizeFor(player.getName());
	}

	@Override
	public void sendTo(String playerName) throws IllegalArgumentException {
		try {
			this.sendTo(UUIDFetcher.getUUIDOf(playerName));
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		catch (ParseException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void sendTo(String playerName, Color color) throws IllegalArgumentException {
		try {
			this.sendTo(UUIDFetcher.getUUIDOf(playerName), color);
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		catch (ParseException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void sendTo(UUID playerUuid) throws IllegalArgumentException {
		this.sendTo(playerUuid, Color.RESET);
	}

	@Override
	public void sendTo(UUID playerUuid, Color color) throws IllegalArgumentException {
		Player player = Bukkit.getPlayer(playerUuid);
		if (player != null) {
			player.sendMessage(ChatColor.valueOf(color.name()) + this.localizeFor(playerUuid));
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean equals(Object otherLocalizable) {
		if (!(otherLocalizable instanceof BukkitLocalizable)) {
			return false;
		}
		BukkitLocalizable bl = (BukkitLocalizable)otherLocalizable;
		return this.getParent().equals(bl.getParent())
				&& this.getKey().equals(bl.getKey());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getParent(), this.getKey());
	}
}
