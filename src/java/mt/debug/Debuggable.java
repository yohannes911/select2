package mt.debug;

/**
 * A class to support debug.
 *
 * TODO: maybe better model nested steps (ifelse, while, etc).
 */
public abstract class Debuggable{
	/**
	 * Shows whether to debug.
	 */
	protected final boolean g_debug;
	
	/**
	 * Represents the current elementary step.
	 */
	private volatile int g_step;
	
	/**
	 * Represents the actor array of the scenario.
	 */
	protected final int[] g_actors;
	
	/**
	 * Represents the number of rounds of the scenario.
	 */
	protected final int g_num_of_rounds;
	
	/**
	 * Represents the number of rounds each thread executed.
	 */
	protected final int[] g_rounds;
	
	/**
	 * Constructs the debugger for the given threads and scenario.
	 */
	protected Debuggable(Scenario scenario){
		g_num_of_rounds = scenario.getRounds();
		g_rounds = new int[2];
		g_actors = scenario.getActors();
		g_debug = true;
	}

	/**
	 * A constructor for no debug.
	 */
	protected Debuggable(){
		g_num_of_rounds = 0;
		g_rounds = null;
		g_actors = null;
		g_debug = false;
	}

	/**
	 * Get the internal thread id used in the protocol, must be implemented in the subclass.
	 */
	protected abstract int getInternalThreadId();
	
	/**
	 * Marks that a thread finished its round.
	 */
	protected boolean g_start_round(){
		int tid = getInternalThreadId();
		int round = g_rounds[tid];
		if (round < g_num_of_rounds){ g_new_round_info(round); return true; }
		else{ g_final_round_info(); throw new RuntimeException("Finished"); }
	}
	
	/**
	 * Marks that a thread finished its round.
	 */
	protected void g_finish_round(){
		int tid = getInternalThreadId();
		int round = g_rounds[tid];
		
		if (round < g_num_of_rounds){ g_rounds[tid] = round + 1; }
	}

	/**
	 * Marks that a thread wants to start a new step. Returns the current step number.
	 */
	protected int g_start_step(){
		int tid = getInternalThreadId();
		while(true){
			if (tid != g_actors[g_step]){ break; }
			Thread.yield();
		}
		return g_step;
	}
	
	/**
	 * Marks that a thread finished its step. 
	 * This variant is used when the step has nested statements (ifelse, while, etc.)
	 */
	protected void g_finish_step(int step){
		if (step == g_step){
			g_step = (g_step + 1) % g_actors.length;
		}
	}
	protected void g_finish_step(){
		g_step = (g_step + 1) % g_actors.length;
	}

	// experimental
	protected void g_new_round_info(int round){
		g_info("new round[" + round + "]");
	}
	
	protected void g_final_round_info(){
		g_info("rounds finished");
	}
	
	protected void g_info(String msg){
		int tid = getInternalThreadId();
		System.out.println("DEBUG:\tTHREAD-" + tid + ": " + msg);
	}
}