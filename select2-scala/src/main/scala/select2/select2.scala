package select2{
	class Select2(threadIds: Array[Long]){
		@volatile
		private var active = new Array[Boolean](2)
		
		@volatile
		private var selected = new Array[Boolean](2)
		
		@volatile
		private var waits = new Array[Boolean](2)
	
		@volatile
		private var token = 0
		
		def apply(block: => Boolean): Boolean = {
			// determine internal thread number
			val i = getNum()
			
			// 1. mark myself as active
			active(i) = true
			
			// 2. check whether I am the token owner
			val token_owner = token == i
		
			// 3. check whether the other thread already entered the selection protocol
			if ( active( (i + 1) % 2 ) ){
				// 3.1. if I am not the token owner then wakeup owner, cleanup and exit 
				if (!token_owner){
					waits( (i+1) % 2 ) = false
					active(i) = false
					return false
				}
				// 3.2. if I am the token owner wait for the other thread till it decides what to do 
				else{
					waits(i) = true;
					while ( token == i && active( (i + 1) % 2 ) && waits(i) ){
						Thread.`yield`()
					}
					waits(i) = true
				}		
			}
			
			var result = false;
			
			// 4. now different cases could happen:
			if (token_owner){
				// 4.1. if I was the token owner but the other thread took the ownership so far, then I am not selected, cleanup and exit
				if (token != i){
					active(i) = false
					return false
				}
				// 4.2. if I was and still is the token owner, then I am selected, give up the token ownership, cleanup and exit
				else{
					selected(i) = true
					assert( !selected( (i+1) % 2 ) )
					result = block
					selected(i) = false					
					token = (i + 1) % 2
					active(i) = false
					return result
				} 
			}
			// 4.3. if I was not the token owner but reached this point, than I am selected, get the token ownership, cleanup and exit
			else {
				token = i
				selected(i) = true
				assert( !selected( (i+1) % 2 ) )
				result = block
				selected(i) = false
				active(i) = false
				return result
			}
		}
		
		private def getNum() = {
			if ( threadIds(0) == Thread.currentThread().getId() ){ 0 }
			else{ 1 }
		}
	}
   
	object Select2{
		def apply(threadIds: Array[Long]) = new Select2(threadIds)
		
		def apply[T <: Thread](threads: Array[T]): Select2 = {
			var threadIds = new Array[Long](2)
			for (i <- 0 to 1){
				threadIds(i) = threads(i).getId()
			}
			new Select2(threadIds)
		}
		
		def main(args: Array[String]) {
		}
	}
}