Protocol
===============================================================================


Select 2
-------------------------------------------------------------------------------

### Datums ###

Threads are numbered as 1 and 2.

The protocol uses two fields:

* active[] - marks whether thread 1 or 2 is active (under selection)
* grantee - in case when both thread is active, the grantee will be chosen

### Protocol ###

Assume that thread i enters the selection (i = 1 or 2). 

Then the pseudo code is the following:

-- check whether the other thread already entered, if yes exit  

    if active[(i + 1) % 2] then return 1 // 1 means not selected

-- flag myself as choosable

    active[i] = true

-- check again whether the other thread already entered and it is the grantee, if yes, cleanup and exit

    if choose[(i + 1) % 2] and grantee != i then
       active[i] = false
       grantee = i
       return 0 // 1 means not selected

-- otherwise i was chosen, cleanup and exit

    else
       active[i] = false
       return 0 // 0 means selected