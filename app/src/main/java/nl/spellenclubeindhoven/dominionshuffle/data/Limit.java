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

    public int getMinimum() {
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

    public GroupOrCard getCondition() {
        return condition;
    }

    public void setCondition(GroupOrCard condition) {
        this.condition = condition;
    }

    public boolean hasCondition() {
        return condition != null;
    }

    /**
     * Count the number of cards in the given collection that this rule
     * applies to.
     *
     * @param cards the cards to countKingdomCards
     * @return the number of cards
     */
    public int count(Collection<Card> cards) {
        int count = 0;
        for (Card card : cards) {
            if (group.getCards().contains(card)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks to see if this Limit applies to the set of cards.<br/>
     * If this Limit is not conditional, it applies to all sets and will always return true.
     */
    public boolean appliesTo(Collection<Card> cards) {
        if (condition == null) {
            return true;
        }
        if (condition.isCard()) {
            // This is a card, check to see if it is in the provided set
            if (cards.contains(condition)) {
                return true;
            }
        } else if (condition.isGroup()) {
            // Technically we don't need the if check, we should only be a group or a card.
            for (Card card : condition.getCards()) {
                if (cards.contains(card)) {
                    // As soon as we find any card in the set, this limit applies
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks to see if this limit is satisfied by the provided collection of cards.
     */
    public boolean isSatisfied(Collection<Card> cards) {
        if (this.appliesTo(cards)) {
            int count = count(cards);
            if (minimum > count || count > maximum) {
                return false;
            }
        }
        // If we don't apply, we're satisfied anyway
        return true;
    }

    /**
     * Checks to see if this limit's minimum is satisfied by the provided collection of cards.<br/>
     * If this limit has no minimum it is always satisfied.
     */
    public boolean minimumSatisfied(Collection<Card> cards) {
        if (minimum > 0 && this.appliesTo(cards)) {
            int count = count(cards);
            if (minimum > count) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks to see if this limit's maximum is satisfied by the provided collection of cards.<br/>
     * If this limit has no maximum (represented by Integer.MAX) it is always satisfied.
     */
    public boolean maximumSatisified(Collection<Card> cards) {
        if (minimum < Integer.MAX_VALUE && this.appliesTo(cards)) {
            if (maximum < count(cards)) {
                return false;
            }
        }
        return true;
    }
}
