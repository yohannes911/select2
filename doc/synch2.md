synch2 synchronization primitive
===============================================================================


Protocol specification
-------------------------------------------------------------------------------

Threads are numbered as `0` and `1` in the protocol.

**pseudo code**: Assume that thread `i` enters the selection (`i = 0 or 1`). Then the pseudo code of the protocol is the following:

    def synch2(i) = 

    # activation
    activate(i) = 
        active[i] = true
        state = GUARD

    # guard
    guard(i) = 
		if active[i + 1]: 
            state[i] = WAIT
            wait(i) =
                case if i == 0: while active[1] and not wait1: yield()
                case if i == 1: while active[0] and wait1: yield()
                state[i] = SELECT
        else: 
            state[i] = SELECT

    # select
    select(i)
    deselect(i)
    maywakeup(i) = if i == 0: wait1 = false
    deactivate(i) = active[i] = false
