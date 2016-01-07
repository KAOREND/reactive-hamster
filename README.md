# reactive-hamster
A reactive persistence and web framework

The Hamster Framework consists of a reactive persistence layer and a web framework for building reactive applications.
This means that changes in the data are instantly pushed to all users. This is ideal for communnication applications like [kaibla.com](kaibla.com) which is build on the Hamster Framework.

##  Reactive Persistence Layer
In order to achieve this the Hamster Framework implements the Model View Controller (MVC) pattern in a very consistent way: 
Queries made against the persistence layer will not just return a static list with the results, but instead a list model which will fire events. It will get updated whenver the underlying data is being changed.
The persistence layer uses MongoDB as database and takes advantage of its flexible document structure. Therefore the schema can be very easily defined directly in the Java code, which makes rapid application development much easier. 

##  Reactive UI
The Hamster UI Framework makes it easy to use the live data models from the persistence layer and will update the UI for all users automatically. The framework allows to easily implement rich reactive Web Application in Java, without having to take care of UI updates yourself.
It also has the advantage that both the backend and the client of the application can be impmented in pure Java.

Even though the reactive hamster persistence layer could be used with other UI frameworks as well, however currently the easiest way is to use the hamster ui framework.

## Advantages

Building a web application with a really reactive MVC implementation has not only the advantage that everything is updated instantly:

1. It also makes it very easy to separate concerns. The code which is changing or creating new data objects does not have to know which other parts of the application need to know about the changes.
2. The persistence layer can cache the data very efficiently without the risk of exposing stalled (not up to date) data to the application. 
3. UI build with this pattern can be extremly fast, as changes in the data are processed asynchronously and only changes have to be transmitted to the client.
