class ConditionalMap{
    
test("conditionalMap")  {
    val list = List(1, 2, 3)

    def calc(x: Int): Either[String, Int] = if(x<3) Right(x + 1) else Left("Error")

    
    def conditionalMap[A, B, E](l: Seq[A], r: List[B], calc: A => B, continue: B => Boolean): List[B] = {
      l match {
        case Nil => r
        case h :: t => {
          val b = calc(h)
          continue(b) match {
            case true => conditionalMap(t, b :: r, calc, continue)
            case false => r
          }
        }
      }

    }

    def continue(x: Either[String, Int]): Boolean = x match {
      case _: Left[String, Int] => false
      case _: Right[String, Int] => true
    }

    val listB = conditionalMap(list, List[Either[String, Int]](), calc, continue).reverse
    listB.foreach(println)

  }
    
}