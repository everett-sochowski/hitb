package example

import org.scalajs.dom.{MessageEvent, Worker}

import scala.scalajs.js

object ScalaJSExample extends js.JSApp {
  def main(): Unit = {

    println("running main")
    val worker = new Worker("/assets/clientworker-fastopt.js")

    // start off worker
    worker.postMessage(null)

    // worker finished, start off next one
    worker.onmessage = { e: Any =>
      println("Received finished message")
      worker.postMessage({})
    }
  }
}