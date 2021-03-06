package com.scalapeno

import scala.collection.immutable.ListMap
import scala.collection.JavaConversions._
import scala.util.control.Exception._

import java.io._

import org.slf4j.LoggerFactory
import org.mozilla.javascript._
import cc.spray.json._


package object rhinos {
  val log = LoggerFactory.getLogger(this.getClass)


  class RhinosScope(val wrapped: ScriptableObject) {

  }

  implicit def scopeToRhinosScope(scope: ScriptableObject): RhinosScope = new RhinosScope(scope)

  implicit def rhinosScopeToScope(rhinosScope: RhinosScope): ScriptableObject = rhinosScope.wrapped


  class RhinosRuntime(
                       val scope: RhinosScope = withContext[RhinosScope](_.initStandardObjects()).get
                       ) extends RhinosEvaluationSupport with RhinosJsonSupport {

    /**
     * Makes an object available to javascript so that it can be called off to
     * @param name
     * @param callbackObj
     */
    def addObject(name: String, callbackObj: Any) {
      withContext {
        context =>
          val jsobj = Context.javaToJS(callbackObj, scope)
          scope.put(name, scope.wrapped, jsobj)


      }
    }

//    def addTransformer[T:JsonReader] {
//      withContext {
//        context =>
//          context.setWrapFactory(new JsWrapFactory[T])
//      }
//    }



    class JsWrapFactory[T:JsonReader] extends WrapFactory with RhinosJsonSupport {
      override def wrap(cx: Context, scope: Scriptable, obj: Any, staticType: Class[_]) = {
        println("trying to wrap up:" + obj)
        Context.javaToJS(toScala[T](obj),scope)
      }
    }

  }


  private[rhinos] def withContext[T](block: Context => T): Option[T] = {
    val context = Context.enter()

    try {
      Option(block(context))
    } catch {
      case jse: EvaluatorException => {
        log.error("Could not evaluate Javascript code: " + jse.getMessage)
        None
      }
      case e: Exception => {
        log.error("Rhinos ran into a problem while evaluating Javascript.", e)
        None
      }
    } finally {
      Context.exit()
    }
  }

  implicit object JsObjectReader extends JsonReader[JsObject] {
    def read(value: JsValue) = value match {
      case o: JsObject => o
      case x => deserializationError("Expected JsObject, but got " + x)
    }
  }

  implicit object JsArrayReader extends JsonReader[JsArray] {
    def read(value: JsValue) = value match {
      case o: JsArray => o
      case x => deserializationError("Expected JsArray, but got " + x)
    }
  }

}
