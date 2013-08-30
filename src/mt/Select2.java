package mt;

/**
 * The Select 2 protocol is a lock free protocol that can select one and only one thread from two threads.<br/>
 * <br/>
 * This synchronization primitive provides the following guaratee:<br/>
 * <br/> 
 * <b>Select 2 guarantees that only one thread is selected at any time</b>
 * <br/>
 * This synchroinzation primitive can be used to execute critical sections:<br/>
 * <br/>
 * <b>Select 2 can be used to execute critical sections atomically in a lock free manner.</b>
 */
public class Select2 extends Debuggable{
	private long[] threads;
	private volatile boolean[] active, selected;
	private volatile int token;
	
	/**
	 * The current implementation does not deal with open systems: one must explicitely give the threads to choose between.
	 */
	public Select2(Thread[] threads){
		active = new boolean[2];
		selected = new boolean[2];
		this.threads = new long[2];
		for (int i = 0; i<2;i++){
			this.threads[i] = threads[i].getId();
		}
	}
	
	/**
	 * Get the thread number used in the protocol (0 or 1).
	 */
	protected int getNum(){
		if (threads[0] == Thread.currentThread().getId()){ return 0; }
		else{ return 1; }
	}
	

	/**
	 * Selects the current thread or not - if it is selected then it executes the given closure, otherwise not.
	 */
	public boolean execute(Closure closure) throws Throwable{
		// determine internal thread number
		int i = getNum();
		
		// 1. mark myself as active
		active[i] = true;
		
		// 2. check whether I am the token owner
		boolean token_owner = token == i;
	
		// 3. check whether the other thread already entered the selection protocol
		if (active[(i + 1) % 2]){
			// 3.1. if I am not the token owner cleanup and exit 
			if (!token_owner){
				active[i] = false;
				return false;	
			}
			// 3.2. if I am the token owner wait for the other thread till it decides what to do 
			else{
				while ( token == i && active[(i + 1) % 2] ){
					Thread.yield();
				}
			}
		}
		
		Throwable throwed = null;
		boolean result = false;
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
				try{ result = closure.execute(); }
				catch(Throwable th){ throwed = th; }
				selected[i] = false;
				token = (i + 1) % 2;
				active[i] = false;
				if (throwed == null){ return result; }
				else{ throw throwed; }
			} 
		}
		// 4.3. if I was not the token owner but reached this point, than I am selected, get the token ownership, cleanup and exit
		else {
			token = i;
			selected[i] = true;
			try{ result = closure.execute(); }
			catch(Throwable th){ throwed = th; }
			selected[i] = false;
			active[i] = false;
			if (throwed == null){ return result; }
			else{ throw throwed; }
		}
	}
}