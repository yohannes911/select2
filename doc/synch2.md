synch2 synchronization primitive
===============================================================================


Summary
-------------------------------------------------------------------------------

### Protocol ###
 
**The `synch2` synchronization primitive is a [wait-free](http://en.wikipedia.org/wiki/Non-blocking_algorithm#Wait-freedom) protocol, that selects threads from 2 threads in a synchronized manner**. The core protocol provides the following guarantees:

* **safe**: at most one thread is selected at any time, that is threads are not selected in parallel
* **wait-free**: the selection eventually terminates, each thread is selected in finite steps

**The primitive can be used to safely execute critical sections.** The application protocol provides similar guarantees as the core one: it can be used to execute critical sections with the following guarantees:

* **safe**: at most one code is executed at any time, parallel execution never happens
* **wait-free**: the code injected to the thread will be eventually executed

This protocol is similar to [`select2`](select2.md)), however this version mimicks the Java `synchronization` primitive.

### Implementation ###

TODO


Protocol specification
-------------------------------------------------------------------------------

Threads are numbered as `0` and `1` in the protocol.

### Pseudo code ###
The protocol uses the following fields:

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* `selected: boolean[2]` - marks whether thread `0` or `1` is selected
* `wait0, wait1a, wait1b` - marks that thread `0` or `1` is in conditional wait

_Note that `i + 1` means `(i+1) % 2` in the following sections._

Assume that thread `i` enters the selection (`i = 0 or 1`). Then the pseudo code of the protocol is the following:

-- 1. `activate(i)`: mark myself as active

    active[i] = true

-- 2. `guard(i)`: check whether the other thread already entered the protocol, if yes the thread will go into some conditional wait

    if active[i + 1]

-- -- 2.1. `wait0`: in case of `thread 0` it goes to conditional wait which loops until the other tread either becomes inactive or wakes up `thread 0`

       if i == 0
           wait0 = true
	       while active[1] and wait0
              wait1a = false
              yield

-- -- 2.2. in case of `thread 1` there are two different potential wait scenarios

      if i == 1 

-- -- -- 2.2.1. `wait(1a)`: if the thread is not marked to wait in `wait1b` then it goes into `wait1a` and stays there until the other tread either becomes inactive or wakes up `thread 1`

          if !wait1b
              wait1a = true
	          while wait1a and active[0]
                  yield

-- -- -- 2.2.2. `wait(1b)`: if the thread is marked to wait in `wait1b` then it goes into there and stays until the other tread either becomes inactive or wakes up `thread 1`

          else 
              wait1b = true
	          while wait1b and active[0]

-- -- -- -- 2.2.2.1. `wakeup(0)`: within the loop in each cycle `thread 1` (potentially) wakes up the other thread from `wait0` wait

                  wait0 = false

                  yield

-- 3. `select`: mark myself as selected, then cleanup (including unselect) and exit

-- -- 3.1. `select(i)`: mark myself as selected

     selected[i] = true

-- -- 3.2. `deselect(i)`: mark myself as unselected

     selected[i] = false

-- -- 3.3. `wait_later(1b)`: here `thread 1` marks itself to wait in `wait1b` in the next round(s)

     if i == 1 then wait1b = true

-- -- 3.4. `wakeup(1b)`: here `thread 0` turns off the `wait1b` flag which wakes up thread `1` from the `wait1b` loop (if it is there) or redirect it to `wait1a`

     if i == 0 then wait1b = false

-- -- 3.5. `deactivate(i)`: finally deactivate myself

	 active[i] = false

### Features ###


**Theorem 1: The protocol is wait-free: if a thread enters the protocol it will be (1) eventually selected and (2) terminate in finite steps**.


**Theorem 2: The protocol is safe: threads are never selected in parallel.**

### Proof ###

The proof is based on an I/O automaton model which corresponds to the above pseudo code.

#### I/O automaton ###

