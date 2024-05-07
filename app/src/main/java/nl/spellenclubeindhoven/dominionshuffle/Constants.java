package nl.spellenclubeindhoven.dominionshuffle;

/**
 * Created by gynn.rickerby on 10/11/2015.
 */
public final class Constants {

    public final static int DEFAULT_CARDS_TO_DRAW = 10;
    public final static int VERSION_ALLIES = 78;

    public final static String CARD_COLONY = "Colony";
    public final static String CARD_OBELISK = "Obelisk";
    public final static String CARD_FERRYMAN = "Ferryman";
    public final static String CARD_PLATINUM = "Platinum";
    public final static String CARD_SHELTER = "Shelter";
    public final static String CARD_YOUNG_WITCH = "Young_Witch";
    public static final String DESCRIPTION_SUFFIX = "_Description";
    public final static String GROUP_ALL = "All";
    public final static String GROUP_ALLIES = "Ally_Cards";
    public final static String GROUP_COST_2 = "Cost_2";
    public final static String GROUP_COST_3 = "Cost_3";
    public final static String GROUP_COST_4 = "Cost_4";
    public final static String GROUP_DARK_AGES = "Dark_Ages";
    public final static String GROUP_EVENTS_LANDMARKS_PROJECTS_WAYS_TRAITS = "Events_Landmarks_Projects_Ways_Traits";
    public final static String GROUP_LIAISONS = "Liaisons";
    public final static String GROUP_PROSPERITY_FIRST = "Prosperity_First";
    public final static String GROUP_PROSPERITY_SECOND = "Prosperity_Second";
    public final static String GROUP_WAYS = "Ways";
    public static final String STRING = "string";
    public final static String TYPE_ACTION = "Action";
    public final static String TYPE_TREASURE = "Treasure";
    public final static String TYPE_ALLY = "Ally";
    public final static String TYPE_EVENT = "Event";
    public final static String TYPE_LANDMARK = "Landmark";
    public final static String TYPE_LIAISON = "Liaison";
    public final static String TYPE_PROJECT = "Project";
    public final static String TYPE_WAY = "Way";
    public final static String TYPE_TRAIT = "Trait";

    /**
     * Private Constructor to prevent this class ever being instantiated.
     */
    private Constants() {
        throw new AssertionError();
    }
}
