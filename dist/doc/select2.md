select2 synchronization primitive
===============================================================================


Summary
-------------------------------------------------------------------------------

### Protocol ###
 
**The `select2` synchronization primitive is a [wait-free](http://en.wikipedia.org/wiki/Non-blocking_algorithm#Wait-freedom) protocol, that selects one and only one thread from 2 threads** (thats why it is called `select2`). The core protocol provides the following guarantees:

* **safe**: one and only one thread is selected at any time
* **wait-free**: the selection protocol eventually terminates (ie. in finite steps)

**The primitive can be used to safely execute critical sections.** The application protocol provides similar guarantees as the core one: it can be used to execute critical sections with the following guarantees:

* **safe**: one and only one code is executed at any time (it follows that parallel execution never happens)
* **wait-free**: if the code injected to the thread is wait-free then the whole thread is wait-free, will eventually terminate

### Implementation ###

The application protocol is **implemented in both Java and Scala.** 

**Both API builds upon the Java-builtin primitive: `volatile`.** Otherwise it does not use anything else, especially **it does not use either `synchronization` or `atomic values`**.

### Application ###

I'm not sure whether the API is of any interest, but here are some thoughts:

* *Soft synchronization primitive*: I'm not a system/JVM engineer hence I'm not sure, just guess that the API, ie. `volatile` doesn't build upon hardware support (ie. CAS). Someone should verify this:-)
* *Java 1.4- support*: As far as I see the API could be extended to work with earlier Java versions prior to 1.5. Since `atomic values` appeared in v.1.5 the API could be useful for earlier Java versions in order to implement lock-free algoritms.
* *Promising benchmarks*: The early benchmark results are promising:-) (see details below).

### Status ###

Both the theoretical protocol and the API is in its **infancy**. As of this writing it is 1 week old:-) It requires **external revision**. 

Any feedback is greatly appriciated!:-)

Protocol specification
-------------------------------------------------------------------------------

### Datums ###


Threads are numbered as `0` and `1` in the protocol.

The protocol uses the following fields:

* `active: boolean[2]` - marks whether thread `0` or `1` is active (entered the selection protocol)
* `token: 0..1` - only that thread can be chosen who owns the token (ie. thread `0` can be chosen if `token == 0`)
* `token_owner` - marks whether thread was the token owner when entered the selection (ie. for thread `0` it is equivalent with `token[0] == 0` at the point when it started executing the selection) 
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

-- -- 3.1. if I am not the token owner, possibly wake the owner up, cleanup and exit

       if !token_owner 
           wait[i+1] = false
           active[i] = false
           return false

-- -- 3.2. if I am the token owner, possibly wake the other thread up, wait till either the other thread changes its state or I am waken up
       
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

**Statement 1: `select2` is safe in the following manner: the protocol guarantees that one and only one thread is selected at any time.** Formally: 

1. At most one thread is selected, ie. `selected[0] and selected[1]` is always false
2. At least one thread is selected:
   1. If only one thread is active then it will be selected. Formally: if `active[i] and not active[i+1]` until the thread `i` exits then `selected[i]` will happen (before the thread exits the protocol).
   1. If both thread is active then at least one of them is selected. Formally: if `active[i] and active[i+1]` at some point in time then `selected[i] or selected [i+1]` will happen  (before each thread exits the protocol).

Note that the above does not mean that it is impossible that both thread is selected one after the other. It is in fact possible (the proof is left to the reader:-)). The thing which is impossible is that they are selected at the same time.
 
Proof: 

**at most one selected**: Indirectly assume, that at some point in time both thread is selected. There are 3 possible cases:

1. Both thread is token owner
1. None of the threads is token owner
1. One of the threads is token owner the other is not

Here by _token owner_ I mean that `token_owner` is true.

_Case 1 - Both thread is token owner_:

This means that both thread executed the above 4.2. section, hence at that time (when starting 4.2 section) both  `token_owner` and `token == i` was true for each thread. 

