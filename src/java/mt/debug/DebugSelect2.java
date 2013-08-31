package mt.debug;

public class DebugSelect2{

	/**
	 * Runs the given test.
	 */
	public static void main(String args[]){
		Select2Thread[] threads = new Select2Thread[2];
		
		for (int i = 0; i<2 ;i++){
			threads[i] = new Select2Thread();
		}	
		
		Select2 select2 = new Select2(threads, new Scenario(10, args[0]));
		
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