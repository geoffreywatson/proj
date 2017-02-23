package views.fieldConstructors


import views.html.fieldConstructors.fieldConstructorTemplate
import views.html.helper.{FieldConstructor, FieldElements}

/**
  * Created by geoffreywatson on 15/02/2017.
  */
object BSHelpers {
  implicit val textInputGroup = new FieldConstructor {

    def apply(elements: FieldElements) = fieldConstructorTemplate(elements)

  }


}
