package mt;

/**
 * Select 2 protocol implementation, building upon the Java-builtin synchronization primitive: volatile.
 */
public class Select2{
	/**
	* Holds the thread ids using the synch service.
	*/
	private final long[] threadIds;
	
	private volatile boolean[] active, selected, wait;
	private volatile int token;
	
	/**
	 * The current implementation does not deal with open systems: one must explicitely give the threads to choose between.
	 */
	public Select2(Thread[] threads){
		threadIds = new long[2];
		for (int i = 0; i<2;i++){
			threadIds[i] = threads[i].getId();threads[i].getId();
		}
		active = new boolean[2];
		selected = new boolean[2];
		wait = new boolean[2];
	}
		

	/**
	 * Select 2 application service that
	 * (1) selects the current thread or not,
	 * (2) if it is selected then it should execute the given closure, otherwise not.
	 * Returns true if the closure was executed and its execution was successful, otherwise false. That is to say: false 
	 * can mean either that the closure was not selected to run or it was executed but failed (returned false). 
	 */	
	public boolean execute(Closure closure){
		// get the internal thread number
		int i =  getInternalThreadId();
		
		// 1. mark myself as active
		active[i] = true;
		
		// 2. check whether I am the token owner
		boolean token_owner = token == i;
	
		// 3. check whether the other thread already entered the selection protocol
		if (active[(i + 1) % 2]){
			// 3.1. if I am not the token owner then wakeup owner, cleanup and exit 
			if (!token_owner){
				wait[(i+1) % 2] = false;
				active[i] = false;
				return false;
			}
			// 3.2. if I am the token owner wait for the other thread till it decides what to do 
			else{
				wait[i] = true;
				while ( token == i && active[(i + 1) % 2] && wait[i]){
					Thread.yield();
				}
				wait[i] = false;
			}
		}
		
		// 4. now different cases could happen:
		if (token_owner){
			// 4.1. if I was the token owner but the other thread took the ownership so far, then I am not selected, cleanup and exit
			if (token != i){
				active[i] = false;
				return false;
			}
			// 4.2. if I was and still is the token owner, then I am selected, give up the token ownership, cleanup and exit
			else{
				selected[i] = true;
				assert !selected[(i+1) % 2];
				boolean result = closure.execute();
				selected[i] = false;
				token = (i + 1) % 2;
				active[i] = false;
				return result; 
			} 
		}
		// 4.3. if I was not the token owner but reached this point, than I am selected, get the token ownership, cleanup and exit
		else {
			token = i;
			selected[i] = true;
			assert !selected[(i+1) % 2];
			boolean result = closure.execute();
			selected[i] = false;
			active[i] = false;
			return result;
		}
	}
	

	/**
	 * Get the internal thread id used in the protocol: 0 or 1.
	 */
	protected int getInternalThreadId(){
		if (threadIds[0] == Thread.currentThread().getId()){ return 0; }
		else{ return 1; }
	}

}