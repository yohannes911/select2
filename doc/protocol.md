Protocol
===============================================================================


Select 2
-------------------------------------------------------------------------------

### Datums ###

Threads are numbered as 0 and 1.

The protocol uses two fields:

* active[] - marks whether thread 1 or 2 is active (under selection)
* grantee - in case when both thread is active, the grantee will be chosen

### Protocol ###

Assume that thread i enters the selection (i = 0 or 1). 

Then the pseudo code is the following:

-- mark myself as active

    active[i] = true

-- check whether the other thread already entered and whether it is the grantee, if yes, cleanup and exit

    if active[(i + 1) % 2] and grantee != i then
       active[i] = false
       grantee = i
       return false

-- otherwise thread i was chosen, cleanup and exit

    else
       active[i] = false
       return true

### Features ###

**The protocol provides the following feature: Only one thread is selected at any time**
 
**Statement 1: If two threads executes the selection protocol symultaneously, then only one is selected**.  
Formally:

If both active[0] and active[1] is true before any thread is exiting, than only one thread will be selected.

That is to say, _simultaneous run_ means that both thread entered the first statement (`active[i] = true`), before anyone exited.
	
Proof: Since both `active[0]` and `active[1]` is `true` only the grantee could be selected.

**Statement 2: Only one thread is selected at any time**

Proof: If only one thread is entering the selection protocol, than obviously it will be selected. This and the above statement 1 together proves this statement.