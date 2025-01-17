import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.psu.ist.vaccine.analyzers.GateHierarchyAnalyzer;
import edu.psu.ist.vaccine.analyzers.StanfordHierarchyAnalyzer;
import edu.psu.ist.vaccine.corpusbuilding.CorpusExposerApi;
import edu.psu.ist.vaccine.geotxt.api.GeoTxtApi;
import edu.psu.ist.vaccine.geotxt.utils.Config;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class Test {

	public static final Logger log = Logger.getLogger(Test.class.getName());

	public static void produceHTMLfromCSV(String csvFileName,
			String outFileName, Filter[] filters) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFileName));
			bw.write("<!DOCTYPE html><html><head><link rel=\"stylesheet\" href=\"treeview.css\"></head><body>");
			CSVReader r = new CSVReader(new InputStreamReader(
					new FileInputStream(csvFileName), "UTF-8"));

			String[] row = null;
			int c = 0;
			int instances = 0;

			while ((row = r.readNext()) != null) {
				c++;
				System.out.println(c + ": " + row[0] + "," + row[1] + ","
						+ row[2] + "," + row[3]);

				// if a filter says row should be filtered out, we move on to
				// the next row
				boolean skip = false;
				for (Filter f : filters) {
					if (f.doFilter(row)) {
						skip = true;
						break;
					}
					;
				}
				if (skip)
					continue;

				instances++;

				int start = Integer.parseInt(row[4]);
				int end = Integer.parseInt(row[5]);
				String line = row[6];

				bw.write("<div><h2>" + instances + " - "
						+ line.substring(0, start) + "<u>"
						+ line.substring(start, end) + "</u>"
						+ line.substring(end) + "</h2>\n");

				bw.write("<p>pattern: " + row[7] + "</p>");

				bw.write("<div class=\"css-treeview\"><ul><li><input type=\"checkbox\" id=\"item-pt-\""
						+ c
						+ "\"/><label for=\"item-pt-\""
						+ c
						+ "\">Matching parse tree</label><ul><p><pre>"
						+ row[9]
						+ "</pre></p></ul></li></ul></div>\n");

				bw.write("<div class=\"css-treeview\"><ul><li><input type=\"checkbox\" id=\"item-ct-\""
						+ c
						+ "\"/><label for=\"item-ct-\""
						+ c
						+ "\">Complete parse tree</label><ul><p><pre>"
						+ row[8]
						+ "</pre></p></ul></li></ul></div>\n");

				if (row.length > 9) { // geocoding results included?

					bw.write("<p style=\"color: #009900\">np1: " + row[10]
							+ "</p>\n");

					bw.write("<p>toponym: " + row[11] + "</p>\n");

					bw.write("<div class=\"css-treeview\"><ul><li><input type=\"checkbox\" id=\"item-gc1-\""
							+ c
							+ "\"/><label for=\"item-gc1-\""
							+ c
							+ "\">GeoJSON np1</label><ul><p><pre style=\"white-space: pre-wrap\">"
							+ row[13] + "</pre></p></ul></li></ul></div>\n");

					bw.write("<p style=\"color: #000099\">np2: " + row[14]
							+ "</p>\n");

					bw.write("<p>toponym: " + row[15] + "</p>\n");

					bw.write("<div class=\"css-treeview\"><ul><li><input type=\"checkbox\" id=\"item-gc2-\""
							+ c
							+ "\"/><label for=\"item-gc2-\""
							+ c
							+ "\">GeoJSON np2</label><ul><p><pre style=\"white-space: pre-wrap\">"
							+ row[17] + "</pre></p></ul></li></ul></div>\n");

					JSONParser jsonParser = new JSONParser();
					bw.write("<p>distance: "
							+ Utility.computeDistanceFromJSON(row[13], row[17],
									jsonParser) + " km</p>");

				}

				bw.write("</div>\n");

			}

			r.close();
			bw.write("</body></html>");
			bw.close();

		} catch (Exception e) {
			log.info("file operation failed, could not read file");
			e.printStackTrace();
		}

	}

	public static void addDistanceColumn(String inCSVName, String outCSVName,
			Filter[] filters) {
		try {

			CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(
					outCSVName), ','));

			CSVReader r = new CSVReader(new InputStreamReader(
					new FileInputStream(inCSVName), "UTF-8"));

			JSONParser jsonParser = new JSONParser();

			int cc = 0;
			String[] rows;

			while ((rows = r.readNext()) != null) {

				// if a filter says row should be filtered out, we move on to
				// the next row
				boolean skip = false;
				for (Filter f : filters) {
					if (f.doFilter(rows)) {
						skip = true;
						break;
					}
					;
				}
				if (skip)
					continue;

				cc++;
				String[] writingRow = new String[19];
				for (int i = 0; i < rows.length; i++) {
					writingRow[i] = rows[i];
				}
				if (!rows[11].equals("No toponym found")
						&& !rows[15].equals("No toponym found"))
					writingRow[18] = ""
							+ Utility.computeDistanceFromJSON(rows[12],
									rows[15], jsonParser);
				else
					writingRow[18] = "";

				writer.writeNext(writingRow);

				log.info(Integer.toString(cc));
			}
			r.close();
			writer.close();

		} catch (Exception e) {
			log.info("file operation failed, could not read file");
			e.printStackTrace();
		}

	}

	public static void geocodeLine(String line, String[] writingRows,
			JSONParser jsonParser, GeoTxtApi geoTxtApi)
			throws IllegalArgumentException, URISyntaxException, IOException,
			ParseException {
		
	  writingRows[12] =  geoTxtApi.geoCodeToGeoJson(writingRows[10],
				"landmarkGeocoder", true, 100, true, true);

		writingRows[11] = GeoLocation.reWriteGeoInfo(writingRows[10], writingRows[12],
				jsonParser, true);

//		writingRows[12] = GeoLocation.getCandidates(writingRows[10], true,
//				geoTxtApi, jsonParser);
		
		
//		writingRows[16] = GeoLocation.getCandidates(writingRows[14], true,
//				geoTxtApi, jsonParser);
		
		writingRows[16] = geoTxtApi.geoCodeToGeoJson(writingRows[14],
				"landmarkGeocoder", true, 100, true, true);

		writingRows[15] = GeoLocation.reWriteGeoInfo (writingRows[14], writingRows[16],
				jsonParser, true);

		
	}

	public static List<String[]> parseLine(String line, String[] writingRows,
			TokenizerFactory<CoreLabel> tokenizerFactory, LexicalizedParser lp,
			JSONParser jsonParser) throws IllegalArgumentException,
			URISyntaxException, IOException, ParseException {
		ArrayList<String[]> result = new ArrayList<String[]>();

		// tokenize and produce parse tree
		Tokenizer<CoreLabel> tok = tokenizerFactory
				.getTokenizer(new StringReader(line));
		// Tokenizer<CoreLabel> tok =
		// tokenizerFactory.getTokenizer(new
		// StringReader("Hotel Jardin d'Eiffel is close to Eiffel tower"));
		// Tokenizer<CoreLabel> tok =
		// tokenizerFactory.getTokenizer(new
		// StringReader("Situated in Paris's Trocadero neighborhood, this hotel is close to Wine Museum, Eiffel Tower, and Arc de Triomphe"));

		List<CoreLabel> rawWords2 = tok.tokenize();

		Tree parse = lp.apply(rawWords2);

		// produce & print the parse tree

		// System.out.println("sentence: >"+line+"<");

		// TODO: parse should become a column in output csv
		// System.out.println("Parse results:\n"+parse.pennString());

		writingRows[8] = parse.pennString();

		// test tregex by checking for a pattern
		String s = "S < NP=np1 <+(VP) (ADVP < (RB < close) < (PP < (TO < to) <+(NP) (NP=np2 !< CC !> (NP !< CC))))";

		writingRows[7] = s;

		// String s = "S < NP=np1 < (VP < (ADJP < (JJ < close)))";

		// TODO: pattern should become a column in the output csv

		TregexPattern p = TregexPattern.compile(s);
		TregexMatcher m = p.matcher(parse);

		while (m.find()) {
			System.out.println("sentence: >" + line + "<");

			// TODO: matching parse tree should become a column in
			// the output csv
			// System.out.println("Parse results match:\n"+m.getMatch().pennString());

			writingRows[9] = m.getMatch().pennString();

			// System.out.println("geotxt: ");
			// String geocodeResults =
			// geoTxtApi.geoCodeToGeoJson(line, "stanfordh", false,
			// 0, false, true);
			// System.out.println(geocodeResults);
			// System.out.println("stanford extraction: ");
			// String sr =
			// StanfordHierarchyAnalyzer.st.tagAlltoGeoJson(line,
			// false, 0, false, true);
			// System.out.println(sr);
			// System.out.println("gate extraction: ");
			// sr = GateHierarchyAnalyzer.gate.tagAlltoGeoJson(line,
			// false, 0, false, true);
			// System.out.println(sr);
			Tree t = m.getNode("np1");
			System.out.println("np1:\n" + Sentence.listToString(t.yield()));

			// np1
			writingRows[10] = Sentence.listToString(t.yield());

			t = m.getNode("np2");
			System.out.println("np2:\n" + Sentence.listToString(t.yield()));

			writingRows[14] = Sentence.listToString(t.yield());

			// System.out.println("match:\n")
			m.getMatch().pennPrint();
			// System.out.println("\n");
			result.add(writingRows.clone());
		}
		return result;
	}

	public static void addParseResults(String filename, String outputCSVName)
			throws FileNotFoundException, IOException {

		LexicalizedParser lp = LexicalizedParser
				.loadModel("englishPCFG.ser.gz");
		TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(
				new CoreLabelTokenFactory(), "");

		JSONParser jsonParser = new JSONParser();

		try {
			CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(
					outputCSVName), ','));

			CSVReader r = new CSVReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));

			int cc = 0;

			String[] rows = r.readNext();
			String[] writingRows = new String[17];

			for (int i = 0; i < rows.length; i++) {
				writingRows[i] = rows[i];
			}

			String line = rows[6];
			while (line != null) {

				if (!line.replaceFirst("^[\\x00-\\x200\\xA0]+", "")
						.replaceFirst("[\\x00-\\x20\\xA0]+$", "").isEmpty()) {
					// System.out.println("here");
					// line =
					// "Timhotel Paris Boulogne is located in Boulogne-Billancourt, close to Pierre de Coubertin Stadium, Eiffel Tower, and Stade de Roland Garros.";
					List<String[]> result = parseLine(line, writingRows,
							tokenizerFactory, lp, jsonParser);

					for (String[] nr : result) {
						System.out.println("writing");
						writer.writeNext(nr);
					}

				}
				cc++;
				log.info(Integer.toString(cc) + " processed.");

				rows = r.readNext();

				try {
					int rowLength = rows.length;
				} catch (Exception e) {
					log.info("Could not read line.");
					r.close();
					writer.close();
				}

				writingRows = new String[17];
				for (int i = 0; i < rows.length; i++) {
					writingRows[i] = rows[i];
				}

				line = rows[6];
			}
			log.info(Integer.toString(cc));
			r.close();
			writer.close();

		} catch (Exception e) {
			log.info("file operation failed, could not read file");
			e.printStackTrace();
		}
	}
