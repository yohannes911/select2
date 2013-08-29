package mt;

/**
 * A class to support mt debug. Not yet really used...
 */
public class Debuggable{
	protected boolean g_debug;

	private int step;
	private int[] scenario;
	
	public Debuggable(){
	}
	
	public Debuggable(int[] scenario){
		this.scenario = scenario;
		g_debug = true;
	}
	
	public Debuggable(String scenario){
		int len = scenario.length();
		this.scenario = new int[len];
		for (int i=0; i<len; i++){
			this.scenario[i] = Integer.parseInt( scenario.substring(i, i+1) );
		}
		g_debug = true;
	}
	
	protected void g_step_start(int threadNum, String msg){
		g_info(msg);
		if (step < scenario.length){
			while(Thread.currentThread().getId() != threadNum){
				Thread.yield();
			}
		}
	}
	
	protected void g_step_finish(int threadNum, String msg){
		g_info(msg);
		if (step < scenario.length){ step++; }
	}
	
	protected void g_info(Object msg){
		System.out.println("DEBUG-" + Thread.currentThread().getId() + ":\t" + msg.toString());
	}
}