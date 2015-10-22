import java.util.Arrays;


public class NPKeywordFilter implements Filter {
	protected String[] nps;
	
	public NPKeywordFilter(String[] nps) {
		this.nps = nps;
	}
	
	public boolean doFilter(String[] row) {
		return Arrays.asList(nps).contains(row[10].toLowerCase()) || Arrays.asList(nps).contains(row[14].toLowerCase());
	}
}