/*	
	public static void addGeometryInformation(String filename, String outputCSVName)
			throws FileNotFoundException, IOException {
	
		try {

			CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(
					outputCSVName), ','));

			CSVReader r = new CSVReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));

			int cc = 0;


		
			String[] nextLine;
			String [] writingRows;
			
			while ((nextLine = r.readNext()) != null) {

				if (!(nextLine[11].equalsIgnoreCase("No toponym found"))) {
					OSMNominatimInterface.OSMResult res = OSMNominatimInterface.getGeometry(nextLine[11], lat, lon) 
				}
				
			
			
			log.info(Integer.toString(cc));
			r.close();
			writer.close();

		} catch (Exception e) {
			log.info("file operation failed, could not read file");
			e.printStackTrace();
		}
	}
			*/
			
	public static void addGeocodeResults(String filename, String outputCSVName)
			throws FileNotFoundException, IOException {

		Config config = new Config(true);

		GeoTxtApi geoTxtApi = new GeoTxtApi(config.getGate_home(),
				config.getStanford_ner());
		JSONParser jsonParser = new JSONParser();

		try {

			CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(
					outputCSVName), ','));

			CSVReader r = new CSVReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));

			int cc = 0;

			String[] rows = r.readNext();
			String[] writingRows = new String[18];

			for (int i = 0; i < rows.length; i++) {
				writingRows[i] = rows[i];
			}

			String line = rows[6];
			while (line != null) {

				if (!line.replaceFirst("^[\\x00-\\x200\\xA0]+", "")
						.replaceFirst("[\\x00-\\x20\\xA0]+$", "").isEmpty()) {
					// line =
					// "Timhotel Paris Boulogne is located in Boulogne-Billancourt, close to Pierre de Coubertin Stadium, Eiffel Tower, and Stade de Roland Garros.";
					geocodeLine(line, writingRows, jsonParser, geoTxtApi);

					writer.writeNext(writingRows);
				}
				cc++;
				log.info(Integer.toString(cc) + " processed.");

				rows = r.readNext();

				try {
					int rowLength = rows.length;
				} catch (Exception e) {
					log.info("Could not read line.");
					r.close();
					writer.close();
				}

				writingRows = new String[18];
				for (int i = 0; i < rows.length; i++) {
					writingRows[i] = rows[i];
				}

				line = rows[6];
			}
			log.info(Integer.toString(cc));
			r.close();
			writer.close();

		} catch (Exception e) {
			log.info("file operation failed, could not read file");
			e.printStackTrace();
		}
	}

	public static void getSpatiallyClosestPair(String filename, String outputCSVName)
			throws FileNotFoundException, IOException {

		Config config = new Config(true);

		JSONParser jsonParser = new JSONParser();

		try {

			CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(
					outputCSVName), ','));

			CSVReader r = new CSVReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));

			int cc = 0;

			String[] nextLine;
			String [] writingRows;
			
			while ((nextLine = r.readNext()) != null) {

				writingRows = Utility.getMinimumDistancePair(nextLine, jsonParser);

				writer.writeNext(writingRows);
				
				cc++;
				
				log.info(Integer.toString(cc) + " processed.");

			
			}
			log.info(Integer.toString(cc));
			r.close();
			writer.close();

		} catch (Exception e) {
			log.info("file operation failed, could not read file");
			e.printStackTrace();
		}
	}

	public final static void main(String[] args) throws Exception {
		 String[] keywords = { "the hotel", "we", "he", "she", "they", "you",
		 "our hotel", "this hotel" };
		 produceHTMLfromCSV("closestPicked.csv", "output2.html",
		 new Filter[] { new ToponymsFoundFilter(),
		 new EditDistanceFilter(3, false),
		 new NPKeywordFilter(keywords)
		 //new DifferentAdmin2Filter(),
		 //new DistanceFilter(100) 
		 });

		/* 
		 
		 
		/*
		 * addDistanceColumn("phrases_20150716WithNPs.csv",
		 * "output_with_distance_filtered100km.csv", new Filter[] { new
		 * ToponymsFoundFilter(), new NPKeywordFilter(keywords), new
		 * DistanceFilter(100) } );
		 */

		// addParseResults("phrases_20150716.csv","parseResults20150716.csv");
		//addGeocodeResults("parseResults20150716.csv","geocodingResults20150716.csv");
		
		//getSpatiallyClosestPair("geocodingResults20150716.csv","closestPicked.csv");

		/*
		 * Config config = new Config();
		 * 
		 * LexicalizedParser lp = LexicalizedParser
		 * .loadModel("englishPCFG.ser.gz"); TokenizerFactory<CoreLabel>
		 * tokenizerFactory = PTBTokenizer.factory( new CoreLabelTokenFactory(),
		 * ""); GeoTxtApi geoTxtApi = new GeoTxtApi(config.getGate_home(),
		 * config.getStanford_ner()); JSONParser jsonParser = new JSONParser();
		 * processLine(
		 * "chicago athletic association is located in chicago, close to pritzker pavilion, millennium park, and art institute of chicago"
		 * ,new String[20],tokenizerFactory,lp,jsonParser,geoTxtApi);
		 */
	}

}
