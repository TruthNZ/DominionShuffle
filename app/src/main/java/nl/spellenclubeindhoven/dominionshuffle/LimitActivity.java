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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import nl.spellenclubeindhoven.dominionshuffle.data.CardComparator;
import nl.spellenclubeindhoven.dominionshuffle.data.CardSelector;
import nl.spellenclubeindhoven.dominionshuffle.data.Data;
import nl.spellenclubeindhoven.dominionshuffle.data.Group;
import nl.spellenclubeindhoven.dominionshuffle.data.GroupOrCard;
import nl.spellenclubeindhoven.dominionshuffle.data.Nothing;

public class LimitActivity extends Activity {
	private CardOrGroupHolder sourceHolder;
	private Application application;
	private Data data;
	private CardSelector cardSelector;
	private Group group;
	private Spinner minSpinner;
	private Spinner maxSpinner;
	private Spinner conditionSpinner;
	private View sourceView;
	private List<GroupOrCard> groupsAndCards;
	private ArrayAdapter<GroupOrCard> conditionAdapter;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.limit_dialog);

		sourceView = findViewById(R.id.sourceGroupOrCard); 
		sourceHolder = new CardOrGroupHolder(sourceView);
		application = (Application) getApplication();
		data = application.getDataReader().getData();
		cardSelector = application.getCardSelector();

		minSpinner = (Spinner) findViewById(R.id.minSpinner);
		minSpinner.setOnItemSelectedListener(onSpinnerItemSelected);
		maxSpinner = (Spinner) findViewById(R.id.maxSpinner);
		maxSpinner.setOnItemSelectedListener(onSpinnerItemSelected);
				
		groupsAndCards = new LinkedList<GroupOrCard>();
		groupsAndCards.add(new Nothing(getString(R.string.no_card_or_group_selected)));
		groupsAndCards.addAll(data.getAll());
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);		
		Collections.sort(groupsAndCards, new CardComparator(CardComparator.SORT_SET_NAME, prefs.getString("lang", "en")));
		
		conditionSpinner = (Spinner) findViewById(R.id.conditionSpinner);
		conditionSpinner.setOnItemSelectedListener(onConditionSpinnerItemSelected);
		conditionAdapter = new ConditionAdapter(this, groupsAndCards);
		conditionSpinner.setAdapter(conditionAdapter);
	}

	@Override
	protected void onStart() {
		super.onStart();

		Intent intent = getIntent();
		group = data.getGroup(intent.getStringExtra("source"));

		sourceHolder.setMinValue(cardSelector.getLimitMinimum(group), cardSelector.getCondition(group) != null);
		sourceHolder.setMaxValue(cardSelector.getLimitMaximum(group));
		sourceHolder.setName(group.getDisplay() + " (" + group.getCards().size() + ")");
		sourceHolder.setDescription(group.getDescription());

		int maxCards = group.getCards().size();

		String[] stringArray = leftArrayString(R.array.minumum_items, maxCards + 1);
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, stringArray);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		minSpinner.setAdapter(arrayAdapter);
		minSpinner.setSelection(Math.min(cardSelector.getLimitMinimum(group), stringArray.length));

		stringArray = leftArrayString(R.array.maximum_items, maxCards + 1);
		arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, stringArray);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		maxSpinner.setAdapter(arrayAdapter);
		maxSpinner.setSelection(Math.min(cardSelector.getLimitMaximum(group), stringArray.length));
		
		GroupOrCard condition = cardSelector.getCondition(group);
		if(condition == null) conditionSpinner.setSelection(0);
		else conditionSpinner.setSelection(conditionAdapter.getPosition(condition));
	}
	
	private String[] leftArrayString(int resourceId, int length) {
		String[] src = getResources().getStringArray(resourceId);
		length = Math.min(length, src.length);
		String[] dst = new String[length];
		System.arraycopy(src, 0, dst, 0, length);
		return dst;		
	}
	
	private OnItemSelectedListener onSpinnerItemSelected = new OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> adapterView, View view, int position, long arg3) {
			if(adapterView == minSpinner) {
				cardSelector.setLimitMinimum(group, position);
				sourceHolder.setMinValue(cardSelector.getLimitMinimum(group), cardSelector.getCondition(group) != null);
			}
			else if(adapterView == maxSpinner) {
				cardSelector.setLimitMaximum(group, position);
				sourceHolder.setMaxValue(cardSelector.getLimitMaximum(group));				
			}
		}

		public void onNothingSelected(AdapterView<?> arg0) {
		}
	};
	
	private OnItemSelectedListener onConditionSpinnerItemSelected = new OnItemSelectedListener() {

		public void onItemSelected(AdapterView<?> parent, View view, int position, long arg3) {
			GroupOrCard groupOrCard = (GroupOrCard) parent.getAdapter().getItem(position);			
			cardSelector.setCondition(group, groupOrCard);
			sourceHolder.setMinValue(cardSelector.getLimitMinimum(group), cardSelector.getCondition(group) != null);
		}

		public void onNothingSelected(AdapterView<?> arg0) {
		}
	};
	
	private static class ConditionAdapter extends ArrayAdapter<GroupOrCard> {
		private LayoutInflater inflater;

		public ConditionAdapter(Context context, List<GroupOrCard> objects) {
			super(context, android.R.layout.simple_spinner_item, objects);
			inflater = LayoutInflater.from(context);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getText1View(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getText1View(position, convertView, parent, android.R.layout.simple_spinner_item);
		}
		
		public View getText1View(int position, View convertView, ViewGroup parent, int resourceId) {
			TextView text1View;
			
			if(convertView == null) {
				convertView = inflater.inflate(resourceId, parent, false);
				text1View = (TextView) convertView.findViewById(android.R.id.text1);
				convertView.setTag(text1View);
			}
			else {
				text1View = (TextView) convertView.getTag();				
			}			
			
			text1View.setText(getText1(position));
						
			return convertView;			
		}
		
		public String getText1(int position) {
			GroupOrCard groupOrCard = getItem(position);
			if(groupOrCard.isCard()) return getContext().getString(R.string.card) + " " + groupOrCard.getDisplay();
			else if(groupOrCard.isGroup()) return getContext().getString(R.string.group) + " " + groupOrCard.getDisplay();
			else return groupOrCard.toString();
		}
	}
}
