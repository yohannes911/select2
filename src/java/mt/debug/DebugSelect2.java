package mt.debug;

public class DebugSelect2{

	/**
	 * Runs the given test.
	 */
	public static void main(String args[]){
		if (args[0].equals("-r")){
			int cycles = Integer.parseInt(args[1]);
			int rounds = args.length == 3 ? ROUNDS : Integer.parseInt(args[2]);
			int actLen = args.length == 3 ? Integer.parseInt(args[2]) : Integer.parseInt(args[3]);
			runRandom(cycles, rounds, actLen);
		}
		else if (args[0].equals("-a")){
			int rounds = args.length == 2 ? ROUNDS : Integer.parseInt(args[1]);
			int actLen = args.length == 2 ? Integer.parseInt(args[1]) : Integer.parseInt(args[2]);
			runAll(rounds, actLen);
		}
		else{
			int rounds = Integer.parseInt(args[0]);
			runManual(rounds, args[1]);
		}
	}
	
	private static void runRandom(int cycles, int rounds, int actLen){
		int selectCount = 0;
		for (int c=0;c<cycles;c++){
			Select2Thread[] threads = new Select2Thread[2];
			
			for (int i = 0; i<2 ;i++){
				threads[i] = new Select2Thread();
			}	
			
			Select2 select2 = new Select2(threads, Scenario2.random(rounds, actLen));
			
			for (int i = 0; i<2 ;i++){
				threads[i].setSelect2(select2);
				threads[i].start();
			}	
			for (int i = 0; i<2 ;i++){
				try{
					threads[i].join();
					selectCount+= threads[i].getSelectCount();
				}
				catch(Throwable ignored){}
			}	
			
			System.out.println();
		}
		System.out.println("Executed " + cycles + " random scenarios of " + actLen + " length with " + rounds + " rounds in each cycle.");
		System.out.println("During the test " + selectCount + " selection was made by the Select 2 protocol.");
	}
	
	private static void runAll(int rounds, int actLen){
		int selectCount = 0;
		int scenarioCount = 0;
		Scenario2 allScenarios = new Scenario2(rounds, actLen);
		
		for (Scenario scenario : allScenarios){
			scenarioCount++;
			System.out.println(scenario);
			Select2Thread[] threads = new Select2Thread[2];
			
			for (int i = 0; i<2 ;i++){
				threads[i] = new Select2Thread();
			}	
			
			Select2 select2 = new Select2(threads, scenario);
			
			for (int i = 0; i<2 ;i++){
				threads[i].setSelect2(select2);
				threads[i].start();
			}	
			for (int i = 0; i<2 ;i++){
				try{
					threads[i].join();
					selectCount+= threads[i].getSelectCount();
				}
				catch(Throwable ignored){}
			}	
			
			System.out.println();
		}
		System.out.println("Executed all " + scenarioCount + " possible scenarios of " + actLen + " length with " + rounds + " rounds in each cycle.");
		System.out.println("During the test " + selectCount + " selection was made by the Select 2 protocol.");
	}
	
	private final static int ROUNDS = 1;
	
	private static void runManual(int rounds, String actors){
		Select2Thread[] threads = new Select2Thread[2];
		
		for (int i = 0; i<2 ;i++){
			threads[i] = new Select2Thread();
		}	
		
		Select2 select2 = new Select2(threads, new Scenario(rounds, actors));
		
		for (int i = 0; i<2 ;i++){
			threads[i].setSelect2(select2);
			threads[i].start();
		}	
	}
	
	/**
	 * A simple class for testing purposes.
	 */	
	private static class Select2Thread extends Thread{
		private Select2 select2;
		private int selectCount;
		
		public void setSelect2(Select2 select2){
			this.select2 = select2;
		}
		
		public int getSelectCount(){
			return selectCount;
		}
		
		public void run(){
			long id = Thread.currentThread().getId();
			try{
				while(true){
					if(select2.execute(TEST)){
						selectCount++;
					}
					Thread.yield();
				}
			}
			catch(Throwable ignored){
				// System.out.println(ignored);
				// throw ignored;
			}
		}
	}
	
	private static final TestClosure TEST = new TestClosure();
	private static class TestClosure implements Closure{
		public boolean execute(){
			return true;
		}
	}
}