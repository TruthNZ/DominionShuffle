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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import nl.spellenclubeindhoven.dominionshuffle.data.Card;
import nl.spellenclubeindhoven.dominionshuffle.data.CardComparator;
import nl.spellenclubeindhoven.dominionshuffle.data.Result;

public abstract class CardListActivity extends ListActivity {
	private static final int MENU_SORT = 1;
	private static final int MENU_CLOSE = 2;
	private static final int MENU_COMPACT = 3;
	protected static final int MENU_LAST = MENU_COMPACT;
	private static final int DIALOG_SORT = 1;
	protected List<Card> cards = new LinkedList<Card>();
	private Result result = null;
	private boolean compact = true;
	private boolean useCardSelector;
	private CardAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result);

		Application application = (Application) getApplication();		
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences globalPrefs = PreferenceManager.getDefaultSharedPreferences(CardListActivity.this);
		
		compact = prefs.getBoolean("compact", compact);

		Collections.sort(cards, new CardComparator(prefs.getInt("sort", CardComparator.SORT_SET_COST_NAME), this));
		adapter = new CardAdapter(this, cards);
		adapter.setCompact(compact);
		adapter.setResult(result);
		if(useCardSelector) {
			adapter.setCardSelector(application.getCardSelector());
		}
		setListAdapter(adapter);
	}
	
	public void addCards(Collection<Card> cards) {
		this.cards.addAll(cards);
	}

	public void setDefaultCompact(boolean compact) {
		this.compact = compact;
	}	
	
	public void setUseCardSelector(boolean useCardSelector) {
		this.useCardSelector = useCardSelector;
	}
	
	public Collection<Card> getCards() {
		return cards;
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item;
		
		if(compact) {
			item = menu.add(Menu.NONE, MENU_COMPACT, Menu.NONE, R.string.menu_expanded_view);
			item.setIcon(R.drawable.last_result_compact);
		}
		else {
			item = menu.add(Menu.NONE, MENU_COMPACT, Menu.NONE, R.string.menu_compact_view);
			item.setIcon(R.drawable.last_result);			
		}
		
		item = menu.add(Menu.NONE, MENU_SORT, Menu.NONE, R.string.menu_sort);
		item.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
				
		item = menu.add(Menu.NONE, MENU_CLOSE, Menu.NONE, R.string.menu_close);
		item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_CLOSE) {
			this.finish();
			return true;
		} 
		else if (item.getItemId() == MENU_SORT) {
			showDialog(DIALOG_SORT);
			return true;
		}
		else if (item.getItemId() == MENU_COMPACT) {
			compact = !compact;
			
			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			prefs.edit().putBoolean("compact", compact).commit();

			adapter.setCompact(compact);
			adapter.notifyDataSetChanged();
			
			if(compact) {
				item.setIcon(R.drawable.last_result_compact);
				item.setTitle(R.string.menu_expanded_view);
			}
			else {
				item.setIcon(R.drawable.last_result);
				item.setTitle(R.string.menu_compact_view);
			}
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_SORT: {
			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			return builder.setTitle(R.string.sort_cards_dialog_title).setSingleChoiceItems(
					getResources().getStringArray(R.array.sort_items),
					prefs.getInt("sort", CardComparator.SORT_SET_COST_NAME),
					onSortClickListener).create();
		}
		default:
			return super.onCreateDialog(id);
		}
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public Result getResult() {
		return result;
	}

	private OnClickListener onSortClickListener = new OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			SharedPreferences globalPrefs = PreferenceManager.getDefaultSharedPreferences(CardListActivity.this);
			prefs.edit().putInt("sort", which).commit();
			
			Collections.sort(cards, new CardComparator(which, CardListActivity.this));
			((CardAdapter) getListAdapter()).notifyDataSetChanged();
			dialog.dismiss();
		}
	};
}
