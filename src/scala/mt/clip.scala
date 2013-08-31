package mt{

	class Clip2(threadIds: Array[Long]){
		@volatile
		private var select2: Select2 = Select2(threadIds)

		@volatile
		private var setable: Boolean = true
	
		@volatile
		private var _value: Any = None
		
		def pop: Option[Any] = {
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
		
		def apply(threads: Array[Thread]): Clip2 = {
			var threadIds = new Array[Long](2)
			for (i <- 0 to 1){
				threadIds(i) = threads(i).getId()
			}
			new Clip2(threadIds)
		}
		
		def main(args: Array[String]) {
		}
	}	
}