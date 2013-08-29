Protocol
===============================================================================

* The Draw 2 API lets the system choose between 2 threads 
* The CAS 2 API implements compare and swap for 2 threads, it builds on Draw 2

Draw 2 protocol
-------------------------------------------------------------------------------

### Datums ###

Threads are numbered as 1 and 2.

The protocol uses two fields:

* choose[] - flags that thread 1 or 2 is choosable
* grantee - in case when both thread is choosable, the grantee will be chosen

### Protocol ###

Assume that thread i enters the draw (i = 1 or 2). 

Then the pseudo code is the following:

-- check whether the other thread already entered, if yes exit  

    if choose[(i + 1) % 2] then return i

-- flag myself as choosable

    choose[i] = 1

-- check again whether the other thread already entered, if yes, cleanup and exit

    if choose[(i + 1) % 2] then
       choose[i] = 0
       grantee = i
       return (i + 1) % 2

-- otherwise i was chosen, cleanup and exit

    else
       choose[i] = 0
       return i