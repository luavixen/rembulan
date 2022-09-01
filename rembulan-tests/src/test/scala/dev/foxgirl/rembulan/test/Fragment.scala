/*
 * Copyright 2016 Miroslav Janíček
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.foxgirl.rembulan.test

import dev.foxgirl.rembulan.{Conversions, LuaFormat, LuaRuntimeException}
import org.scalatest.FunSpec

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.util.Try

trait Fragment {

  def description: String

  private var _code: String = null

  def code: String = _code

  protected def code_=(v: String): Unit = {
    require (v != null)
    this._code = v.stripMargin
  }

}

object Fragment {

  class DefaultImpl(_desc: String, _code: String) extends Fragment {
    require (_desc != null)
    require (_code != null)

    override val description = _desc
    override val code = _code.stripMargin
  }

  def apply(description: String, code: String): Fragment = new DefaultImpl(description, code)

}

trait FragmentBundle {

  implicit protected val bundle = this

  private val fragments = ArrayBuffer.empty[Fragment]

  def name: String = this.getClass.getSimpleName

  protected def register(fragment: Fragment): Fragment = {
    fragments.append(fragment)
    fragment
  }

  def lookup(name: String): Option[Fragment] = fragments find { _.description == name }

  def all: Iterable[Fragment] = fragments

  protected def fragment(name: String)(body: String): Fragment = {
    register(Fragment(name, body))
  }

}

trait FragmentExpectations {

  import FragmentExpectations._

  protected val EmptyContext = Env.Empty
  protected val BasicContext = Env.Basic
  protected val ModuleContext = Env.Module
  protected val CoroContext = Env.Coro
  protected val MathContext = Env.Math
  protected val StringLibContext = Env.Str  // must not be called StringContext -- messes up with string interpolation
  protected val IOContext = Env.IO
  protected val TableContext = Env.Tab
  protected val DebugContext = Env.Debug
  protected val FullContext = Env.Full

  private val expectations = mutable.Map.empty[Fragment, mutable.Map[Env, Expect]]

  def expectationFor(fragment: Fragment): Option[Map[Env, Expect]] = {
    expectations.get(fragment) map { _.toMap }
  }

  private def addExpectation(fragment: Fragment, ctx: Env, expect: Expect): Unit = {
    val es = expectations.getOrElseUpdate(fragment, mutable.Map.empty)
    es(ctx) = expect
  }

  protected class RichFragment(fragment: Fragment) {
    def in(ctx: Env) = new RichFragment.InContext(fragment, ctx)
  }

  protected object RichFragment {

    class InContext(fragment: Fragment, ctx: Env) {
      def succeedsWith(values: Any*) = {
        addExpectation(fragment, ctx, Expect.Success(values map toRembulanValue))
      }
      def failsWith(clazz: Class[_ <: Throwable]) = {
        addExpectation(fragment, ctx, Expect.Failure.ExceptionFailure(Some(clazz), None))
      }
      def failsWith(clazz: Class[_ <: Throwable], message: StringMatcher) = {
        addExpectation(fragment, ctx, Expect.Failure.ExceptionFailure(Some(clazz), Some(message)))
      }
      def failsWith(message: StringMatcher) = {
        addExpectation(fragment, ctx, Expect.Failure.ExceptionFailure(None, Some(message)))
      }
      def failsWithLuaError(errorObject: AnyRef) = {
        addExpectation(fragment, ctx, Expect.Failure.LuaErrorFailure(errorObject))
      }
    }

  }

  protected implicit def fragmentToRichFragment(frag: Fragment): RichFragment = new RichFragment(frag)

  // for code structuring purposes only
  protected def expect(body: => Unit): Unit = {
    body
  }

  protected def stringStartingWith(prefix: String) = ValueMatch.StringStartingWith(prefix)
  protected val NaN = ValueMatch.NaN

  implicit def stringToMatcher(s: String): StringMatcher = StringMatcher(StringMatcher.Strict(s) :: Nil)

}

object FragmentExpectations {

  sealed trait Env
  object Env {
    case object Empty extends Env
    case object Basic extends Env
    case object Module extends Env
    case object Coro extends Env
    case object Math extends Env
    case object Str extends Env
    case object IO extends Env
    case object Tab extends Env
    case object Debug extends Env

    case object Full extends Env

  }

  sealed trait Expect {
    def tryMatch(actual: Try[Seq[AnyRef]], onFail: () => Unit)(spec: FunSpec): Unit
  }

  object Expect {
    case class Success(vms: Seq[ValueMatch]) extends Expect {
      override def tryMatch(actual: Try[Seq[AnyRef]], onFail: () => Unit)(spec: FunSpec) = {
        actual match {
          case scala.util.Success(vs) =>
            if (vs.size != vms.size) {
              onFail()
              spec.fail("result list size does not match: expected " + vms.size + ", got " + vs.size)
            }
            spec.assertResult(vs.size)(vms.size)

            for (((v, vm), i) <- (vs zip vms).zipWithIndex) {
              if (!vm.matches(v)) {
                onFail()
                spec.fail("value #" + i + " does not match: expected " + vm + ", got " + v)
              }
            }

          case scala.util.Failure(ex) =>
            onFail()
            spec.fail("Expected success, got an exception: " + ex.getMessage, ex)
        }
      }
    }

    sealed trait Failure extends Expect {

      protected def matchError(ex: Throwable, onFail: () => Unit)(spec: FunSpec): Unit

      override def tryMatch(actual: Try[Seq[AnyRef]], onFail: () => Unit)(spec: FunSpec) = {
        actual match {
          case scala.util.Success(vs) =>
            onFail()
            spec.fail("Expected failure, got success")

          case scala.util.Failure(ex) => matchError(ex, onFail)(spec)
        }
      }

    }

    object Failure {

      case class ExceptionFailure(optExpectClass: Option[Class[_ <: Throwable]], optExpectMessage: Option[StringMatcher]) extends Failure {
        override protected def matchError(ex: Throwable, onFail: () => Unit)(spec: FunSpec) = {
          for (expectClass <- optExpectClass) {
            val actualClass = ex.getClass
            if (!expectClass.isAssignableFrom(actualClass)) {
              onFail()
              spec.fail("Expected exception of type " + expectClass.getName + ", got " + actualClass.getName)
            }
          }

          for (messageMatcher <- optExpectMessage) {
            val actualMessage = ex.getMessage
            if (!messageMatcher.matches(actualMessage)) {
              onFail()
              spec.fail("Error message mismatch: expected \"" + messageMatcher + "\", got \"" + actualMessage + "\"")
            }
          }
        }
      }

      case class LuaErrorFailure(expectErrorObject: AnyRef) extends Failure {
        override protected def matchError(ex: Throwable, onFail: () => Unit)(spec: FunSpec) = {
          ex match {
            case le: LuaRuntimeException =>
              val actualErrorObject = Conversions.javaRepresentationOf(le.getErrorObject)
              if (expectErrorObject != actualErrorObject) {
                onFail()
                spec.fail("Error object mismatch: expected [" + expectErrorObject + "], got [" + actualErrorObject + "]")
              }
          }
        }
      }

    }

  }

  case class StringMatcher(parts: List[StringMatcher.Part]) {
    def <<(arg: String) = StringMatcher(parts :+ StringMatcher.NonStrict(arg))
    def >>(arg: String) = StringMatcher(parts :+ StringMatcher.Strict(arg))
    def matches(actual: String): Boolean = StringMatcher.matches(parts, actual)

    override def toString = parts.mkString("")
  }

  object StringMatcher {
    sealed trait Part
    case class Strict(s: String) extends Part {
      override def toString = s
    }
    case class NonStrict(s: String) extends Part {
      override def toString = "<<" + s + ">>"
    }

    def matches(parts: List[StringMatcher.Part], actual: String): Boolean = {

      def either(parts: List[StringMatcher.Part], idx: Int): Boolean = {
        parts.headOption match {
          case Some(h: Strict) => strict(parts, 0)
          case Some(h: NonStrict) => nonStrict(parts, 0)
          case None => true
        }
      }

      def strict(parts: List[StringMatcher.Part], idx: Int): Boolean = {
        parts match {
          case Strict(s) :: tail if actual.indexOf(s, idx) == idx => strict(tail, idx + s.length)
          case NonStrict(_) :: tail => nonStrict(tail, idx)
          case Nil => true
          case _ => false
        }
      }

      def nonStrict(parts: List[StringMatcher.Part], idx: Int): Boolean = {
        parts match {
          case Strict(s) :: tail =>
            val nextIdx = actual.indexOf(s, idx)
            (nextIdx >= 0) && (either(tail, nextIdx + s.length()) || nonStrict(parts, idx + 1))
          case NonStrict(_) :: tail => nonStrict(tail, idx)
          case Nil => true
        }
      }

      either(parts, 0)
    }

  }

  sealed trait ValueMatch {
    def matches(o: AnyRef): Boolean
  }
  object ValueMatch {
    case class Eq(v: AnyRef) extends ValueMatch {
      override def matches(o: AnyRef) = {
        if (o == null || v == null) {
          o eq v
        }
        else {
          v.getClass == o.getClass && v == o
        }
      }
    }
    case class SubtypeOf(c: Class[_]) extends ValueMatch {
      override def matches(o: AnyRef) = if (o == null) false else c.isAssignableFrom(o.getClass)
    }
    case class StringStartingWith(prefix: String) extends ValueMatch {
      override def matches(o: AnyRef) = {
        o match {
          case s: String if s.startsWith(prefix) => true
          case _ => false
        }
      }
    }
    case object NaN extends ValueMatch {
      override def matches(o: AnyRef) = {
        o match {
          case d: java.lang.Double if d.isNaN => true
          case _ => false
        }
      }
    }

  }

  private def toRembulanValue(v: Any): ValueMatch = {
    import ValueMatch._
    v match {
      case vm: ValueMatch => vm
      case null => Eq(null)
      case b: Boolean => Eq(java.lang.Boolean.valueOf(b))
      case i: Int => Eq(java.lang.Long.valueOf(i))
      case l: Long => Eq(java.lang.Long.valueOf(l))
      case f: Float => Eq(java.lang.Double.valueOf(f))
      case d: Double => Eq(java.lang.Double.valueOf(d))
      case s: String => Eq(s)
      case c: Class[_] => SubtypeOf(c)
      case _ => throw new IllegalArgumentException("illegal value: " + v)
    }
  }

}

trait OneLiners { this: FragmentBundle with FragmentExpectations =>

  private var prefixes: List[String] = Nil

  def about(desc: String)(body: => Unit): Unit = {
    val oldPrefixes = prefixes
    try {
      prefixes = desc :: oldPrefixes
      body
    }
    finally {
      prefixes = oldPrefixes
    }
  }

  private var context: FragmentExpectations.Env = null

  def in(env: FragmentExpectations.Env)(body: => Unit): Unit = {
    val oldContext = context
    try {
      context = env
      body
    }
    finally {
      context = oldContext
    }
  }

  protected def thisContext: FragmentExpectations.Env = context

  def program(body: String): RichFragment.InContext = {
    val name = (LuaFormat.escape(body) :: prefixes).reverse.mkString(": ")
    fragment(name)(body) in context
  }

}