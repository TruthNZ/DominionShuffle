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
import java.util.HashSet;
import java.util.Set;

import nl.spellenclubeindhoven.dominionshuffle.R;

public class Solution {
	private Set<Card> pool = new HashSet<Card>();
	private Set<Card> cards = new HashSet<Card>();
	private Set<Limit> activeLimits = new HashSet<Limit>();
	private boolean selectPlatinumColony;
	private boolean selectShelter;
	
	public Solution() {
	}
	
	public Solution(Solution solution) {
		this.pool.addAll(solution.pool);
		this.cards.addAll(solution.cards);
		this.activeLimits.addAll(solution.activeLimits);
		this.selectPlatinumColony = solution.selectPlatinumColony;
	}
	
	public void resetLimits() {
		activeLimits.clear();
	}
	
	public void add(Collection<Card> cards) {
		pool.addAll(cards);
	}
	
	public void add(Card card) {
		pool.add(card);
	}
	
	public void remove(Collection<Card> cards) {
		pool.removeAll(cards);
	}
	
	public void remove(Card card) {
		pool.remove(card);
	}
	
	public void activate(Limit limit) {
		activeLimits.add(limit);
	}
	
	public boolean isActive(Limit limit) {
		return !limit.hasCondition() || activeLimits.contains(limit);
	}
	
	public Card pick(Card card) throws SolveError {
		if(!pool.contains(card)) {
			throw new IllegalArgumentException("Trying to pick a card that is not in the pool");
		}

		int kingdomCardsPicked = 0;
		for (Card checkCard : cards) {
			if (!checkCard.isNonKingdom()) {
				kingdomCardsPicked++;
			}
		}
		if(kingdomCardsPicked >= 10) {
			throw new SolveError(R.string.solveerror_too_many_cards, "Trying to pick more then 10 cards");
		}
		
		pool.remove(card);
		cards.add(card);
		
		return card;
	}
	
	public Card forcePick(Card card) {
		cards.add(card);
		pool.remove(card);
		
		return card;
	}
	
	public Card pick(Collection<Card> cards) throws SolveError {
		return pick(RandomCardPicker.pickRandom(cards));
	}
	
	public Card forcePick(Collection<Card> cards) {
		return forcePick(RandomCardPicker.pickRandom(cards));
	}
	
	public void validateEnoughCardsToSolve() throws SolveError {
		if(cards.size() + pool.size() < 10) {
			throw new SolveError(R.string.solveerror_not_enough_cards, "Not enough cards to select from");
		}
	}
	
	public Collection<Card> getPool() {
		return pool;
	}
	
	public Collection<Card> getCards() {
		return cards;
	}
		
	public int countKingdomCards() {
		int kingdomCardsPicked = 0;
		for (Card checkCard : cards) {
			if (!checkCard.isNonKingdom()) {
				kingdomCardsPicked++;
			}
		}
		return kingdomCardsPicked;
	}

	public boolean containsCardOrCardFromGroup(GroupOrCard condition) {
		if(condition instanceof Card) {
			return cards.contains(condition);
		}
		else {
			for(Card card : cards) {
				if(condition.getCards().contains(card)) {
					return true;
				}
			}
			return false;
		}
	}

	public void setSelectPlatinumColony(boolean mustSelectPlatinumColony) {
		this.selectPlatinumColony = mustSelectPlatinumColony;
	}

	public boolean isSelectPlatinumColony() {
		return selectPlatinumColony;
	}

	public void setSelectShelter(boolean mustSelectShelter) {
		this.selectShelter = mustSelectShelter;		
	}
	
	public boolean isSelectShelter() {
		return selectShelter;
	}
}
