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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import net.amigocraft.mglib.api.Locale;
import net.amigocraft.mglib.api.Localizable;

public final class CombinedLocalizable extends BukkitLocalizable {

	LinkedList<Localizable> elements = Lists.newLinkedList();
	LinkedList<CharSequence> separators = Lists.newLinkedList();

	CombinedLocalizable(Localizable[] localizables, CharSequence[] separators) {
		this.elements.addAll(Arrays.asList(localizables));
		this.separators.addAll(Arrays.asList(separators));
	}

	@Override
	public String getKey() {
		StringBuilder key = new StringBuilder();
		for (Localizable e : elements) {
			key.append(e.getKey()).append('+');
		}
		return key.toString();
	}

	@Override
	public Locale getParent() {
		return elements.get(0).getParent();
	}

	@Override
	public Object[] getReplacementSequences() {
		List<Object> replacements = Lists.newLinkedList();
		for (Localizable e : elements) {
			replacements.addAll(Arrays.asList(e.getReplacementSequences()));
		}
		return replacements.toArray();
	}

	@Override
	public Localizable concat(Localizable localizable, CharSequence separator) {
		List<Localizable> elClone = new LinkedList<Localizable>(elements);
		List<CharSequence> sepClone = new LinkedList<CharSequence>(separators);
		if (localizable instanceof CombinedLocalizable) {
			elClone.addAll(((CombinedLocalizable)localizable).elements);
			sepClone.addAll(((CombinedLocalizable)localizable).separators);
		}
		else {
			elClone.add(localizable);
		}
		return new CombinedLocalizable((Localizable[])elClone.toArray(), (CharSequence[])sepClone.toArray());
	}

	@Override
	public Localizable concat(Localizable localizable) {
		return this.concat(localizable, " ");
	}

	@Override
	public String localize(String locale) {
		StringBuilder sb = new StringBuilder();
		for (Localizable l : elements) {
			sb.append(l.localize(locale)).append(' ');
		}
		sb.substring(0, sb.length() - 1);
		return sb.toString();
	}
}
