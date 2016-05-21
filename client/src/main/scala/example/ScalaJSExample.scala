package example

import scala.scalajs.js
import org.scalajs.dom
import shared.{Functions, SharedMessages}

object ScalaJSExample extends js.JSApp {
  def main(): Unit = {
    dom.document.getElementById("scalajsShoutOut").textContent = {

      /*import scala.spores._

      val foo = spore {
        x : Int => x + 1
      }

      foo(5).toString*/

      Functions.f1(5)

      //SharedMessages.itWorks
    }
  }
}