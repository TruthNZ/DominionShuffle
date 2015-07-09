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

import nl.spellenclubeindhoven.dominionshuffle.data.Card;
import nl.spellenclubeindhoven.dominionshuffle.data.CardSelector;
import nl.spellenclubeindhoven.dominionshuffle.data.Group;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

class CardOrGroupHolder {
	public enum CheckBoxEnum {
		INCLUDE, EXCLUDE, NONE, REQUIRE
	}
	
	private View row;
	private TextView nameView;
	private TextView descriptionView;
	private TextView iconValueView;
	private TextView setView;
	private TextView setCompactView;
	private ImageView iconView;
	private ImageView checkboxView;
	private TextView checkboxValueView;
	private View extendedInfoView;
	private boolean baneCard;
	static private int baneColor;
	static private String baneText;
	
	public CardOrGroupHolder(View row) {
		this.row = row;
		nameView = (TextView) row.findViewById(R.id.name);
		descriptionView = (TextView) row.findViewById(R.id.description);
		setView = (TextView) row.findViewById(R.id.set);
		iconValueView = (TextView) row.findViewById(R.id.iconValue);
		iconView = (ImageView) row.findViewById(R.id.icon);
		checkboxView = (ImageView) row.findViewById(R.id.checkbox);
		checkboxValueView = (TextView) row.findViewById(R.id.checkboxValue);
		extendedInfoView = (View) row.findViewById(R.id.extendedInfo);
		setCompactView = (TextView) row.findViewById(R.id.setCompact);
		
		if(baneText == null) {
			baneText = " (" + row.getContext().getString(R.string.bane) + ")";
			baneColor = row.getContext().getResources().getColor(R.color.bane_color);
		}
	}
	
	public void setIconValue(String cost) {
		if(cost == null) {
			iconView.setVisibility(View.GONE);
			iconValueView.setVisibility(View.GONE); 
		}
		else {
			if(cost.startsWith("P")) {
				iconView.setImageResource(R.drawable.potion);
				cost = cost.substring(1);
			}
			else {
				iconView.setImageResource(R.drawable.coin);
			}
			iconView.setVisibility(View.VISIBLE);
			iconValueView.setText(cost);
			iconValueView.setVisibility(View.VISIBLE);
		}
	}
	
	public void hideCheckBoxValue() {
		checkboxValueView.setVisibility(View.GONE);
	}
	
	public void setMinValue(int minimum, boolean conditional) {
		if(minimum == 0) {
			iconView.setImageResource(R.drawable.gray_min);
			iconValueView.setVisibility(View.GONE);
		}
		else {
			if(conditional) {
				iconView.setImageResource(R.drawable.cond);
			}
			else {
				iconView.setImageResource(R.drawable.min);
			}
			iconValueView.setText(Integer.toString(minimum));
			iconValueView.setVisibility(View.VISIBLE);
		}
	}
		
	public void setMaxValue(int maximum) {
		if(maximum == 0 || maximum == Integer.MAX_VALUE) {
			checkboxView.setImageResource(R.drawable.grey_max);
			checkboxValueView.setVisibility(View.GONE);
		}
		else {
			checkboxView.setImageResource(R.drawable.max);
			checkboxValueView.setText(Integer.toString(maximum));
			checkboxValueView.setVisibility(View.VISIBLE);
		}
	}

	public void hideCheckBox() {
		checkboxView.setVisibility(View.GONE);
	}
	
	public void setCheckBox(CardOrGroupHolder.CheckBoxEnum status) {
		if(status == CheckBoxEnum.INCLUDE) {
			checkboxView.setImageResource(R.drawable.plus_include);
		}
		else if(status == CheckBoxEnum.EXCLUDE) {
			checkboxView.setImageResource(R.drawable.min_exclude);
		}
		else if(status == CheckBoxEnum.REQUIRE) {
			checkboxView.setImageResource(R.drawable.include);
		}
		else {
			checkboxView.setImageResource(R.drawable.grey_circle);
		}
	}
		
	public void setCheckBox2(CardSelector cardSelector, Card card) {
		if(cardSelector.hasRequiredCard(card)) {
			setCheckBox(CheckBoxEnum.REQUIRE);
		}
		else if(cardSelector.hasExlcudedCard(card)) {
			setCheckBox(CheckBoxEnum.EXCLUDE);
		}
		else {
			setCheckBox(CheckBoxEnum.NONE);
		}
	}
	
	public void setCheckBox(CardSelector cardSelector, Object cardOrGroup) {
		if(cardOrGroup instanceof Card) {
			Card card = (Card) cardOrGroup;
			
			if(cardSelector.hasIncludedCard(card)) {
				setCheckBox(CheckBoxEnum.INCLUDE);
			}
			else if(cardSelector.hasExlcudedCard(card)) {
				setCheckBox(CheckBoxEnum.EXCLUDE);
			}
			else {
				setCheckBox(CheckBoxEnum.NONE);
			}
		}
		else if(cardOrGroup instanceof Group) {
			Group group = (Group) cardOrGroup;
			
			if(cardSelector.hasIncludedGroup(group)) {
				setCheckBox(CheckBoxEnum.INCLUDE);
			}
			else if(cardSelector.hasExcludedGroup(group)) {
				setCheckBox(CheckBoxEnum.EXCLUDE);
			}
			else {
				setCheckBox(CheckBoxEnum.NONE);
			}				
		}			
	}

	public void setName(String name) {
		if(baneCard) {
			Spannable spannable = new SpannableString(name + baneText);
			int start = name.length() + 1;
			int end = spannable.length();
			spannable.setSpan(new ForegroundColorSpan(baneColor), start, end, 0);
			spannable.setSpan(new RelativeSizeSpan(0.60f), start, end, 0);
			nameView.setText(spannable);
		}
		else nameView.setText(name);
	}
	
	public void setDescription(String description) {
		descriptionView.setText(description);
	}
	
	public void invalidateCheckBox() {
		checkboxView.invalidate();
	}
	
	public void setSet(String set) {
		setView.setText(set);
		if(setCompactView != null) {
			setCompactView.setText(set);
		}
	}
	
	public void setCompact(boolean compact) {
		if(compact) {
			extendedInfoView.setVisibility(View.GONE);
			setCompactView.setVisibility(View.VISIBLE);
			
		}
		else {
			extendedInfoView.setVisibility(View.VISIBLE);
			setCompactView.setVisibility(View.GONE);
		}
	}

	public void setRequiredCheckBox(boolean hasRequiredCard) {
		checkboxValueView.setVisibility(View.GONE);
		if(hasRequiredCard) {
			checkboxView.setImageResource(R.drawable.include);			
		}
		else {
			checkboxView.setImageResource(R.drawable.grey_circle);			
		}		
	}

	public float getLeftArea() {
		return row.getWidth() / 3.0f;
	}

	public float getRightArea() {
		return row.getWidth() - row.getWidth() / 3.0f;
	}

	public void setBaneCard(boolean baneCard) {
		this.baneCard = baneCard;
	}
}