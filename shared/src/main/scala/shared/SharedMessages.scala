package shared

object SharedMessages {
  def itWorks = "It works!"
}

object Functions {

  val f1 : Int => String = x => s"It's ${x + 1}"

}
