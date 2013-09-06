synch2 synchronization primitive
===============================================================================


Protocol specification
-------------------------------------------------------------------------------

Threads are numbered as `0` and `1` in the protocol.

### A safe but non-wait-free protocol ###

_Note that `i + 1` means `(i+1) % 2` in the following sections._

**states**: The protocol uses the following states:

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* wait1

**pseudo code**: Assume that thread `i` enters the selection (`i = 0 or 1`). Then the pseudo code of the protocol is the following:

    # mark myself as active
    activate(i) = active[i] = true

    # guard
    guard(i) = 
		if i == 0
			while active[1]
                wait1 = false 
                yield()
		if i == 1
            wait1 = false
			while active[0] and wait1 yield()

    # select then exit
    select(i)
    deselect(i)
    deactivate(i) = active[i] = false


### A safe and wait-free? protocol ###

**states**:

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* `wait1: boolean` - marks whether thread i is in wait

**pseudo code**:

    # activation
    case if i == 0: activate(0) = active[0] = true
    case if i == 1:
       # pre-activation guard
       if active[0]:
           while active[0]: yield()
       activate(1) = active[1] = true

    # guard
    guard(i) = 
		case if i == 0:
			while active[1]:
                wait[1] = false
                yield()
		case if i == 1:
            wait1 = true
			while active[0] and wait1: yield()

    # selection
    select(i)
    deselect(i)
    deactivate(i) = active[i] = false
