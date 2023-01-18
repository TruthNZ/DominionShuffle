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

import java.util.LinkedList;
import java.util.List;

/**
 * Class containing result of a shuffle including extra information like Bane card.
 *
 * @author joran
 * @author Truth
 */
public class Result {
    public static final String bane = "bane";
    private Card baneCard;
    private Card obeliskCard;
    private List<Card> traitCards;
    private List<Card> cards = new LinkedList<>();

    public Result() {
    }

    public Result(Result result) {
        this.baneCard = result.baneCard;
        this.obeliskCard = result.obeliskCard;
        this.traitCards = result.traitCards;
        this.cards = new LinkedList<>(result.cards);
    }

    public void addCard(Card card) {
        this.cards.add(card);
    }

    public Card getBaneCard() {
        return baneCard;
    }

    public Card getObeliskCard() {
        return obeliskCard;
    }

    public List<Card> getTraitCards() {
        return traitCards;
    }

    public void setBaneCard(Card baneCard) {
        this.baneCard = baneCard;
    }

    public void setObeliskCard(Card obeliskCard) {
        this.obeliskCard = obeliskCard;
    }

    public void addTraitCard(Card traitCard) { this.traitCards.add(traitCard); }

    public void setTraitCards(List<Card> traitCards) { this.traitCards = traitCards; }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("[");
        for (final Card card : cards) {
            stringBuilder.append(card.getName());
            stringBuilder.append(", ");
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
