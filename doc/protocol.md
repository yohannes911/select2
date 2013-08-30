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

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* `token: 0..1` - only that thread can be chosen who owns the token (ie. thread `0` can be chosen if `token == 0`)
* `selected: boolean[2]` - marks whether thread `0` or `1` is selected

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
          selected[i] = true
          selected[i] = false
          token = i + 1
          active[i] = false
          return true

-- 4.3. if I was not the token owner but reached this point, than I am selected, get the token ownership, cleanup and exit

    if !token_owner
          token = i
          selected[i] = true
          selected[i] = false
          active[i] = false
          return true

### Features ###

**Statement: The protocol provides the following feature: Only one thread is selected at any time. Formally: either `not selected[0]` or `not selected[1]` always holds.**
  
	
Proof: Indirectly assume, that at some point in time both thread is selected. There are 3 possible cases:

1. Bot thread is token owner
1. None of the threads is token owner
1. One of the threads is token owner the other is not

_Case 1 - Both thread is token owner_:

This means that both thread executed the above 4.2. section. Let thread `0` be the one who executed token check (section 2) first. At that time thread `0` was the token owner. Hence this means that thread `1` executed the token check (section 2), after thread `0` gave up token ownership, however at that point thread `0` was already unselected. Contradiction.

_Case 2 - None of the threads is token owner_:  

This cannot happen, since one of the threads must fail on rule 3.1. when threads check whether other thread is active or not. Due to the following lemma one of the threads detects that the other one is active and since it is not the token owner, it exits. Contradiction.

_Case 3 - One of the threads is token owner the other is not_

Then one of the threads, say thread `0` is the token owner, hence executes the 4.2. section. Meanwhile, the other thread, say thread `1` executes section 4.3.
Again, due to the following lemma, one of the threads should have detected that the other one is active in section 3.1.:

1. If thread `1` detected thread `0` as active than it exited since it was not the token owner.
1. If thread `1` did not detect thread `0` as active, than thread `0` detected thread `1`. Since thread `0` was token owner it waited till thread `1` took the token ownership (in section 3.2) and than it did not run 4.2. section instead 4.1. 

Contradiction.

**Lemma: If two threads run in parallel (`active[0] and active[1]` holds at any time) than one of the threads detects that the other is active in the 3.1. section.**

Proof: The thread that activated itself later must detect the other one as active. 

### Application - Executing critical sections ###

TODO

### TODO ###

* Check the protocol in multiprocessor envs, where true parallelism might occur. Hence such argument might not be correct: _The thread that activated itself later must detect the other one as active_.
* Document protocol application
* Formal verification through code
* Fix the API
* Benchmark the API