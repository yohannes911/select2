`synch2` synchronization primitive
===============================================================================

Protocol
-------------------------------------------------------------------------------

    # #########################################################################
    # start
    # #########################################################################

    # mark this thread as active
    active[i] = true

    # #########################################################################
    # guard
    # #########################################################################
    if active[i + 1]:

        # mark this to be in wait 
        wait[i] = true

        # wait until the other thread is deactived or wakes this up
        while active[i + 1] and wait[i]:
            if waker == i:
                # if waker change is not in progress wakeup other thread
                if not waker_change: wait[i + 1] = false
                    
                # waker change protocol of receiver
                else:
                    # ackmowledge initiator
                    waker_change = false

            yield

        # change waker if it was not the one
        if not waker == i:
            # change waker to this thread
            waker = i

            # acknowledge other thread about change
            waker_change = true

            # wait until other thread is either deactivated or acknowledges 
            while active[i + 1] and waker_change: yield

    
    # #########################################################################
    # finish
    # #########################################################################

    # mark this selected 
    select[i] = true

    # mark this deselected
    select[i] = false

    # mark this inactive
    active[i] = false

Properties
-------------------------------------------------------------------------------

### Internals ###

#### enter guard ####

* either thread enters
 
#### exit guard ####

* how exits: other thread is either deactivated or wakes this up
  * the latter could only done by the waker
  * only other thread sets wait[i] to false
* is the waker role exclusive?

### Wait-free ###

### Safe ###

