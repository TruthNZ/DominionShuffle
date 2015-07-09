/*
 * Copyright (C) 2011 Tim Kramp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nl.spellenclubeindhoven.dominionshuffle.data;

import java.util.Collection;
import java.util.Collections;

public class Card extends GroupOrCard {
	private String set;
	private String cost;
	private String type;
	private boolean nonKingdom;
	
	public Card(String name, String display, String set, String cost, String type) {
		super(name, display);
		this.set = set;
		this.cost = cost;
		this.type = type;
		this.nonKingdom = false;
	}

	public Card(String name, String display, String set, String cost, String type, boolean nonKingdom) {
		super(name, display);
		this.set = set;
		this.cost = cost;
		this.type = type;
		this.nonKingdom = nonKingdom;
	}
		
	public String getSet() {
		return set;
	}
	
	public String getCost() {
		return cost;
	}

	public String getType() {
		return type;
	}

	public boolean isNonKingdom() {
		return nonKingdom;
	}

	@Override
	public String toString() {
		return "Card " + getDisplay();
	}

	@Override
	public Collection<Card> getCards() {
		return Collections.singletonList(this);
	}

	@Override
	public boolean isCard() {
		return true;
	}

	@Override
	public boolean isGroup() {
		return false;
	}	
}
