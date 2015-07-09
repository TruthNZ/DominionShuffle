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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nl.spellenclubeindhoven.dominionshuffle.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CardSelector {
	private final static int cardsToDraw = 10;
	private int solveAttempts = 100;
	private Set<Group> includedGroups = new HashSet<Group>();
	private Set<Group> excludedGroups = new HashSet<Group>();
	private Set<Card> includedCards = new HashSet<Card>();
	private Set<Card> excludedCards = new HashSet<Card>();
	private Set<Card> requiredCards = new HashSet<Card>();
	private Map<Group, Limit> limits = new HashMap<Group, Limit>();
	private Random random = new Random();

	public CardSelector() {
	}

	public Result generate(Data data) throws SolveError {
		Solution baseSolution = new Solution();

		Card platinum = data.getCard("Platinum");
		Card colony = data.getCard("Colony");
		Card shelter = data.getCard("Shelter");

		if(requiredCards.contains(platinum) || requiredCards.contains(colony)) {
			baseSolution.setSelectPlatinumColony(true);
		}
		
		if(requiredCards.contains(shelter)) {
			baseSolution.setSelectShelter(true);
		}
		
		for (Group group : includedGroups) {
			baseSolution.add(group.getCards());
		}

		for (Group group : excludedGroups) {
			baseSolution.remove(group.getCards());
		}

		for (Card card : excludedCards) {
			// We ignore exluded colony or platinum cards
			baseSolution.remove(card);
		}

		for (Card card : includedCards) {
			baseSolution.add(card);
		}

		for (Card card : requiredCards) {
			// shelter, colony and platinum can only be required (or excluded) so only here they need
			// to be filtered out.
			if(card != colony && card != platinum && card != shelter) {
				baseSolution.add(card);
				baseSolution.pick(card);
			}
		}
		
		// If these go wrong the first time, we don't need to
		// do multiple search attempts
		ensureMaximumConstraints(baseSolution);
		selectBestMinimumLimit(baseSolution);

		for (int i = 0; i < solveAttempts; i++) {
			Solution solution = new Solution(baseSolution);

			try {
				while (solution.countKingdomCards() < cardsToDraw) {
					ensureMaximumConstraints(solution);
					Limit selectedLimit = selectBestMinimumLimit(solution);

					if (selectedLimit == null) {
						solution.pick(solution.getPool());
					} else {
						Set<Card> intersection = new HashSet<Card>(selectedLimit
								.getGroup().getCards());
						intersection.retainAll(solution.getPool());
						solution.pick(intersection);
					}
					
					activateLimits(solution, limits);
				}

				// Check that all constraints satisfy
				for (Limit limit : limits.values())
					limit.satisfy(solution, solution.getCards());

				Result result = new Result();
				handleAlchemy(data, solution);
				handleProsperity(data, solution);
				handleCornucopia(data, solution, result);
				handleDarkAges(data, solution);
				result.setCards(solution.getCards());
				
				return result;
			} catch (SolveError e) {
				// Ignore, let's try it again
			}
		}

		throw new SolveError(
				R.string.solveerror_rules_to_strict, "Can not select cards using given rules, try relaxing them");
	}

	private void handleAlchemy(Data data, Solution solution) {
		Group potionCards = data.getGroup("Potion");
		for (Card card : solution.getCards()) {
			if (potionCards.contains(card)) {
				// Force potions to be added
				solution.forcePick(data.getCard("Potion"));
				return;
			}
		}
	}

	private void handleProsperity(Data data, Solution solution) {
		if(solution.isSelectPlatinumColony()) {
			// Force the cards to be added
			solution.forcePick(data.getCard("Colony"));
			solution.forcePick(data.getCard("Platinum"));
		}
		else {
			// Count the number of prosperity cards
			Group prosperityCards = data.getGroup("Prosperity");
			int count = 0;
			for(Card card : solution.getCards()) {
				if(prosperityCards.contains(card)) count++;
			}
			
			// Randomly determine if prosperity cards should be added
			if(random.nextInt(cardsToDraw) < count) {
				solution.forcePick(data.getCard("Colony"));
				solution.forcePick(data.getCard("Platinum"));
			}
		}
	}
	
	private void handleCornucopia(Data data, Solution solution, Result result) throws SolveError {
		// Is Young Witch selected, no? Don't do anything
		if(!solution.getCards().contains(data.getCard("Young Witch"))) return;
		
		Set<Card> cost2Or3Cards = new HashSet<Card>();
		Group cost2 = data.getGroup("Cost 2");
		Group cost3 = data.getGroup("Cost 3");
		for(Card card : solution.getPool()) {
			if(cost2.contains(card) || cost3.contains(card)) {
				cost2Or3Cards.add(card);
			}
		}
		
		if(cost2Or3Cards.isEmpty()) {
			throw new SolveError(R.string.solveerror_not_enough_cards, "Not enough cost 2 or 3 cards for selecting a bane card");
		}
		
		Card baneCard = solution.forcePick(cost2Or3Cards);
		result.setBaneCard(baneCard);
	}
	
	private void handleDarkAges(Data data, Solution solution) {
		if(solution.isSelectShelter()) {
			// Force the cards to be added
			solution.forcePick(data.getCard("Shelter"));
		}
		else {
			// Count the number of dark ages cards
			Group darkAgesCards = data.getGroup("Dark Ages");
			int count = 0;
			for(Card card : solution.getCards()) {
				if(darkAgesCards.contains(card)) count++;
			}
			
			// Randomly determine if the shelter cards should be used
			if(random.nextInt(cardsToDraw) < count) {
				solution.forcePick(data.getCard("Shelter"));
			}
		}

		Group looterCards = data.getGroup("Looter");
		for (Card card : solution.getCards()) {
			if (looterCards.contains(card)) {
				solution.forcePick(data.getCard("Ruins"));
				// Break early - if we don't we get a
				// ConcurrentModificationException, because we modified
				// solution.
				break;
			}
		}

		Group getSpoilsCards = data.getGroup("Gain Spoils");
		for (Card card : solution.getCards()) {
			if (getSpoilsCards.contains(card)) {
				solution.forcePick(data.getCard("Spoils"));
				// Break early - if we don't we get a
				// ConcurrentModificationException, because we modified
				// solution.
				break;
			}
		}

		Card hermit = data.getCard("Hermit");
		for (Card card : solution.getCards()) {
			if (hermit.equals(card)) {
				solution.forcePick(data.getCard("Madman"));
				// Break early - if we don't we get a
				// ConcurrentModificationException, because we modified
				// solution.
				break;
			}
		}

		Card urchin = data.getCard("Urchin");
		for (Card card : solution.getCards()) {
			if (urchin.equals(card)) {
				solution.forcePick(data.getCard("Mercenary"));
				// Break early - if we don't we get a
				// ConcurrentModificationException, because we modified
				// solution.
				break;
			}
		}
	}

	private void activateLimits(Solution solution, Map<Group, Limit> limits) {
		for(Limit limit : limits.values()) {
			if(!limit.hasCondition()) continue;
			if(solution.isActive(limit)) continue;
			
			if(solution.containsCardOrCardFromGroup(limit.getCondition())) {
				solution.activate(limit);
			}
		}
	}

	private Limit selectBestMinimumLimit(Solution solution) throws SolveError {
		// Select a minimum rule that has the least number of cards
		// in the group that is still selectable
		Collection<Limit> minimumLimits = new LinkedList<Limit>();
		int minimumScore = Integer.MAX_VALUE;
		for (Limit limit : limits.values()) {
			if (limit.getMinimum(solution) <= 0)
				continue; // check that this is a minimum rule
			if (!solution.isActive(limit))
				continue; // Skip inactive mimimum rules

			int count = limit.count(solution.getCards());
			if (count >= limit.getMinimum(solution))
				continue; // rule is already satisfied

			count += limit.count(solution.getPool());

			int score = count - limit.getMinimum(solution);
			if (score < minimumScore) {
				minimumLimits.clear();
				minimumLimits.add(limit);
				minimumScore = score;
			} else if (score == minimumScore) {
				minimumLimits.add(limit);
			}
		}

		if (minimumScore < 0) {
			throw new SolveError(R.string.solveerror_overconstrainted_mimumums, "Not all minimum rules can be satisfied");
		}

		if (minimumLimits.isEmpty())
			return null;

		return RandomCardPicker.pickRandom(minimumLimits);
	}

	private void ensureMaximumConstraints(Solution solution) throws SolveError {
		for (Limit limit : limits.values()) {
			int count = limit.count(solution.getCards());
			if (count == limit.getMaximum()) {
				solution.remove(limit.getGroup().getCards());
			}
		}

		solution.validateEnoughCardsToSolve();
	}

	public void addIncludedGroup(Group group) {
		includedGroups.add(group);
	}

	public void removeIncludedGroup(Group group) {
		includedGroups.remove(group);
	}

	public boolean hasIncludedGroup(Group group) {
		return includedGroups.contains(group);
	}

	public void addExcludedGroup(Group group) {
		excludedGroups.add(group);
	}

	public void removeExcludedGroup(Group group) {
		excludedGroups.remove(group);
	}

	public boolean hasExcludedGroup(Group group) {
		return excludedGroups.contains(group);
	}

	public void addIncludedCard(Card card) {
		includedCards.add(card);
	}

	public void removeIncludedCard(Card card) {
		includedCards.remove(card);
	}

	public boolean hasIncludedCard(Card card) {
		return includedCards.contains(card);
	}

	public void addExcludedCard(Card card) {
		excludedCards.add(card);
	}

	public void removeExcludedCard(Card cards) {
		excludedCards.remove(cards);
	}

	public boolean hasExlcudedCard(Card card) {
		return excludedCards.contains(card);
	}

	public void addRequiredCard(Card card) {
		requiredCards.add(card);
	}

	public void removeRequiredCard(Card card) {
		requiredCards.remove(card);
	}

	public boolean hasRequiredCard(Card card) {
		return requiredCards.contains(card);
	}

	public void removeLimit(Group group) {
		limits.remove(group);
	}

	public Limit getLimit(Group group) {
		if (!limits.containsKey(group)) {
			limits.put(group, new Limit(group));
		}

		return limits.get(group);
	}

	public boolean hasLimit(Group group) {
		return limits.containsKey(group);
	}
	
	public void setSolveAttempts(int solveAttempts) {
		this.solveAttempts = solveAttempts;
	}

	public void clear() {
		includedGroups.clear();
		excludedGroups.clear();
		includedCards.clear();
		excludedCards.clear();
		requiredCards.clear();
		limits.clear();
	}

	public void cycleIncludeExclude(Object cardOrGroup) {
		if (cardOrGroup instanceof Card) {
			cycleIncludeExclude((Card) cardOrGroup);
		} else if (cardOrGroup instanceof Group) {
			cycleIncludeExclude((Group) cardOrGroup);
		}
	}

	private void cycleIncludeExclude(Card card) {
		if (hasIncludedCard(card)) {
			removeIncludedCard(card);
			addExcludedCard(card);
		} else if (hasExlcudedCard(card)) {
			removeExcludedCard(card);
		} else {
			addIncludedCard(card);
		}
	}

	private void cycleIncludeExclude(Group group) {
		if (hasIncludedGroup(group)) {
			removeIncludedGroup(group);
			addExcludedGroup(group);
		} else if (hasExcludedGroup(group)) {
			removeExcludedGroup(group);
		} else {
			addIncludedGroup(group);
		}
	}
	
	public void cycleRequireExclude(Card card) {
		if(hasRequiredCard(card)) {
			removeRequiredCard(card);
			removeExcludedCard(card);
		}
		else if(hasExlcudedCard(card)) {
			removeExcludedCard(card);
			addRequiredCard(card);
		}
		else {
			addExcludedCard(card);
			removeRequiredCard(card);
		}
	}

	public int getLimitMinimum(Group group) {
		if (hasLimit(group)) {
			return getLimit(group).getLimitMinimum();
		} else {
			return 0;
		}
	}
	
	// Will map MAX_VALUE back to 0
	public int getLimitMaximum(Group group) {
		if (hasLimit(group)) {
			Limit limit = getLimit(group);
			if (limit.getMaximum() == Integer.MAX_VALUE) {
				return 0;
			} else {
				return limit.getMaximum();
			}
		} else {
			return 0;
		}
	}

	public GroupOrCard getCondition(Group group) {
		if(hasLimit(group)) {
			Limit limit = getLimit(group);
			return limit.getCondition();
		}
		else {
			return null;
		}
	}
	
	public void setLimitMinimum(Group group, int which) {
		if (which == 0 && !hasLimit(group))
			return;
		Limit limit = getLimit(group);
		limit.setMinimum(which);
		removeLimitIfUseless(group);
	}

	// Will map the value 0 to MAX_VALUE
	public void setLimitMaximum(Group group, int which) {
		if (which == 0 && !hasLimit(group))
			return;
		if (which == 0)
			which = Integer.MAX_VALUE;
		Limit limit = getLimit(group);
		limit.setMaximum(which);
		removeLimitIfUseless(group);
	}

	public void removeLimitIfUseless(Group group) {
		if (hasLimit(group)) {
			Limit limit = getLimit(group);
			if (limit.getLimitMinimum() == 0 && limit.getMaximum() == Integer.MAX_VALUE && limit.getCondition() == null) {
				removeLimit(group);
			}
		}
	}

	public void setCondition(Group group, GroupOrCard groupOrCard) {
		if(groupOrCard == null || groupOrCard instanceof Nothing && !hasLimit(group)) return;
		Limit limit = getLimit(group);
		if(groupOrCard instanceof Nothing) {
			limit.setCondition(null);
		}
		else {
			limit.setCondition(groupOrCard);
		}
		removeLimitIfUseless(group);
	}
	
	private static JSONArray createJSONArray(Collection<?> list) {
		LinkedList<String> result = new LinkedList<String>();
		for (Object item : list) {
			if (item instanceof GroupOrCard) {
				result.add(((GroupOrCard) item).getName());
			} else {
				result.add(item.toString());
			}
		}
		return new JSONArray(result);
	}

	public void fromJson(String json, Data data) throws JSONException {
		JSONObject root = new JSONObject(json);
		JSONArray array;

		includedCards.clear();
		array = root.getJSONArray("includedCards");
		if (array != null) {
			for (int i = 0; i < array.length(); i++) {
				Card card = data.getCard(array.getString(i));
				if (card != null)
					includedCards.add(card);
			}
		}

		excludedCards.clear();
		array = root.getJSONArray("excludedCards");
		if (array != null) {
			for (int i = 0; i < array.length(); i++) {
				Card card = data.getCard(array.getString(i));
				if (card != null)
					excludedCards.add(card);
			}
		}

		requiredCards.clear();
		array = root.getJSONArray("requiredCards");
		if (array != null) {
			for (int i = 0; i < array.length(); i++) {
				Card card = data.getCard(array.getString(i));
				if (card != null)
					requiredCards.add(card);
			}
		}

		includedGroups.clear();
		array = root.getJSONArray("includedGroups");
		if (array != null) {
			for (int i = 0; i < array.length(); i++) {
				Group group = data.getGroup(array.getString(i));
				if (group != null)
					includedGroups.add(group);
			}
		}

		excludedGroups.clear();
		array = root.getJSONArray("excludedGroups");
		if (array != null) {
			for (int i = 0; i < array.length(); i++) {
				Group group = data.getGroup(array.getString(i));
				if (group != null)
					excludedGroups.add(group);
			}
		}

		limits.clear();
		array = root.getJSONArray("rules");
		if (array != null) {
			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonLimit = array.getJSONObject(i);
				if (jsonLimit == null)
					continue;
				Group group = data.getGroup(jsonLimit.getString("group"));
				if (group == null)
					continue;
				Limit limit = getLimit(group);
				if (jsonLimit.has("min"))
					limit.setMinimum(jsonLimit.getInt("min"));
				if (jsonLimit.has("max"))
					limit.setMaximum(jsonLimit.getInt("max"));
				if (jsonLimit.has("condition"))
					limit.setCondition(data.getGroupOrCard(jsonLimit.getString("condition")));
				limits.put(group, limit);
			}
		}
	}

	public String toJson() throws JSONException {
		JSONObject root = new JSONObject();

		root.put("includedCards", createJSONArray(includedCards));
		root.put("excludedCards", createJSONArray(excludedCards));
		root.put("requiredCards", createJSONArray(requiredCards));
		root.put("includedGroups", createJSONArray(includedGroups));
		root.put("excludedGroups", createJSONArray(excludedGroups));

		JSONArray jsonRules = new JSONArray();
		for (Group group : limits.keySet()) {
			Limit limit = limits.get(group);
			JSONObject jsonLimit = new JSONObject();
			jsonLimit.put("group", group.getName());
			jsonLimit.put("min", limit.getLimitMinimum());
			jsonLimit.put("max", limit.getMaximum());
			if(limit.getCondition() != null) {
				jsonLimit.put("condition", limit.getCondition().getName());
			}
			jsonRules.put(jsonLimit);
		}
		root.put("rules", jsonRules);
		
		return root.toString();
	}

	public void removeExcludedCard(Collection<Card> cards) {
		for(Card card : cards) {
			removeExcludedCard(card);
		}
	}

	public void removeRequiredCard(Collection<Card> cards) {
		for(Card card : cards) {
			removeRequiredCard(card);
		}
	}

	public void addExcludedCard(Collection<Card> cards) {
		for(Card card : cards) {
			addExcludedCard(card);
		}
	}

	public void addRequiredCard(Collection<Card> cards) {
		for(Card card : cards) {
			addRequiredCard(card);
		}
	}
}
