package select2.debug;

/**
 * Represents a closure that can be executed. Typically it is implemented as an anonymous class within the business class.
 */
public interface Closure{

	/**
	 * The method the closure encapsulates.
	 */
	public boolean execute();
}