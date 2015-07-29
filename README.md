.ManhattanConnectionRouter
==========================

Reuse.ManhattanConnectionRouter from BPMN2 modeler in your bundle 

===========================
Main Changes in this fork:

* Do not use all the anchor in the component to calculate the route. Just consider the anchors the user is trying to connect.
* Make it possible to use more then one anchor with the same anchor location property (RIGHT, LEFT etc.)
* Configure classpath to use the required dependency form bpmn2. The required version cannot be installed in eclipse luna, so it must be set directly in the classpath.
