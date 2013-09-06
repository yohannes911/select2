synch2 synchronization primitive
===============================================================================


Protocol specification
-------------------------------------------------------------------------------

Threads are numbered as `0` and `1` in the protocol.

**pseudo code**: Assume that thread `i` enters the selection (`i = 0 or 1`). Then the pseudo code of the protocol is the following:

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
               case if i == 1: 
                   wait1 = true; 
                   while active[0] and wait1: yield()
               state[i] = SELECT
        else: 
            state[i] = SELECT

    # select
    select(i)
    deselect(i)
    unwait1() = 
        if i == 0 and wait1:
            wait1 = false
        state[i] = DEACTIVATE
    deactivate(i) = active[i] = false


**Wait-free**: If a thread enters the `synch2` protocol, then it will be (1) eventually selected and (2) terminate in finite steps.

Proof: Formally this means that each `activate(i)` input event is eventually followed by a  `select(i)` event and then a `deactivate(i)` event.

It's obvious that if a thread terminates then it was selected before. Hence it's enough to prove that the thread eventually terminates.

Indirectly assume that either thread 0 or thread 1 never terminates. 

_Case 1 - `thread 0` never terminates_

(1) This case `thread 0` must be blocked in the `wait(0)` loop. Its history should look like this:

    guard(0), [state[0]=WAIT], wait(0)

which is not followed by any more `thread 0` event.

(2) `thread 1` must remain active for infite many times, after `thread 0` went into its infinite loop. Otherwise `thread 0` will eventually wake up, since its wait condition `active[1]` becomes false:

>     wait(i) =
>         case if i == 0: while active[1] and not wait1: yield()
>         ...

This means that `thread 1` is either blocked in a wait loop infitely or enters the protocol in infinite many times.

(3) If `thread 1` is blocked in `wait(1)` forever then its wait flag is set to true and remains true since `thread 0` is blocked:

>     wait(i) =
>         ....
>         case if i == 1: 
>             wait1 = true; 
>             while active[0] and wait1: yield()

Hence `thread 0` will detect that one of its wait conditions, `not wait1` became false and wake up. 

Therefore `thread 1` will enter the protocol in infinite many times. However at some point in time it will then detect that `thread 0` is active, go into the `wait(1)` loop and stay there forever, since `thread 0` is blocked. Hence we fall back to the previous case: `thread 0` will eventually detect that `thread 1` is waiting and then `thread 0` will wake up from its own wait loop.

After all there's no way that `thread 0` could be blocked forever.

_Case 2 - `thread 1` never terminates_

(1) This case `thread 1` must be blocked in the `wait(1)` loop. Its history should look like this:

    guard(1), [state[1]=WAIT], wait(1)

which is not followed by any more `thread 1` event.

(2) `thread 0` must remain active for infite many times, after `thread 1` went into its infinite loop. Otherwise `thread 1` will eventually wake up, since its wait condition `active[0]` becomes false:

>     wait(i) =
>         ....
>         case if i == 1: 
>             wait1 = true; 
>             while active[0] and wait1: yield()


This means that `thread 0` is either blocked in a wait loop infitely or enters the protocol in infinite many times. However we just proved that thread 0 cannot be blocked, therefore

(3) `thread 0` will enter the protocol in infinite many times, hence it will enter after `thread 1` was blocked in its `wait(0)` loop, as well. Since `thread 0` terminates in finite steps it will set the `wait1` flag to false, which wakes `thread 0` up:

>     select(i)
>     deselect(i)
>     unwait1() = 
>         if i == 0 and wait1:
>             wait1 = false
>         state[i] = DEACTIVATE
>     deactivate(i) = active[i] = false

This shows that `thread 0` cannot be blocked forever.

---

**Lemma 1**: If a code contains an activation and a deactivation of kind:

    (i)    activate(i) = active[i] = true  : i = 0, 1
    (ii)   deactivate(i) = active[i] = false  : i = 0, 1

and 

    (iii)  there's no other event which would set active[i] to false

and the code contains a guard of kind:

    (iv)   guard(i) = if active[i + 1]: wait(i) : i = 0, 1

and if the guard is executed after activation and before deactivation:

    (v)    activate(i) < guard(i) < deactivate(i) : i = 0, 1

then if running in parallel then one of the threads goes into the `wait(i)` section if the guards are executed before threads become inactive, that is

if guards are executed before threads become inactive

    (vi)   guard(i) < deactivate(i + 1) : i = 0, 1

then one of them goes into the wait loop:

    (vii)  guard(i) < wait(i) : i = 0 or 1

Proof: It is obvious that one of the threads issues its guard later than the other - that thread must detect the other one as active and hence go into `wait(i)`. Formally: assume that `thread i + 1` issued its guard later:

    (1)    guard(i) < guard(i + 1)

Since executing the guard statement always happens after activation, it follows from the (v, vi) conditions that `thread i + 1` guard was executed after `thread i` activated itself, but before it deactivated itself:

    (2)    activate(i) < guard(i) < guard(i + 1) < deactivate(i)

hence due to the (iii) condition `thread i + 1` detected `thread i` as active and went into its wait loop, that is

    (3)    guard(i + 1) < wait(i + 1)

---

**Safety**: The protocol is safe, threads are never selected in parallel.
 
Proof: Indirectly assume that both thread is selected in parallel.

`thread 0` history is either misses the `wait(0)` loop or it does contain it:

_Case 1 - `thread 0` did not enter the `wait(0)` loop_: it has such a history as:

    [state(0) = GUARD, active[0] = true, active[1] = false], guard(0), 
    [state(0) = SELECT, active[0] = true, active[1] = false], ..., select(0)

Since this case `thread 0` has not detected `thread 1` as active, hence `thread 1` did detect it as active and went into its own wait loop

    [state(1) = GUARD, active[0] = true, active[1] = true], guard(1), 
    [state(1) = WAIT, active[0] = true, active[1] = true], wait(1), ..., select(1)
    
In order to wake up `thread 1` at least one of its wait conditions `active[0]` or `wait1` must become false:

>     wait(i) =
>         ....
>         case if i == 1: 
>             wait1 = true; 
>             while active[0] and wait1: yield()


However when `thread 1` entered its wait loop `thread 0` was not just already active but already executed its guard statement as well, beacuse it executed its guard before `thread 1` became active:

    guard(0) < activate(1) < wait(1)

Hence `thread 1` can can wake up only after `thread 0` becomes unselected. Contradiction.


_Case 2 - `thread 0` did enter the `wait(0)` loop_: it has such a history as:

    [state(0) = GUARD, active[1] = true], guard(0), [state(0) = WAIT, active[1] = true], wait(0), ..., select(0)

`thread 0` could be waken up from its wait iff `thread 1` is either deactivated or entered its wait loop:

_Case 2a - `thread 1` is deactivated_: hence its history is kinda:

    deactivate(1), [state(0) = SELECT, active[1] = false], ..., select(0)

Since deactivation happens after selection, `thread 1` must have been deactivated in a previous round. However in the next round it then detects `thread 0` as active, hence goes into its `wait(1)` loop and stays there until `thread 0` is deselected then deactivated. Contradiction.

_Case 2b - `thread 1` entered its wait loop_: we fall back to the above case, since `thread 1` will stay in its loop until `thread 0` is deselected.


