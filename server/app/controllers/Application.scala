package controllers

import betterviews.StatusPageView
import play.api.Environment
import play.api.mvc._
import shared.{Result, Functions}
import upickle.default._

class Application()(implicit environment: Environment) extends Controller {

  def index = Action {
    Ok(views.html.index(Functions.f1(5)))
  }

  def getWorkItem = Action {
    Ok(write(WorkQueue.dequeue())).as("application/json")
  }

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
      WorkQueue.addJob(jsCode)
      Ok
  }
}

