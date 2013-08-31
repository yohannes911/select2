package mt.debug;

/**
 * Represents a test scenario entity. Each scenario consists of:
 * (1) some rounds - the number of method invocations that should be executed durint the test.
 * (2) a list of actor ids - which shows which thread to run in the given step
 */
public class Scenario{
	/**
	 * Round represents the number of method invocations that should be executed durint the test.
	 */
	private int rounds;
	
	/**
	 * The array of actors represents the active threads (thread id) in each elementary step.
	 */
	private int[] actors;
	
	/**
	 * Step represents the ord of the current elementary step.
	 */
	private int step;
	
	/**
	 * Construct a scenario by the given rounds and list of actors.
	 * The actor array must contain internal thread numbers that is 0s and 1s.
	 */
	public Scenario(int rounds, int[] actors){
		this.rounds = rounds;
		this.actors = actors;			
	}
	
	/**
	 * Construct a scenario by the given number of rounds and list of actors.
	 * Actor array is given as a binary string, ie.: "001101011".
	 */
	public Scenario(int rounds, String actors){
		this.rounds = rounds;
		
		int len = actors.length();
		this.actors = new int[len];		
		for (int i=0; i<len; i++){
			this.actors[i] = Integer.parseInt( actors.substring(i, i+1) );
		}		
	}
	
	public int[] getActors(){
		return actors;
	}
	
	public int getRounds(){
		return rounds;
	}
}