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

    
    # #########################################################################
    # finish
    # #########################################################################

    # mark this selected 
    select[i] = true

    # mark this deselected
    select[i] = false

    # change waker if it was not the one
    if not waker == i:
       # change waker to this thread
       waker = i

       # acknowledge other thread about change
       waker_change = true

       # wait until other thread is either deactivated or acknowledges 
       while active[i + 1] and waker_change: yield

    # mark this inactive
    active[i] = false

Properties
-------------------------------------------------------------------------------

### Internal properties ###

#### enter guard ####

1. **either thread enters**
 
#### exit guard ####

1. **how exits?** other thread is either deactivated or this is waken up by setting wait[i] to false
1. **waker and wake up by setting wait[i] to false**
   1. only the other thread can set this wait flag to false
   1. wakeup can only happen in the guard
   1. only the waker thread can wakeup
   1. the waker role is exclusive
   1. if a thread was not the waker when entered, it becomes just before deactivating
   1. if a thread exits the guard it was not the waker at that time
   1. if a thread exits the guard either (1) the other thread was inactive or (2) lefts in the guard as waker

#### general ####

1. if a thread exits or avoids the guard **it did not wake the other thread up**
1. if this thread was still active when the other thread deactivated then either (1) this thread will deactivate before the other reenters or **(2) the other thread enters the guard**

### Wait-free ###

### Safe ###

