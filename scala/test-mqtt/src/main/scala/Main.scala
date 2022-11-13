import akka.actor.ActorSystem
import akka.stream.{BoundedSourceQueue, *}
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

implicit val system: ActorSystem = ActorSystem("QuickStart")

val connectionSettings: MqttConnectionSettings = MqttConnectionSettings(
  "tcp://fuzzy-guacamole-mqtt-1:1883", // (1)
  "scala-main-server", // (2)
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

/*def publish(): Unit =
  val messages = Vector("one", "two", "three", "four", "five")
  val sink = MqttSink(connectionSettings, MqttQoS.AtLeastOnce)
//  Source(messages).runWith(sink)
val lastWill = MqttMessage("willTopic", ByteString("ohi"))
  .withQos(MqttQoS.AtLeastOnce)
  .withRetained(true)*/
var badGlobalQ: BoundedSourceQueue[MqttMessage]=null
def singleFlow(jobs: Map[String, String]): Unit =
  val mqttFlow: Flow[MqttMessage, MqttMessage, Future[Done]] =
    MqttFlow.atMostOnce(
      connectionSettings.withClientId("flow-spec/flow"),
      MqttSubscriptions(Map("new_client" -> MqttQoS.AtLeastOnce, "results" -> MqttQoS.AtLeastOnce)),
      bufferSize = 10,
      MqttQoS.AtLeastOnce
    )

  /*
  * subscribed future di done, termina quanto sottoscrizione avvenuta
  * promise, materializzato dalla sorgente
  * */
  val ((mqttSendQueue, subscribed), mqttReceiveQueue) = Source.queue(100)
    .viaMat(mqttFlow)(Keep.both)
    .toMat(Sink.foreach(
      m => handleNewMessage(m, jobs) //println(m.topic+m.payload.decodeString("utf-8"))
    ))(Keep.both)
    .run()

  //badGlobalQ=mqttSendQueue
  Thread.sleep(5000)
  jobs.foreach((k,v)=>{
    mqttSendQueue.offer(MqttMessage("unit_"+k, ByteString(v)))
  })

  //  mqttSendQueue.offer(MqttMessage("pippo", ByteString("blabla")))
  //  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    Await.ready(mqttReceiveQueue, Duration.Inf)//.onComplete(_=>println("done"))

def handleNewMessage(msg: MqttMessage, jobs: Map[String, String]): Unit =
  val payload = msg.payload.decodeString("utf-8")
  msg.topic match
    case "results" => printResults(payload)
    case "new_client" => handleConnection(payload, jobs)

def printResults(res: String): Unit =
  println("\nNew results!\n" + res)

def handleConnection(id: String, jobs: Map[String, String]): Unit =
  println("tell " + id + " what to do")
  //badGlobalQ.offer(MqttMessage("unit_"+id, ByteString(jobs(id))))

@main def hello(): Unit =
  printMessage()
  //  read()

  // TODO: uncomment fetch from env
  val unitCount = scala.util.Properties.envOrElse("UNIT_NUMBER", "undefined")
  val filename = scala.util.Properties.envOrElse("FILENAME", "undefined") //"/library/Bible.txt"
  val regex = scala.util.Properties.envOrElse("REGEX", "undefined")

  printf("We have %s units, we are looking for %s in %s", unitCount, regex, filename)

  val file = io.Source.fromFile(filename)
  val lines = file.getLines.size
  file.close()
  println("It has " + lines + " lines")
  val splitting = computeSplitting(lines, unitCount.toInt)
    .map((k,v)=>{
      k-> (filename+":::"+regex+v)
    })

  singleFlow(splitting)

def computeSplitting(l: Int, u: Int): Map[String, String] =
  val lPerUnit = (l / u)
  var map: Map[String, String] = Map.empty
  // from 1 to u-1 both included
  (1 until u).foreach(n => {
    map += (n.toString -> (":::" + lPerUnit + ":::" + (n - 1)))
  })
  val remaining=if (lPerUnit*u < l) lPerUnit*(u-1) else lPerUnit
  map+=(u.toString -> (":::" + remaining + ":::" + (u - 1)))
  map

// eg working msg: mosquitto_pub -t unit_44 -m "/library/Bible.txt:::...heaven...:::10:::10"


def printMessage(): Unit =
  println("Hello world!")
  println(msg)
def msg = "I was compiled by Scala 3. :) yay hope will work fine"

