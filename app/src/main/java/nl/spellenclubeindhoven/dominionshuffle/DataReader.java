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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import nl.spellenclubeindhoven.dominionshuffle.data.CardSelector;
import nl.spellenclubeindhoven.dominionshuffle.data.Data;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class DataReader {
	private Data dominionData;
	private Application application;
	

	public DataReader(Application application) {
		this.application = application;
	}

	public boolean loadData() {
		if(dominionData != null) return false;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
		String langCode = prefs.getString("lang", "en");
		String resourceName = langCode.equals("en") ? "data" : "data_" + langCode;
		int resourceId = application.getResources().getIdentifier(resourceName, "raw", application.getPackageName());

		try {
			InputStream inputStream = application.getResources().openRawResource(resourceId);
			dominionData = Data.read(readStringFromStream(inputStream));
		} catch (Exception ignore) {
			ignore.printStackTrace();
			return false;
		}
		
		return true;
	}

	public void reset() {
		dominionData = null;
	}

	public Data getData() {
		return dominionData;
	}
	
	public void saveCardSelectorState(Context context, CardSelector cardSelector) {
		if (cardSelector == null)
			return;

		try {
			writeStringToFile(context, "card_selector.json", cardSelector
					.toJson());
		} catch (JSONException ignore) {
		}
	}

	public static String readStringFromStream(InputStream inputStream) {
		InputStreamReader in = null;
		try {
			in = new InputStreamReader(inputStream);
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[1024];
			int numread = 0;

			numread = in.read(buffer, 0, 1024);
			while (numread != -1) {
				builder.append(buffer, 0, numread);
				numread = in.read(buffer, 0, 1024);
			}

			return builder.toString();
		} catch (IOException ignore) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignore) {
				}
			}
		}

		return "";
	}
	
	public static String readStringFromFile(Context context, String filename) {
		try {
			return readStringFromStream(context.openFileInput(filename));
		} catch (FileNotFoundException ignore) {
			ignore.printStackTrace();
		}

		return "";
	}

	public static boolean writeStringToFile(Context context, String filename,
			String content) {
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(context.openFileOutput(filename,
					Activity.MODE_PRIVATE));
			out.write(content);
		} catch (FileNotFoundException ignore) {
			return false;
		} catch (IOException ignore) {
			return false;
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException ignore) {
				return false;
			}
		}

		return true;
	}
}
