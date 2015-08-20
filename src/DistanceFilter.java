import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class DistanceFilter implements Filter {

	JSONParser jsonParser = new JSONParser();
	int distThreshold;
	
	public DistanceFilter(int distance) {
		this.distThreshold = distance;
	}
	
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

		JSONObject properties1 = (JSONObject) feature1.get("geometry");
		JSONObject properties2 = (JSONObject) feature2.get("geometry");

		JSONArray id1 = (JSONArray) properties1.get("coordinates");
		JSONArray id2 = (JSONArray) properties2.get("coordinates");
		
		return Utility.computeDist((Double)id1.get(0),(Double)id1.get(1),(Double)id2.get(0),(Double)id2.get(1)) > distThreshold;
	}
	


}
