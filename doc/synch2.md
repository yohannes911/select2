`synch2` synchronization primitive
===============================================================================

Protocol
-------------------------------------------------------------------------------

    # #########################################################################
    # start
    # #########################################################################

    # mark this thread as active
    active[i] = true

    # record whether this is the thread who wakes up other
    is_waker[i] = waker == i

    # record start timestamp 
    start[i] = timestamp 

    # #########################################################################
    # guard
    # #########################################################################
    if active[i + 1]

        # wait while other thread is active and this is not awaken
	    wait(i) =
            # mark this in wait 
            wait[i] = true

            # wait loop
            while active[i + 1] and wait[i]:
                if is_waker[i]:
                    # if waker change is not in progress wakeup other thread
                    if not waker_change:
                        wait[i + 1] = false
                    
                    # waker change protocol of receiver
                    else:
                        # mark this as not waker
                        is_waker[i] = false

                        # ackmowledge initiator
                        waker_change = false
                yield

            # change waker if this thread ran more times than the waker 
            if not is_waker[i] and start[i] > start[i + 1]
                # change waker to this thread
                waker = i
                
                # mark this as waker
                is_waker[i] = true

                # acknowledge other thread about change
                waker_change = true

                # wait until other thread is deactivated or acknowledges 
                while active[i + 1] and waker_change: yield

                # necessary?
                waker_change = false 

                # restart waiting but now as a waker
                wait(i)
    
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

#### guard ####

* either thread enters

#### waker ####

* waker_change can only happen when other thread is still in wait loop?
* is the role exclusive?

### Wait-free ###

### Safe ###

