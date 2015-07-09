import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

import edu.psu.ist.vaccine.analyzers.GateHierarchyAnalyzer;
import edu.psu.ist.vaccine.analyzers.StanfordHierarchyAnalyzer;
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


public class Test {

	public static void parseFile(String filename) throws FileNotFoundException, IOException {
		Config config = new Config();
		
		LexicalizedParser lp = LexicalizedParser.loadModel("englishPCFG.ser.gz");
		TokenizerFactory<CoreLabel> tokenizerFactory =
				PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
		GeoTxtApi geoTxtApi = new GeoTxtApi(config.getGate_home(), config.getStanford_ner());
		
		
		try {
			BufferedReader r = new BufferedReader(new FileReader(filename));
			int cc = 0;

			String line = r.readLine();
			while (line != null) {

				if (!line.replaceFirst("^[\\x00-\\x200\\xA0]+", "").replaceFirst("[\\x00-\\x20\\xA0]+$", "").isEmpty()) {
					//line = "Timhotel Paris Boulogne is located in Boulogne-Billancourt, close to Pierre de Coubertin Stadium, Eiffel Tower, and Stade de Roland Garros.";
					
					
					// tokenize and produce parse tree
					Tokenizer<CoreLabel> tok = tokenizerFactory.getTokenizer(new StringReader(line));
					//Tokenizer<CoreLabel> tok = tokenizerFactory.getTokenizer(new StringReader("Hotel Jardin d'Eiffel is close to Eiffel tower"));
					//Tokenizer<CoreLabel> tok = tokenizerFactory.getTokenizer(new StringReader("Situated in Paris's Trocadero neighborhood, this hotel is close to Wine Museum, Eiffel Tower, and Arc de Triomphe"));

					
					List<CoreLabel> rawWords2 = tok.tokenize();

					Tree parse = lp.apply(rawWords2);

					// produce & print the parse tree
					TreePrint tp = new TreePrint("penn,typedDependencies");
					StringWriter sw = new StringWriter();
					PrintWriter writer = new PrintWriter(sw);
					//System.out.println("sentence: >"+line+"<");
					//tp.printTree(parse,writer);
					//System.out.println("Parse results:\n"+sw.toString());

					// test tregex by checking for a pattern
					String s = "S < NP=np1 <+(VP) (ADVP < (RB < close) < (PP < (TO < to) <+(NP) (NP=np2 !< CC)))";
					 //String s = "S < NP=np1 < (VP < (ADJP < (JJ < close)))";
					
					
					TregexPattern p = TregexPattern.compile(s);
					TregexMatcher m = p.matcher(parse);
					
					while (m.find()) {
						System.out.println("sentence: >"+line+"<");
						System.out.println("geotxt: ");
						String geocodeResults = geoTxtApi.geoCodeToGeoJson(line, "stanfordh", false, 0, false, true);
						System.out.println(geocodeResults);
						System.out.println("stanford extraction: ");
						String sr = StanfordHierarchyAnalyzer.st.tagAlltoGeoJson(line, false, 0, false, true);
						System.out.println(sr);
						System.out.println("gate extraction: ");
						sr = GateHierarchyAnalyzer.gate.tagAlltoGeoJson(line, false, 0, false, true);
						System.out.println(sr);
						Tree t = m.getNode("np1");
						System.out.println("np1:\n"+Sentence.listToString(t.yield()));
						t = m.getNode("np2");
						System.out.println("np2:\n"+Sentence.listToString(t.yield()));
						//System.out.println("match:\n");
						m.getMatch().pennPrint();
						System.out.println("\n");
						cc++;
					}
				}
				line = r.readLine();
			}
			System.out.println(cc);
			r.close();

		} catch (Exception e) {
			System.out.println("file operation failed, could not read file");
			e.printStackTrace();
		}
	}

	public final static void main(String[] args) throws Exception {

		parseFile("phrases_20150625.txt");
		//parseFile("phrases_cities_20150708.txt");
	}

}
