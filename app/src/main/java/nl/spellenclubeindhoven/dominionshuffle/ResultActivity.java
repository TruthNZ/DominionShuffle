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

import java.util.ArrayList;
import java.util.List;

import nl.spellenclubeindhoven.dominionshuffle.data.Card;
import nl.spellenclubeindhoven.dominionshuffle.data.CardSelector;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ResultActivity extends CardListActivity {
	private static final String TAG = "ResultActivity";
	private static final int MENU_CLEAR_ALL = MENU_LAST + 1;
	private static final int MENU_EXCLUDE_ALL = MENU_LAST + 2;
	private static final int MENU_REQUIRE_ALL = MENU_LAST + 3;
	private static final int MENU_LAUNCH_ANDROMINION = MENU_LAST + 4;
	private int screenTimeout = 3 * 60000;
	//private PowerManager.WakeLock wakeLock;
	private DataReader dataReader;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Application application = (Application) getApplication();
		dataReader = application.getDataReader();
		if(application.getResult() != null) {
			addCards(application.getResult().getCards());
			setResult(application.getResult());
		}
		setDefaultCompact(true);
		setUseCardSelector(true);

		super.onCreate(savedInstanceState);
		
		getListView().setOnItemClickListener(onItemClickListener);

		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//		wakeLock = powerManager.newWakeLock(
//				PowerManager.SCREEN_BRIGHT_WAKE_LOCK
//						| PowerManager.ON_AFTER_RELEASE, TAG);
//		wakeLock.setReferenceCounted(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = menu.add(Menu.NONE, MENU_CLEAR_ALL, Menu.NONE, R.string.menu_clear_all);
		item.setIcon(R.drawable.clear_all);

		item = menu.add(Menu.NONE, MENU_EXCLUDE_ALL, Menu.NONE, R.string.menu_exclude_all);
		item.setIcon(R.drawable.min_exclude_all);
		
		item = menu.add(Menu.NONE, MENU_REQUIRE_ALL, Menu.NONE, R.string.menu_require_all);
		item.setIcon(R.drawable.require_all);
		
        if(getAndrominionIntentIfAvailable(this) != null) {
            item = menu.add(Menu.NONE, MENU_LAUNCH_ANDROMINION, Menu.NONE, R.string.menu_launch_androminion);
            item.setIcon(R.drawable.ic_menu_androminion);
        }
        
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case MENU_CLEAR_ALL: {
			clearAll();
			return true;
		}
		case MENU_EXCLUDE_ALL: {
			excludeAll();
			return true;
		}
		case MENU_REQUIRE_ALL:
			requireAll();
			return true;
		case MENU_LAUNCH_ANDROMINION:
		    launchAndrominion();
		    return true;
		default:
			return super.onOptionsItemSelected(item);		
		}
	}

	private void requireAll() {
		getCardSelector().addRequiredCard(getCards());
		notifyDataSetChanged();
	}

	private void excludeAll() {
		CardSelector cardSelector = getCardSelector();
		cardSelector.addExcludedCard(getCards());
		cardSelector.removeRequiredCard(getCards());
		notifyDataSetChanged();
	}

	private void clearAll() {
		CardSelector cardSelector = getCardSelector();
		cardSelector.removeExcludedCard(getCards());
		cardSelector.removeRequiredCard(getCards());
		notifyDataSetChanged();
	}
	
	private void launchAndrominion() {
	    try {
    	    if(cards != null && cards.size() > 0) {
    	        Intent intent = getAndrominionIntentIfAvailable(this);
        	    ArrayList<String> cardNames = new ArrayList<String>();
        	    for(Card card : cards) {
        	        StringBuilder sb = new StringBuilder(); 
                    if(getResult() != null && getResult().getBaneCard() != null && getResult().getBaneCard().equals(card)) {
                        sb.append("bane+");
                    }
        	        for(char c : card.getName().toCharArray()) {
        	            if(Character.isLetterOrDigit(c)) {
        	                sb.append(c);
        	            }
        	        }
        	        cardNames.add(sb.toString());
        	    }
        	    intent.putExtra("cards", cardNames.toArray(new String[cardNames.size()]));
        	    startActivity(intent);
    	    }
    	}
    	catch(Exception e) {
    	    e.printStackTrace();
    	}
	}

	private void notifyDataSetChanged() {
		CardAdapter cardAdapter = (CardAdapter) getListAdapter();
		cardAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		dataReader.saveCardSelectorState(this, getCardSelector());
		((Application) getApplication()).saveResult();

		//wakeLock.release();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		//wakeLock.acquire(screenTimeout);
	}
	
	private CardSelector getCardSelector() {
		CardAdapter adapter = (CardAdapter) getListAdapter();
		return adapter.getCardSelector();
	}
	
	private OnItemClickListener onItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> item, View view, int position, long id) {
			CardAdapter adapter = (CardAdapter) getListAdapter();
			CardSelector cardSelector = adapter.getCardSelector();
			
			Card card = adapter.getItem(position);
			cardSelector.cycleRequireExclude(card);

			CardOrGroupHolder holder = (CardOrGroupHolder) view.getTag();
			holder.setCheckBox2(cardSelector, card);
		}
	};
	
    private Intent getAndrominionIntentIfAvailable(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        // Look for the test version of Androminion
        intent.setComponent(new ComponentName("com.ridgelineapps.testandrominion","com.mehtank.androminion.Androminion"));
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, 0);
        if(list.size() > 0) {
            return intent;
        }
        
        // Test version does exist not there, look for the release version 
        intent.setComponent(new ComponentName("com.mehtank.androminion","com.mehtank.androminion.Androminion"));
        list = packageManager.queryIntentActivities(intent, 0);
        if(list.size() > 0) {
            return intent;
        }
        
        return null;
    }
}
