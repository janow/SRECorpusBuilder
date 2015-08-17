
public class ToponymsFoundFilter implements Filter {

	@Override
	public boolean doFilter(String[] row) {
		
		return row[11].equals("No toponym found") || row[14].equals("No toponym found");
	}

}
