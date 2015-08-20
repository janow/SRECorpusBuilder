import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Utility {
	public static double computeDist(double lat1, double lng1, double lat2, double lng2) {
	    double earthRadius = 6371; //kilometers
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	               Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    return dist;
	}
	
	public static double computeDistanceFromJSON(String json1, String json2, JSONParser jsonParser) {
		
		Object geoCodedObj1 = null;
		Object geoCodedObj2 = null;
		try {
			geoCodedObj1 = jsonParser.parse(json1);
			geoCodedObj2 = jsonParser.parse(json2);
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
		
		return Utility.computeDist((Double)id1.get(0),(Double)id1.get(1),(Double)id2.get(0),(Double)id2.get(1)) ;
	}
	
}
