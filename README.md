# Contentsquare Technical Assessment: User Reconciliation

## About the project

One of the main missions of Contentsquare's Data Engineering team is to build data pipelines that receive, transform,
store and aggregate navigation events.

This project is a RESTful Web Service that will collect data and store into an in memory database. It is built with ZIO
ecosystem using `zio`, `zio-http`, `zio-config` and `circe` for json (de)serialization.



## Getting Started

### Prerequisites

To run this project you need to
install [sbt](https://docs.scala-lang.org/getting-started/sbt-track/getting-started-with-scala-and-sbt-on-the-command-line.html)
.

### Usage

The server is binding on port **8080** by default. You can change that value in `src/main/resources/application.conf`.

**To start the server**, in a sbt shell run the command `reStart`.

**To check the server status**, in a sbt shell run the command `reStatus`.

**To stop the server**, in a sbt shell run the command `reStop`

### Unit Testing

To run unit tests, in a sbt shell run the command `test`



## Algorithms Explanation

User matching consists on identifying events that belongs to a user. Two events belong to the same user if they share
the same at least one `userId`. I assume that a `User` is defined by a set of `userIds`, a set of `Event` and a set of `sources`:

```scala
final case class User(
                       linkedUserIds: Set[String],
                       events: Set[Event],
                       sources: Set[Source]
                     )
```

The set of `userIds` is what makes a User unique. I will use that as a key for the in memory database hash map, so the values will be store
in a `HashMap[Set[String], User]`.

Having the set of userIds as key will facilitate us to identify if an incoming event belongs to a user by checking if 
`event.userIds.intersect(user.key)` is non-empty.

### Inserting event
Here are the steps done by the algorithm in order to insert an event:
* Creates a new `User` with the incoming events values (if there are linked users, they will be merged. If not it will be inserted as it is.)
* Gets all the users that share at least one `userId` by checking the keys in the `HashMap`
* Removes from database the linked users previously retrieved because they will be merged into one `User`
* Merges the linked users into one `User` and insert it into the database

### Updating event
Here are the steps done by the algorithm in order to update an event:
* Gets the `User` that contains the event to update.
* Removes the `User` from the database
* Gets the `Event` and update the field userIds
* Re-insert all the events from the `User` that we removed to make sure that linking events is considering updating `userIds` values

### Retrieving metrics
Here are the steps done by the algorithm in order to retrieve metrics:
* Gets users in database
* Unique Users count corresponds to number of total users in database
* Bounced Users is count by checking if a user has only one event and the `EventType` is `display`
* Cross Device Users is count by checking if `User.sources` contains 2 elements in its set.



## To Discuss Further

**How would you turn this into a production-ready project ? What are the changes needed in the architecture and what are the deployments choices that you would make ?**

I would implement a distributed event streaming platform that will receive the upcoming event (eg. Kafka). I would also define a data storage capable of ingesting real-time data.
Assuming that the cloud provider is Amazon Web Services, I would build a serverless microservices using AWS Lambda.


**If you had a team of developers to work on this project, how would you organize it ?**

Admitting that the business needs is to implement new API endpoints, I think that each developer can work on a different endpoint.
We could also seperate the product related and performance related tasks but I believe that developers should be able to do any tasks !


## Contact

If you have any question, feel free to send me a mail at kchuang.pro@gmail.com !