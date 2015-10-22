
public class ToponymsFoundFilter implements Filter {

	@Override
	public boolean doFilter(String[] row) {
		
		return (row[11].equalsIgnoreCase("No toponym found") || row[15].equalsIgnoreCase("No toponym found"));
	}

}
