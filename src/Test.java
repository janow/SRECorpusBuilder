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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.json.simple.parser.JSONParser;

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

	public static void produceHTMLfromCSV(String csvFileName, String outFileName, Filter[] filters) {
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

				// if a filter says row should be filtered out, we move on to the next row
				boolean skip = false;
				for (Filter f : filters) { 
					if (f.doFilter(row)) {
						skip = true;
						break;
					};
				}
				if (skip) continue;
				
				instances++;
				
				int start = Integer.parseInt(row[4]);
				int end = Integer.parseInt(row[5]);
				String line = row[6];

				bw.write("<div><h2>" + instances + " - " + line.substring(0, start) + "<u>"
						+ line.substring(start, end) + "</u>"
						+ line.substring(end) + "</h2>\n");

				bw.write("<p>pattern: "+row[7]+"</p>");
				
				bw.write("<div class=\"css-treeview\"><ul><li><input type=\"checkbox\" id=\"item-pt-\""+c+"\"/><label for=\"item-pt-\""+c+"\">Matching parse tree</label><ul><p><pre>" + row[9] + "</pre></p></ul></li></ul></div>\n");

				bw.write("<div class=\"css-treeview\"><ul><li><input type=\"checkbox\" id=\"item-ct-\""+c+"\"/><label for=\"item-ct-\""+c+"\">Complete parse tree</label><ul><p><pre>" + row[8] + "</pre></p></ul></li></ul></div>\n");

				
				if (row.length > 9) { // geocoding results included?

					bw.write("<p style=\"color: #009900\">np1: " + row[10]  + "</p>\n");

					bw.write("<p>toponym: " + row[11] + "</p>\n");
					
					bw.write("<div class=\"css-treeview\"><ul><li><input type=\"checkbox\" id=\"item-gc1-\""+c+"\"/><label for=\"item-gc1-\""+c+"\">GeoJSON np1</label><ul><p><pre style=\"white-space: pre-wrap\">" + row[12] + "</pre></p></ul></li></ul></div>\n");
					
					bw.write("<p style=\"color: #000099\">np2: " + row[13] + "</p>\n");
					
					bw.write("<p>toponym: " + row[14] + "</p>\n");
					
					bw.write("<div class=\"css-treeview\"><ul><li><input type=\"checkbox\" id=\"item-gc2-\""+c+"\"/><label for=\"item-gc2-\""+c+"\">GeoJSON np2</label><ul><p><pre style=\"white-space: pre-wrap\">" + row[15] + "</pre></p></ul></li></ul></div>\n");

					JSONParser jsonParser = new JSONParser();
					bw.write("<p>distance: "+Utility.computeDistanceFromJSON(row[12],row[15],jsonParser)+ " km</p>");
					
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
	

	public static void parseFile(String filename, String outputCSVName)
			throws FileNotFoundException, IOException {
		Config config = new Config();

		LexicalizedParser lp = LexicalizedParser
				.loadModel("englishPCFG.ser.gz");
		TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(
				new CoreLabelTokenFactory(), "");
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
			String[] writingRows = new String[16];

			for (int i = 0; i < rows.length; i++) {
				writingRows[i] = rows[i];
			}

			String line = rows[6];
			while (line != null) {

				if (!line.replaceFirst("^[\\x00-\\x200\\xA0]+", "")
						.replaceFirst("[\\x00-\\x20\\xA0]+$", "").isEmpty()) {
					// line =
					// "Timhotel Paris Boulogne is located in Boulogne-Billancourt, close to Pierre de Coubertin Stadium, Eiffel Tower, and Stade de Roland Garros.";

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
					String s = "S < NP=np1 <+(VP) (ADVP < (RB < close) < (PP < (TO < to) <+(NP) (NP=np2 !< CC)))";

					writingRows[7] = s;

					// String s = "S < NP=np1 < (VP < (ADJP < (JJ < close)))";

					// TODO: pattern should become a column in the output csv

					TregexPattern p = TregexPattern.compile(s);
					TregexMatcher m = p.matcher(parse);

					while (m.find()) {
						// System.out.println("sentence: >" + line + "<");

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
						// System.out.println("np1:\n"
						// + Sentence.listToString(t.yield()));

						// np1
						writingRows[10] = Sentence.listToString(t.yield());

						writingRows[11] = GeoLocation.getGeoInfo(
								Sentence.listToString(t.yield()), geoTxtApi,
								jsonParser, false);

						writingRows[12] = GeoLocation.getCandidates(
								Sentence.listToString(t.yield()), true,
								geoTxtApi, jsonParser);

						// System.out.println("<p>toponym: "
						// + GeoLocation.getGeoInfo(
						// Sentence.listToString(t.yield()),
						// geoTxtApi, jsonParser, true) + "</p>\n");

						t = m.getNode("np2");
						// System.out.println("np2:\n"
						// + Sentence.listToString(t.yield()));

						writingRows[13] = Sentence.listToString(t.yield());

						writingRows[14] = GeoLocation.getGeoInfo(
								Sentence.listToString(t.yield()), geoTxtApi,
								jsonParser, false);

						writingRows[15] = GeoLocation.getCandidates(
								Sentence.listToString(t.yield()), true,
								geoTxtApi, jsonParser);

						// System.out.println("<p>toponym: "
						// + GeoLocation.getGeoInfo(
						// Sentence.listToString(t.yield()),
						// geoTxtApi, jsonParser, true) + "</p>\n");

						// System.out.println("match:\n")
						m.getMatch().pennPrint();
						// System.out.println("\n");

						writer.writeNext(writingRows);

						for (int i = 9; i < writingRows.length; i++) {
							writingRows[i] = null;
						}

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

				for (int i = 0; i < rows.length; i++) {
					writingRows[i] = rows[i];
				}

				for (int i = rows.length; i < writingRows.length; i++) {
					writingRows[i] = null;
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

	public final static void main(String[] args) throws Exception {
		String[] keywords = { "the hotel", "we", "he", "she", "they", "you", "our hotel", "this hotel" };
		produceHTMLfromCSV("phrases_20150716WithNPs.csv", "output2.html", new Filter[] { new ToponymsFoundFilter(), 
																						 new NPKeywordFilter(keywords),
																						 //new DifferentAdmin2Filter(),
																						 new DistanceFilter(100) });
		//parseFile("phrases_20150716.csv", "output.csv");
		// parseFile("phrases_cities_20150708.txt","output.csv");
	}

}
