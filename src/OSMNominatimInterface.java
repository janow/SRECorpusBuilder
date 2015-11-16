import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;


public class OSMNominatimInterface {

	public static class OSMResult {
		String placeID = "???";
		String osmType = "???";
		String bb1 = "???";
		String bb2 = "???";
		String bb3 = "???";
		String bb4 = "???";
		String geometry = "???";
		String plat = "???";
		String plon = "???";
		String dname = "???";
	}
	
	static String readDocument(String queryString) throws MalformedURLException, IOException {
		String res = "";

		URL url = new URL(queryString);
		HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
		urlConn.addRequestProperty("User-Agent", "Mozilla/4.0");

		//String contentType = urlConn.getContentType();

		InputStream is = urlConn.getInputStream();
		BufferedReader br=new BufferedReader(new InputStreamReader(is));
		String temp;
		while ((temp=br.readLine())!=null) res += temp + "\n";

		br.close();

		return res;
	}

	
	static OSMResult getGeometry(String name, double lat, double lon) {
		OSMResult res = new OSMResult();
		String query = ("http://nominatim.openstreetmap.org/search?q="+name+"&format=xml&polygon_geojson=1").replaceAll(" ","+");
		System.out.println(query);
		boolean found = false;
		try {
			String docor = readDocument(query);
			//System.out.println(docor);

			
			Matcher m = Pattern.compile("<place place_id=\'.*?\' osm_type=\'(.*?)\' osm_id=\'(.*?)\' place_rank=\'.*?\' boundingbox=\"(.*?),(.*?),(.*?),(.*?)\" geojson=\'(.*?)\' lat=\'(.*?)\' lon=\'(.*?)\' display_name=\'(.*?)\'").matcher(docor);
			while (m.find()) {
				res.placeID = docor.substring(m.start(2),m.end(2));
				res.osmType = docor.substring(m.start(1),m.end(1));
				res.bb1 = docor.substring(m.start(3),m.end(3));
				res.bb2 = docor.substring(m.start(4),m.end(4));
				res.bb3 = docor.substring(m.start(5),m.end(5));
				res.bb4 = docor.substring(m.start(6),m.end(6));
				res.geometry = docor.substring(m.start(7),m.end(7));
				res.plat = docor.substring(m.start(8),m.end(8));
				res.plon = docor.substring(m.start(9),m.end(9));
				res.dname = docor.substring(m.start(10),m.end(10));
				System.out.println(res.dname+"["+res.osmType+": "+res.placeID+"] ("+res.plat+","+res.plon+";"+res.bb1+","+res.bb2+","+res.bb3+","+res.bb4+"] "+res.geometry);
				
				if (lat > Double.parseDouble(res.bb1) && lat < Double.parseDouble(res.bb2) && lon > Double.parseDouble(res.bb3) && lon < Double.parseDouble(res.bb4)) {
					System.out.println("does fit");
					found = true;
					break;
				} else {
					System.out.println("no fit");
				}
				
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (found)		return res;
		return new OSMResult();
	}
	
	
	public static void complement(String inFile, String outputFile) {

		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(inFile));
			CSVWriter bw = new CSVWriter(new FileWriter(outputFile), ',');

			List myEntries = reader.readAll();
			
			for (int i = 0; i < myEntries.size(); i++) {
				String[] row = (String[])myEntries.get(i);

				String[] nr = new String[row.length+5];
				for (int j = 0; j < row.length; j++ ) nr[j] = row[j];

				if (i == 0) {

					nr[row.length] = "OSM name";
					nr[row.length+1] = "OSM place ID";
					nr[row.length+2] = "OSM type";
					nr[row.length+3] = "OSM geometry";
					nr[row.length+4] = "OSM url";

				} else {
					OSMResult res = getGeometry(row[7], Double.parseDouble(row[8]), Double.parseDouble(row[9])); 
					
					nr[row.length] = res.dname;
					nr[row.length+1] = res.placeID;
					nr[row.length+2] = res.osmType;
					nr[row.length+3] = res.geometry;
					nr[row.length+4] = "http://www.openstreetmap.org/"+res.osmType+"/"+res.placeID;
					
				} 
				bw.writeNext(nr);
			}


			reader.close();
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("file operation failed");
		}
	}
	
	public static final void main(String[] args) {
		complement("/Users/jow/Dropbox/RelationCorpus/corpus_without_duplicates manually filtered adapted.csv", "/Users/jow/Dropbox/RelationCorpus/corpus_without_duplicates manually filtered adapted complemented.csv");
		//getGeometry("Eiffel tower",48.85815,2.29452);
	}
}
