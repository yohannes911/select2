package select2.bench;

import java.util.Date;

/**
 * A very simple benchmark. Features:
 *
 * (1) uses a simple (empty) closure
 * (2) supports simple warmup
 * (3) support one test cycle, where the round could be configured
 * (4) measure run time
 *
 * TODO: implement a deterministic test variant, where mt scenarios could be given.
 */
public class SimpleBenchSelect2{
	public void execute(int rounds){
		int warmup = 100000;
		executeCasSelect2Test(warmup);
		executeSelect2Test(warmup);
		
		for (int i = 0; i< 10; i++){
			// test Select2
			stopWatch.push();
			executeSelect2Test(rounds);
			stopWatch.push();
			System.out.println("Select2:\t" + rounds + " rounds in " + stopWatch.time() + " msec");
			
			// test CasSelect2
			stopWatch.push();
			executeCasSelect2Test(rounds);
			stopWatch.push();
			System.out.println("CasSelect2:\t" + rounds + " rounds in " + stopWatch.time() + " msec");
		}
	}
	
	protected void executeSelect2Test(int rounds){
		Select2Thread[] threads = new Select2Thread[2];
		
		for (int i = 0; i<2 ;i++){
			threads[i] = new Select2Thread();
		}	
		
		AbstractSelect2 select2 = new Select2(threads);
		
		for (int i = 0; i<2 ;i++){
			threads[i].setSelect2(select2);
			threads[i].setRounds(rounds);
			threads[i].start();
		}
		
		for (int i = 0; i < 2; i++) {
			try {
			   threads[i].join();
			} 
			catch (InterruptedException ignore) {}
		}		
	}
	
	protected void executeCasSelect2Test(int rounds){
		Select2Thread[] threads = new Select2Thread[2];
		
		for (int i = 0; i<2 ;i++){
			threads[i] = new Select2Thread();
		}	
		
		AbstractSelect2 select2 = new CasSelect2(threads);
		
		for (int i = 0; i<2 ;i++){
			threads[i].setSelect2(select2);
			threads[i].setRounds(rounds);
			threads[i].start();
		}
		
		for (int i = 0; i < 2; i++) {
			try {
			   threads[i].join();
			} 
			catch (InterruptedException ignore) {}
		}		
	}
	
	/**
	 * Runs the given test.
	 */
	public static void main(String args[]){
		SimpleBenchSelect2 bench = new SimpleBenchSelect2();
		bench.execute(Integer.parseInt(args[0]));
	}
	
	/**
	 * A simple class for testing purposes.
	 */	
	private class Select2Thread extends Thread{
		private AbstractSelect2 select2;
		public void setSelect2(AbstractSelect2 select2){
			this.select2 = select2;
		}
		
		private int rounds;
		public void setRounds(int rounds){
			this.rounds = rounds;
		}
		
		public void run(){
			long id = Thread.currentThread().getId();
			for(int i=0; i<rounds; i++){
				// System.out.println( "DEBUG-" + id + ":\texecuting..." );
				boolean executed = select2.execute(TEST);
				// System.out.println( "DEBUG-" + id + ":\texecuted? " + executed);
			}
			Thread.yield();
		}			
	}
	
	private static final TestClosure TEST = new TestClosure();
	private static class TestClosure implements Closure{
		public boolean execute(){
			return true;
		}
	}

	private StopWatch stopWatch = new StopWatch();
	private static class StopWatch{
		private boolean started;
		private long start, stop;
		
		public void push(){
			if (!started){
				start = (new Date()).getTime();
				started = true;
			}
			else{
				stop = (new Date()).getTime();
				started = false;
			}
		}
		
		public long time(){
			if (!started){
				return stop - start;
			}
			else{
				return (new Date()).getTime() - start;
			}
		}
	}
}