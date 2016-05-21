package controllers

import java.util.NoSuchElementException

import betterviews.{IndexView, StatusPageView}
import play.api.Environment
import play.api.http.HttpEntity
import play.api.mvc._
import shared.{Result, ReturnDouble}
import upickle.default._

class Application()(implicit environment: Environment) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index = Action {
    Ok(IndexView.static()).as("text/html")
  }

  def getWorkItem = Action {
    try {
      val workItem = WorkQueue.dequeue()
      Ok(write(workItem.data)).as("application/json")
    } catch {
      case e: NoSuchElementException => {
        Result(
          header = ResponseHeader(NO_CONTENT, Map.empty),
          body = HttpEntity.NoEntity
        )
      }
    }
  }

  def leftPad(str: String, len: Int) = Action.async {
    WorkQueue.runLeftPad(str, len).map(jsonResult)
  }

  def rot13(str: String) = Action.async {
    WorkQueue.rot13(str).map(jsonResult)
  }

  def jsonResult(result: String) = Ok(s"{result: $result}").as("application/json")

  def statusPage = Action {
    Ok(StatusPageView.dynamic()).as("text/html")
  }

  def statusJSON = Action {
    Ok(write(WorkQueue.status)).as("application/json")
  }

  def postResult = Action { implicit request =>
    val result = read[Result](request.body.asText.get)
    WorkQueue.completeJob(result)
    Ok
  }

  def createJob = Action { implicit request =>
      val jsCode = request.body.asText.get
      WorkQueue.addJob(jsCode, None, ReturnDouble) //TODO: support other return types
      Ok
  }
}

