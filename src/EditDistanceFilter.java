import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class EditDistanceFilter implements Filter {

	JSONParser jsonParser = new JSONParser();
	double editDistThreshold;
	boolean considerAlternateNames;

	public EditDistanceFilter(double editDistThreshold,
			boolean considerAlternateNames) {
		this.editDistThreshold = editDistThreshold;
		this.considerAlternateNames = considerAlternateNames;
	}

	@Override
	public boolean doFilter(String[] row) {
		if (row[11].equals("No toponym found")
				|| row[14].equals("No toponym found"))
			return true;

	

		

//		return (((Utility.computeEditDistanceFromJSON(row[10], row[12],
//				jsonParser) / row[10].length()) < editDistThreshold) && ((Utility
//				.computeEditDistanceFromJSON(row[13], row[15], jsonParser) / row[13]
//				.length()) < editDistThreshold));
		
		return (((Utility.computeEditDistanceFromJSON(row[10], row[12],
				jsonParser)) < editDistThreshold) && ((Utility
				.computeEditDistanceFromJSON(row[13], row[15], jsonParser)) < editDistThreshold));

	}
}
