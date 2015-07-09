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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import nl.spellenclubeindhoven.dominionshuffle.data.Card;
import nl.spellenclubeindhoven.dominionshuffle.data.CardComparator;
import nl.spellenclubeindhoven.dominionshuffle.data.CardSelector;
import nl.spellenclubeindhoven.dominionshuffle.data.Group;
import nl.spellenclubeindhoven.dominionshuffle.data.Limit;
import nl.spellenclubeindhoven.dominionshuffle.data.SectionedCardOrGroupAdapter;
import nl.spellenclubeindhoven.dominionshuffle.data.SolveError;
import nl.spellenclubeindhoven.dominionshuffle.widget.CustomFastScrollView;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

public class SelectActivity extends TabActivity implements OnScrollListener {
	private static final int MENU_SORT = 1;
	private static final int MENU_SHUFFLE = 2;
	private static final int MENU_LAST_RESULT = 3;
	private static final int MENU_CLEAR_ALL = 4;
	private static final int MENU_LOAD = 5;
	private static final int MENU_SAVE = 6;
	private static final int MENU_CARD_LANGUAGE = 7;
	private static final int MENU_ABOUT = 8;
	private static final int DIALOG_SOLVE_ERROR = 1;
	private static final int DIALOG_NO_RESULT_ERROR = 2;
	private static final int DIALOG_SORT = 3;
	private static final int DIALOG_MINMAX = 4;
	private static final int DIALOG_CARD_LANGUAGE = 5;
	private static final int DIALOG_MINIMUM = 100;
	private static final int DIALOG_MAXIMUM = 200;
	private ListView inexList;
	private ListView constraintList;
	private DataReader dataReader;
	private CardSelector cardSelector;
	private Application application;
	private InExAdapter inExAdapter;
	private ConstraintAdapter constraintAdapter;
	private CustomFastScrollView inexFastScrollView, constraintFastScrollView;
	@SuppressWarnings("unused")
	private DialogInterface dialog;
	private List<Object> groupsAndCards;
	private int dialogMimimumCount;
	private int dialogMaximumCount;
	private float lastActionUpX;

