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

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import nl.spellenclubeindhoven.dominionshuffle.Constants;
import nl.spellenclubeindhoven.dominionshuffle.R;
import nl.spellenclubeindhoven.dominionshuffle.SettingsActivity;

public class CardSelector {

    // Final Class variables
    final private Context context;
    final private Set<Group> includedGroups = new HashSet<>();
    final private Set<Group> excludedGroups = new HashSet<>();
    final private Set<Card> includedCards = new HashSet<>();
    final private Set<Card> excludedCards = new HashSet<>();
    final private Set<Card> requiredCards = new HashSet<>();
    final private SortedMap<Group, Limit> allLimits = new TreeMap<>((g1, g2) -> {
        final int g1Size = g1.getCards().size();
        final int g2Size = g2.getCards().size();
        if (g1Size != g2Size) {
            return g1Size - g2Size;
        }
        return g1.getName().compareTo(g2.getName());
    });
    final private Random random = new Random();

    private int version;

    // Dynamic Class variables
    private Data data;

    public CardSelector(Context context) {
        this.context = context;
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

    public Result generate(Data data) throws SolveError {
        this.data = data;

        Set<Card> availableCards = new HashSet<>();

        // Add all included groups
        for (Group group : includedGroups) {
            availableCards.addAll(group.getCards());
        }

        // Excluded groups take priority over included groups
        for (Group group : excludedGroups) {
            availableCards.removeAll(group.getCards());
        }

        // Specifically excluded Cards have higher priority over groups
        availableCards.removeAll(excludedCards);

        // Specifically included Cards are highest priority
        availableCards.addAll(includedCards);

        Result result = new Result();

        // First add required Cards
        for (Card card : requiredCards) {
            result.getCards().add(card);
        }

        // Generate a Solution
        result = generateSolution(result, availableCards, allLimits);

        // We have a valid solution in terms of draw cards
        // Now check for and apply rules for specific cards
        //addPotionIfNeeded(result);
        drawColonyPlatinum(result);
        drawShelter(result);
        drawObelisk(result);
        drawTrait(result);
        drawAlly(result);

        return result;
    }

    private Result generateSolution(final Result existingResult, final Set<Card> existingAvailableCards, final SortedMap<Group, Limit> existingLimits) throws SolveError {
        Log.d(CardSelector.class.getSimpleName(), "Current Card List: " + existingResult);

        // Check to ensure most recent card added hasn't taken us over any maximum limits
        for (final Limit limit : existingLimits.values()) {
            if (!limit.maximumSatisified(existingResult.getCards())) {
                throw new SolveError(R.string.solveerror_unsatisfied_rule, "");
            }
        }

        // Check if we have completed the Solution
        if (countDrawCards(existingResult.getCards(), existingResult) == getCardsToDraw()) {
            // We've drawn the correct number of cards, now check our limits
            if (checkLimitsSatisfied(existingResult, existingLimits)) {
                // First add the Bane card if needed.
                addBaneIfNeeded(existingResult, existingAvailableCards);
                // Also add an extra pile for Ferryman if needed.
                addFerrymanExtraIfNeeded(existingResult, existingAvailableCards);
                // Add a Prophecy if needed
                addProphecyIfNeeded(existingResult);
                // This is a valid Solution!
                return existingResult;
            } else {
                // Not a valid Solution.
                throw new SolveError(R.string.solveerror_unsatisfied_rule, "");
            }
        }

        // Loop trying cards, and seeing if having picked that card we can complete a Solution
        final Set<Card> availableCards = new HashSet<>(existingAvailableCards);
        final SortedMap<Group, Limit> limits = new TreeMap<>(existingLimits);
        while (true) { // Loop until we either return, or throw an exception

            // Pick the next card
            final Card pickedCard = pickCard(existingResult, availableCards, limits);

            // Remove from the availableList
            availableCards.remove(pickedCard);

            Result result = new Result(existingResult);
            result.addCard(pickedCard);

            checkAllies(pickedCard, limits);

            // Try generating more of the Solution with this card
            try {
                return generateSolution(result, availableCards, limits);
            } catch (SolveError solveError) {
                // Nope, continue loop to try next card
            }
        }
    }

    /**
     * Check we satisfy the rules for Allies and Liaisons, using Limits to do so
     * @param pickedCard
     * @param limits
     */
    private void checkAllies(Card pickedCard, SortedMap<Group, Limit> limits) {
        // Ensure we have a least one Ally card if we have an Liaison card
        if (pickedCard.getTypes().contains(Constants.TYPE_LIAISON)) {
            final Group alliesGroup = data.getGroup(Constants.GROUP_ALLIES);
            final Limit alliesLimit;
            if (!limits.containsKey(alliesGroup)) {
                alliesLimit = new Limit(alliesGroup);
                limits.put(alliesGroup, alliesLimit);
            } else {
                alliesLimit = limits.get(alliesGroup);
            }
            if (alliesLimit.getMinimum() < 1) {
                alliesLimit.setMinimum(1);
            }
        }

        // Ensure we have an Liaison card if we have a Ally card
        if (pickedCard.getTypes().contains(Constants.TYPE_ALLY)) {
            final Group liaisonsGroup = data.getGroup(Constants.GROUP_LIAISONS);
            final Limit liaisonsLimit;
            if (!limits.containsKey(liaisonsGroup)) {
                liaisonsLimit = new Limit(liaisonsGroup);
                limits.put(liaisonsGroup, liaisonsLimit);
            } else {
                liaisonsLimit = limits.get(liaisonsGroup);
            }
            if (liaisonsLimit.getMinimum() < 1) {
                liaisonsLimit.setMinimum(1);
            }
        }
    }


    private boolean checkLimitsSatisfied(final Result result, final SortedMap<Group, Limit> limits) {
        for (final Limit limit : limits.values()) {
            if (!limit.isSatisfied(result.getCards())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pick a Card.<br/>
     * If we have any minimumLimit, pick from the smallest (first in list) to satisfy it.
     */
    private Card pickCard(final Result existingResult, final Set<Card> availableCards, final SortedMap<Group, Limit> limits) throws SolveError {
        Set<Card> pickSource = availableCards;

        // Check to see if there's still a minimumLimit to satisfy
        for (final Limit minimumLimit : limits.values()) {
            if (!minimumLimit.minimumSatisfied(existingResult.getCards())) {
                pickSource = new HashSet<>(availableCards);
                pickSource.retainAll(minimumLimit.getGroup().getCards());
                break;
            }
        }

        if (pickSource.isEmpty()) {
            throw new SolveError(R.string.solveerror_rules_to_strict, "Can not select cards using given rules, try relaxing them");
        }

        // Randomise selection from the set - Iterating through isn't very efficient, but without writing a combined List Set class ourself is the easiest choice.
        final int cardToFetch = this.random.nextInt(pickSource.size());
        final Iterator<Card> iterator = pickSource.iterator();
        for (int i = 0; i < cardToFetch; i++) {
            iterator.next();
        }
        final Card pickedCard = iterator.next();

        return pickedCard;
    }

    /**
     * Counts the cards, excluding those that don't count against the draw limit.<br/>
     * E.g. Events, Basic Cards, Non-Supply Cards
     */
    private int countDrawCards(final Collection<Card> cards, final Result result) {
        int count = 0;
        for (final Card card : cards) {
            if (!card.isBasicOrNonSupply() && !card.getTypes().contains(Constants.TYPE_EVENT) && !card.getTypes().contains(Constants.TYPE_LANDMARK) && !card.getTypes().contains(Constants.TYPE_PROJECT) && !card.getTypes().contains(Constants.TYPE_WAY) && !card.getTypes().contains(Constants.TYPE_ALLY) && !card.getTypes().contains(Constants.TYPE_TRAIT) && !(result.getBaneCard() == card)) {
                count++;
            }
        }
        return count;
    }

    /**
     * If either Colony or Platinum have been added, add the other (if not excluded).<br/>
     * If they're not both excluded
     * Checks the number of prosperity cards to the total drawn, and randomly draws the Colony and Platinum based on the ratio
     */
    private void drawColonyPlatinum(final Result result) {
        final Card colony = data.getCard(Constants.CARD_COLONY);
        final Card platinum = data.getCard(Constants.CARD_PLATINUM);
        final boolean colonyExcluded = this.excludedCards.contains(colony);
        final boolean platinumExcluded = this.excludedCards.contains(platinum);

        // If they have both been explicitly excluded, do nothing
        if (colonyExcluded && platinumExcluded) {
            return;
        }

        final boolean colonyAlreadyAdded = result.getCards().contains(colony);
        final boolean platinumAlreadyAdded = result.getCards().contains(platinum);

        if (colonyAlreadyAdded) {
            if (platinumAlreadyAdded) {
                // We already have both, do nothing
                return;
            }
            if (!platinumExcluded) {
                // If we have Colony, and Platinum isn't excluded, add it.
                result.addCard(platinum);
            }
            return;
        }
        if (platinumAlreadyAdded) {
            if (!colonyExcluded) {
                // If we have Platinum, and Colony isn't excluded, add it.
                result.addCard(colony);
            }
            return;
        }

        // We have neither Colony nor Platinum, do calculation to determine if we should add them

        // Count the number of prosperity cards
        Group prosperityFirstCards = data.getGroup(Constants.GROUP_PROSPERITY_FIRST);
        Group prosperitySecondCards = data.getGroup(Constants.GROUP_PROSPERITY_FIRST);
        int count = 0;
        for (Card card : result.getCards()) {
            if (prosperityFirstCards.contains(card) || prosperitySecondCards.contains(card)) {
                count++;
            }
        }

        // Randomly determine if prosperity cards should be added
        if (random.nextInt(getCardsToDraw()) < count) {
            // Add the Colony and Platinum, where they're not excluded.
            if (!colonyExcluded) {
                result.addCard(colony);
            }
            if (!platinumExcluded) {
                result.addCard(platinum);
            }
        }
    }

    /**
     * Check the current results to determine if we need to add a pile for Ferryman
     */
    private void addFerrymanExtraIfNeeded(final Result result, final Set<Card> availableCards) throws SolveError {
        // Firstly have we already done the Ferryman extra card
        if (result.getFerrymanExtraCard() != null) {
            return;
        }

        // Check if Ferryman is in the selection
        boolean ferrymanInSelection = false;
        Card ferryman = data.getCard(Constants.CARD_FERRYMAN);
        for (Card card : result.getCards()) {
            if (card == ferryman) {
                ferrymanInSelection = true;
                break;
            }
        }
        if (!ferrymanInSelection) {
            // Ferryman isn't in selection, don't do anything.
            return;
        }

        final Group cost3 = data.getGroup(Constants.GROUP_COST_3);
        final Group cost4 = data.getGroup(Constants.GROUP_COST_4);
        final Set<Card> cost3Or4Cards = new HashSet<>(cost3.getCards());
        cost3Or4Cards.addAll(cost4.getCards());
        cost3Or4Cards.retainAll(availableCards);

        // Filter out Basic and Non-Supply Cards
        // While it means looping through again before we find our random card, we have to so we know how many to count from.
        for (Iterator<Card> i = cost3Or4Cards.iterator(); i.hasNext(); ) {
            if (i.next().isBasicOrNonSupply()) {
                i.remove();
            }
        }

        if (cost3Or4Cards.isEmpty()) {
            throw new SolveError(R.string.solveerror_not_enough_cards, "Not enough cost 3 or 4 cards for selecting an extra pile for Ferryman");
        }

        // Randomise selection from the set
        final int cardToFetch = this.random.nextInt(cost3Or4Cards.size());
        final Iterator<Card> iterator = cost3Or4Cards.iterator();
        for (int i = 0; i < cardToFetch; i++) {
            iterator.next();
        }
        final Card ferrymanExtraCard = iterator.next();

        result.setFerrymanExtraCard(ferrymanExtraCard);
        result.addCard(ferrymanExtraCard);

        availableCards.remove(ferrymanExtraCard);
    }

    private void addProphecyIfNeeded(final Result result) throws SolveError {
        boolean omenExists = false;
        // Have to use this old style search to support older android
        for (Card card: result.getCards()) {
            if (card.getTypes().contains(Constants.TYPE_OMEN)) {
                omenExists = true;
                break;
            }
        }

        if (omenExists) {
            Set<Card> prophecies = new HashSet<>(data.getGroup(Constants.GROUP_PROPHECIES).getCards());
            prophecies.removeAll(excludedCards);
            if (prophecies.isEmpty()) {
                throw new SolveError(R.string.solveerror_no_prophecies_found, "No prophecies found to draw from");
            }

            // Randomise selection from the set - Iterating through isn't very efficient, but without writing a combined List Set class ourself is the easiest choice.
            final int cardToFetch = this.random.nextInt(prophecies.size());
            final Iterator<Card> iterator = prophecies.iterator();
            for (int i = 0; i < cardToFetch; i++) {
                iterator.next();
            }
            final Card pickedProphecy = iterator.next();

            result.addCard(pickedProphecy);
        }
    }

    /**
     * Check the current results to determine if need to pick a Bane
     */
    private void addBaneIfNeeded(final Result result, final Set<Card> availableCards) throws SolveError {
        // Firstly have we already done the bane card
        if (result.getBaneCard() != null) {
            return;
        }

        // Check if Young Witch is in the selection
        boolean youngWitchInSelection = false;
        Card youngWitch = data.getCard(Constants.CARD_YOUNG_WITCH);
        for (Card card : result.getCards()) {
            if (card == youngWitch) {
                youngWitchInSelection = true;
                break;
            }
        }
        if (!youngWitchInSelection) {
            // Young Witch isn't in selection, don't do anything.
            return;
        }

        final Group cost2 = data.getGroup(Constants.GROUP_COST_2);
        final Group cost3 = data.getGroup(Constants.GROUP_COST_3);
        final Set<Card> cost2Or3Cards = new HashSet<>(cost2.getCards());
        cost2Or3Cards.addAll(cost3.getCards());
        cost2Or3Cards.retainAll(availableCards);

        // Filter out Basic and Non-Supply Cards
        // While it means looping through again before we find our random card, we have to so we know how many to count from.
        for (Iterator<Card> i = cost2Or3Cards.iterator(); i.hasNext(); ) {
            if (i.next().isBasicOrNonSupply()) {
                i.remove();
            }
        }

        if (cost2Or3Cards.isEmpty()) {
            throw new SolveError(R.string.solveerror_not_enough_cards, "Not enough cost 2 or 3 cards for selecting a bane card");
        }

        // Randomise selection from the set
        final int cardToFetch = this.random.nextInt(cost2Or3Cards.size());
        final Iterator<Card> iterator = cost2Or3Cards.iterator();
        for (int i = 0; i < cardToFetch; i++) {
            iterator.next();
        }
        final Card baneCard = iterator.next();

        result.setBaneCard(baneCard);
        result.addCard(baneCard);

        availableCards.remove(baneCard);
    }

    /**
     * Shelter is actually a type of Basic cards, but we treat and display it as a single card for convenience.
     */
    private void drawShelter(Result result) {
        final Card shelter = data.getCard(Constants.CARD_SHELTER);

        // Check if it was already a required card
        if (result.getCards().contains(shelter)) {
            return;
        }

        // Count the number of dark ages cards
        Group darkAgesCards = data.getGroup(Constants.GROUP_DARK_AGES);
        int count = 0;
        for (Card card : result.getCards()) {
            if (darkAgesCards.contains(card)) {
                count++;
            }
        }

        // Randomly determine if shelter cards should be added
        if (random.nextInt(getCardsToDraw()) < count) {
            result.addCard(shelter);
        }
    }

    /**
     * Check if we need to identify a card for the Obelisk Landmark
     */
    private void drawObelisk(Result result) {
        final Card obelisk = data.getCard(Constants.CARD_OBELISK);

        // If we don't have obelisk, just return
        if (!result.getCards().contains(obelisk)) {
            return;
        }

        // Create a list of only action cards from the result
        List<Card> actionCards = new ArrayList<>();
        for (Card card : result.getCards()) {
            if (card.getTypes().contains(Constants.TYPE_ACTION)) {
                actionCards.add(card);
            }
        }

        if (actionCards.isEmpty()) {
            // Wierd, but technically possible - means they will have the obelisk card in play, but it has no effect on the game (under rules at the time of this).
            return;
        }

        final int cardToFetch = this.random.nextInt(actionCards.size());
        result.setObeliskCard(actionCards.get(cardToFetch));
    }

    /**
     * Check if we need to identify a card for a Trait to apply to
     */
    private void drawTrait(Result result) {
        // If we have no traits, just return
        int numTraits = 0;
        for (Card card: result.getCards())
        {
            if(card.getTypes().contains(Constants.TYPE_TRAIT))
            {
                numTraits += 1;
            }
        }
        
        if (numTraits == 0) {
            return;
        }

        // Create a list of only action and treasure cards from the result
        List<Card> actionTreasureCards = new ArrayList<>();
        for (Card card : result.getCards()) {
            if (card.getTypes().contains(Constants.TYPE_ACTION) || card.getTypes().contains(Constants.TYPE_TREASURE)) {
                actionTreasureCards.add(card);
            }
        }

        if (actionTreasureCards.isEmpty()) {
            // Wierd, but technically possible - means they will have the trait card in play, but it has no effect on the game (under rules at the time of this).
            return;
        }
        if (actionTreasureCards.size() < numTraits)
        {
            // If not enough cards to apply traits to, just select as many as possible and the rest will have no effect
            numTraits = actionTreasureCards.size();
        }
        List<Card> traitCards = new ArrayList<>();
        int cardToFetch;
        for (int i=0; i<numTraits; i++)
        {
            cardToFetch = this.random.nextInt(actionTreasureCards.size());
            traitCards.add(actionTreasureCards.get(cardToFetch));
            actionTreasureCards.remove(cardToFetch);
        }
        result.setTraitCards(traitCards);
    }

    private void drawAlly(Result result) {
        //@TODO
    }

    private int getCardsToDraw() {
        String cardsToDraw = PreferenceManager.getDefaultSharedPreferences(this.context).getString(SettingsActivity.CARDS_TO_DRAW, null);
        if (cardsToDraw == null) {
            return Constants.DEFAULT_CARDS_TO_DRAW;
        }
        return Integer.parseInt(cardsToDraw);
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
        allLimits.remove(group);
    }

    public Limit getLimit(Group group) {
        if (!allLimits.containsKey(group)) {
            allLimits.put(group, new Limit(group));
        }

        return allLimits.get(group);
    }

    public boolean hasLimit(Group group) {
        return allLimits.containsKey(group);
    }

    public void clear() {
        includedGroups.clear();
        excludedGroups.clear();
        includedCards.clear();
        excludedCards.clear();
        requiredCards.clear();
        allLimits.clear();
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
        if (hasRequiredCard(card)) {
            removeRequiredCard(card);
            removeExcludedCard(card);
        } else if (hasExlcudedCard(card)) {
            removeExcludedCard(card);
            addRequiredCard(card);
        } else {
            addExcludedCard(card);
            removeRequiredCard(card);
        }
    }

    public int getLimitMinimum(Group group) {
        if (hasLimit(group)) {
            return getLimit(group).getMinimum();
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
        if (hasLimit(group)) {
            Limit limit = getLimit(group);
            return limit.getCondition();
        } else {
            return null;
        }
    }

    public void setLimitMinimum(Group group, int which) {
        if (which == 0 && !hasLimit(group)) {
            return;
        }
        Limit limit = getLimit(group);
        limit.setMinimum(which);
        removeLimitIfUseless(group);
    }

    // Will map the value 0 to MAX_VALUE
    public void setLimitMaximum(Group group, int which) {
        if (which == 0 && !hasLimit(group)) {
            return;
        }
        if (which == 0) {
            which = Integer.MAX_VALUE;
        }
        Limit limit = getLimit(group);
        limit.setMaximum(which);
        removeLimitIfUseless(group);
    }

    public void removeLimitIfUseless(Group group) {
        if (hasLimit(group)) {
            Limit limit = getLimit(group);
            if (limit.getMinimum() == 0 && limit.getMaximum() == Integer.MAX_VALUE && limit.getCondition() == null) {
                removeLimit(group);
            }
        }
    }

    public void setCondition(Group group, GroupOrCard groupOrCard) {
        if (groupOrCard == null || groupOrCard instanceof Nothing && !hasLimit(group)) {
            return;
        }
        Limit limit = getLimit(group);
        if (groupOrCard instanceof Nothing) {
            limit.setCondition(null);
        } else {
            limit.setCondition(groupOrCard);
        }
        removeLimitIfUseless(group);
    }

    public void fromJson(String json, Data data) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray array;

        includedCards.clear();
        array = root.getJSONArray("includedCards");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Card card = data.getCard(array.getString(i));
                if (card != null) {
                    includedCards.add(card);
                }
            }
        }

        excludedCards.clear();
        array = root.getJSONArray("excludedCards");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Card card = data.getCard(array.getString(i));
                if (card != null) {
                    excludedCards.add(card);
                }
            }
        }

        requiredCards.clear();
        array = root.getJSONArray("requiredCards");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Card card = data.getCard(array.getString(i));
                if (card != null) {
                    requiredCards.add(card);
                }
            }
        }

        includedGroups.clear();
        array = root.getJSONArray("includedGroups");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Group group = data.getGroup(array.getString(i));
                if (group != null) {
                    includedGroups.add(group);
                }
            }
        }

        excludedGroups.clear();
        array = root.getJSONArray("excludedGroups");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Group group = data.getGroup(array.getString(i));
                if (group != null) {
                    excludedGroups.add(group);
                }
            }
        }

        allLimits.clear();
        array = root.getJSONArray("rules");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonLimit = array.getJSONObject(i);
                if (jsonLimit == null) {
                    continue;
                }
                Group group = data.getGroup(jsonLimit.getString("group"));
                if (group == null) {
                    continue;
                }
                Limit limit = getLimit(group);
                if (jsonLimit.has("min")) {
                    limit.setMinimum(jsonLimit.getInt("min"));
                }
                if (jsonLimit.has("max")) {
                    limit.setMaximum(jsonLimit.getInt("max"));
                }
                if (jsonLimit.has("condition")) {
                    limit.setCondition(data.getGroupOrCard(jsonLimit.getString("condition")));
                }
                allLimits.put(group, limit);
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
        for (Group group : allLimits.keySet()) {
            Limit limit = allLimits.get(group);
            JSONObject jsonLimit = new JSONObject();
            jsonLimit.put("group", group.getName());
            jsonLimit.put("min", limit.getMinimum());
            jsonLimit.put("max", limit.getMaximum());
            if (limit.getCondition() != null) {
                jsonLimit.put("condition", limit.getCondition().getName());
            }
            jsonRules.put(jsonLimit);
        }
        root.put("rules", jsonRules);

        return root.toString();
    }

    public void removeExcludedCard(Collection<Card> cards) {
        for (Card card : cards) {
            removeExcludedCard(card);
        }
    }

    public void removeRequiredCard(Collection<Card> cards) {
        for (Card card : cards) {
            removeRequiredCard(card);
        }
    }

    public void addExcludedCard(Collection<Card> cards) {
        for (Card card : cards) {
            addExcludedCard(card);
        }
    }

    public void addRequiredCard(Collection<Card> cards) {
        for (Card card : cards) {
            addRequiredCard(card);
        }
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
