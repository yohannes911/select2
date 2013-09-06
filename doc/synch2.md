synch2 synchronization primitive
===============================================================================


Protocol specification
-------------------------------------------------------------------------------

Threads are numbered as `0` and `1` in the protocol.

### A safe but non-wait-free protocol ###

_Note that `i + 1` means `(i+1) % 2` in the following sections._

**states**: The protocol uses the following states:

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* `wait1: boolean` - marks whether thread 1 is in wait

**pseudo code**: Assume that thread `i` enters the selection (`i = 0 or 1`). Then the pseudo code of the protocol is the following:

    # mark myself as active
    activate(i) = active[i] = true

    # guard
    guard(i) = 
		if i == 0:
			wait(0) = while active[1]:
                wait1 = false 
                yield()
		if i == 1:
            wait(1) = 
                wait1 = false
			    while active[0] and wait1: yield()

    # select then exit
    select(i)
    deselect(i)
    deactivate(i) = active[i] = false

This is not wait-free since thread 1 can 'eat the whole...' - if thread 1 is very busy then it might be possible that thread 0 will never detect it as inactive. The following protocol repairs it with a small addin:

### A safe and wait-free protocol? ###

**states**: same as above

* `active: boolean[2]` - marks whether thread 0 or 1 is active (entered the synch2 protocol)
* `wait1: boolean` - marks whether thread 1 is in wait

**pseudo code**: an additional pre-activation guard is added to thread 1, otherwise it is the same as the non wait-free protocol:

    # activation
    case if i == 0: activate(0) = active[0] = true
    case if i == 1:
       # pre-activation guard for thread 1
       if active[0]:
           while active[0]: yield()
       activate(1) = active[1] = true

    # guard
    guard(i) = 
		case if i == 0:
			wait(0) = while active[1]:
                wait[1] = false
                yield()
		case if i == 1:
            wait(1) = 
                wait1 = true
			    while active[0] and wait1: yield()

    # selection
    select(i)
    deselect(i)
    deactivate(i) = active[i] = false

**Wait-free**:

**Guard**: Either thread goes into the guard loop.

**Safety**: Indirectly assume that both thread is selected in parallel.

thread 0 history is either misses the `wait(0)` loop or it does contain it:

_Case 1 - thread 0 did not enter the `wait(0)` loop_: it has such a history as:

    [state(0) = GUARD, active[0] = true, active[1] = false], guard(0), 
    [state(0) = SELECT, active[0] = true, active[1] = false], ..., select(0)

Since this case thread 0 has not detected thread 0 as active, hence thread 1 did detect it as active and went into its own wait loop

    [state(1) = GUARD, active[0] = true, active[1] = true], guard(1), 
    [state(1) = WAIT, active[0] = true, active[1] = true], wait(1), ..., select(1)
    
At the time of this wait thread 0 was not just already active but already executed its guard statement as well, beacuse it executed the guard before thread 1 became active. Hence thread 1 can wake up only after thread 0 becomes inactive, bust just before inactivation, it is unselected. Contradiction.


_Case 2 - thread 0 did enter the `wait(0)` loop_: it has such a history as:

    [state(0) = GUARD, active[1] = true], guard(0), [state(0) = WAIT, active[1] = true], wait(0), ..., select(0)

Thread 0 could wake up from its wait iff the thread 1 deactivates itself:

    deactivate(1), [state(0) = SELECT, active[1] = false], ..., select(0)

Since deactivation happens after selection, thread 1 must have been deactivated in a previous round. However in the next round it then detects thread 0 as active, hence goes into the pre-activation guard and stays there until thread 0 is deselected then deactivated. Contradiction.