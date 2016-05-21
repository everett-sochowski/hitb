package controllers

import play.api.Environment
import play.api.mvc._
import shared.{Functions, SharedMessages}

class Application()(implicit environment: Environment) extends Controller {

  def index = Action {
    Ok(views.html.index(Functions.f1(5)))
  }

}
