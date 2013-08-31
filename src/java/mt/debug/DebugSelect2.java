package mt.debug;

import java.util.Random;

public class DebugSelect2{

	/**
	 * Runs the given test.
	 */
	public static void main(String args[]){
		if (args[0].equals("-r")){
			int rounds = Integer.parseInt(args[1]);
			int actLen = Integer.parseInt(args[2]);
			runRandom(rounds, actLen);
		}
		else{
			int rounds = Integer.parseInt(args[0]);
			runManual(rounds, args[1]);
		}
	}
	
	protected static void runRandom(int rounds, int actLen){
		int oneRoundLen = (Select2.Step.values().length / 2);
		oneRoundLen *= oneRoundLen;
		for (int r=0;r<rounds;r++){
			Select2Thread[] threads = new Select2Thread[2];
			
			for (int i = 0; i<2 ;i++){
				threads[i] = new Select2Thread();
			}	
			
			Random random = new Random();
			int[] actors = new int[2*actLen];
			int actor = 0;
			for (int i=0;i<actLen;i++){
				actor = random.nextInt(2);
				actors[2*i] = actor;
				actors[2*i+1] = actor;
			}
			
			Select2 select2 = new Select2(threads, new Scenario(oneRoundLen, actors));
			
			for (int i = 0; i<2 ;i++){
				threads[i].setSelect2(select2);
				threads[i].start();
			}	
			for (int i = 0; i<2 ;i++){
				try{
					threads[i].join();
				}
				catch(Throwable ignored){}
			}	
			
			System.out.println();
		}
		System.out.println("Executed " + rounds + " test cycles, " + oneRoundLen + " rounds in each");
	}
	
	protected static void runManual(int rounds, String actors){
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
		
		public void setSelect2(Select2 select2){
			this.select2 = select2;
		}
		
		public void run(){
			long id = Thread.currentThread().getId();
			try{
				while(true){
					boolean executed = select2.execute(TEST);
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