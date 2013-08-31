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
	
	protected static void runRandom(int cycles, int actLen){
		int rounds = Select2.Step.values().length;
		rounds *= rounds;	
		Random random = new Random();
		int selectCount = 0;
		for (int c=0;c<cycles;c++){
			Select2Thread[] threads = new Select2Thread[2];
			
			for (int i = 0; i<2 ;i++){
				threads[i] = new Select2Thread();
			}	
			
			int[] actors = new int[actLen];
			for (int i=0;i<actLen;i++){
				actors[i] = random.nextInt(2);
			}
			
			Select2 select2 = new Select2(threads, new Scenario(rounds, actors));
			
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
		System.out.println("Executed " + cycles + " test cycles, " + rounds + " rounds with " + actLen + " scenario length in each cycle.");
		System.out.println("During the test " + selectCount + " selection was made by the Select 2 protocol.");
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