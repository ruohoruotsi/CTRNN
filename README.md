CTRNN
=====================================

CTRNN is a [MAX external](http://cycling74.com/max) that implements a Continuous-Time Recurrent Neural Network, a dynamical systems model of a biological neural network. When combined with evolutionary algorithms, CTRNNs can perform simple cognitive tasks. 

A CTRNN uses a system of [ordinary differential equations](http://en.wikipedia.org/wiki/Ordinary_differential_equation) (ODE) to model the effects on a neuron receiving an input spike train. For example, a simulated "creature" can learn the best way to walk in a process that is as follows:

* Create a population of CTRNNs with random internal state values to control a creature's locomotion
* Observe which individual CTRNN has the creature crawling the furthest (via an objective function)
* Use the best individuals from the population to create new CTRNNs
* Repeat for a few thousand generations.

Afterwards, you'll have a CTRNN that's very skilled at controlling creature locomotion. Though the initial population may have produced hobbling, the final generations will coordinate smooth locomotive behaviour. In the literature, CTRNNs have been "evolved" thusly to control various tasks from chemotaxis to simple learning.

This work provides a simple, efficient implementation to be used in MAX, where the neural network I/O can be embedded as to a host of multimedia applications.


Credits
-------
This work is largely influenced by: 

* The biologically inspired Dynamical Systems research of [Dr. Randall Beer](http://mypage.iu.edu/~rdbeer/) 
* The Live Algorithms research and MAX patches by [Dr. Ollie Bown](http://www.olliebown.com/main_blog/?p=73)


Licence
-------
CTRNN is Copyright © 2010-2011  &nbsp; [iroro orife](http://github.com/ruohoruotsi)

Java source code and MAX patch are provided under the [MIT License](https://en.wikipedia.org/wiki/MIT_License)

Dependencies
------------
You need to be comfortable patching in [MAX](http://cycling74.com/max). There's a bit of a learning curve, but the creative yield will be great!


Version history
------------
Version 0.1

#### Version 0.1 (August 11th, 2013):
Initial commit

