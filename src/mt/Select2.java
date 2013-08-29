package mt;

/**
 * Lets the system select between two threads. It can be used to synchronize threads w/o locking.
 */
public class Select2 extends Debuggable{
	private long[] threads;
	private volatile boolean[] active;
	private volatile int grantee;
	
	/**
	 * The current implementation does not deal with open systems: one must explicitely give the threads to choose between.
	 */
	public Select2(Thread[] threads){
		active = new boolean[2];
		this.threads = new long[2];
		for (int i = 0; i<2;i++){
			this.threads[i] = threads[i].getId();
		}
	}
	
	/**
	 * Get the thread number used in this class (0 or 1).
	 */
	protected int getNum(){
		if (threads[0] == Thread.currentThread().getId()){ return 0; }
		else{ return 1; }
	}
	
	/**
	 * Selects the current thread or not. Returns 0 if the current thread is selected, 1 otherwise.
	 * The return value represents a binary ordinal, not a boolean value. 
	 */
	public int select(){
		// determine internal number
		int i = getNum();
		
		// check whether other thread is active, if yes return not selected
		// g_info("step1: [" + active[0] + ", " + active[1] + "]");
		if (active[(i + 1) % 2]){
			return 1;
		}
		
		// mark myself as active
		active[i] = true;
		
		// check again whether other thread is active and whether it is the grantee, or round number changed
		// if yes cleanup and return not selected
		// g_info("step2: [" + active[0] + ", " + active[1] + "]");
		if( active[(i + 1) % 2] && grantee != i){
			active[i] = false;
			grantee = i;
			return 1;
		}
		
		// mark myself as inactive and return selected
		active[i] = false;
		return 0;
	}

	/**
	 * Selects the current thread or not - if it is selected then it executes the given clojure, otherwise not.
	 */
	public boolean execute(GuardedClojure clojure){
		if ( !clojure.isLegalState() ){
			return false;
		}
		
		// determine internal number
		int i = getNum();
		
		// check whether other thread is active, if yes return not selected
		// g_info("step1: [" + active[0] + ", " + active[1] + "]");
		if (active[(i + 1) % 2]){
			return false;
		}
		
		// mark myself as active
		active[i] = true;
		
		// check again whether other thread is active and whether it is the grantee, or round number changed
		// if yes cleanup and return not selected
		// g_info("step2: [" + active[0] + ", " + active[1] + "]");
		if( active[(i + 1) % 2] && grantee != i){
			active[i] = false;
			grantee = i;
			return false;
		}
		
		// execute clojure, mark myself as inactive and return selected
		clojure.execute();
		active[i] = false;
		return true;
	}
	
	/**
	 * Represents a clojure that can be executed.
	 */
	public interface GuardedClojure{
		public boolean isLegalState();
		
		public void execute();

	}	
}