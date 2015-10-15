import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import edu.psu.ist.vaccine.geotxt.utils.Config;

import edu.psu.ist.vaccine.geotxt.api.GeoTxtApi;

public class GeoLocation {

	/**
	 * @param args
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws IllegalArgumentException
	 * @throws ParseException
	 */

	public static String getGeoInfo(String placeName, GeoTxtApi geoTxtApi,
			JSONParser parser, boolean formatOutput)
			throws IllegalArgumentException, URISyntaxException, IOException,
			ParseException {

		String response = "";
		System.out.println(placeName);

		String geoCodedString = geoTxtApi.geoCodeToGeoJson(placeName,
				"landmarkGeocoder", false, 0, true, true);
		System.out.println(geoCodedString);
		Object geoCodedObj = parser.parse(geoCodedString);

		JSONObject geoCodedJson = (JSONObject) geoCodedObj;

		JSONArray features = (JSONArray) geoCodedJson.get("features");

		JSONObject feature = (JSONObject) features.get(0);

		JSONObject properties = (JSONObject) feature.get("properties");

		Long geoNameId = (Long) properties.get("geoNameId");

		String fCode = (String) properties.get("featureCode");

		if (geoNameId == null) {
			return "No toponym found";
		}

		String toponym = (String) properties.get("toponym");

		if (formatOutput) {
			response = "<b>Toponym</b>: " + toponym;
		} else {
			response = toponym;
		}

		JSONObject hierarchyFc = (JSONObject) properties.get("hierarchy");
		JSONArray hierarchyArray = (JSONArray) hierarchyFc.get("features");

		Iterator<JSONObject> iterator = hierarchyArray.iterator();

		String hierarchy = "";

		while (iterator.hasNext()) {

			JSONObject hFeature = (JSONObject) iterator.next();

			JSONObject hProperties = (JSONObject) hFeature.get("properties");

			String hToponym = (String) hProperties.get("toponym");

			hierarchy += hToponym + ", ";

		}

		if (hierarchy.length() > 0) {
			hierarchy = hierarchy.substring(0, hierarchy.length() - 2);
		}

		response += ", " + hierarchy;

		if (formatOutput) {

			response += " <b> FeatureCode: </b> " + fCode;

			response += "  <a target=\"_blank\" href=\"http://www.geonames.org/"
					+ geoNameId.toString()
					+ "\">See on GeoNames</a> or <a target=\"_blank\" href=\"http://api.geonames.org/get?geonameId="
					+ geoNameId.toString()
					+ "&username=demo&style=full\">Check ID "
					+ geoNameId
					+ "</a>";
		} else {

			response += " - " + fCode;
			response += " " + geoNameId.toString();
		}

		return response;
	}

	public static String getCandidates(String placeName,
			boolean includeAlternates, GeoTxtApi geoTxtApi, JSONParser parser)
			throws IllegalArgumentException, URISyntaxException, IOException,
			ParseException {

		String response = "";

		String geoCodedString = geoTxtApi.geoCodeToGeoJson(placeName,
				"landmarkGeocoder", includeAlternates, 100, true, true);

		return geoCodedString;

	}

	public static void main(String[] args) throws FileNotFoundException,
			IOException, IllegalArgumentException, URISyntaxException,
			ParseException {
		// TODO Auto-generated method stub
		Config config = new Config();

		GeoTxtApi geoTxtApi = new GeoTxtApi(config.getGate_home(),
				config.getStanford_ner());

		JSONParser jsonParser = new JSONParser();

		System.out.println(GeoLocation.getGeoInfo("London", geoTxtApi,
				jsonParser, true));

	}

}
