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

package nl.spellenclubeindhoven.dominionshuffle;

import java.util.LinkedList;

import nl.spellenclubeindhoven.dominionshuffle.data.Card;
import nl.spellenclubeindhoven.dominionshuffle.data.CardSelector;
import nl.spellenclubeindhoven.dominionshuffle.data.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Application extends android.app.Application {
	private DataReader dataReader = new DataReader(this);
	private CardSelector cardSelector = new CardSelector(this);
	private boolean cardSelectorLoaded = false;
	private Result result;

	public DataReader getDataReader() {
		return dataReader;
	}

	synchronized public boolean isCardSelectorLoaded() {
		return cardSelectorLoaded;
	} 

	synchronized public void setCardSelectedLoaded(boolean value) {
		cardSelectorLoaded = value;
	}

	public CardSelector getCardSelector() {
		return cardSelector;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public Result getResult() {
		return result;
	}

	public void loadResult() {
		try {
			JSONObject jsonResult = new JSONObject(DataReader.readStringFromFile(this, "result.json"));
			
			JSONArray jsonCards = jsonResult.getJSONArray("cards");
			LinkedList<Card> cards = new LinkedList<Card>();

			for (int i = 0; i < jsonCards.length(); i++) {
				Card card = dataReader.getData().getCard(jsonCards.getString(i));
				if (card == null) return;
				cards.add(card);
			}
			
			Card baneCard = null;
			if(jsonResult.has("baneCard")) {
				baneCard = dataReader.getData().getCard(jsonResult.getString("baneCard"));
			}

			result = new Result();
			result.setCards(cards);
			result.setBaneCard(baneCard);
		} catch (JSONException ignore) {
			ignore.printStackTrace();
		} 
	}

	public void saveResult() {
		if (result == null) return;

		JSONObject jsonResult = new JSONObject();
		
		JSONArray jsonCards = new JSONArray();
		for (Card card : result.getCards()) {
			jsonCards.put(card.getName());
		}
		
		try {
			jsonResult.put("cards", jsonCards);
			if(result.getBaneCard() != null) {
				jsonResult.put("baneCard", result.getBaneCard().getName());
			}
			DataReader.writeStringToFile(this, "result.json", jsonResult.toString());
		} catch (JSONException ignore) {
			ignore.printStackTrace();
		}		
	}
}
