package org.hyperscala.javascript.dsl2

/**
 * @author Matt Hicks <matt@outr.com>
 */
object Test {
  def main(args: Array[String]): Unit = {
    val js = new JavaScriptContext {
      val a = v(5.3)
      a := 6.0
      val b = Math.floor(a)
      b += 2.0
    }
    println(js.toJS())
    val js2 = new JavaScriptContext {
      val f1 = new JSFunction2[Double, Double, Double]("add") {
        val f2 = new JSFunction2[String, String, String]("concat") {
          p1 + p2
        }

        document.writeln(s("Hello World! ") + window.innerWidth + "x" + window.innerHeight)

        p1 + p2
      }
    }
    println(js2.toJS())
  }
}