package org.rifidi.emulator.reader.thingmagic.tagbuffer;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rifidi.emulator.reader.thingmagic.database.IDBRow;
import org.rifidi.services.tags.impl.RifidiTag;


/*
 * We use AbstractMap here since we are defining a key-value pair of tag data and to keep from
 * re-inventing the wheel with Map collections.
 */
//TODO implement this better.
public class TagRowData extends AbstractMap<String, String> implements IDBRow {
	private static Log logger = LogFactory.getLog(ThingMagicTagTableMemory.class);
	
	private RifidiTag tag;

	private Set<Map.Entry<String, String>> tagData = new HashSet<Map.Entry<String, String>>();
	
	/**
	 * This creates a map of tag data keys (based on the way the Mercury 4, ThingMagic reader
	 * defines them) and their respective values.
	 * Note: Must call updateTagData() before these key value pairs are created.
	 * @param tag The RFID tag to get the tag data from
	 */
	public TagRowData(RifidiTag tag){
		logger.debug("Creating tag row data");
		this.tag = tag;
		//updateTagData();
	}
	
	public void updateTagData(){
		tagData.clear();
		/*
		 * the great thing about doing it this way all we have to do is add our key-value pair into
		 * the Set and let the Java Framework do the rest.
		 */
		tagData.add( new AbstractMap.SimpleEntry<String, String>( "id", "0x" + tag.toString().replace(" ", "")) );
		tagData.add( new AbstractMap.SimpleEntry<String, String>("antenna_id", tag.getAntennaLastSeen().toString()) );
		tagData.add( new AbstractMap.SimpleEntry<String, String>("read_count", tag.getReadCount().toString()) );
		tagData.add( new AbstractMap.SimpleEntry<String, String>("protocol_id", "[unimplimented]") );
		tagData.add( new AbstractMap.SimpleEntry<String, String>("killed", "[unimplimented]") );
		tagData.add( new AbstractMap.SimpleEntry<String, String>("password", "[unimplimented]") );
		tagData.add( new AbstractMap.SimpleEntry<String, String>("locked", "[unimplimented]") );
		tagData.add( new AbstractMap.SimpleEntry<String, String>("frequency", "[unimplimented]") );
		tagData.add( new AbstractMap.SimpleEntry<String, String>("dspmicros", "[unimplimented]") );
		tagData.add( new AbstractMap.SimpleEntry<String, String>("timestamp", "[unimplimented]") );
		
	}
	
	@Override
	public Set<Map.Entry<String, String>> entrySet() {
		return tagData;
	}

	public RifidiTag getTag(){
		return this.tag;
	}
	
	/**
	 * Just calls hashCode on the internal tag.
	 * @return hashCode
	 */
	@Override
	public int hashCode(){
		return tag.hashCode();
	}
	
	/**
	 * Just calls equals on the two internal tags using the internal tags equals methods.
	 * Returns true if both TagRowData's internal RifidiTag objects are equal, false if not.
	 */
	@Override
	public boolean equals(Object object){
		if (object instanceof TagRowData){
			return tag.equals( ((TagRowData)object).getTag() );
		} else {
			return false;
		}
	}
}