select2 synchronization primitve
===============================================================================


Summary
-------------------------------------------------------------------------------

### Protocol ###
 
**`select2` is a wait-free synchronization primitive, that selects one and only one thread from 2 threads** (thats why it is called `select2`). The core protocol provides the following guarantees:

* **safe**: one and only one thread is selected at any time
* **wait-free**: the selection protocol eventually terminates (ie. in finite steps)

**The primitive can be used to safely execute critical sections.** The application protocol provides similar guarantees as the core one: it can be used to execute critical sections with the following guarantees:

* **safe**: one and only one code is executed at any time (ie. does not occur parallel execution)
* **wait-free**: if the code injected to the thread is wait-free then the whole thread is wait-free, will eventually terminate

### Implementation ###

The application protocol is **implemented in both Java and Scala.** 

**Both API builds upon the Java-builtin primitive: `volatile`.** Otherwise it does not use anything else, especially **it does not use either `synchronization` or `atomic values`**.

I'm not sure whether the API is of any interest, but here are some thoughts:

* *Soft synchronization primitive*: I'm not a system/JVM engineer hence I'm not sure, just guess that the API doesn't build upon hardware support (ie. CAS). Someone should verify this:-)
* *Java 1.4- support*: As far as I see the API could be extended to work with earlier Java versions prior to 1.5.
* *Promising benchmarks*: The early benchmark results are promising:-) (see details below).

### Status ###

Both the theoretical protocol and the API is in its **infancy**. As of this writing it is 3 days old:-)

Both of them requires **external revisions**. Any feedback is greatly appriciated:-)

Protocol specification
-------------------------------------------------------------------------------

### Datums ###


Threads are numbered as `0` and `1` in the protocol.

The protocol uses the following fields:

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* `token: 0..1` - only that thread can be chosen who owns the token (ie. thread `0` can be chosen if `token == 0`)
* `selected: boolean[2]` - marks whether thread `0` or `1` is selected
* `wait: boolean[2]` - marks whether thread `0` or `1` is waiting

### Core protocol ###

_Note that `i + 1` means `(i+1) % 2` in the following sections._

Assume that thread `i` enters the selection (`i = 0 or 1`). Then the pseudo code of the protocol is the following:

-- 1. mark myself as active

    active[i] = true

-- 2. check whether I am the token owner

    token_owner = token == i

-- 3. check whether the other thread already entered the selection protocol

    if active[i + 1]

-- -- 3.1. if I am not the token owner, wake the owner up, cleanup and exit

       if !token_owner 
           wait[i+1] = false
           active[i] = false
           return false

-- -- 3.2. if I am the token owner, wait till either the other thread changes its state or I am waked up
       
       else 
		  wait[i] = true
	      while token == i and active[i + 1] and wait[i]
             yield
          wait[i] = false

-- 4. finish the protocol, where different cases could happen

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

### Core features ###

**Statement 1: `select2` is safe in the following manner: the protocol guarantees that one and only one thread is selected at any time. Formally:  `not selected[0] or not selected[1]` always holds.**
	
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

**Statement 2: `select2` is wait-free in the following manner: each thread that entered the selection protocol terminates in finite steps**

Proof: Only the token owner has a conditional wait in section 3.2., a thread who does not own the token obviously terminates in finite steps.

Assume that thread `0` is a token owner and it entered the conditional wait in section 3.2. At some time before this event the other thread was active as well, otherwise thread `0` would not go into the loop. At that time, when it was detected active, the other thread, thread `1` could not be token owner as well (`token != 1`). Hence the other thread finishes in finite steps. Then there are two possible scenarios:

1. The next loop cycle comes before thread `1` becomes active again. Since the loop checks if the other thread is passive, it will exit in the next cycle (because `active[0]` becomes false).
2. It is also possible that thread `1` becomes active again between two loop cycles. In this case there are two possible scenarios:
   1. Thread `1` exited the previous selection w/o taking the token. In this case it will wake up thread `0` in section 3.1, in finite steps, hence the loop exits (because `wait[0]` becomes false).
   1. Thread `1` exited the previous selection by taking the token, ie. it became the new token owner. Since neither thread would change token ownership again until thread `0` is in the wait loop and the loop itself checks whether the token ownership changed, the loop will exit in the next cycle (because `token == 0` becomes false).

