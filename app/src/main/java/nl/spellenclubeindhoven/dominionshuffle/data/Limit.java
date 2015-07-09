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

import nl.spellenclubeindhoven.dominionshuffle.R;

public class Limit {
	private Group group;
	private int minimum = 0;
	private int maximum = Integer.MAX_VALUE;
	private GroupOrCard condition;

	public Limit(Limit original) {
		this.group = original.group;
		this.minimum = original.minimum;
		this.maximum = original.maximum;
		this.condition = original.condition;
	}
	
	public Limit(Group group) {
		this.group = group;
	}
	
	public int getMinimum(Solution solution) {
		if(solution.isActive(this)) {
			return minimum;
		}
		else {
			return 0;
		}
	}
		
	public int getLimitMinimum() {
		return minimum;
	}
	
	public void setMinimum(int minimum) {
		this.minimum = minimum;
	}
	
	public int getMaximum() {
		return maximum;
	}
	
	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}

	public Group getGroup() {
		return group;
	}
	
	public void setCondition(GroupOrCard condition) {
		this.condition = condition;
	}

	public GroupOrCard getCondition() {
		return condition;
	}
	
	public boolean hasCondition() {
		return condition != null;
	}
		
	/**
	 * Count the number of cards in the given collection that this rule
	 * applies to.
	 * @param cards the cards to countKingdomCards
	 * @return the number of cards
	 */
	public int count(Collection<Card> cards) {
		int count = 0;
		for(Card card : cards) {
			if(group.getCards().contains(card)) {
				count++;
			}
		}
		return count;
	}
	
	public void satisfy(Solution solution, Collection<Card> cards) throws SolveError {
		int count = count(cards);
		if((solution.isActive(this) && minimum > count) || count > maximum) {
			throw new SolveError(R.string.solveerror_unsatisfied_rule, "Rule is not satisfied for selected cards");
		}
	}
}
