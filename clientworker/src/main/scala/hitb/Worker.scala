package hitb

import org.scalajs.dom.ext.Ajax
import shared._
import upickle.default._

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.scalajs.js

object Worker extends js.JSApp {
  val self = js.Dynamic.global

  def main(): Unit = {
    println("in scalajs worker")

    self.onmessage = { _: scala.scalajs.js.Any =>
      // start off worker
      this.processNextWorkItem()
    }
  }

  private def processNextWorkItem() = {
    Ajax
      .get("/getWorkItem")
      .onSuccess { case xhr =>

        xhr.status match {
          case HttpCodes.OK => {
            val workItem = read[WorkItemData](xhr.responseText)

            val computationResult = js.eval(workItem.jsCode)

            workItem.returnType match {
              case ReturnDouble => postResult(DoubleResult(workItem.id, computationResult.asInstanceOf[Double]))
              case ReturnOptionalDouble => postResult(OptionalDoubleResult(workItem.id, Option(computationResult.asInstanceOf[Double])))
            }
          }
          case HttpCodes.NO_CONTENT => println("No work to do ...")
        }
      }
  }

  private def postResult[T: Writer](result: T) = {
    self.postMessage(write(result))
    Ajax.post("/postResult", write(result))
  }
}