### Application protocol ###

In order to use the protocol to execute critical sections it must be extended in some way. The extended protocol works like this:

During the selection period (while the thread is selected) the thread can execute some injected code (closure or kinda). The pseudo code is the following:

    selected[i] = true
    block
    selected[i] = false

, where `block` is a black box function (closure or such) injected into the selection protocol.

The extended Select 2 protocol is safe and wait-free in the following manner:

### Application protocol features ###

**Statement 3: The `select2` application protocol is safe in the following manner: blocks are never executed in parallel.**

Proof: When blocks are executed in a thread, that thread must be selected. However due to Statement 1, threads are never selected in parallel, hence blocks are never executed in parallel.

Note that this safety feature just guarantees that critical sections are executed atomically / sequentally and never in parallel. However it does not guarantee that the block will be ever executed. This is where `Select 2` differs from the `synhronization` primitive. Both are safe, however while `synhronization` blocks until execution, `Select 2` neither blocks nor does it guarentee execution. 

**Statement 4: The `select2` application protocol is wait-free in the following manner. If the block injected to the thread is wait-free then the whole thread will be wait-free as well. In other words: if the thread gets enough processor time then it will eventually terminate.**

Proof: 

Select 2 logic does not depend on the injected code block of either thread and according to Statement 2 the protocol itself is wait-free. Hence if the injected block is wait-free as well, then the whole thread becomes wait-free, too.


Implementation
-------------------------------------------------------------------------------

### Platforms ###

The protocol is currently implemented in Java and Scala. See `src/java` and `src/scala`. The code builds upon the Java-builtin synchronization primitive: `volatile` and nothing else. Especially it does not use either `synchronization` or `atomic values`.

Note that the above means that the API (with some possible modifications) might be used for earlier versions of Java prior to 1.5. Some modifications might be necessary since the behaviour of `volatile` changed in Java 1.5.

### Code quality ###

The API is experimental. Especially the Java API is more extensively tested then the Scala one.

### Empirical evidence ###

There are some empirical evidences that the Java API works, not just the above formal proof. The `src/java/debug` package implements a debugable version of the API. With this I've tested thousands of concurent scenarios w/o failure. Of course this doesn't replace the formal proof.

### Benchmark ###

A microbenchmark is implemented for the Java version. See `src/java/bench`.

The `select2` application service was reimplemented based upon the Java-builtin synchronization primitive: `compare and set`. That is to say this code implements the above features but using a different algorithm and a different primitive (ie. `AtomicInteger`).

The microbenchmark compares the custom `volatile` based implementation and the `CAS` based one. The early results are promising:

In my machine the custom, `volatile` based implementation runs 2-3x faster then the `CAS` one. Of course this is an orange-apple comparision, since the `select` protocol (in its current form) deals with only 2 threads. Still the benchmark shows that the API might perform well and even better than the Java builtin at least for small number of threads.

### Demo ###

Both the Java and the Scala API provides a simplified clipboard implementation for demo purposes - see the `Clip2` class. This provides the following features:

* one can `push` an object to the clipboard which then 
* can be `popped`

The clipboard follows the following (synchronization) protocol:

* after `pushing` and object no more object can be `pushed` till the current one is `popped`
* also if an object is `popped` then no more object could be `popped` until a new one is `pushed` onto the clipboard

Since it is a demo of the `select2` protocol it handles only 2 threads and not more.

TODO
-------------------------------------------------------------------------------

The most important one: 

**External revisions are necessary both for the proof and the API. Any feedback is highly appreciated! :-)**

### Protocol ###

* Revise the proof of Statement 1:
  * according to lemma changes
  * clarify this: token owner has two meanings: `token == i` at any time and `token_owner is true` - the two might not be the same  
* Revise the proof assuming multiprocessor env

### API ###

* More testing and 
benchmarking
* More demos

### Extensions ###

* Handle more threads
* Implement other primitives in the 'Select-style', such as: increment/decrement, ring buffer