states:

    active(i): Boolean
    wait(i): Boolean | i = 0, 1a, 1b 
    selected(i): Boolean
    state(i): # states shared between threads 
        IDLE # starting state
        GUARD
        SELECT
        DESELECT 
        DEACTIVATE
    state(0):
        WAIT 
        WAKEUP(1a)
        WAKEUP(1b)
    state(1): 
        WAIT(1a)
        WAIT(1b)
        WAKEUP(0)
        NEXT_WAIT(1b)

input events:

    activate(i)

output events:

    select(i)
    deselect(i)
    deactivate(i)

internal events:

    guard(i)
	wait(i), wakeup(i), wokeup(i) | i = 0, 1a, 1b
    nextwait(1b)

#### activate ####

    activate(i): # initial action
		post_condition: 
			if state(i) = IDLE
			    active(i) = true
			    state(i) = GUARD
            // else does nothing - reentrant execution is not supported

#### guard of thread 0 ####

    guard(0): # checks whether thread 1 is active
		pre_conditions: 
			state(0) = GUARD
        post_conditions:			
			if active(1) then 
                state(0) = WAIT
                wait(0) = true
			else state(0) = SELECT

    wait(0): # conditional wait of thread 0
		pre_conditions: 
			state(0) = WAIT
			wait(0) and active(1)
        post_conditions: 
			state = WAKEUP(1a)

    wakeup(1a): # wakes up thread 1 from wait1a, goes back to conditional wait
		pre_conditions: 
			state(0) = WAKEUP(1a)
        post_conditions: 
			state(0) = WAIT
			wait(1a) = false

    wokeup(0) # triggered when thread 0 is either waken up by thread 1 or the latter becomes inactive
		pre_conditions: 
			state(0) = WAIT
			not wait(0) or not active(1)
        post_conditions: 
			state(0) = SELECT

#### guard of thread 1 ####

    guard(1): # checks whether thread 0 is active
		pre_conditions:
			state(1) = GUARD
        post_conditions:			
			if active(0) then
				if not wait(1b)
					state(1) = WAIT(1a) 
                    wait(1a) = true
				else
					state(1) = WAIT(1b)
			else 
				state(1) = SELECT

##### wait1a #####

    wait(1a): # conditional wait of thread 1 if thread 0 was active
		pre_conditions: 
			state(1) = WAIT(1a)
			wait(1a) and active(0)
    
    wokeup(1a) # triggered when thread 1 is waken up from wait1a by thread 0
		pre_conditions: 
			state(1) = WAIT(1a)
			not wait(1a) or not active(0)
        post_conditions: 
			state(0) = SELECT

##### wait1b #####

    wait(1b): # conditional wait of thread 1 if thread 0 was active
		pre_conditions: 
			state(1) = WAIT(1b)
			wait(1a) and active(0)
        post_conditions: 
			state(1) = WAKEUP(0)

    wakeup(0): # wakes up thread 0, goes back to conditional wait
		pre_conditions: 
			state(1) = WAKEUP(0)
        post_conditions: 
			state(1) = WAIT(1b)
			wait(0) = false
    
    wokeup(1b) # triggered when thread 1 is waken up from wait1b by thread 0
		pre_conditions: 
			state(1) = WAIT(1b)
			not wait(1b) or not active(0)
        post_conditions: 
			state(0) = SELECT

#### select thread i ####

    select(i) # selects thread i
		pre_condition: state(i) = SELECT
		post_conditions: 
			selected(i) = true
            state(i) = DESELECT

#### deselect thread 0 ####

    deselect(0) # deselects thread 0
		pre_condition: state(0) = DESELECT
		post_conditions: 
			selected(0) = false
			wait(1b) = false
            state(0) = WAKEUP(1b)

    wakeup(1b) # wakes up thread 1 from wait1b
		pre_condition: state(0) = WAKEUP(1b)
		post_conditions: 
			selected(0) = false
			wait(1b) = false
            state(0) = DEACTIVATE

