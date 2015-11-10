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

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Card extends GroupOrCard {
    private final String set;
    private final String cost;
    private final List<String> types;
    /**
     * Basic and Non-Supply Cards do not count against those drawn,
     * and in addition will not be shown in the list unless a special condition requires them to be
     * (e.g. Platinum/Colony/Potion)
     **/
    private final boolean basicOrNonSupply;

    public Card(String name, String set, String cost, List<String> types, boolean basicOrNonSupply) {
        super(name);
        this.set = set;
        this.cost = cost;
        this.types = types;
        this.basicOrNonSupply = basicOrNonSupply;
    }

    public String getSet() {
        return set;
    }

    public String getCost() {
        return cost;
    }

    public List<String> getTypes() {
        return types;
    }

    public boolean isBasicOrNonSupply() {
        return basicOrNonSupply;
    }

    @Override
    public String toString() {
        return "Card " + getName();
    }

    @Override
    public Set<Card> getCards() {
        return Collections.singleton(this);
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
