package mt;

/**
 * A clipboard object threads can share objects through.
 */
public class Clip2 extends Debuggable{
	private volatile Select2 select2;
	private volatile boolean setable;
	
	private volatile Object value;
	
	/**
	 * The current implementation does not deal with open systems: one must explicitely give the threads willing to use the clipboard.
	 */
	public Clip2(Thread[] threads){
		select2 = new Select2(threads);
		setable = true;
	}
	
	/**
	 * Besides threads, one can configure debug as well.
	 */
	public Clip2(Thread[] threads, boolean debug){
		this(threads);
		g_debug = debug;
	}
	
	/**
	 * Pops the object from the clipboard if any. 
	 * After popping the clipboard is cleaned, waits for another object.
	 */
	public Object pop(){
		try{
			if ( select2.execute(pop) ){
				return pop.getOldVal();
			}
			else{
				return null;
			}
		}
		catch(Throwable th){
			return null;
		}
	}
	
	/**
	 * Pushes an object to the clipboard, if it is waiting for it, otherwise does nothing. 
	 * After pushing, the clipboard gets full, one must pop before another push.  
	 */
	public boolean push(Object newVal){
		try{
			if (newVal == null){
				throw new NullPointerException();
			}
			
			push.setNewVal(newVal);
			
			return select2.execute(push);
		}
		catch(Throwable th){
			return false;
		}
	}
	
	protected Pop pop = new Pop();
	
	/**
	 * A clojure that implements pop.
	 */
	protected class Pop implements Closure{
		private Object oldVal;

		public Object getOldVal(){
			return oldVal;
		}
		
		public boolean execute(){
			if (!setable){
				oldVal = value;
				value = null;
				setable = true;
				if (g_debug){
					g_info("popped " + oldVal);
				}
				return true;
			}
			else{
				if (g_debug){
					g_info("invalid state: nothing popped");
				}
				return false;
			}
		}
	}
	
	protected Push push = new Push();
	
	/**
	 * A clojure that implements push.
	 */
	protected class Push implements Closure{
		private Object newVal;
		
		public Push(){
		}
		
		public void setNewVal(Object newVal){
			this.newVal = newVal;
		}
		
		public boolean execute(){
			if (setable){
				value = newVal;
				setable = false;
				if (g_debug){
					g_info("pushed " + newVal);
				}
				return true;
			}
			else{
				if (g_debug){
					g_info("invalid state: " + newVal + " not pushed");
				}			
				return false;
			}
		}
	}	


	/**
	 * Runs a simple test (or call it demo if you like).
	 */
	public static void main(String[] args){
		Clip2Thread[] threads = new Clip2Thread[2];
		
		for (int i = 0; i<2 ;i++){
			threads[i] = new Clip2Thread();
		}
		
		Clip2 clip2 = new Clip2(threads, true);
		
		for (int i = 0; i<2 ;i++){
			threads[i].setClip2(clip2);
			threads[i].start();
		}
	}
	
	/**
	 * A simple class for testing purposes.
	 */
	private static class Clip2Thread extends Thread{
		private Clip2 clip2;
		public void setClip2(Clip2 clip2){
			this.clip2 = clip2;
		}
		public void run(){
			long id = Thread.currentThread().getId();
			for (int i = 0; i< 10; i++){
				
				System.out.println( "DEBUG-" + id + ":\tpushing... " + id);
				boolean pushed = clip2.push(id + "");
				System.out.println( "DEBUG-" + id + ":\tpushing " + id + " successful? " + pushed);
				Thread.yield();
				
				System.out.println( "DEBUG-" + id + ":\tpopping..." );
				Object popped = clip2.pop();
				System.out.println( "DEBUG-" + id + ":\tpopping successful? " + (popped != null) + " popped: " + popped);
				Thread.yield();
			}
		}	
	}
}