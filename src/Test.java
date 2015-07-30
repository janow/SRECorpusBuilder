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

public class Test {

	public static final Logger log = Logger.getLogger(Test.class.getName());

	public static void parseFile(String filename, String outfile)
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
			CSVReader r = new CSVReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));

			BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
			bw.write("<!DOCTYPE html><html><head></head><body>\n");

			int cc = 0;
			

			String[] rows = r.readNext();
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
					TreePrint tp = new TreePrint("penn,typedDependencies");
					StringWriter sw = new StringWriter();
					PrintWriter writer = new PrintWriter(sw);
					
					
					// System.out.println("sentence: >"+line+"<");
					// tp.printTree(parse,writer);
					// System.out.println("Parse results:\n"+sw.toString());

					// test tregex by checking for a pattern
					String s = "S < NP=np1 <+(VP) (ADVP < (RB < close) < (PP < (TO < to) <+(NP) (NP=np2 !< CC)))";
					// String s = "S < NP=np1 < (VP < (ADJP < (JJ < close)))";

					TregexPattern p = TregexPattern.compile(s);
					TregexMatcher m = p.matcher(parse);

					int start = Integer.parseInt(rows[4]);
					int end = Integer.parseInt(rows[5]);

					while (m.find()) {
						// System.out.println("sentence: >" + line + "<");
						bw.write("<div><h2>" + line.substring(0, start) + "<u>"
								+ line.substring(start, end) + "</u>"
								+ line.substring(end) + "</h2>\n");

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
						bw.write("<p>np1: " + Sentence.listToString(t.yield())
								+ "</p>\n");
						// System.out.println("<p>toponym: "
						// + GeoLocation.getGeoInfo(
						// Sentence.listToString(t.yield()),
						// geoTxtApi, jsonParser) + "</p>\n");

						bw.write("<p>toponym: "
								+ GeoLocation.getGeoInfo(
										Sentence.listToString(t.yield()),
										geoTxtApi, jsonParser) + "</p>\n");

						t = m.getNode("np2");
						// System.out.println("np2:\n"
						// + Sentence.listToString(t.yield()));
						bw.write("<p>np2: " + Sentence.listToString(t.yield())
								+ "</p>\n");
						// System.out.println("<p>toponym: "
						// + GeoLocation.getGeoInfo(
						// Sentence.listToString(t.yield()),
						// geoTxtApi, jsonParser) + "</p>\n");

						bw.write("<p>toponym: "
								+ GeoLocation.getGeoInfo(
										Sentence.listToString(t.yield()),
										geoTxtApi, jsonParser) + "</p>\n");

						// System.out.println("match:\n")
						m.getMatch().pennPrint();
						// System.out.println("\n");
						bw.write("<p><pre>" + m.getMatch().pennString()
								+ "</pre></p>\n");

						bw.write("</div>\n");
						
					}
				}
				cc++;
				log.info(Integer.toString(cc) +" processed.");
				rows = r.readNext();
				line = rows[6];
			}
			log.info(Integer.toString(cc));
			r.close();
			bw.write("</body></html>");
			bw.close();

		} catch (Exception e) {
			log.info("file operation failed, could not read file");
			e.printStackTrace();
		}
	}

	public final static void main(String[] args) throws Exception {

		parseFile("phrases_20150716.csv", "parse_results.html");
		// parseFile("phrases_cities_20150708.txt","parse_results.html");
	}

}
