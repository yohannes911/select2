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

### Application ###

I'm not sure whether the API is of any interest, but here are some thoughts:

* *Soft synchronization primitive*: I'm not a system/JVM engineer hence I'm not sure, just guess that the API, ie. `volatile` doesn't build upon hardware support (ie. CAS). Someone should verify this:-)
* *Promising benchmarks*: The early benchmark results are promising:-) (see details below).


Protocol specification
-------------------------------------------------------------------------------

### Datums ###


Threads are numbered as `0` and `1` in the protocol.

The protocol uses the following fields:

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* `selected: boolean[2]` - marks whether thread `0` or `1` is selected
* `wait0, wait1a, wait1b` - marks that thread `0` or `1` is in conditional wait

### Core protocol ###

_Note that `i + 1` means `(i+1) % 2` in the following sections._

Assume that thread `i` enters the selection (`i = 0 or 1`). Then the pseudo code of the protocol is the following:

-- 1. `activate(i)`: mark myself as active

    active[i] = true

-- 2. `guard(i)`: check whether the other thread already entered the protocol, if yes the thread will go into some conditional wait

    if active[i + 1]

-- -- 2.1. `wait(0):`

       if i == 0
           wait0 = true
	       while active[1] and wait0

-- -- -- 2.1.1. `wakeup(1a)`:

               wait1a = false

               yield

-- -- -- 2.1.2 `wakeup(0)`


-- -- 2.2. `wait(1)`:

      if i == 1 

-- -- -- 2.2.1. `wait(1a)`:

          if !wait1b
              wait1a = true
	          while wait1a and active[0]
                  yield

-- -- -- 2.2.2. `wait(1b)`:

          else 
              wait1b = true
	          while wait1b and active[0]

-- -- -- -- 2.2.2.1. `wakeup(0)`:

                  wait0 = false

                  yield

-- 3. `finish`: finish the protocol, mark myself as selected, then cleanup and exit

-- -- 3.1. `select(i)`: mark myself as selected

     selected[i] = true

-- -- 3.2. `deselect(i)`: mark myself as unselected

     selected[i] = false

-- -- 3.3. `wakeup(1b)`: potentially wake up thread `1` from it' `wait1b` loop

     if i == 0 then wait1b = false

-- -- 3.4. `wait_later(1b)`: mark that in the next round thread `1` will wait int `wait1b`

     if i == 1 then wait1b = true

-- -- 3.1. `deactivate(i)`: deactivate myself

	 active[i] = false

### Core features ###


**Theorem 1: The protocol is wait-free: if a thread enters the protocol it will be (1) eventually selected and (2) terminate in finite steps**.

_Proof_: It's evident that if the thread terminates then it was selected before. Hence it's enough to prove the second statement.

Indirectly assume that a thread, never terminates. There could be two cases, this is `thread 0` or `thread 1`

_Case 1 - `thread 0` never terminates_

Then `thread 0` is blocked in the `wait0` loop. Hence:

1. `thread 1` must be active in infite many times otherwise `thread 0` would wakeup due to `active[1] = false`. This means that `thread 1` is either blocked infitely as well or reenters the protocol in infinite many times.
1. If `thread 1` entered `wait1b` at any time, then it would wake up `thread 0`. Hence `thread 1` can never enter `wait1b` after `thread 0` went into the infinite loop.
1. It follows from 1., 2. that `thread 1` is either blocked in the `wait1a` loop or enters it always and in infinite many times. None is possible. It cannot be blocked in `waita` since the blocked `thread 0` would wake it up. It cannot always enter `wait1a` since it issues `wait1b = true` just before terminating a round. This means that in the next round it will enter  the`wait1b` loop since the blocked `thread 0` cannot switch back the flag to `false`.

Contradiction.

_Case 2 - `thread 1` never terminates_

It is either blocked in the `wait1a` loop or the `wait1b`. None could happen. It cannot be blocked in `wait1a` since either `thread 0` will wake it up if it becomes active later, or `active[0] = false` would trigger a wake up, if `thread 0` remains deactivated. The same applies to `waitb`.

Contradiction.

It is left to the reader to prove that `synch2` is not just wait-free but is **strongly wait-free**, ie. the steps necessary to terminate is not just finite, but is bounded.


**Theorem 2: The protocol is safe: threads are never selected in parallel.**. Formally `selected[0] and selected[1]` is never true.

We first need the following Lemma:

**Lemma: If both thread is active at some point in time, then one will detect the other  thread as active and hence go into conditional wait.** Formally:

if both thread is active at some point in time and executes its guard before the other  thread deactivates itself, formmaly: 

    activate(i) < deactivate(i + 1) : i = 0,1

    and

    guard(i) < deactivate(i + 1) : i = 0,1
    
then at least one of the threads detects the other one as active, hence goes into the wait loop, formally:

    wait(0) < deactivate(0) 

    or
  
    wait(1) < deactivate(1) 

_Proof_: TODO (similar to the one in [`select2`](select2.md))

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

1. Egyik detektálja

Vizsgálandó: a waitek közül melyik fordulhat elő párhuzamosan.

non-blocking:

* wait0 and wait1a => 1 felébred (wait1a=false), elvégzi a dolgát, passzív lesz
    * ha újra aktivizálódik, mielőtt 0 felébred és a wait1b-t false-ra állítja, akkor bemegy a 2.2-be és felébreszti a 0-t
    * ha csak később aktivizálódik, akkor a 0 felébred !active[1] miatt  
* wait0 and wait1b => 0 felébred, majd előbb-utóbb false-ra állítja a wait1b-t, így 1 felébred

safe:

* indirekte: selected[0] = selected[1], valamelyik bement a wait blokkban, a fentiek szerint a másik nem.
  * 0 bement wait0-ba, 1 elkerülte wait1x-et
    * 0 akkor jön ki, ha 1 passzív lesz, vagy felébresztik, egyik sem lehetséges
  * 1 bement wait1a-ba, 0 elkerülte wait0-t    
    * 1 akkor jön ki, ha 0 passzív lesz, vagy felébresztik, egyik sem lehetséges
  * 1 bement wait1b-be, 0 elkerülte wait0-t    
    * 1 akkor jön ki, ha 0 passzív lesz, vagy felébresztik, egyik sem lehetséges

wait:

* before_wait
* in_wait(x)
* after_wait(x)
* avoided_wait

* 0 bement wait0-ba, 1 elkerülte wait1x-et
  * 0 akkor jön ki, ha 1 passzív lesz, vagy felébresztik, egyik sem lehetséges

in_wait(0) < selected(0) < unselected(r, 1) => 

vagy inaktiválódott 1 vagy felébresztette 0-t:

    (1) wait(0) < inactive(r', 1) < selected(0)

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