#### deselect thread 1 ####

    deselect(1) # deselects thread 0
		pre_condition: state(1) = DESELECT
		post_condition: 
			selected(1) = false
            state(1) = NEXT_WAIT(1b)

    next_wait(1b) # marks that next wait goes into wait1b
		pre_condition: state(1) = NEXT_WAIT(1b)
		post_conditions: 
			wait(1b) = false
            state(1) = DEACTIVATE

#### deactivate thread i ####

	deactivate(i) # deactivates thread i
		pre_condition: state(i) = DEACTIVATE
		post_condition: 
			active(i) = false
            state(i) = IDLE

**Proof of Theorem 1**: 

Remind that Theorem 1 states that 

> **The protocol is wait-free: if a thread enters the protocol it will be (1) eventually selected and (2) terminate in finite steps**

Formally this means that each `activate(i)` input event is eventually followed by a  `select(i)` then a `deactivate(i)` event.

It's evident that if the thread terminates then it was selected before, formally the (partial) history per thread flows like this:

    thread 0: ..., SELECT, select(0), DESELECT, deselect(0), WAKEUP(1b), wakeup(1b), DEACTIVATE, deactivate(0)

    thread 1: ..., SELECT, select(1), DESELECT, deselect(1), NEXT_WAIT(1b), next_wait(1b), DEACTIVATE, deactivate(1)

Hence it's enough to prove the second statement.

Indirectly assume that either `thread 0` or `thread 1` never terminates. 

_Case 1 - `thread 0` never terminates_

(1) `thread 0` must be blocked in the `WAIT(0)` loop, namely it will oscillate between state `WAIT` and `WAKEUP(0)` forever. Remind that: 

>     wait(0): # conditional wait of thread 0
> 		pre_conditions: 
> 			state(0) = WAIT
> 			wait(0) and active(1)
>         post_conditions: 
> 			state = WAKEUP(1a)
> 
>     wakeup(1a): # wakes up thread 1 from wait1a, goes back to conditional wait
> 		pre_conditions: 
> 			state(0) = WAKEUP(1a)
>         post_conditions: 
> 			state(0) = WAIT
> 			wait(1a) = false

Hence `thread 0` (partial) history should look like this:

    WAIT, wait(0), WAKEUP(0), wakeup(0), WAIT, wait(0), ...

(2) `thread 1` must remain active for infite many times, after `thread 0` went into its infinite loop. Otherwise `thread 0` will eventually wake up, since `not active[1]` is one of the wait conditions:

>     wokeup(0) # triggered when thread 0 is either waken up by thread 1 or the latter becomes inactive
> 		pre_conditions: 
> 			state(0) = WAIT
> 			not wait(0) or not active(1)
>         post_conditions: 
> 			state(0) = SELECT

 
This means that `thread 1` is either blocked in a wait loop infitely as well or enters the protocol in infinite many times.

(3) If `thread 1` entered `WAIT(1b)` at any time, then it would eventually wake up `thread 0`:

>     wait(1b): # conditional wait of thread 1 if thread 0 was active
> 		pre_conditions: 
> 			state(1) = WAIT(1b)
> 			wait(1a) and active(0)
>         post_conditions: 
> 			state(1) = WAKEUP(0)
> 
>     wakeup(0): # wakes up thread 0, goes back to conditional wait
> 		pre_conditions: 
> 			state(1) = WAKEUP(0)
>         post_conditions: 
> 			state(1) = WAIT(1b)
> 			wait(0) = false

Hence `thread 1` can never enter `WAIT(1b)` after `thread 0` went into its infinite loop.

(4) It follows from (2, 3) that `thread 1` is either blocked in the `WAIT(1a)` loop or enters this loop always and in infinite many times. None of them is possible:

It cannot be blocked in `WAIT(1a)` since the blocked `thread 0` would eventually wake it up, just as the following history illustrates:

    [WAIT, WAIT(1a)], wait(0), [WAKEUP(0), WAIT(1a)], 
    wakeup(0), [WAIT, WAIT(1a), wait(1a)=false], wokeup(1a)

