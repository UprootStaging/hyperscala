package org.hyperscala.bootstrap.component

import org.hyperscala.css.attributes.Visibility
import org.hyperscala.html._
import org.hyperscala.html.constraints.BodyChild
import org.hyperscala.html.extension.ClassBooleanProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Description extends tag.Dl {
  val horizontal = new ClassBooleanProperty(this, enabled = Some("dl-horizontal"))

  def addInvisible() = {
    val term = new tag.Dt(content = "&#160;")
    val definition = new tag.Dd(content = "&#160;")
    term.style.visibility := Visibility.Hidden
    definition.style.visibility := Visibility.Hidden
    contents.addAll(term, definition)
  }

  def add(term: BodyChild, definition: BodyChild) = {
    contents += new tag.Dt(content = term)
    contents += new tag.Dd(content = definition)
  }
}
