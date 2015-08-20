import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class DifferentAdmin2Filter implements Filter {

	JSONParser jsonParser = new JSONParser();
	
	@Override
	public boolean doFilter(String[] row) {
		if (row[11].equals("No toponym found") || row[14].equals("No toponym found")) return true;
		
		
		Object geoCodedObj1 = null;
		Object geoCodedObj2 = null;
		try {
			geoCodedObj1 = jsonParser.parse(row[12]);
			geoCodedObj2 = jsonParser.parse(row[15]);
		} catch (ParseException e) {
			System.out.println("Parse exception in SameAdmin2Filter...");
			e.printStackTrace();
			System.exit(1);
		}
		
		JSONObject feature1 =  (JSONObject) ((JSONArray) ((JSONObject)geoCodedObj1).get("features")).get(0);
		JSONObject feature2 =  (JSONObject) ((JSONArray) ((JSONObject)geoCodedObj2).get("features")).get(0);

		JSONObject properties1 = (JSONObject) feature1.get("properties");
		JSONObject properties2 = (JSONObject) feature2.get("properties");

		Long id1 = (Long) properties1.get("admin2geonameid");
		Long id2 = (Long) properties2.get("admin2geonameid");
		
		return id1 != id2;
	}

}