Assume that thread `0` entered section 4.2. first, it means that `token == 0` was true before thread `1` entered section 4.2. Since (due to the indirect assumption) thread `0` was selected when thread `1` was selected as well, the former did not issue `selected[0] = false` before the later, thread `1` entered section 4.2. Hence `token == 1` was false when thread `1` reached section 4.2., hence could not enter it to be selected. Contradiction.

_Case 2 - None of the threads is token owner_:  

Due to the following lemma one of the threads detects that the other one is active. Since none of the threads is token owner, the one who detected the other exits w/o being selected. Contradiction.

_Case 3 - One of the threads is token owner the other is not_

Then one of the threads, say thread `0` is the token owner, hence executed 4.2. section when selected. Meanwhile, the other thread, thread `1` executed section 4.3.

Again, due to the following lemma, one of the threads should have detected that the other one is active in section 3. Two cases might happen:

1. If thread `1` detected thread `0` as active than it exited since it was not the token owner, hence was not selected. Contradiction.
1. If thread `1` did not detect thread `0` as active, than thread `0` detected thread `1`. Since thread `0` was the token owner it went into the wait loop. Also since thread `1` did not detect thread `0` it did not wake it up in section 3.1 instead it went on and executed section 4.3. This is contradiction since  in this scenario thread `0` woke up after thread `1` took the ownership in section 4.3. (`token = i`) and for the same reason did not enter section 4.2, hence was not selected. Contradiction.

The following lemma is applicable beacuse if (indirectly) both thread is  selected at some point in time, then (1) both thread is active and (2) already executed the checks when selected. Hence the lemma conditions are true.

**at least one selected**: The first case is evident, ie. when only one thread is active, then obviously it will be selected. Otherwise if the threads run in parallel and one of them is not selected, then:

* if the non-selected thread exits at section 3.1, it means that the other one is active, it is the token owner and will be selected in section 4.2
* if the non-selected thread exits at section 4.1, it means that it was the token owner, but lost its ownership, hence the other thread is selected.


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

The `select2` protocol has another feature, namely it is wait-free:


**Statement 2: `select2` is wait-free in the following manner: each thread that entered the selection protocol terminates in finite steps**

Proof: Only the token owner has a conditional wait in section 3.2., a thread who does not own the token obviously terminates in finite steps.

Now indirectly assume that thread `0` is a token owner and it entered the conditional wait in section 3.2 and stays there forever. 

At some time before thread `0` went into the wait loop the other thread, thread `1` was active as well, otherwise thread `0` would not go into the loop. 

Now two case could have happed, at that time, when the other thread was detected active, that thread, thread `1` was not the token owner (`token_owner was false` for thread `1`) or it was (`token_owner was true` for thread `1`). 

_Case 1 - Thread `1` was not token owner_

Hence the other thread finishes in finite steps. 

_Case 2 - Thread `1` was token owner as well_

Since both threads was token owner when thread `0` detected thread `1` as active, someone must have changed and gave up ownership. However it couldn't be thread `0` since during the period of Section 1 - Section 3.2, thread `0` does not change token ownership. Hence the token ownership must have been changed by thread `1`.

So thread `1` gave up its ownership in section 4.2 and then it clearly terminated in finite steps.

So in each case the other thread, thread `1` terminates in finite steps after it was detected active by thread `0`.  Then there are two possible scenarios:

1. The next loop cycle comes before thread `1` becomes active again. Since the loop checks if the other thread is passive, thread `0` will break it in the next cycle (because `active[1]` becomes false).
2. It is also possible that thread `1` becomes active again between two loop cycles. In this case there are two possible scenarios:
   1. Thread `1` exited the previous selection w/o taking the token. Hence currently it is not the token owner. In this case it will wake up thread `0` in section 3.1, hence the loop breaks (because `wait[0]` becomes false and then it will be never changed back to true until thread `0` is in wait loop).
   1. Thread `1` exited the previous selection by taking the token, ie. it is currently the token owner, hence token value is `1`. Thread `1` cannot change this value until thread `0` is in the wait loop. This could be proven indirectly. If thread `1` changed the token while thread `0` was in the wait loop, then thread `0` would have been detected as active by thread `1`. Hence thread `1` would have entered the loop itself, too and could not exit it until thread `0` is waiting as well. Contradiction. So the token value is `1` which will wake up thread `0` (because `token == 0` is false).

