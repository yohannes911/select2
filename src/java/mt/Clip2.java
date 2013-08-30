package mt;

import java.util.Random;

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
	 * After popping the clipboard is cleaned, waits for another object, one must push before another pop.
	 * Returns the clipboard object if pop was successful, otherwise null.
	 */
	public Object pop(){
		if (!setable){
			try{
				if ( select2.execute(pop) ){
					return pop.getValue();
				}
				else{
					return null;
				}
			}
			catch(Throwable th){
				return null;
			}
		}
		else{
			return null;
		}
	}
	
	/**
	 * Pushes an object to the clipboard, if it is waiting for it, otherwise does nothing. 
	 * After pushing, the clipboard gets full, one must pop before another push.  
	 * Returns true if push was successful, otherwise false.
	 */
	public boolean push(Object newVal){
		if (setable){
			try{
				if (newVal == null){
					throw new NullPointerException();
				}
				
				push.setValue(newVal);
				
				return select2.execute(push);
			}
			catch(Throwable th){
				return false;
			}
		}
		else{
			return false;
		}
	}
	
	protected Pop pop = new Pop();
	
	/**
	 * A closure that implements pop.
	 */
	protected class Pop implements Closure{
		private Object _value;

		/**
		 * Returns the value popped.
		 */
		public Object getValue(){
			return _value;
		}
		
		public boolean execute(){
			if (!setable){
				_value = value;
				value = null;
				setable = true;
				if (g_debug){
					g_info("popped " + _value);
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
	 * A closure that implements push.
	 */
	protected class Push implements Closure{
		private Object _value;
		
		/**
		 * Sets the value to push.
		 */
		public void setValue(Object value){
			_value = value;
		}
		
		public boolean execute(){
			if (setable){
				value = _value;
				setable = false;
				if (g_debug){
					g_info("pushed " + _value);
				}
				return true;
			}
			else{
				if (g_debug){
					g_info("invalid state: " + _value + " not pushed");
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
		
		threads[0] = new Clip2Thread(true);
		threads[1] = new Clip2Thread(false);
		
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
		private boolean push;
		private Clip2 clip2;
		private Random random;
		
		public Clip2Thread(boolean push){
			this.push = push;
			if (push){
				random = new Random();
			}
		}
		public void setClip2(Clip2 clip2){
			this.clip2 = clip2;
		}
		public void run(){
			long id = Thread.currentThread().getId();
			int val;
			for (int i = 0; i< 10; i++){
				if (push){
					val = 1 + random.nextInt(100);
					System.out.println( "DEBUG-" + id + ":\tpushing... " + val);
					boolean pushed = clip2.push(val + "");
					System.out.println( "DEBUG-" + id + ":\tpushing " + val + " successful? " + pushed);
					Thread.yield();
				}
				else{
					System.out.println( "DEBUG-" + id + ":\tpopping..." );
					Object popped = clip2.pop();
					System.out.println( "DEBUG-" + id + ":\tpopping successful? " + (popped != null) + " popped: " + popped);
					Thread.yield();
				}
			}
		}	
	}
}