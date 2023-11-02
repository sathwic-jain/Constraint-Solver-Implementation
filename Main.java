
public class Main {

	public static void main(String[] args) {
        try {    
		    if (args.length < 1) {
			    System.out.println("Usage: java Main <file.csp> <fc|mac> <asc>") ;
			    return ;
            }
			if(args[1].equals("fc")) {
				new FC(args);
			} else if (args[1].equals("mac")) {
                new MAC(args);
			}
		} 
			 catch (Exception e) {
				 System.out.println("Usage: java Main <file.csp> <fc|mac> <asc>") ;
			}
	}
}