The whole protocol is wait-free in the following manner as well:


### Application protocol ###

In order to use the protocol to execute critical sections it must be extended in some way. This works like this:

During the selection period (while the thread is selected) the thread can execute some injected code (closure or kinda). The pseudo code is the following:

    selected[i] = true
    block
    selected[i] = false

, where `block` is a black box function (closure or such) injected into the selection protocol.

The extended `select2` protocol is safe and wait-free in the following manner:

### Application protocol features ###

**Statement 3: The `select2` application protocol is safe in the following manner: at least one block is executed, however blocks are never executed in parallel.**

Proof: 

**at least one executed**: this formally means:

1. If the other thread is not active during the thread's own execution, that thread's code will be executed.
1. If two threads are active at any point in time then at least one one of them will be executed.

This follows from Statement 1: at least one thread is selected which then will execute its block.

**never executed in parallel**: When blocks are executed in a thread, that thread must be selected. However due to Statement 1, threads are never selected in parallel, hence blocks are never executed in parallel.

*Note*: This safety feature just guarantees that critical sections are executed atomically and the system as a whole moves on (at least one section is executed). However it does not guarantee that the block of a given thread will be ever executed. This is where `select2` differs from the Java `synhronization` primitive. Both primitive is safe, however while `synhronization` blocks until execution, `select2` neither blocks nor does it guarentee execution. 


**Statement 4: The `select2` application protocol is wait-free in the following manner**: If the block injected to the thread is wait-free then the whole thread will be wait-free as well. In other words: if the thread gets enough processor time then it will eventually terminate.

Proof: The `select2` logic does not depend on the injected code block of either thread and according to Statement 2 the core protocol itself is wait-free. Hence if the injected block is wait-free as well, then the whole thread becomes wait-free, too.


Implementation
-------------------------------------------------------------------------------

### Platforms ###

The protocol is currently implemented in Java and Scala - see the `Select2` class under `src/java` and `src/scala`. 

### Implementation logic ###

The code builds upon the Java-builtin synchronization primitive: `volatile` and nothing else. Especially it does not use either `synchronization` or `atomic values`. (This also means that the API (with some possible modifications) might be used for earlier versions of Java prior to 1.5. Some modifications might be necessary since the behaviour of `volatile` changed in Java 1.5.)

As said the Java implementation builds upon the Java-builtin `volatile` primitive. All protocol fields except `token_owner` is defined as `volatile`. To my understanding this ensures that the protocol steps cannot be reordered by the compiler, hence the protocol works.

_Note on earlier Java versions, prior to Java 1.5_: 

The behaviour of `volatile` [was changed in Java 1.5](http://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html#volatile):

> Under the old memory model, accesses to volatile variables could not be reordered with each other, but they could be reordered with nonvolatile variable accesses.

This means that the protocol might not work in earlier Java versions. 

Note also that the algoritm uses only one non-volatile, stack variable: `token_owner`. Hence a possible fix might be the change of this variable from stack var to a volatile one, ie. instead of the `boolean token_owner` stack variable use the `volatile int[] token_owner` member field. However no such fix is implemented, hence the current code is only for Java v1.5+.

### Build ###

You can build by hand of course, by using `javac` and `scalac`:-) However to make life easier an ant script is provided as well. Assuming that your environment is configured properly, you can execute build tasks as follows:

To build both the Java and the Scala API and package them into `dist/lib`, issue the following:

    > ant

This will compile both implementations and package them into `java-select.jar` and `scala-select.jar`

To build just the Java API:

    > ant java-dist

To build just the Scala API:

    > ant scala-dist 

See `build.xml` for more details.

### Quality ###

The API is experimental. Especially the Java API is more extensively tested then the Scala one. A simple functional test and microbenchmark is implemented to check the Java implementation.

