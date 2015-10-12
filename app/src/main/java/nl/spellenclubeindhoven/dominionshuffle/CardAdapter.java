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

import java.util.List;

import nl.spellenclubeindhoven.dominionshuffle.data.Card;
import nl.spellenclubeindhoven.dominionshuffle.data.CardSelector;
import nl.spellenclubeindhoven.dominionshuffle.data.Result;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

class CardAdapter extends ArrayAdapter<Card> {
	private LayoutInflater inflater;
	private boolean compact = true;
	private CardSelector cardSelector;
	private Result result;

	public CardAdapter(Context context, List<Card> result) {
		super(context, R.layout.result_listitem, result);
		inflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		CardOrGroupHolder holder;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.result_listitem, null);
			holder = new CardOrGroupHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (CardOrGroupHolder) convertView.getTag();
		}

		Card card = getItem(position);

		holder.setBaneCard(result != null && card == result.getBaneCard());
		holder.setCompact(compact);
		holder.setName(card.getDisplay());
		StringBuilder type = new StringBuilder();
		for (String cardType: card.getTypes()) {
			if (type.length() > 0) {
				type.append(" - ");
			}
			type.append(cardType);
		}
		holder.setDescription(type.toString());
		holder.setIconValue(card.getCost());
		holder.setSet(card.getSet());		
		if(cardSelector == null) {
			holder.hideCheckBox();
		}
		else {
			holder.setCheckBox2(cardSelector, card);
		}

		return convertView;
	}

	public boolean isCompact() {
		return compact;
	}

	public void setCompact(boolean compressed) {
		this.compact = compressed;
	}

	public void setCardSelector(CardSelector cardSelector) {
		this.cardSelector = cardSelector;
	}
	
	public CardSelector getCardSelector() {
		return cardSelector;
	}

	public void setResult(Result result) {
		this.result = result;
	}
}