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
		if (row[11].equals("No toponym found") || row[15].equals("No toponym found")) return true;
		
		return Utility.computeDistanceFromJSON(row[13], row[17], jsonParser) > distThreshold;
	}
}
