package select2{

	class Clip2(threadIds: Array[Long]){
		private val select2: Select2 = Select2(threadIds)

		@volatile
		private var setable: Boolean = true
	
		@volatile
		private var _value: Any = None
		
		def pop(): Option[Any] = {
			var value: Any = None
			
			select2{
				if (!setable){
					value = _value
					_value = None
					setable = true
					true
				}
				else{ false }
			}
			
			value match{
				case None => None
				case _ => Some(value)
			}
		}
		
		def push(value: Any): Boolean = {
			select2{
				if ( setable && value != None ){
					_value = value
					setable = false
					true
				}
				else{ false }
			}
		}
	}

	object Clip2{ 
		def apply(threadIds: Array[Long]) = new Clip2(threadIds)
		
		def apply[T <: Thread](threads: Array[T]): Clip2 = {
			var threadIds = new Array[Long](2)
			for (i <- 0 to 1){
				threadIds(i) = threads(i).getId()
			}
			new Clip2(threadIds)
		}
		
		def main(args: Array[String]) {		
			val rounds: Integer = args.length match{
				case 0 => 10
				case _ => Integer.parseInt(args(0))
			}
			val threads: Array[Clip2Thread] = Array( new Clip2Thread(rounds, true), new Clip2Thread(rounds, false) )
			val clip2 = Clip2(threads)
		
			for (i <- 0 to 1){
				threads(i).clip2 = clip2
				threads(i).start()
			}
		}
	}	
	
	import java.util.Random
		
	class Clip2Thread(rounds: Integer, push: Boolean) extends Thread{
		private var _clip2: Clip2 = null
		private val random = new Random()
		
		def clip2 = _clip2
		def clip2_= (clip2: Clip2): Unit = _clip2 = clip2
		
		override def run(){
			val id = Thread.currentThread().getId()
			var randomValue = 0
			var pushed = false
			var popped: Option[Any] = None
			for (i <- 1 to rounds){
				if (push){
					randomValue = 1 + random.nextInt(100)
					if ( _clip2.push(randomValue + "") ){
						println( "DEBUG: THREAD-" + id + ":\tPUSHED\t" + randomValue)
					}
					else{
						println( "DEBUG: THREAD-" + id + ":\tNOT_PUSHED\t" + randomValue)					
					}
					Thread.`yield`()
				}
				else{
					popped = _clip2.pop()
					popped match{
						case Some(value) => println( "DEBUG: THREAD-" + id + ":\tPOPPED\t" + value)
						case _ => println( "DEBUG: THREAD-" + id + ":\tNOT_POPPED")
					}
					Thread.`yield`()
				}
			}
		}	
	}	
	
}