#### Functional test ####

There are some empirical evidences that the Java API works, not just the above formal proof. The `src/java/debug` package implements a debugable version of the API. With this I've tested thousands of concurent scenarios w/o failure. 

*Note*: The test is a simplified one, is far from covering all possible, concurent execution paths. The sole purpose of the test is to add some empirical insight/evidence and not to replace the formal proof. Also anyone is welcome to provide a stronger testkit:-)

##### All scenarios (of specific length) #####

To run some test scenarios issue the following or such:

    java -ea -cp lib/java-select.jar select2.debug.DebugSelect2 -a 4 

This will test `2 ^ 4 - 2 = 14` possible concurent scenarios. Such a scenario looks like this: `0010` which means that:
 
1. `00..`: thread `0` will run 2 steps of the protocol, then
2. `..1.`: thread `1` will run 1 step of the protocol, then again
3. `...0`: thread `0` will run 1 step, then 

the whole starts from the begining (ie. it is cyclic)... 

Note that theoretically there are `2 ^ 4 = 16` such 4-length scenarios. However `0000` and `1111` does not count since they represent single threaded scenarios.

The above command will run one round, that is it terminates if each thread ran the protocol (at least) once. If you want more rounds, issue this or such:

    java -ea -cp lib/java-select.jar select2.debug.DebugSelect2 -a 2 4 

The above will run all possible 4-length scenarios, however it will only terminate if each thread ran the protocol (at least) twice.

##### Random scenarios #####

For larger scenarios running all combinations might be very time consuming. For instance there are more than 1 million 20-length scenarios. For this and such situations you can run random scenarios:

For instance to run random 40-length scenarios 10 times, issue:

    java -ea -cp lib/java-select.jar select2.debug.DebugSelect2 -r 10 40

If each thread has to run the protocol at least 3 times, then issue:

    java -ea -cp lib/java-select.jar select2.debug.DebugSelect2 -r 10 3 40

##### Specific scenario #####

Finally if you have a specific scenario, say `01001110110`, then issue:

    java -ea -cp lib/java-select.jar select2.debug.DebugSelect2 -m 01001110110

or 

    java -ea -cp lib/java-seletc.jar select2.debug.DebugSelect2 -m 10 01001110110

in order to run 10 rounds in each thread.

##### Steps #####

As said, scenarios are built upon (quasi-elementary) steps. In each round a thread is selected (according to the current test scenario) and executes a step.

The current debug implementation uses the following steps:

1. `MARK_ACTIVE` - statement 1. when thread marks itself as active 
2. `IS_OWNER` - statement 2. when thread checks whether it is the token owner
3. `OWNER_ACTIVE` - statement block 3.1 when thread detects that the other thread, the token owner is active and exits 
4. `WAIT_CONDITION` - the wait condition in block 3.1 when the token owner thread detects that the other thread is active, goes onto the wait loop
5. `LOST_OWNERSHIP` - statement block 4.1 when the token owner detects that it lost ownership, hence exits
6. `OWNER_SELECTED` - statement block 4.2 when the token owner thread is selected
7. `NOT_OWNER_SELECTED` - statement block 4.3 when the other thread is selected, the one which is not the token owner

*Note*: As you might see the test is far from being perfect, it can be enchanced in several ways:

1. Steps could be defined in a more fine grained manner in order to cover all Java statements not just blocks.
2. Go down and test at the machine level (which would be a hard stuff for me:-) but might be easy for others).
3. Test in a truely parallel way. The current tests are deterministic ones, where elementary 'steps' are executed sequentally. However in multiprocessor environments even elementary steps might be executed in parallel. This direction leads to non-deterministic tests.

##### Checklist #####

* You shouldn't see any `assertion error` - otherwise it would mean that two threads were simultaneously selected, hence the safety feature is broken
* None of the threads should wait 'forever' - otherwise it would mean that a thread blocks, hence the wait-free feature is broken (look at repeating lines of `WAIT_CONDITION`)
* At the end of the test you should see at least as many selections as the number of scenarios multipled by the number of rounds: `number of selections > number of scenarios * rounds` - why? in each round of a scenario at least one thread must be selected, it never happens that none of the threads is selected (oops this must be proven!)


