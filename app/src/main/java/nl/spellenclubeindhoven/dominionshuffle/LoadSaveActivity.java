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

import nl.spellenclubeindhoven.dominionshuffle.data.CardSelector;
import nl.spellenclubeindhoven.dominionshuffle.data.Data;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class LoadSaveActivity extends ListActivity {
	private static final int NUM_OF_SLOTS = 10;
	private static final int DIALOG_SLOTNAME = 1;
	private boolean isLoadDialog;
	private View slotNameView;
	private EditText slotNameEditText;
	private String slotName = null;
	private ArrayAdapter<Slot> adapter;
	private int selectedSlot;
	
	private class Slot {
		public String name;

		public Slot() {			
		}
		
		public Slot(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			if(name == null)
				return getString(R.string.empty);
			else
				return name;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		adapter = new ArrayAdapter<Slot>(this, android.R.layout.simple_list_item_1, getSlotList());
		setListAdapter(adapter);
		
		isLoadDialog = getIntent().getBooleanExtra("load", true);
		
		if(isLoadDialog) {
			setTitle(R.string.load_dialog_title);
			addLoadClickListener();
		}
		else {
			setTitle(R.string.save_dialog_title);
			addSaveClickListener();
		}
		
		if(savedInstanceState != null) {
			if(savedInstanceState.containsKey("slotNameEditText")) {
				getSlotNameEditText().onRestoreInstanceState(savedInstanceState.getParcelable("slotNameEditText"));
			}
			
			selectedSlot = savedInstanceState.getInt("selectedSlot", -1);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_SLOTNAME: {
			initializeDialogSlotName();
			return new AlertDialog.Builder(this)
				.setTitle(R.string.save_rules_as)
				.setPositiveButton(R.string.save, saveDialogButtonClicked)
				.setNegativeButton(R.string.cancel, null)
				.setView(getSlotNameView())
				.create();
		}
		default:
			return super.onCreateDialog(id);
		}		
	}

	private void initializeDialogSlotName() {
		getSlotNameEditText().setText(slotName);
		getSlotNameEditText().selectAll();
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id) {
		case DIALOG_SLOTNAME:
			initializeDialogSlotName();
		default:
			super.onPrepareDialog(id, dialog);
		}		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if(slotNameEditText != null) {
			outState.putParcelable("slotNameEditText", slotNameEditText.onSaveInstanceState());			
		}
		
		outState.putInt("selectedSlot", selectedSlot);
	}

	private EditText getSlotNameEditText() {
		if(slotNameEditText != null) return slotNameEditText;		
			
		View slotNameView = getSlotNameView();
		slotNameEditText = (EditText) slotNameView.findViewById(R.id.edit);
		
		return slotNameEditText;
	}
	
	private View getSlotNameView() {
		if(slotNameView != null) return slotNameView;
		
		LayoutInflater inflater = LayoutInflater.from(this);
		
		slotNameView = inflater.inflate(R.layout.edit_dialog, null);
		
		return slotNameView;
	}
	
	public void showSlotNameDialog(Slot slot) {
		if(slot.name == null) 
			this.slotName = getString(R.string.unnamed);
		else
			this.slotName = slot.name;
		
		showDialog(DIALOG_SLOTNAME);
	}

	private void addSaveClickListener() {
		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectedSlot = position;
				showSlotNameDialog(adapter.getItem(position));
			}
		});
	}

	private void addLoadClickListener() {
		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Slot slot = adapter.getItem(position);
				
				if(slot.name == null) {
					Toast.makeText(LoadSaveActivity.this, R.string.toast_no_data_found, Toast.LENGTH_SHORT).show();					
				}
				else {				
					loadSlot(position, getCardSelector());
					Toast.makeText(LoadSaveActivity.this, String.format(getString(R.string.toast_loaded_rules), slot.name), Toast.LENGTH_SHORT).show();
					finish();
				}
			}
		});
	}
	
	public CardSelector getCardSelector() {
		return ((Application) getApplication()).getCardSelector(); 
	}
	
	public Slot[] getSlotList() {
		Slot[] result = new Slot[NUM_OF_SLOTS];
		int slotCount = 0; 
		
		try {
			JSONArray jsonArray = new JSONArray(DataReader.readStringFromFile(this, "slots.json"));
			for(int i = 0; i < jsonArray.length() && i < NUM_OF_SLOTS; i++) {
				if(jsonArray.isNull(i))
					result[i] = new Slot();
				else
					result[i] = new Slot(jsonArray.getString(i));
				slotCount++;
			}			
		} catch (JSONException ignore) {
		}
		
		for(int i = slotCount; i < NUM_OF_SLOTS; i++) {
			result[i] = new Slot();
		}
				
		return result;
	}
	
	public boolean saveSlot(int number, String name, CardSelector cardSelector) {
		try {
			boolean result = DataReader.writeStringToFile(this, String.format("slot%d.json", number), getCardSelector().toJson());
			if(result == false) {
				return false;
			}
		} catch (JSONException ignore) {
			return false;
		}
		
		// Save slotname (if saving the selection worked)
		adapter.getItem(number).name = name;		
		JSONArray jsonArray = new JSONArray();
		for(int i = 0; i < adapter.getCount(); i++) {
			jsonArray.put(adapter.getItem(i).name);
		}
		
		return DataReader.writeStringToFile(this, "slots.json", jsonArray.toString());
	}
	
	public void loadSlot(int number, CardSelector cardSelector) {
		Data data = ((Application) getApplication()).getDataReader().getData();
		try {
			getCardSelector().fromJson(DataReader.readStringFromFile(this, String.format("slot%d.json", number)), data);
		} catch (JSONException ignore) {
		}
	}
	
	private OnClickListener saveDialogButtonClicked = new OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {	
			String slotName = slotNameEditText.getText().toString();
			if(saveSlot(selectedSlot, slotName, getCardSelector())) {
				Toast.makeText(LoadSaveActivity.this, String.format(getString(R.string.toast_saved_rules_as), slotName), Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(LoadSaveActivity.this, R.string.toast_error_saving_rules, Toast.LENGTH_SHORT).show();
			}
			finish();
		}
	};
}
