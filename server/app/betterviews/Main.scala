package betterviews


import scalatags.Text.all._
import scalatags.Text.tags2.{title => titleTag}


object Main {
  def apply(title: String)(content: Frag*): String = {
    "<!DOCTYPE html>" +
    html(
      head(
        titleTag(title),
        link(rel:="stylesheet", media:="screen", href:="/assets/stylesheets/main.css"),
        link(rel:="shortcut icon", media:="image/png", href:="/assets/images/favicon.png"),
        script(src := "/assets/lib/jquery/jquery.min.js", `type`:="text/javascript")
      ),
      body(
        content,
        scriptAssetLink("client-jsdeps.js"),
        scriptAssetLink("client-fastopt.js")
      )
    )
  }

  private def scriptAssetLink(filename: String): Frag = script(
    src:=s"/assets/$filename",
    tpe:="text/javascript"
  )
}

