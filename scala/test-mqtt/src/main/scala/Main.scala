import akka.actor.ActorSystem
import akka.stream.*
import akka.stream.alpakka.mqtt.*
import akka.stream.alpakka.mqtt.scaladsl.{MqttFlow, MqttMessageWithAck, MqttSink, MqttSource}
import akka.stream.scaladsl.*
import akka.stream.scaladsl.MergeHub.source
import akka.util.ByteString
import akka.{Done, NotUsed}

import javax.net.ssl.SSLContext
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.collection.immutable.Seq
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success}

/*@main def hello(): Unit =
  println("Hello world!")
  println(msg)
  val connectionSettings = MqttConnectionSettings(
    "tcp://localhost:1883",
    "test-scala-client", // (2)
    new MemoryPersistence // (3)
  )

  val mqttSource: Source[MqttMessageWithAck, Future[Done]] =
    MqttSource.atLeastOnce(
      connectionSettings
        .withClientId(clientId = "source-spec/source1")
        .withCleanSession(false),
      MqttSubscriptions("topic", MqttQoS.AtLeastOnce),
      bufferSize = 8
    )

  val input = Vector("one", "two", "three", "four", "five")
  val businessLogic: Flow[MqttMessageWithAck, MqttMessageWithAck, NotUsed] = Flow[MqttMessageWithAck]
  val result = mqttSource
    .via(businessLogic)
    .mapAsync(1)(messageWithAck => messageWithAck.ack().map(_ => messageWithAck.message))
    .take(input.size)
    .runWith(Sink.seq)*/

implicit val system: ActorSystem = ActorSystem("QuickStart") //stream di akka nati da omonimo progetto, ambiente per sviluppare con paradigma attori, tuttavia messi gli stream hanno voluto tenere separati i giochi; serie di specifiche per operare tramite stream che rappresentano interfaccia indipendente da implementazione. Stream approcio funzionale al problema, immutabili innescati alla fine. Tu utilizzi sistema magico per avere la blueprint, ma ad un certo punto va Materializzato (ecco l'errore Materializer) quindi di default co sta roba si usa la loro che e' basata su attori, quindi motore di default per mettere in esecuzione questi stream. Perche una volta che ho la blueprint questa va materializzata, questo Materializer appunto si basa sull'ActorSystem di akka. Rendendolo implicito ecco che gli stream alla ricerca di un materializer sanno che fare

val connectionSettings: MqttConnectionSettings = MqttConnectionSettings(
  "tcp://fuzzy-guacamole-mqtt-1:1883", // (1)
  "test-scala-client", // (2)
  new MemoryPersistence // (3)
)

def read(): Unit =
  val mqttSource: Source[MqttMessage, Future[Done]] =
    MqttSource.atMostOnce(
      connectionSettings.withClientId(clientId = "source-spec/source"),
      MqttSubscriptions(Map("topic1" -> MqttQoS.AtLeastOnce, "topic2" -> MqttQoS.AtLeastOnce)),
      bufferSize = 8
    )
  val messages = Vector("one", "two", "three", "four", "five")
  val (subscribed: Future[Done], streamResult: Future[Seq[MqttMessage]]) = mqttSource
    .take(messages.size)
    .toMat(Sink.seq)(Keep.both)
    .run()
// TODO: connection lost (you might want to enable `automaticReconnect` in `MqttConnectionSettings`)

def publish(): Unit =
  val messages = Vector("one", "two", "three", "four", "five")
  val sink = MqttSink(connectionSettings, MqttQoS.AtLeastOnce)

//  Source(messages).runWith(sink)
/*val lastWill = MqttMessage("willTopic", ByteString("ohi"))
  .withQos(MqttQoS.AtLeastOnce)
  .withRetained(true)*/

def singleFlow(): Unit =
  val messages = Vector("one", "two", "three", "four", "five")
  val mqttFlow: Flow[MqttMessage, MqttMessage, Future[Done]] =
    MqttFlow.atMostOnce(
      connectionSettings.withClientId("flow-spec/flow"),
      MqttSubscriptions("pippo", MqttQoS.AtLeastOnce),
      bufferSize = 8,
      MqttQoS.AtLeastOnce
    )

  //result set elementi
  // subscribed future di done, termina quanto sottoscrizione avvenuta
  /*
  * promise, materializzato dalla sorgente
  * */
  val ((mqttSendQueue, subscribed), mqttReceiveQueue) = Source.queue(100)
    .viaMat(mqttFlow)(Keep.both)
    .toMat(Sink.foreach(
      m=>println(m.payload.decodeString("utf-8"))
    ))(Keep.both)
    .run()

//  mqttSendQueue.offer(MqttMessage("pippo", ByteString("blabla")))
  List.range(1,10).foreach(n=>mqttSendQueue.offer(MqttMessage("pippo", ByteString("blabla N"+n))))

//  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  Await.ready(mqttReceiveQueue, Duration.Inf)//.onComplete(_=>println("done"))

//  printf("\n\nresult: %s\npromise: %s\nsub: %s\n\n", result toString, mqttMessagePromise toString, subscribed toString)


@main def hello(): Unit =
  printMessage()
  //  read()
  singleFlow()


def printMessage(): Unit =
  println("Hello world!")
  println(msg)
def msg = "I was compiled by Scala 3. :) yay hope will work fine"

