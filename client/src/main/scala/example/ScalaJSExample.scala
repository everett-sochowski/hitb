package example

import org.scalajs.dom.Worker

import scala.scalajs.js

object ScalaJSExample extends js.JSApp {
  def main(): Unit = {

    println("running main")
    new Worker("/assets/clientworker-fastopt.js")
  }
}