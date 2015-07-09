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

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public class CardComparator implements Comparator<Object> {
	private Collator collator;
	public static final int SORT_COST_SET_NAME = 0;
	public static final int SORT_SET_COST_NAME = 1;
	public static final int SORT_COST_NAME = 2;
	public static final int SORT_SET_NAME = 3;
	public static final int SORT_NAME = 4;

	private int sort;
	
	public CardComparator(int sort, String locale) {
		this.sort = sort;
		this.collator = Collator.getInstance(new Locale(locale));
	}
	
	public int getSort() {
		return sort;
	}
	
	public int compare(Object obj1, Object obj2) {
		if(obj1 instanceof Card && obj2 instanceof Card) {
			Card card1 = (Card)obj1;
			Card card2 = (Card)obj2;
			int result;
			
			switch(sort) {
			case SORT_COST_SET_NAME:
				result = card1.getCost().compareTo(card2.getCost());
				if(result == 0) result = collator.compare(card1.getSet(), card2.getSet());
				if(result == 0) result = collator.compare(card1.getDisplay(), card2.getDisplay());
				return result;
			case SORT_SET_COST_NAME:
				result = collator.compare(card1.getSet(), card2.getSet());
				if(result == 0) result = card1.getCost().compareTo(card2.getCost());
				if(result == 0) result = collator.compare(card1.getDisplay(), card2.getDisplay());
				return result;
			case SORT_SET_NAME:
				result = collator.compare(card1.getSet(), card2.getSet());
				if(result == 0) result = collator.compare(card1.getDisplay(), card2.getDisplay());
				return result;
			case SORT_COST_NAME:
				result = card1.getCost().compareTo(card2.getCost());
				if(result == 0) result = collator.compare(card1.getDisplay(), card2.getDisplay());
				return result;
			case SORT_NAME:
				return collator.compare(card1.getDisplay(), card2.getDisplay());
			}
		}
		
		return 0;
	}
}
