import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Utility {
	public static double computeDist(double lat1, double lng1, double lat2,
			double lng2) {
		double earthRadius = 6371; // kilometers
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2)
				* Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = earthRadius * c;

		return dist;
	}

	public static double computeDistanceFromJSON(String json1, String json2,
			JSONParser jsonParser) {

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

		JSONObject feature1 = (JSONObject) ((JSONArray) ((JSONObject) geoCodedObj1)
				.get("features")).get(0);
		JSONObject feature2 = (JSONObject) ((JSONArray) ((JSONObject) geoCodedObj2)
				.get("features")).get(0);

		JSONObject properties1 = (JSONObject) feature1.get("geometry");
		JSONObject properties2 = (JSONObject) feature2.get("geometry");

		JSONArray id1 = (JSONArray) properties1.get("coordinates");
		JSONArray id2 = (JSONArray) properties2.get("coordinates");

		return Utility.computeDist((Double) id1.get(0), (Double) id1.get(1),
				(Double) id2.get(0), (Double) id2.get(1));
	}

	public static boolean computeEditDistanceFromJSON(String np, String gJson,
			double threshold, boolean considerAlternates, JSONParser jsonParser) {

		Object geoCodedObj = null;

		try {
			geoCodedObj = jsonParser.parse(gJson);
		} catch (ParseException e) {
			System.out.println("Parse exception...");
			e.printStackTrace();
			System.exit(1);
		}

		JSONObject feature = (JSONObject) ((JSONArray) ((JSONObject) geoCodedObj)
				.get("features")).get(0);

		JSONObject properties = (JSONObject) feature.get("properties");
		

		String toponym = (String) properties.get("toponym");
		
		ArrayList<Double> distance = new ArrayList<Double>();
				
		if (considerAlternates == true){
			JSONArray alternateNamesJsonArray = (JSONArray) feature.get("alternateNames");

		}

		//TODO alternate names and the toponym are given the same weight. 
		
		return (minEditDistance(np, toponym) / Math.max(np.length(),
				toponym.length())) > threshold;
				
				
	}

	public static double minEditDistance(String word1, String word2) {
		int len1 = word1.length();
		int len2 = word2.length();

		// len1+1, len2+1, because finally return dp[len1][len2]
		int[][] dp = new int[len1 + 1][len2 + 1];

		for (int i = 0; i <= len1; i++) {
			dp[i][0] = i;
		}

		for (int j = 0; j <= len2; j++) {
			dp[0][j] = j;
		}

		// iterate though, and check last char
		for (int i = 0; i < len1; i++) {
			char c1 = word1.charAt(i);
			for (int j = 0; j < len2; j++) {
				char c2 = word2.charAt(j);

				// if last two chars equal
				if (c1 == c2) {
					// update dp value for +1 length
					dp[i + 1][j + 1] = dp[i][j];
				} else {
					int replace = dp[i][j] + 1;
					int insert = dp[i][j + 1] + 1;
					int delete = dp[i + 1][j] + 1;

					int min = replace > insert ? insert : replace;
					min = delete > min ? min : delete;
					dp[i + 1][j + 1] = min;
				}
			}
		}

		return dp[len1][len2];
	}

}
