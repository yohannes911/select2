package select2.debug;

/**
 * Represents a test scenario entity. Each scenario consists of:
 * (1) some rounds - the number of method invocations that should be executed durint the test,
 * (2) a list of actor ids - each represent thread to run at the given step
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
	
	/**
	 * Constructor for subclasses.
	 */
	protected Scenario(){}
	
	/**
	 * Get actors (thread ids of scenario).
	 */
	public int[] getActors(){
		return actors;
	}
	
	/**
	 * Set actors (thread ids of scenario).
	 */
	protected void setActors(int[] actors){
		this.actors = actors;
	}
	
	/**
	 * Get number of rounds.
	 */
	public int getRounds(){
		return rounds;
	}
	
	/**
	 * Set number of rounds.
	 */
	protected void setRounds(int rounds){
		this.rounds = rounds;
	}
	
	public String toString(){
		String s = "Test scenario: rounds: " + rounds + ", actors: ";
		for (int i=0; i<actors.length; i++){
			s += (actors[i] + "");
		}
		return s;
	}
}