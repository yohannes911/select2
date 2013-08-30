Protocol
===============================================================================


Select 2
-------------------------------------------------------------------------------

The Select 2 protocol can select one and only one thread from two threads. 
This synchronization primivite provides the following guaratee:

**Only one thread is selected at any time**

This primitive can be used for instance to execute critical sections of code.

### Datums ###

Threads are numbered as `0` and `1` in the protocol.

The protocol uses two fields:

* `active[]` - marks whether thread 0 or 1 is active (entered the selection protocol)
* `token` - only that thread can be chosen who owns the token

### Protocol ###

Assume that thread `i` enters the selection (`i = 0 or 1`). 

Note: that `i + 1` means `(i+1) % 2` in the following code.

Then the pseudo code is the following:

-- 1. mark myself as active

    active[i] = true

-- 2. check whether I am the token owner

    token_owner = token == i

-- 3. check whether the other thread already entered the selection protocol

    if active[i + 1]

-- -- 3.1. if I am not the token owner cleanup and exit

       if !token_owner
          active[i] = false
          return false

-- -- 3.2. if I am the token owner wait for the other thread till it decides what to do 
       
       else while token == i and active[i + 1]
          yield

-- 4. now different cases could happen

-- 4.1. if I was the token owner but the other thread took the ownership so far, then I am not selected, cleanup and exit

    if token_owner and token != i
          active[i] = false
          return false

-- 4.2. if I was and still is the token owner, then I am selected, give up the token ownership, cleanup and exit

    if token_owner and token == i
          token = i + 1
          active[i] = false
          return true

-- 4.3. if I was not the token owner but reached this point, than I am selected, get the token ownership, cleanup and exit

    if !token_owner
          token = i
          active[i] = false
          return true

### Features ###

**The protocol provides the following feature: Only one thread is selected at any time**

**Definition 1.: Two thread is said to run the selection protocol in parallel, if both thread is active. Formally: 
thread `0` and `1` run in parallel if `active[0] and active[1]` holds at some point in time.**

**Statement 1: If two threads runs the selection protocol in parallel, then only one is selected**.  
	
Proof: TODO

**Statement 2: Only one thread is selected at any time**

Proof: If only one thread is entering the selection protocol, than obviously it will be selected. This and the above statement 1 gives the proof.

### Use case 1 - Critical sections ###

TODO

### Use case 2 - Guarded synch ###

TODO
