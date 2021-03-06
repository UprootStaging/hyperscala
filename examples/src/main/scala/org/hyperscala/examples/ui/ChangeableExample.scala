package org.hyperscala.examples.ui


import org.hyperscala.html._
import org.hyperscala.examples.Example
import language.reflectiveCalls
import org.hyperscala.realtime._
import org.powerscala.Color
import org.hyperscala.ui.wrapped.{Changes, Changing, Changeable}
import org.hyperscala.css.attributes.Position
import org.hyperscala.javascript.dsl._
import org.hyperscala.jquery.dsl._
import org.hyperscala.css.Style
import org.hyperscala.web._
import com.outr.net.http.session.Session

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ChangeableExample extends Example {
  import ChangeableExample._

  this.require(Changeable)
  this.require(Realtime)

  connected[Webpage[Session]] {
    case webpage => webpage.connectForm()
  }

  contents += new tag.P {
    contents += "Changeable module allows the use of a JavaScript DSL to define style changes to be applied a tag."
  }

  val div = new tag.Div(id = "myDiv", content = "Positioned Element")
  div.style.width := 100.px
  div.style.height := 100.px
  div.style.paddingAll(5.px)
  div.style.position := Position.Absolute
  div.style.backgroundColor := Color.LightBlue
  contents += div

  var horizontal: JSFunction1[Changes, Unit] = HorizontalCenter
  var vertical: JSFunction1[Changes, Unit] = VerticalMiddle

  val changeable = Changeable(div)
  changeable.frequency := 0.1
  updatePositioning()

  contents += new tag.Div {
    contents += new tag.Button(content = "Left") {
      clickEvent.on {
        case evt => updatePositioning(x = HorizontalLeft)
      }
    }
    contents += new tag.Button(content = "Center") {
      clickEvent.on {
        case evt => updatePositioning(x = HorizontalCenter)
      }
    }
    contents += new tag.Button(content = "Right") {
      clickEvent.on {
        case evt => updatePositioning(x = HorizontalRight)
      }
    }
  }

  contents += new tag.Div {
    contents += new tag.Button(content = "Top") {
      clickEvent.on {
        case evt => updatePositioning(y = VerticalTop)
      }
    }
    contents += new tag.Button(content = "Middle") {
      clickEvent.on {
        case evt => updatePositioning(y = VerticalMiddle)
      }
    }
    contents += new tag.Button(content = "Bottom") {
      clickEvent.on {
        case evt => updatePositioning(y = VerticalBottom)
      }
    }
  }

  contents += new tag.Div {
    contents += new tag.Button(content = "Under Logo") {
      clickEvent.on {
        case evt => updatePositioning(x = HorizontalLeftLogo, y = VerticalUnderLogo)
      }
    }
  }

  def updatePositioning(x: JSFunction1[Changes, Unit] = null, y: JSFunction1[Changes, Unit] = null) = {
    if (x != null) {
      horizontal = x
    }
    if (y != null) {
      vertical = y
    }
    changeable.changing := List(horizontal, vertical)
  }
}

object ChangeableExample {
  val myDiv = $("#myDiv")
  val logo = $("#logo")

  val HorizontalLeft = Changing(Style.left, 0.px)
  val HorizontalCenter = Changing(Style.left, (window.innerWidth - myDiv.width()) / 2)
  val HorizontalRight = Changing(Style.left, window.innerWidth - myDiv.width())

  val VerticalTop = Changing(Style.top, 0.px)
  val VerticalMiddle = Changing(Style.top, (window.innerHeight - myDiv.height()) / 2)
  val VerticalBottom = Changing(Style.top, window.innerHeight - myDiv.height())

  val VerticalUnderLogo = Changing(Style.top, logo.offset().top + logo.height())
  val HorizontalLeftLogo = Changing(Style.left, logo.offset().left)
}