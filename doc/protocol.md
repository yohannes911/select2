Protocol
===============================================================================


Select 2
-------------------------------------------------------------------------------

The Select 2 protocol is a lock free protocol that can select one and only one thread from two threads.

This synchronization primitive provides the following guaratee:

**Select 2 guarantees that only one thread is selected at any time**

This synchroinzation primitive can be used to execute critical sections:

**Select 2 can be used to execute critical sections atomically in a lock free manner.**

### Datums ###

Threads are numbered as `0` and `1` in the protocol.

The protocol uses two fields:

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* `token: 0..1` - only that thread can be chosen who owns the token (ie. thread `0` can be chosen if `token == 0`)
* `selected: boolean[2]` - marks whether thread `0` or `1` is selected

### Protocol ###

Assume that thread `i` enters the selection (`i = 0 or 1`). 

_Note that `i + 1` means `(i+1) % 2` in the following code._

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

**Statement 1: The protocol provides the following feature: Only one thread is selected at any time. Formally: either `not selected[0]` or `not selected[1]` always holds.**
	
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

**Lemma: If two threads run in parallel (ie. `active[0] and active[1]` holds at any time) than one of the threads detects that the other is active in the 3.1. section.**

Proof: Assume that thread `0` did not detect thread `1` as active. This means that thread `1` activated itself only after thread `0` executed its check. This also means that thread `1` activated itself later than thread `0`. However this means that thread `1` executed the check after thread `0` was already active, hence it detected thread `0` as active.

The Select 2 protocol has another feature, namely it is lock free. In order to see what it means, we need to examine the protocol's application:

### Application protocol ###

In order to use the protocol to execute critical sections it must be extended in the following way:

During the selection period (while the thread is selected) the thread can execute some injected code (closures or kinda). The pseudo code is the following:

    selected[i] = true
    block()
    selected[i] = false

, where `block` is a black box function (closure or such) injected into the selection protocol.

The Select 2 protocol in the above manner is lock free and safe:

**Statement 2: Select 2 is safe in the following manner: blocks are never executed in parallel.**

Proof: When blocks are executed in a thread, that thread must be selected. However due to Statement 1, threads are never selected in parallel, hencec blocks are never executed in parallel.

**Statement 3: Select 2 is lock free in the following manner: none of the threads depends on / waits for the business code of the other.**

Proof: The Select 2 protocol does not depend on the injected code.

Notes: 

* The above safety feature guarantees that critical sections are executed atomically or sequentally and never in parallel. However Select 2 does not guarantee that the block will be ever executed. It only guarantees that if it is executed than no other block is executed in parallel.
* Lock free (in the above manner) does not mean that the business code running in one thread cannot block the other thread. There could be situations when the scheduler does not let other threads run before the business code exits.

### TODO ###

* Check the proof in multiprocessor envs
* Formal verification through code
* Fix the API
* Benchmark the API