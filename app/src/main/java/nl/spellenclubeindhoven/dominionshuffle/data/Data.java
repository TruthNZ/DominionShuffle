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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Data {
	private HashMap<String, Card> cardsMap = new HashMap<String, Card>();
	private HashMap<String, Card> nonKingdomCardsMap = new HashMap<String, Card>();
	private HashMap<String, Group> groupsMap = new HashMap<String, Group>();
	private Collection<Card> cards;
	private Collection<Group> groups;
	private Collection<Card> nonKingdomCards;
	private List<GroupOrCard> all = new LinkedList<GroupOrCard>();
	
	public Data(Collection<Card> cards, Collection<Group> groups, Collection<Card> nonKingdomCards) {
		this.cards = Collections.unmodifiableCollection(cards);
		this.groups = Collections.unmodifiableCollection(groups);
		this.nonKingdomCards = Collections.unmodifiableCollection(nonKingdomCards);
		initializeMappings();
	}
	
	private void initializeMappings() {
		for(Card card : cards) cardsMap.put(card.getName(), card);
		for(Group group : groups) groupsMap.put(group.getName(), group);
		for(Card card : nonKingdomCards) nonKingdomCardsMap.put(card.getName(), card);
		all.addAll(groups);
		all.addAll(cards);
	}

	public Collection<Card> getCards() {
		return cards;
	}
	
	public Collection<Card> getNonKingdomCards() {
		return nonKingdomCards;
	}
		
	public Card getCard(String name) {
		Card card = cardsMap.get(name);
		if(card == null) card = nonKingdomCardsMap.get(name);
		return card;
	}
	
	public Collection<Group> getGroups() {
		return groups;
	}
	
	public Group getGroup(String name) {
		return groupsMap.get(name);
	}
	
	public Collection<GroupOrCard> getAll() {
		return all;
	}
	
	public GroupOrCard getGroupOrCard(String name) {
		GroupOrCard groupOrCard = getGroup(name);
		if(groupOrCard != null) return groupOrCard;
		groupOrCard = getCard(name);
		if(groupOrCard != null) return groupOrCard;
		return null;
	}
	
	public Collection<Group> getGroupsWithCard(Card card) {
		List<Group> groups = new LinkedList<Group>();
		
		for(Group group : this.groups) {
			if(group.getCards().contains(card)) {
				groups.add(group);
			}
		}
		
		return groups;
	}
		
	public static Data read(String json) throws JSONException {
		JSONObject jsonData = new JSONObject(json);
		
		LinkedList<Card> cards = new LinkedList<Card>();
		HashMap<String, Card> cardsMap = new HashMap<String, Card>();
		JSONArray jsonCards = jsonData.getJSONArray("cards");
		for(int i = 0; i < jsonCards.length(); i++) {
			JSONObject jsonCard = jsonCards.getJSONObject(i);
			JSONArray jsonTypes = jsonCard.getJSONArray("type");
			List<String> types = new ArrayList<>(jsonTypes.length());
			for (int j = 0; j < jsonTypes.length(); j++) {
				types.add(jsonTypes.getString(j));
			}
			boolean basicOrNonSupply = jsonCard.optBoolean("basic", false) || jsonCard.optBoolean("nonSupply", false);
			Card card = new Card(
				jsonCard.getString("card"),
				jsonCard.optString("display"),
				jsonCard.getString("set"),
				jsonCard.getString("cost"),
				types,
				basicOrNonSupply);
			cards.add(card);
			cardsMap.put(card.getName(), card);
		}
		
		LinkedList<Group> groups = new LinkedList<Group>();
		JSONArray jsonGroups = jsonData.getJSONArray("groups");
		for(int i = 0; i < jsonGroups.length(); i++) {
			JSONObject jsonGroup = jsonGroups.getJSONObject(i);
			JSONArray jsonGroupCards = jsonGroup.getJSONArray("cards");
			Set<Card> groupCards = new HashSet<>();
			for(int j = 0; j < jsonGroupCards.length(); j++) {
				Card groupCard = cardsMap.get(jsonGroupCards.getString(j));
				if(groupCard == null) {
					throw new RuntimeException("Can not find card '" + jsonGroupCards.getString(j) + "' (specified in group '" + jsonGroup.getString("group")+ "')");
				}
				groupCards.add(cardsMap.get(jsonGroupCards.getString(j)));
			}
			Group group = new Group(
					jsonGroup.getString("group"),
					jsonGroup.optString("display"),
					jsonGroup.getString("description"),
					groupCards
					);
			groups.add(group);			
		}

		// Load Non Kingdom Cards after groups, so we can import an entire group if needed (ie. Events)
		LinkedList<Card> nonKingdomCards = new LinkedList<Card>();
		JSONArray jsonNonKingdomCards = jsonData.getJSONArray("non_kingdom_cards");
		for(int i = 0; i < jsonNonKingdomCards.length(); i++) {
			JSONObject jsonCard = jsonNonKingdomCards.getJSONObject(i);
			JSONArray jsonTypes = jsonCard.getJSONArray("type");
			List<String> types = new ArrayList<>(jsonTypes.length());
			for (int j = 0; j < jsonTypes.length(); j++) {
				types.add(jsonTypes.getString(j));
			}
			Card card = new Card(
					jsonCard.getString("card"),
					jsonCard.optString("display"),
					jsonCard.getString("set"),
					jsonCard.getString("cost"),
					types,
					true);
			nonKingdomCards.add(card);
		}

		return new Data(cards, groups, nonKingdomCards);
	}
}