	// State will be remembered of the following variables
	private String dialogMessage;
	private Group selectedGroup;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select);

		application = (Application) getApplication();
		dataReader = application.getDataReader();
		cardSelector = application.getCardSelector();

		TabHost tabs = getTabHost();

		TabHost.TabSpec spec = tabs.newTabSpec("inex");
		spec.setContent(R.id.inex_fastscrollview);
		spec.setIndicator(getResources().getString(R.string.tab_select), getResources().getDrawable(R.drawable.select_tab));
		tabs.addTab(spec);

		spec = tabs.newTabSpec("constraint");
		spec.setContent(R.id.constraint_fastscrollview);
		spec.setIndicator(getResources().getString(R.string.tab_constraints), getResources().getDrawable(R.drawable.limits_tab));
		tabs.addTab(spec);

		spec = tabs.newTabSpec("generate");
		spec.setContent(R.id.generateButtons);
		spec.setIndicator(getResources().getString(R.string.tab_generate), getResources().getDrawable(R.drawable.shuffle_tab));
		tabs.addTab(spec);

		inexFastScrollView = ((CustomFastScrollView)findViewById(R.id.inex_fastscrollview));
		inexFastScrollView.setOnScrollListener(this);
		
		constraintFastScrollView = ((CustomFastScrollView)findViewById(R.id.constraint_fastscrollview));
		constraintFastScrollView.setOnScrollListener(this);
		
		
		groupsAndCards = new LinkedList<Object>();

		inExAdapter = new InExAdapter(this, groupsAndCards, cardSelector, getPreferredCardComparator());
		inexList = (ListView) findViewById(R.id.inexList);
		inexList.setAdapter(inExAdapter);
		inexList.setOnItemClickListener(onItemClickListener);
		inexList.setOnItemLongClickListener(onItemLongClickListener);

		constraintList = (ListView) findViewById(R.id.constraintList);
		constraintAdapter = new ConstraintAdapter(this, groupsAndCards,
				cardSelector, getPreferredCardComparator());
		constraintList.setAdapter(constraintAdapter);
		constraintList.setOnItemClickListener(onItemClickListener);
		constraintList.setOnTouchListener(onConstraintListTouchListener);
		constraintList.setOnKeyListener(onConstraintKeyListener);
		constraintList.setOnItemLongClickListener(onItemLongClickListener);

		Button generateButton = (Button) findViewById(R.id.generateButton);
		generateButton.setOnClickListener(onGenerateClickListener);

		Button lastResultButton = (Button) findViewById(R.id.lastResultButton);
		lastResultButton.setOnClickListener(onLastResultClickListener);

		if (savedInstanceState != null) {
			dialogMessage = savedInstanceState.getString("dialogMessage");
			if(dataReader.getData() != null) {
				selectedGroup = dataReader.getData().getGroup(
						savedInstanceState.getString("selectedGroup"));
			}
		}

		SharedPreferences prefs = getPreferences(MODE_PRIVATE);

		dialogMimimumCount = getResources().getStringArray(
				R.array.minumum_items_conditional).length;
		dialogMaximumCount = getResources().getStringArray(
				R.array.maximum_items).length;
	}
	

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

		switch (view.getId()) {
			case R.id.inexList:
				constraintList.setSelection(firstVisibleItem);
				break;
			case R.id.constraintList:
				inexList.setSelection(firstVisibleItem);
				break;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// do nothing
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString("dialogMessage", dialogMessage);
		if (selectedGroup != null) {
			outState.putString("selectedGroup", selectedGroup.getName());
		}
	}

	@Override
	protected void onPause() {
		dataReader.saveCardSelectorState(this, cardSelector);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadData();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private static String[] subArray(String[] array, int start, int end) {
		int len = end - start;
		String[] result = new String[len];
		System.arraycopy(array, start, result, 0, len);
		return result;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (DIALOG_MINIMUM <= id && id <= DIALOG_MINIMUM + dialogMimimumCount) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			AlertDialog alertDialog = builder.setSingleChoiceItems(
					subArray(getResources().getStringArray(
							R.array.minumum_items_conditional), 0, id - DIALOG_MINIMUM),
					cardSelector.getLimitMinimum(selectedGroup),
					onMinClickListener).create();
			alertDialog.setCanceledOnTouchOutside(true);
			return alertDialog;
		} else if (DIALOG_MAXIMUM <= id
				&& id <= DIALOG_MAXIMUM + dialogMaximumCount) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			AlertDialog alertDialog = builder.setSingleChoiceItems(
					subArray(getResources().getStringArray(
							R.array.maximum_items), 0, id - DIALOG_MAXIMUM),
					cardSelector.getLimitMaximum(selectedGroup),
					onMaxClickListener).create();
			alertDialog.setCanceledOnTouchOutside(true);
			return alertDialog;
		}

		switch (id) {
		case DIALOG_SOLVE_ERROR: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			return builder.setTitle(R.string.solve_dialog_title).setMessage(dialogMessage)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setPositiveButton(R.string.ok, null).create();
		}
		case DIALOG_NO_RESULT_ERROR: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			return builder.setTitle(R.string.display_result_dialog_title).setMessage(
					R.string.result_no_result).setPositiveButton(R.string.ok, null)
					.setIcon(android.R.drawable.ic_dialog_info).create();
		}
		case DIALOG_SORT: {
			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			return builder.setTitle(R.string.sort_cards_dialog_title).setSingleChoiceItems(
					getResources().getStringArray(R.array.sort_items),
					prefs.getInt("sort", CardComparator.SORT_SET_COST_NAME),
					onSortClickListener).create();
		}
		case DIALOG_MINMAX: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			AlertDialog alertDialog = builder.setItems(
					getResources().getStringArray(R.array.minmax_array),
					onMinMaxClickListener).create();
			alertDialog.setCanceledOnTouchOutside(true);
			return alertDialog;
		}
		case DIALOG_CARD_LANGUAGE: {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			int langIndex = prefs.getInt("lang", 0);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			AlertDialog alertDialog = builder
				.setTitle(R.string.choose_card_language)
				.setSingleChoiceItems(R.array.languages, langIndex, onChooseLanguageClickListener)
				.create();
			return alertDialog;
		}
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (DIALOG_MINIMUM <= id && id <= DIALOG_MINIMUM + dialogMimimumCount) {
			AlertDialog alertDialog = (AlertDialog) dialog;
			ListView list = alertDialog.getListView();
			
			int pos;
			if(cardSelector.getCondition(selectedGroup) == null) {
				pos = cardSelector.getLimitMinimum(selectedGroup);
				if(pos >= 1) pos++;
			}
			else {
				pos = 1;
			}
			
			list.clearChoices();
			list.setItemChecked(pos, true);
			list.setSelectionFromTop(pos, list.getHeight() / 2);
		} else if (DIALOG_MAXIMUM <= id
				&& id <= DIALOG_MAXIMUM + dialogMaximumCount) {
			AlertDialog alertDialog = (AlertDialog) dialog;
			ListView list = alertDialog.getListView();
			int pos = cardSelector.getLimitMaximum(selectedGroup);
			list.clearChoices();
			list.setItemChecked(pos, true);
			list.setSelectionFromTop(pos, list.getHeight() / 2);
		}

		switch (id) {
		case DIALOG_SOLVE_ERROR: {
			AlertDialog alertDialog = (AlertDialog) dialog;
			alertDialog.setMessage(dialogMessage);
			break;
		}
		default:
			break;
		}

		super.onPrepareDialog(id, dialog);
	}

	private void loadCardSelectorState() {
		if (dataReader.getData() == null)
			return;
		
		try {
			cardSelector.fromJson(DataReader.readStringFromFile(this, "card_selector.json"), dataReader.getData());
		} catch (JSONException ignore) {
			ignore.printStackTrace();
		}
	}

	private void showSolveErrorDialog(String errorMessage) {
		this.dialogMessage = errorMessage;
		showDialog(DIALOG_SOLVE_ERROR);
	}

	private void showMinimumDialog(int maxValue) {
		if(maxValue != 0) maxValue++;
		if (maxValue >= dialogMimimumCount) {
			maxValue = dialogMimimumCount - 1;
		}

		showDialog(DIALOG_MINIMUM + maxValue + 1);
	}

	private void showMaximumDialog(int maxValue) {
		if (maxValue >= dialogMaximumCount) {
			maxValue = dialogMaximumCount - 1;
		}

		showDialog(DIALOG_MAXIMUM + maxValue + 1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = menu.add(Menu.NONE, MENU_SORT, Menu.NONE, R.string.menu_sort);
		item.setIcon(android.R.drawable.ic_menu_sort_alphabetically);

		item = menu.add(Menu.NONE, MENU_CLEAR_ALL, Menu.NONE, R.string.menu_clear_all);
		item.setIcon(R.drawable.clear_all);

		item = menu.add(Menu.NONE, MENU_LOAD, Menu.NONE, R.string.menu_load);
		item.setIcon(android.R.drawable.ic_menu_upload);
		
		item = menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, R.string.menu_save);
		item.setIcon(android.R.drawable.ic_menu_save);
		
		item = menu.add(Menu.NONE, MENU_CARD_LANGUAGE, Menu.NONE, R.string.menu_card_language);
		item.setIcon(R.drawable.globe);

		item = menu.add(Menu.NONE, MENU_SHUFFLE, Menu.NONE, R.string.menu_shuffle);
		item.setIcon(R.drawable.shuffle);

		item = menu.add(Menu.NONE, MENU_LAST_RESULT, Menu.NONE, R.string.menu_last_result);
		item.setIcon(R.drawable.last_result);

		item = menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, "About");
		item.setIcon(android.R.drawable.stat_sys_warning);
				
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SORT:
			showDialog(DIALOG_SORT);
			return true;
		case MENU_SHUFFLE:
			doShuffle();
			return true;
		case MENU_LAST_RESULT:
			showLastResult();
			return true;
		case MENU_CLEAR_ALL:
			clearAll();
			return true;
		case MENU_LOAD:
			doLoadSelection();
			return true;
		case MENU_SAVE:
			doSaveSelection();
			return true;
		case MENU_CARD_LANGUAGE:
			showDialog(DIALOG_CARD_LANGUAGE);
			return true;
		case MENU_ABOUT:
			showAbout();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void doSaveSelection() {
		Intent intent = new Intent(this, LoadSaveActivity.class);
		intent.putExtra("load", false);
		startActivity(intent);
	}

	private void doLoadSelection() {
		Intent intent = new Intent(this, LoadSaveActivity.class);
		intent.putExtra("load", true);
		startActivity(intent);
	}

	private void clearAll() {
		cardSelector.clear();
		constraintAdapter.notifyDataSetChanged();
		inExAdapter.notifyDataSetChanged();
		Toast.makeText(this, R.string.toast_all_cleared, Toast.LENGTH_SHORT)
				.show();
	}

	private void doShuffle() {
		Application application = (Application) getApplication();
		try {
			cardSelector.setSolveAttempts(20);
			application.setResult(cardSelector.generate(dataReader.getData()));
			startActivity(new Intent(getApplicationContext(),
					ResultActivity.class));
		} catch (SolveError e) {
			showSolveErrorDialog(getResources().getString(e.getResourceId()));
		}
	}

	private void showAbout() {
		startActivity(new Intent(getApplicationContext(),
				ReleaseNotesActivity.class));
	}

	private void showLastResult() {
		Application application = (Application) getApplication();
		if (application.getResult() == null) {
			showDialog(DIALOG_NO_RESULT_ERROR);
		} else {
			startActivity(new Intent(getApplicationContext(),
					ResultActivity.class));
		}
	}

	private void showLimitActivity() {
		Intent intent = new Intent(getApplicationContext(), LimitActivity.class);
		intent.putExtra("source", selectedGroup.getName());
		startActivity(intent);
	}	

	private OnItemClickListener onItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (parent.getId() == R.id.inexList) {
				Object cardOrGroup = inExAdapter.getItem(position);
				cardSelector.cycleIncludeExclude(cardOrGroup);

				CardOrGroupHolder holder = (CardOrGroupHolder) view.getTag();
				holder.setCheckBox(cardSelector, cardOrGroup);
			} else if (parent.getId() == R.id.constraintList) {
				Object cardOrGroup = inExAdapter.getItem(position);
				CardOrGroupHolder holder = (CardOrGroupHolder) view.getTag();
				if (cardOrGroup instanceof Card) {
					Card card = (Card) cardOrGroup;

					if (cardSelector.hasRequiredCard(card)) {
						cardSelector.removeRequiredCard(card);
						holder.setRequiredCheckBox(false);
					} else {
						cardSelector.addRequiredCard(card);
						holder.setRequiredCheckBox(true);
					}
				} else { // cardOrGroup instanceof Group
					selectedGroup = (Group) cardOrGroup;

					if (lastActionUpX < holder.getLeftArea()) {
						showMinimumDialog(selectedGroup.getCards().size());
					} else if (lastActionUpX > holder.getRightArea()) {
						showMaximumDialog(selectedGroup.getCards().size());
					} else {
						showDialog(DIALOG_MINMAX);
					}
				}
			}
		}
	};
	
	private OnItemLongClickListener onItemLongClickListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			Object cardOrGroup;
			if(parent.getId() == R.id.inexList) {			
				cardOrGroup = inExAdapter.getItem(position);
			}
			else {
				cardOrGroup = constraintAdapter.getItem(position);				
			}
		
			if(cardOrGroup instanceof Group) {
				Group group = (Group) cardOrGroup;
				Intent intent = new Intent(getApplicationContext(), ShowGroupActivity.class);
				intent.putExtra("group", group.getName());
				startActivity(intent);
				
				return true;
			}
			return false;
		}
	};
		
	private OnTouchListener onConstraintListTouchListener = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				lastActionUpX = event.getX();
			}

			return false;
		}
	};
	
	private OnKeyListener onConstraintKeyListener = new OnKeyListener() {
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
				// If dpad click, reset the lastActionUpX
				lastActionUpX = v.getWidth() / 2;
			}
			return false;
		}		
	};	

	private OnClickListener onGenerateClickListener = new OnClickListener() {
		public void onClick(View v) {
			doShuffle();
		}
	};

	private OnClickListener onLastResultClickListener = new OnClickListener() {
		public void onClick(View v) {
			showLastResult();
		}
	};

	private android.content.DialogInterface.OnClickListener onSortClickListener = new android.content.DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			SharedPreferences globalPrefs = PreferenceManager.getDefaultSharedPreferences(SelectActivity.this);
			
			prefs.edit().putInt("sort", which).commit();
			CardComparator comparator = new CardComparator(
				which,
				getResources().getStringArray(R.array.locales)[globalPrefs.getInt("lang", 0)]
			);
			Collections.sort(groupsAndCards, comparator);
			inExAdapter.setCardComparator(comparator);
			constraintAdapter.setCardComparator(comparator);
			inExAdapter.notifyDataSetChanged();
			constraintAdapter.notifyDataSetChanged();
			inExAdapter.refreshSections();
			inexFastScrollView.listItemsChanged();
			constraintAdapter.refreshSections();
			constraintFastScrollView.listItemsChanged();
			dialog.dismiss();
		}
	};
	
	private CardComparator getPreferredCardComparator() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences globalPrefs = PreferenceManager.getDefaultSharedPreferences(SelectActivity.this);
		return new CardComparator(
			prefs.getInt("sort", CardComparator.SORT_SET_NAME),
			getResources().getStringArray(R.array.locales)[globalPrefs.getInt("lang", 0)]
		);
	}

	private android.content.DialogInterface.OnClickListener onMinMaxClickListener = new android.content.DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case 0:
				showMinimumDialog(selectedGroup.getCards().size());
				break;
			case 1:
				showMaximumDialog(selectedGroup.getCards().size());
				break;
			case 2:
				showLimitActivity();				
				break;
			case 3:
				cardSelector.removeLimit(selectedGroup);
				constraintAdapter.notifyDataSetChanged();
			}
		}
	};

	private android.content.DialogInterface.OnClickListener onMinClickListener = new android.content.DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			if(which == 1) {
				showLimitActivity();				
			}
			else {
				if(which > 1) which--;
				cardSelector.setLimitMinimum(selectedGroup, which);
				cardSelector.setCondition(selectedGroup, null);
				constraintAdapter.notifyDataSetChanged();
			}
						
			dialog.dismiss();
		}
	};

	private android.content.DialogInterface.OnClickListener onMaxClickListener = new android.content.DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			cardSelector.setLimitMaximum(selectedGroup, which);
			constraintAdapter.notifyDataSetChanged();
			dialog.dismiss();
		}
	};

	private void loadData() {
		dataReader.loadData();
		afterDataLoadedStuff();
	}
	
	private void reloadData() {
		application.setCardSelectedLoaded(false);
		dataReader.reset();
		loadData();
	}
		
	private void afterDataLoadedStuff() {
		groupsAndCards.clear();		
		groupsAndCards.addAll(dataReader.getData().getAll());

		CardComparator comparator = getPreferredCardComparator();
		inExAdapter.setCardComparator(comparator);
		constraintAdapter.setCardComparator(comparator);
		Collections.sort(groupsAndCards, comparator);
		
		if(!application.isCardSelectorLoaded()) {
			loadCardSelectorState();
			application.setCardSelectedLoaded(true);
			application.loadResult(); 
		}
		
		inExAdapter.notifyDataSetChanged();
		constraintAdapter.notifyDataSetChanged();		
		inExAdapter.refreshSections();
		inexFastScrollView.listItemsChanged();
	}
		
	private DialogInterface.OnClickListener onChooseLanguageClickListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SelectActivity.this);
			
			if(prefs.getInt("lang", 0) != which) {			
				Editor editor = prefs.edit();
				editor.putInt("lang", which);
				editor.commit();
				
				reloadData();
								
				// Completely cleanup this dialog
				removeDialog(DIALOG_CARD_LANGUAGE);
			}
			else {			
				dialog.dismiss();
			}
		}
	};	

	private static class InExAdapter extends SectionedCardOrGroupAdapter {

		public InExAdapter(Context context, List<Object> list,
				CardSelector cardSelector, CardComparator comparator) {
			super(context, R.layout.inex_listitem, list, cardSelector, comparator);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CardOrGroupHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.inex_listitem, null);
				holder = new CardOrGroupHolder(convertView);
				convertView.setTag(holder);

				holder.hideCheckBoxValue();
			} else {
				holder = (CardOrGroupHolder) convertView.getTag();
			}

			Object cardOrGroup = getItem(position);
			if (cardOrGroup instanceof Card) {
				Card card = (Card) cardOrGroup;

				holder.setName(card.getDisplay());
				holder.setDescription(card.getSet());
				holder.setIconValue(card.getCost());
				holder.setCheckBox(cardSelector, card);
			} else if (cardOrGroup instanceof Group) {
				Group group = (Group) cardOrGroup;

				holder.setName(group.getDisplay() + " (" + group.getCards().size()
						+ ")");
				holder.setDescription(group.getDescription());
				holder.setIconValue(null);
				holder.setCheckBox(cardSelector, group);
			}

			return convertView;
		}
	}

	private static class ConstraintAdapter extends SectionedCardOrGroupAdapter {

		public ConstraintAdapter(Context context, List<Object> list,
				CardSelector cardSelector, CardComparator comparator) {
			super(context, android.R.layout.simple_list_item_1, list, cardSelector, comparator);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CardOrGroupHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.inex_listitem, null);
				holder = new CardOrGroupHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (CardOrGroupHolder) convertView.getTag();
			}

			Object cardOrGroup = getItem(position);
			if (cardOrGroup instanceof Card) {
				Card card = (Card) cardOrGroup;

				holder.setName(card.getDisplay());
				holder.setDescription(card.getSet());
				holder.setIconValue(card.getCost());
				holder.setRequiredCheckBox(cardSelector.hasRequiredCard(card));
			} else if (cardOrGroup instanceof Group) {
				Group group = (Group) cardOrGroup;

				holder.setName(group.getDisplay() + " (" + group.getCards().size()
						+ ")");
				holder.setDescription(group.getDescription());
				if (cardSelector.hasLimit(group)) {
					Limit rule = cardSelector.getLimit(group);
					holder.setMinValue(rule.getLimitMinimum(), rule.getCondition() != null);
					holder.setMaxValue(rule.getMaximum());
				} else {
					holder.setMinValue(0, false);
					holder.setMaxValue(0);
				}
			}

			return convertView;
		}
	}
}