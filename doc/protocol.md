Protocol
===============================================================================


Select 2
-------------------------------------------------------------------------------

The Select 2 protocol is a wait-free protocol that can select one and only one thread from two threads.

This synchronization primitive provides the following guaratee:

**Select 2 guarantees that only one thread is selected at any time**

This synchroinzation primitive can be used to execute critical sections:

**Select 2 can be used to execute critical sections atomatically, in a wait-free manner.**

### Datums ###

Threads are numbered as `0` and `1` in the protocol.

The protocol uses the following fields:

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* `token: 0..1` - only that thread can be chosen who owns the token (ie. thread `0` can be chosen if `token == 0`)
* `selected: boolean[2]` - marks whether thread `0` or `1` is selected
* `wait: boolean[2]` - marks whether thread `0` or `1` is waiting

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

-- -- 3.1. if I am not the token owner, wake up token owner, cleanup and exit

       if !token_owner 
           wait[i+1] = false
           active[i] = false
           return false

-- -- 3.2. if I am the token owner wait for the other thread till it decides what to do 
       
       else 
		  wait[i] = true
	      while token == i and active[i + 1] and wait[i]
             yield
          wait[i] = false

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

PROOF MUST BE REVISED DUE TO THE CHANGES OF THE Lemma

**Statement 1: Select 2 is safe in the following manner: the protocol guarantees that one and only one thread is selected at any time. Formally:  `not selected[0] or not selected[1]` always holds.**
	
Proof: Indirectly assume, that at some point in time both thread is selected. There are 3 possible cases:

1. Both thread is token owner
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


**Lemma: If two threads run in parallel than one of the threads detects that the other is active in section 3. Formally:** 

if the following conditions hold:

1. both thread is active at some point in time
2. each check under 3 is executed before threads become passive

then one of the threads detects that the other thread is active.

Proof:
 
1. Assume that thread `0` did not detect thread `1` as active. 
2. Due to condition 2. this means that thread `1` activated itself only after thread `0` executed its check. (The other scenario when thread `1` passivated itself before the check is not allowed by condition 2.)
3. This also means that thread `1` activated itself later than thread `0`. 
4. However this means that thread `1` executed the check after thread `0` was already active. 
5. Due to condition 2 thread `1` detected thread `0` as active.

The Select 2 protocol has another feature, namely it is lock free. In order to see what it means, we need to examine the protocol's application:

**Statement 2: Select 2 is wait-free in: each thread that entered the protocol terminates in finite steps**

Proof: Only the token owner has a conditional wait in section 3.2., a thread who does not own the token obviously terminates in finite steps.

Assume that thread `0` is a token owner and it entered the conditional wait in section 3.2. At some time before this event the other thread was active as well, otherwise thread `0` would not go into the loop. At that time, when it was detected active, the other thread, thread `1` could not be token owner as well (`token != 1`). Hence the other thread finishes in finite steps. Then there are two possible scenarios:

1. The next loop cycle comes before thread `1` becomes active again. Since the loop checks if the other thread is passive, it will exit in the next cycle (because `active[0]` becomes false).
2. It is also possible that thread `1` becomes active again between two loop cycles. In this case there are two possible scenarios:
   1. Thread `1` exited the previous selection w/o taking the token. In this case it will wake up thread `0` in section 3.1, in finite steps, hence the loop exits (because `wait[0]` becomes false).
   1. Thread `1` exited the previous selection by taking the token, ie. it became the new token owner. Since token ownership won't change until thread `0` is in the wait loop and the loop checks if the token ownership changed, the loop will exit in the next cycle (because `token == 0` becomes false).

### Application protocol ###

In order to use the protocol to execute critical sections it must be extended in the following way:

During the selection period (while the thread is selected) the thread can execute some injected code (closures or kinda). The pseudo code is the following:

    selected[i] = true
    block()
    selected[i] = false

, where `block` is a black box function (closure or such) injected into the selection protocol.

The Select 2 protocol in the above manner is lock free and safe:

**Statement 3: The extended Select 2 protocol is safe in the following manner: blocks are never executed in parallel.**

Proof: When blocks are executed in a thread, that thread must be selected. However due to Statement 1, threads are never selected in parallel, hence blocks are never executed in parallel.

**Statement 4: The extended Select 2 protocol is wait-free in the following manner:**

1. **The execution of one thread does not depend on (wait for) the execution of the other thread.**
1. **If the injected block is wait-free then the whole Select 2 system is wait-free as well.**

Proof: 

1. Select 2 logic does not depend on the injected block.
2. According to Statement 2 above Select 2 itself is wait-free. Hence if the business logic is wait-free as well, then the whole is wait-free, too.

Notes: 

* The above safety feature guarantees that critical sections are executed atomically or sequentally and never in parallel. However Select 2 does not guarantee that the block will be ever executed. It only guarantees that if it is executed than no other block is executed in parallel.
* Independency does not mean that the business code running in one thread cannot block the other thread. There could be situations when the scheduler does not let other threads run before the business code exits. 

### TODO ###

* Revise the proof according to lemma changes
* Check the proof in multiprocessor envs
* Formal verification through code
* Benchmark the API
* Extend the protocol (ie. handle more threads)