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

package dev.foxgirl.rembulan.compiler

import dev.foxgirl.rembulan.compiler.analysis._
import dev.foxgirl.rembulan.compiler.ir.Code
import dev.foxgirl.rembulan.compiler.util.IRPrinterVisitor
import dev.foxgirl.rembulan.parser.Expressions
import dev.foxgirl.rembulan.parser.analysis.NameResolver
import dev.foxgirl.rembulan.parser.ast.{Chunk, Expr}
import dev.foxgirl.rembulan.parser.{Expressions, Parser}
import dev.foxgirl.rembulan.test.Util
import dev.foxgirl.rembulan.test.fragments._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, MustMatchers}

import java.io.{ByteArrayInputStream, PrintWriter}
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class IRTranslationTest extends FunSpec with MustMatchers {

  val bundles = Seq(
    BasicFragments,
    BasicLibFragments,
    CoroutineLibFragments,
    DebugLibFragments,
    IOLibFragments,
    MathLibFragments,
    MetatableFragments,
    OperatorFragments,
    StringLibFragments,
    TableLibFragments
  )

  def parseExpr(s: String): Expr = {
    val bais = new ByteArrayInputStream(s.getBytes)
    val parser = new Parser(bais)
    val result = parser.Expr()
    parser.Eof()
    result
  }

  def parseChunk(s: String): Chunk = {
    val bais = new ByteArrayInputStream(s.getBytes)
    val parser = new Parser(bais)
    parser.Chunk()
  }

  def resolveNames(c: Chunk): Chunk = NameResolver.resolveNames(c)

  def typeInfo(fn: IRFunc): TypeInfo = Typer.analyseTypes(fn)

  def dependencyInfo(fn: IRFunc): DependencyInfo = DependencyAnalyser.analyse(fn)

  def slotInfo(fn: IRFunc): SlotAllocInfo = SlotAllocator.allocateSlots(fn)

  def printTypes(types: TypeInfo): Unit = {
    println("Return type:")
    println("\t" + types.returnType())

    println("Values:")
    for (v <- types.vals().asScala) {
      println("\t" + v + " -> " + types.typeOf(v))
    }

    println("Multi-values:")
    for (mv <- types.multiVals().asScala) {
      println("\t" + mv + " -> " + types.typeOf(mv))
    }

    println("Variables:")
    for (v <- types.vars().asScala) {
      val reified = types.isReified(v)
      println("\t" + v + (if (reified) " (reified)" else ""))
    }
  }

  def printDeps(depInfo: DependencyInfo): Unit = {
    println("Nested refs:")
    if (depInfo.nestedRefs().isEmpty) {
      println("\t(none)")
    }
    else {
      val ids = depInfo.nestedRefs().asScala
      println("\t" + (ids map { id => "[" + id + "]"}).mkString(", "))
    }
  }

  def printSlots(types: TypeInfo, slots: SlotAllocInfo): Unit = {
    println("Slot info (%d total):".format(slots.numSlots()))
    println("Variables:")
    for (v <- types.vars().asScala) {
      println("\t" + v + " --> " + slots.slotOf(v))
    }
    println("Values:")
    for (v <- types.vals().asScala) {
      println("\t" + v + " --> " + slots.slotOf(v))
    }
  }

  def printBlocks(blocks: Code): Unit = {
    val pw = new PrintWriter(System.out)
    val printer = new IRPrinterVisitor(pw)
    printer.visit(blocks)
    pw.flush()

    println()
  }

  describe ("expression") {
    describe ("can be translated to IR:") {
      Util.silenced {

        for ((s, o) <- Expressions.get if o) {

          val ss = "return " + s

          it (ss) {
            val ck = resolveNames(parseChunk(ss))

            val mod = IRTranslator.translate(ck)
            for (fn <- mod.fns().asScala) {
              println("Function [" + fn.id + "]")
              println()
              printBlocks(fn.code)
            }
          }
        }

      }
    }
  }

  case class CompiledFn(fn: IRFunc, types: TypeInfo)
  case class CompiledModule(fns: Seq[CompiledFn])

  def translate(chunk: Chunk): Module = {
    val resolved = resolveNames(chunk)
    val modBuilder = new ModuleBuilder()
    IRTranslator.translate(resolved)
  }

  def compile(fn: IRFunc): CompiledFn = {
    val compiler = new LuaCompiler()
    val pfn = compiler.processFunction(fn)
    CompiledFn(pfn.fn, pfn.types)
  }

  def compile(mod: Module): CompiledModule = {
    CompiledModule(for (fn <- mod.fns().asScala) yield compile(fn))
  }

  for (b <- bundles) {
    describe ("from " + b.name + " :") {
      for (f <- b.all) {
        describe (f.description) {
          it ("can be translated to IR (translation only)") {
            Util.silenced {
              val code = f.code

              println("--BEGIN--")
              println(code)
              println("---END---")

              val ir = translate(parseChunk(code))

              for (fn <- ir.fns.asScala) {
                println("Function [" + fn.id + "]" + (if (fn.id.isRoot) " (main)" else ""))
                println()
                println("Code:")
                printBlocks(fn.code)
                println()
              }
            }

          }

          it ("can be translated to IR and optimised") {
            Util.silenced {
              val code = f.code

              println("--BEGIN--")
              println(code)
              println("---END---")

              val ir = translate(parseChunk(code))
              val cmod = compile(ir)

              def printCompiledFn(cfn: CompiledFn): Unit = {
                println("Function [" + cfn.fn.id + "]" + (if (cfn.fn.id.isRoot) " (main)" else ""))
                println()

                println("Params: (" + cfn.fn.params.asScala.mkString(", ") + ")")
                println("Upvals: (" + cfn.fn.upvals.asScala.mkString(", ") + ")")
                println()

                println("Code:")
                printBlocks(cfn.fn.code)
                println()

                println("Type information:")
                printTypes(cfn.types)
                println()

                printDeps(dependencyInfo(cfn.fn))
                println()

                printSlots(cfn.types, slotInfo(cfn.fn))
                println()
              }

              for (cfn <- cmod.fns) {
                printCompiledFn(cfn)
              }

            }

          }
        }
      }
    }
  }


}
