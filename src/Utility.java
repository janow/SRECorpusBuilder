import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Utility {

	public static Logger log = Logger.getLogger("Utility");

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

	public static String[] getMinimumDistancePair(String[] line,
			JSONParser jsonParser) {

		String[] writingRows = new String[18];

		for (int i = 0; i < line.length; i++) {
			writingRows[i] = line[i];
		}

		Object geoCodedObj1 = null;
		Object geoCodedObj2 = null;
		if (!(line[11].equalsIgnoreCase("No toponym found") || line[14]
				.equalsIgnoreCase("No toponym found"))) {
			try {
				System.out.println(line[12]);
				geoCodedObj1 = jsonParser.parse(line[12]);
				geoCodedObj2 = jsonParser.parse(line[15]);
			} catch (ParseException e) {
				System.out.println("Parse exception ....");
				e.printStackTrace();
				System.exit(1);
			}

			JSONObject feature1 = (JSONObject) ((JSONArray) ((JSONObject) geoCodedObj1)
					.get("features")).get(0);
			JSONObject feature2 = (JSONObject) ((JSONArray) ((JSONObject) geoCodedObj2)
					.get("features")).get(0);

			JSONObject alternatesFeature1 = (JSONObject) ((JSONObject) feature1
					.get("properties")).get("alternates");

			JSONObject alternatesFeature2 = (JSONObject) ((JSONObject) feature2
					.get("properties")).get("alternates");

			JSONArray alternates1 = new JSONArray();
			JSONArray alternates2 = new JSONArray();

			boolean noAlternates[] = new boolean[2];

			if (alternatesFeature1 == null) {
				alternates1.add((JSONObject) geoCodedObj1);
				noAlternates[0] = true;
			} else {
				alternates1 = (JSONArray) alternatesFeature1.get("features");

			}
			if (alternatesFeature2 == null) {
				alternates2.add((JSONObject) geoCodedObj2);
				noAlternates[1] = true;
			} else {
				alternates2 = (JSONArray) alternatesFeature2.get("features");
			}

			Object[] alternates1ObjArray = alternates1.toArray();
			Object[] alternates2ObjArray = alternates2.toArray();

			// double [] distances = new
			// double[alternates1ObjArray.length*alternates2ObjArray.length];

			ArrayList<Double> distances = new ArrayList<Double>();

			JSONObject pickedFeature1 = new JSONObject();
			JSONObject pickedFeature2 = new JSONObject();
			

			for (int a1 = 0; a1 < alternates1ObjArray.length; a1++) {

				JSONObject al1 = (JSONObject) alternates1ObjArray[a1];
				JSONObject prop1 = (JSONObject) al1.get("properties");
				Long geoNameId1 = (Long) prop1.get("geoNameId");
				JSONObject geom1 = (JSONObject) al1.get("geometry");
				JSONArray coo1 = (JSONArray) geom1.get("coordinates");

				for (int a2 = 0; a2 < alternates2ObjArray.length; a2++) {

					JSONObject al2 = (JSONObject) alternates2ObjArray[a2];
					JSONObject prop2 = (JSONObject) al2.get("properties");
					Long geoNameId2 = (Long) prop2.get("geoNameId");
					JSONObject geom2 = (JSONObject) al2.get("geometry");
					JSONArray coo2 = (JSONArray) geom2.get("coordinates");

					// implicit double (from computeDist) to Double
					Double distance = Utility.computeDist((Double) coo1.get(0),
							(Double) coo1.get(1), (Double) coo2.get(0),
							(Double) coo2.get(1));

					Double minDist;

					if (!distances.isEmpty()) {
						minDist = Collections.min(distances);
					} else {
						minDist = distance;
					}

					if (distance < minDist || distances.size() == 1) {
						pickedFeature1 = al1;
						pickedFeature2 = al2;
					}

					// implicit double to Double
					distances.add(distance);

				}
			}

			JSONObject pickedFeature1Json = new JSONObject();
			pickedFeature1Json.put("type", "FeatureCollection");
			JSONArray features1Array = new JSONArray();
			features1Array.add(pickedFeature1);
			pickedFeature1Json.put("features", features1Array);
			writingRows[16] = pickedFeature1Json.toJSONString();

			JSONObject pickedFeature2Json = new JSONObject();
			pickedFeature2Json.put("type", "FeatureCollection");
			JSONArray features2Array = new JSONArray();
			features2Array.add(pickedFeature2);
			pickedFeature2Json.put("features", features2Array);
			writingRows[17] = pickedFeature1Json.toJSONString();

		}

		return writingRows;
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

		if (considerAlternates == true) {
			JSONArray alternateNamesJsonArray = (JSONArray) feature
					.get("alternateNames");

		}

		// TODO alternate names and the toponym are given the same weight.

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
