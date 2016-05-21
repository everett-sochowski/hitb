package controllers

import play.api.Environment
import play.api.mvc._
import shared.Functions
import upickle.default._

class Application()(implicit environment: Environment) extends Controller {

  def index = Action {
    Ok(views.html.index(Functions.f1(5)))
  }

  def getWorkItem = Action {
    Ok(write(WorkQueue.dequeue())).as("application/json")
  }
}

