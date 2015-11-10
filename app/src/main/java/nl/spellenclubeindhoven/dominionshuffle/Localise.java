package nl.spellenclubeindhoven.dominionshuffle;

import android.content.Context;

/**
 * Created by gynn.rickerby on 10/11/2015.
 */
public class Localise {

    /**
     * Gets the Card Name from the Localisation Resources.<br/>
     * If it doesn't exist, use the internal card name. Not ideal, but it avoids causing an error.
     */
    public static String getCardName(String internalCardName, Context context) {
        return getString(internalCardName, context);
    }

    /**
     * Gets the Set Description from the Localisation Resources.<br/>
     * If it doesn't exist, use the internal set name. Not ideal, but it avoids causing an error.
     */
    public static String getSetDescription(String internalSetName, Context context) {
        return getString(internalSetName + Constants.DESCRIPTION_SUFFIX, context);
    }

    /**
     * Gets the Set Name from the Localisation Resources.<br/>
     * If it doesn't exist, use the internal set name. Not ideal, but it avoids causing an error.
     */
    public static String getSetName(String internalSetName, Context context) {
        return getString(internalSetName, context);
    }

    /**
     * Gets the Type Name from the Localisation Resources.<br/>
     * If it doesn't exist, use the internal type name. Not ideal, but it avoids causing an error.
     */
    public static String getTypeName(String internalTypeName, Context context) {
        return getString(internalTypeName, context);
    }

    /**
     * Gets the String from the Localisation Resources.<br/>
     * If it doesn't exist, use the passed in String. Not ideal, but it avoids causing an error.
     */
    public static String getString(String string, Context context) {
        int stringIdentifier = context.getResources().getIdentifier(string, Constants.STRING, context.getPackageName());
        String localisedString = string;
        if (stringIdentifier != 0) {
            localisedString = context.getResources().getString(stringIdentifier);
        }
        return localisedString;
    }
}
