package org.hyperscala.examples.basic

import org.hyperscala.html._
import org.hyperscala.ui.dynamic.{DynamicWebpage, DynamicTag}
import com.outr.net.http.session.Session
import org.hyperscala.web.Website

/**
 * @author Matt Hicks <mhicks@outr.com>
 */
class DynamicPageExample[S <: Session](website: Website[S]) extends DynamicWebpage(website) {
  def dynamicTag = DynamicTag.url[tag.HTML](getClass.getName, getClass.getClassLoader.getResource("dynamic_page.html"))

  val message = getById[tag.Strong]("message")

  message.contents.replaceWith("Dynamically updated content from an existing HTML page!")
}