It cannot always and infinitely enter `WAIT(1a)` since it issues `nextwait(1b)` just before terminating a round, ie. its history is kinda this:

	DESELECT, deselect(1), NEXT_WAIT(1b), next_wait(1b), DEACTIVATE, deactivate(1),...

Remind that:

>     deselect(1) # deselects thread 0
> 		pre_condition: state(1) = DESELECT
> 		post_condition: 
> 			selected(1) = false
>             state(1) = NEXT_WAIT(1b)
> 
>     next_wait(1b) # marks that next wait goes into wait1b
> 		pre_condition: state(1) = NEXT_WAIT(1b)
> 		post_conditions: 
> 			wait(1b) = false
>             state(1) = DEACTIVATE

This means that in the next round `thread 1` will enter  the `WAIT(1b)` loop, since the blocked `thread 0` won't switch the `wait(1b)` flag back to `false`. Remind that:

>     guard(1): # checks whether thread 0 is active
> 		pre_conditions:
> 			state(1) = GUARD
>         post_conditions:			
> 			if active(0) then
> 				if not wait(1b)
> 					state(1) = WAIT(1a) 
> 				else
> 					state(1) = WAIT(1b)
> 			else 
> 				state(1) = SELECT

Hence `thread 1` will enter `WAIT(1b)`:

	IDLE, activate(1), GUARD, guard(1), WAIT(1b), ...

From (1, 2, 3, 4) contradiction follows.

_Case 2 - `thread 1` never terminates_

The thread must be either blocked in the `WAIT(1a)` loop or in the `WAIT(1b)` one. None of them could happen. It cannot be blocked in `WAIT(1a)` forever, since either `thread 0` will eventually wake it up if it becomes active again (see `wakeup(1)`), or the `not active(0)` condition would trigger a wake up, if `thread 0` remains deactivated (see `wokeup(1)`). The same applies to `WAIT(1b)`. Contradiction.

It is left to the reader to prove that `synch2` is not just wait-free but is **strongly wait-free**, ie. the steps necessary to terminate is not just finite, but is bounded.

In order to prove Theorem 2 we first need some Lemmas:

**Lemma 1: If both thread is active at some point in time, then one will detect the other thread as active and hence go into a conditional wait.** Formally:

if both thread is active at some point in time and executes its guard before the other thread deactivates itself, formmaly:

    (1) activate(i) < guard(i) < deactivate(i + 1) : i = 0, 1
    
then at least one of the threads detects the other one as active, hence goes into the wait loop, formally:

    (2a) activate(0) < guard(1) < wait(1j) : j = a or b

    or
  
    (2b) activate(1) < guard(0) < wait(0)

_Proof_: One of the threads issued its guard later than the other - that thread must detect the other one as active. Formally: w/o breaking generality we can assume that `thread 1` issued its guard later:

    (3) guard(0) < guard(1)

Since executing the guard statement always happens after activation:

    (4) activate(0) < guard(0)

it immediately follows from (1, 3, 4) that `thread 1` guard was executed after `thread 0` activated itself, but before it deactivated itself:

    (5) activate(0) < guard(1) < deactivate(0)

hence `thread 1` detected `thread 0` as active and went into its wait loop:

    activate(0), ..., GUARD, guard(1), WAIT(1j), wait(1j), ...

hence `activate(0) < guard(1) < wait(1j)`.


The proof of Theorem 2 is an indirectional one. Here we prove two further lemmas that would be the result of the indirect assumption:

**Lemma 2: if both thread is selected and one of them was in a wait loop before it was selected then the other thread woke it up in a previos round**. Formally:

if both thread is selected in some `r0` and `r1` rounds

    (1) selected[r0][0] and selected[r1][1]

and one thread went into a conditional wait loop

    (2) wait[ri](i) < selected[ri][i] : i = 0 or 1
 