#### Benchmark ####

A simple microbenchmark is implemented for the Java version to gain some preliminary insights. See `src/java/bench`.

The `select2` application service was reimplemented based upon the Java-builtin synchronization primitive: `compare and set` (`CAS`). That is to say this code implements the `select2` features but uses a different algoritm and is based on a different synchronization primitive, uses the `CAS`-based `AtomicInteger` class built into Java.

The microbenchmark compares the custom `volatile` based implementation with the above `CAS` based one. The early results seem promising:

In my machine the custom, `volatile` based implementation runs 2-3x faster then the `CAS` one. Of course this is an orange-apple comparison, since the `select2` protocol deals with only 2 threads. Still the benchmark shows that the API might perform well and even better than the Java builtin at least for small number of threads.

To run the benchmark, issue the following or such:

    java -cp lib/java-select.jar select2.bench.SimpleBenchSelect2 1000

This will run 10 test cycles and in each cycle it will run both implementations 1000 times.

*Note*: This is just a simple, preliminary benchmark, kinda checkpoint just to see whether to move on:-) Stronger and more extensive benchmarks will be necessary, if and when the protocol is implemented to handle more than 2 threads.

### Demo ###

Both the Java and the Scala API provides a simplified clipboard implementation for demo purposes, ie. to demonstrate the API (usage). See the `Clip2` class for details. 

The clipboard provides the following features:

* one (thread) can `push` an object to the clipboard which then 
* can be `popped` (possibly by another thread)

The clipboard uses the following protocol:

* after `pushing` an object no more object can be `pushed` till the current one is `popped`
* also if an object is `popped` then no more object could be `popped` until a new one is `pushed` onto the clipboard

The clipboard builds upon `select2` in order to implement the above protocol. Hence it is thought to be wait-free and thread safe. However since it is a demo of the `select2` protocol it can handle only 2 threads and not more.

To run a simple demo issue:

    java -cp lib/java-select.jar select2.Clip2

This will run 2 threads - one for pushing 10 objects, the other for popping 10 ones. The results printed onto the console. 

In order to to run more rounds, say 100, issue:

    java -cp lib/java-select.jar select2.Clip2 100

_Note on message orders:_

Messages might not be printed out in order. To check what's happenning pair pushed objects with popped ones. For instance here's a partial output from an execution:

    ...
    DEBUG: THREAD-9:	PUSHED	47
    DEBUG: THREAD-9:	PUSHED	55
    DEBUG: THREAD-9:	NOT_PUSHED	49
    DEBUG: THREAD-9:	NOT_PUSHED	70
    ...
    DEBUG: THREAD-10:	POPPED	47
    DEBUG: THREAD-9:	NOT_PUSHED	60
    DEBUG: THREAD-10:	POPPED	55
    ...

What happened in real? 

    Thread-9 pushed 47 and it is printed out to console
    Thread-10 popped 47 but it is printed out just later
    Thread-9 pushed 55 but then
    Thread-9 could not push objects till
    Thread-10 popped 55

The demo later might be enchanced in order to print messages out in order which itself might be a demo for `select2`.


TODO
-------------------------------------------------------------------------------

The most important one: 

**External revisions are necessary both for the proof and the API. Any feedback is highly appreciated! :-)**

Other tasks:

* More rigid, formal proof (based upon some formal model)
* Simplify the protocol, namely the wait condition (sleep on only `wait`) and simplify the proof 
* More testing and benchmarking
* Handle more than 2 threads
* More demos
* Implement other primitives in the 'Select-style', such as: increment/decrement, ring buffer

References
-------------------------------------------------------------------------------


* [Wikipedia: Non-blocking algoritm](http://en.wikipedia.org/wiki/Non-blocking_algorithm)
* [JSR 133 (Java Memory Model) FAQ](http://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html)