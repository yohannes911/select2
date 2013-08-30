package mt{
	class Select2(threadIds: Array[Long]){
		@volatile
		private var active = new Array[Boolean](2)
		
		@volatile
		private var selected = new Array[Boolean](2)
		
		@volatile
		private var waits = new Array[Boolean](2)
	
		@volatile
		private var token = 0
		
		def apply(block: => Boolean): Boolean = block
	}
   
	object Select2{
		def apply(threadIds: Array[Long]) = new Select2(threadIds)
		
		def apply(threads: Array[Thread]): Select2 = {
			var threadIds = new Array[Long](2)
			for (i <- 0 to 1){
				threadIds(i) = threads(i).getId()
			}
			new Select2(threadIds)
		}
		
		def main(args: Array[String]) {
			val select = Select2( Array( new Thread(), new Thread() ) )
			
			var b = select{
				false
			}
			println("result: " + b)
		}
	}
}