then the other thread woke it up

    (3) wait[ri](i) < wakeup[ri+1'](i)

in a previous round

    (4) ri+1' < ri+1
 
Proof: Indirectly assume that the thread in wait loop was woken up by the other thread in the same round (`ri+1' = ri+1`):

    (5) wait[ri](i) < wakeup[ri+1](i)

There are three cases depending on the type of the wait loop:

_Case 1a - `thread 1` was in `wait1a` loop_: Hence (5) means that `thread 0` woke it up in the same round when it was selected:

    (6) wait[r1](1) < wakeup[r0](1) < select(0)

However `thread 0` can wake up `thread 1` from `wait1a` only in two ways: it is either deactivated (hence after unselected) or it wakes it up from the `wait0` loop. The former is impossible since both thread is selected. Hence `thread 0` woke the other one up from `wait0`:

    (7) wait[r0](0) < wakeup[r0](1)

That also means that `thread 0` should have been woken up by `thread 1` in order to be selected:

    (8) wait[r0](0) < wakeup[r1'](0) < select[r0](0)

However `thread 1` could not wake it up in a previous round (`r1' < r1` is impossible). 

_Case 2 - `thread 0` was in `wait0` loop_: Hence (5) means that `thread 1` woke it up in the same round:

    () wait[r0](0) < wakeup[r1](0)

However `thread 1` can wake up `thread 0` only in two ways: it either deactivated (hence after unselected) or it wakes it up from `wait1b` loop...

.........................


_Proof of Theorem 2_: 

Indirectly assume that some point in time both thread is selected.

    (1) selected[0] and selected[1]

Due to the Lemma, at least one of the threads went into its wait loop. The threads fulfill the lemma conditions due to the indirect assumption, ie.:

    (2) selected[0] and selected[1] => selected(0) < unselected(1) < deactivate(1)

in natural words: due to the indirect assumption `thread 0` was selected before `thread 1` became unselected and then deactivated. 

Meanwhile the guard runs before the thread is selected:

    (3) guard(0) < selected(0) 

Hence it follows from (2,3) that `guard(0) < deactivate(1)`. The other conditions could be proved similarly.

There could be many cases: 

_Case 1 - `thread 0` entered `wait0` but `thread 1` did not enter conditional wait`_

_Case 2a - `thread 1` entered `wait1a` but `thread 0` did not enter conditional wait_

_Case 2b - `thread 1` entered `wait1b` but `thread 0` did not enter conditional wait_

_Case 3a - `thread 0` entered `wait0` and `thread 1` entered `wait1a`_

_Case 3b - `thread 0` entered `wait0` and `thread 1` entered `wait1b`_


### Application protocol ###

In order to use the protocol to execute critical sections it must be extended in some way. This works like this:

During the selection period (while the thread is selected) the thread can execute some injected code (closure or kinda). The pseudo code is the following:

    selected[i] = true
    block
    selected[i] = false

, where `block` is a black box function (closure or such) injected into the selection protocol.

**The `synch2` application protocol is wait-free and safe - however there is one precondition: in order to be wait-free the injected code block must be wait-free as well.** Obviously the protocol cannot itself guarantee anything about a black box function. That is its left to the developer to avoid dead-locks and kinda in the business logic.
vagy

    (2) wait(0) < wakeup(r', 0) < after_wait(0) < selected(0) < inactive(0)

Az első nem lehetséges. Az inaktiválásnak korábbi körben kellett történnie (r'< r), hiszen különben nem lenne mindkettő kiválasztva. Ekkor viszont thread 1 be kellett menjen a waitbe, ami nem történt meg.

    unselected(0) < inactive(0) < check(r, 0) < selected(1) 

A második az r roundba csakkor történhetett meg, ha bement volna a wait(1b)-be, ami nem igaz, így r' < r. Node akkor 1 bement volna a wait-be. Hiszen különben inaktiválnia kellet volna magát 0-nak:

    unselected(0) < inactive(0) < check(r, 0) < selected(1) 

TODO
-------------------------------------------------------------------------------

* external revision
* API implementation
* reentrant feature