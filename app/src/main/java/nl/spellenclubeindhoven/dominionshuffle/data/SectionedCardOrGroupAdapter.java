package nl.spellenclubeindhoven.dominionshuffle.data;

import java.util.ArrayList;
import java.util.List;

import nl.spellenclubeindhoven.dominionshuffle.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

/**
 * Abstract class intended to be extended by the InexAdapter and the ConstraintsAdapter.  Basically, it's an
 * adapter of Cards or Groups that shows sections for the Cards.  The sections vary based on the sorting, but 
 * the default is the card set, e.g. "Hinterlands," "Intrigue," etc.
 * @author nolan
 *
 */
public abstract class SectionedCardOrGroupAdapter extends ArrayAdapter<Object> implements SectionIndexer {

	
	private List<Object> objects;
	
	private CardComparator cardComparator;
	protected CardSelector cardSelector;
	protected LayoutInflater inflater;
	private SectionIndexer sectionIndexer;
	
	public SectionedCardOrGroupAdapter(Context context, int layoutId, List<Object> list,
			CardSelector cardSelector, CardComparator cardComparator) {
		super(context, R.layout.inex_listitem, list);
		this.cardSelector = cardSelector;
		this.objects = list;
		this.cardComparator = cardComparator;
		inflater = LayoutInflater.from(context);
	}
	
	public void setCardComparator(CardComparator comparator) {
		this.cardComparator = comparator;
		
	}
	

	@Override
	public int getPositionForSection(int section) {
		return getSectionIndexer().getPositionForSection(section);
	}

	@Override
	public int getSectionForPosition(int position) {
		return getSectionIndexer().getSectionForPosition(position);
	}

	@Override
	public Object[] getSections() {
		return getSectionIndexer().getSections();
	}
	private SectionIndexer getSectionIndexer() {
		if (sectionIndexer == null) {
			sectionIndexer = createSectionIndexer();
		}
		return sectionIndexer;
	}
	
	/**
	 * Create a SectionIndexer to have named sections for the cards, corresponding to their base set,
	 * e.g. Hinterlands, Intrigue, etc.  Or use another method if there's a different comparator being
	 * used for sorting.
	 * @return
	 */
	private SectionIndexer createSectionIndexer() {
	
		List<String> sections = new ArrayList<String>();
		final List<Integer> sectionsToPositions = new ArrayList<Integer>();
		final List<Integer> positionsToSections = new ArrayList<Integer>();
		
		
		// assume the cards are properly sorted
		for (int i = 0; i < objects.size(); i++) {
			Object object = objects.get(i);
			
			// if it's a card, use the card set - otherwise there's no section
			String section = getSectionTitle(object);
			if (sections.isEmpty() || !sections.get(sections.size() - 1).equals(section)) {
				// add a new section
				sections.add(section);
				// map section to position
				sectionsToPositions.add(i);
			}
			
			// map position to section
			positionsToSections.add(sections.size() - 1);
		}
		
		final String[] sectionsArray = sections.toArray(new String[sections.size()]);
		
		return new SectionIndexer() {
			
			@Override
			public Object[] getSections() {
				return sectionsArray;
			}
			
			@Override
			public int getSectionForPosition(int position) {
				return positionsToSections.get(position);
			}
			
			@Override
			public int getPositionForSection(int section) {
				return sectionsToPositions.get(section);
			}
		};
	}

	/**
	 * Determine which section title to use, depending on the current sorting
	 * @param object
	 * @return
	 */
	private String getSectionTitle(Object object) {
		if (object instanceof Group) {
			return ""; // no section title for this part
		}
		
		Card card = (Card)object;
		
		switch (cardComparator.getSort()) {
		case CardComparator.SORT_COST_SET_NAME:
		case CardComparator.SORT_COST_NAME:
			// sections are the cost
			return card.getCost();
		case CardComparator.SORT_NAME:
			// sections are first character, upper case
			return Character.toString(Character.toUpperCase(card.getName().charAt(0)));
		case CardComparator.SORT_SET_COST_NAME:
		case CardComparator.SORT_SET_NAME:
		default:
			// sections are the set
			return card.getSet();
		}
	}

	/**
	 * Call this if the sections change.
	 */
	public void refreshSections() {
		sectionIndexer = null;
		getSectionIndexer();
	}		
}
