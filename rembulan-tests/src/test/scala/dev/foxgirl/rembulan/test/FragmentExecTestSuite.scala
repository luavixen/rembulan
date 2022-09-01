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

import dev.foxgirl.rembulan.compiler.CompilerSettings.CPUAccountingMode
import dev.foxgirl.rembulan.compiler.{CompilerChunkLoader, CompilerSettings}
import dev.foxgirl.rembulan.env.RuntimeEnvironments
import dev.foxgirl.rembulan.exec._
import dev.foxgirl.rembulan.impl.StateContexts
import dev.foxgirl.rembulan.lib._
import dev.foxgirl.rembulan.load.{ChunkClassLoader, ChunkLoader}
import dev.foxgirl.rembulan.runtime.RuntimeCallInitialiser
import dev.foxgirl.rembulan.test.FragmentExpectations.Env
import dev.foxgirl.rembulan.test.Util.{BufferPrinter, Printer}
import dev.foxgirl.rembulan.{Conversions, StateContext, Table, Variable}
import org.scalatest.{FunSpec, MustMatchers}

import scala.util.{Failure, Success}

trait FragmentExecTestSuite extends FunSpec with MustMatchers {

  def bundles: Seq[FragmentBundle]
  def expectations: Seq[FragmentExpectations]
  def contexts: Seq[FragmentExpectations.Env]

  def steps: Seq[Int]

  def compilerConfigs: CompilerConfigs = CompilerConfigs.DefaultOnly

  protected val Empty = FragmentExpectations.Env.Empty
  protected val Basic = FragmentExpectations.Env.Basic
  protected val Mod = FragmentExpectations.Env.Module
  protected val Coro = FragmentExpectations.Env.Coro
  protected val Math = FragmentExpectations.Env.Math
  protected val Str = FragmentExpectations.Env.Str
  protected val IO = FragmentExpectations.Env.IO
  protected val Tab = FragmentExpectations.Env.Tab
  protected val Debug = FragmentExpectations.Env.Debug
  protected val Full = FragmentExpectations.Env.Full

  protected def envForContext(state: StateContext, ctx: Env, ldr: ChunkLoader, printer: Printer): Table = {
    val env = state.newTable()
    val runtimeEnv = RuntimeEnvironments.system()  // FIXME
    val moduleClassLoader = this.getClass().getClassLoader

    ctx match {
      case Empty =>
        // no-op

      case Basic =>
        BasicLib.installInto(state, env, runtimeEnv, ldr)

      case Mod =>
        BasicLib.installInto(state, env, runtimeEnv, ldr)
        ModuleLib.installInto(state, env, runtimeEnv, ldr, moduleClassLoader)

      case Coro =>
        BasicLib.installInto(state, env, runtimeEnv, ldr)
        CoroutineLib.installInto(state, env)

      case Math =>
        BasicLib.installInto(state, env, runtimeEnv, ldr)
        MathLib.installInto(state, env)

      case Str =>
        BasicLib.installInto(state, env, runtimeEnv, ldr)
        StringLib.installInto(state, env)

      case IO =>
        BasicLib.installInto(state, env, runtimeEnv, ldr)
        IoLib.installInto(state, env, runtimeEnv)

      case Tab =>
        BasicLib.installInto(state, env, runtimeEnv, ldr)
        TableLib.installInto(state, env)

      case Debug =>
        BasicLib.installInto(state, env, runtimeEnv, ldr)
        DebugLib.installInto(state, env)

      case Full =>
        BasicLib.installInto(state, env, runtimeEnv, ldr)
        ModuleLib.installInto(state, env, runtimeEnv, ldr, moduleClassLoader)
        CoroutineLib.installInto(state, env)
        MathLib.installInto(state, env)
        StringLib.installInto(state, env)
        IoLib.installInto(state, env, runtimeEnv)
        OsLib.installInto(state, env, runtimeEnv)
        TableLib.installInto(state, env)
        Utf8Lib.installInto(state, env)
        DebugLib.installInto(state, env)
    }

    env
  }

  sealed trait ChkLoader {
    def name: String
    def loader(): ChunkLoader
  }

  def compilerSettingsToString(settings: CompilerSettings): String = {
    val cpu = settings.cpuAccountingMode() match {
      case CPUAccountingMode.NO_CPU_ACCOUNTING => "n"
      case CPUAccountingMode.IN_EVERY_BASIC_BLOCK => "a"
    }
    val cfold = settings.constFolding() match {
      case true => "t"
      case false => "f"
    }
    val ccache = settings.constCaching() match {
      case true => "t"
      case false => "f"
    }
    val nlimit = settings.nodeSizeLimit() match {
      case 0 => "0"
      case n => n.toString
    }
    cpu + cfold + ccache + "_" + nlimit
  }

  case class RembulanChkLoader(settings: CompilerSettings) extends ChkLoader {
    def name = "RemC" + "_" + compilerSettingsToString(settings)
    def loader() = CompilerChunkLoader.of(new ChunkClassLoader(), settings, "fragment_test_")
  }

  class CompilerConfigs private (configs: Seq[CompilerSettings]) {
    def loaders: Seq[RembulanChkLoader] = configs.distinct map RembulanChkLoader
  }
  object CompilerConfigs {
    val bools = Seq(true, false)
    val limits = Seq(0, 10)
//    val limits = Seq(0)

    val allConfigs = for (
      cpu <- CPUAccountingMode.values();
      cfold <- bools;
      ccache <- bools;
      nlimit <- limits
    ) yield CompilerSettings.defaultSettings()
        .withCPUAccountingMode(cpu)
        .withConstFolding(cfold)
        .withConstCaching(ccache)
        .withNodeSizeLimit(nlimit)

    case object DefaultOnly extends CompilerConfigs(Seq(CompilerSettings.defaultSettings()))
    case object All extends CompilerConfigs(allConfigs)
  }

  val ldrs = compilerConfigs.loaders

  for (bundle <- bundles;
       fragment <- bundle.all;
       ctx <- contexts) {

    val prefix = ""

    describe (prefix + fragment.description + " in " + ctx + ":") {

      for (s <- steps; l <- ldrs) {

        val stepDesc = s match {
          case Int.MaxValue => "max"
          case i => i.toString
        }

        it (l.name + " / " + stepDesc) {

          val printer = new BufferPrinter()

          val (state, func) = Util.timed(printer, "Compilation and setup") {

            val ldr = l.loader()

            val state = StateContexts.newDefaultInstance()

            val env = envForContext(state, ctx, ldr, printer)
            val func = ldr.loadTextChunk(new Variable(env), "test", fragment.code)

            (state, func)
          }

          var steps = 0

          val before = System.nanoTime()

          val callExecutor = DirectCallExecutor.newExecutorWithTickLimit(s)

          var resultValues: Array[AnyRef] = null
          var continuation: Continuation = RuntimeCallInitialiser.forState(state).newCall(func)
          var error: CallException = null

          do {
            try {
              steps += 1
              resultValues = callExecutor.resume(continuation)
            }
            catch {
              case ex: CallPausedException => continuation = ex.getContinuation
              case ex: CallException => error = ex
            }
          } while (error == null && resultValues == null)

          val res = if (error != null) {
            Failure(error.getCause)
          }
          else {
            require (resultValues != null, "result must not be null")
            Success(resultValues.toSeq)
          }

          val after = System.nanoTime()

          val totalTimeMillis = (after - before) / 1000000.0
//          val totalCPUUnitsSpent = preemptionContext.totalCost
//          val avgTimePerCPUUnitNanos = (after - before).toDouble / totalCPUUnitsSpent.toDouble
//          val avgCPUUnitsPerSecond = (1000000000.0 * totalCPUUnitsSpent) / (after - before)

          printer.println("Execution took %.1f ms".format(totalTimeMillis))
//          println("Total CPU cost: " + preemptionContext.totalCost + " LI")
          printer.println("Computation steps: " + steps)
//          println()
//          println("Avg time per unit: %.2f ns".format(avgTimePerCPUUnitNanos))
//          println("Avg units per second: %.1f LI/s".format(avgCPUUnitsPerSecond))
          printer.println()

          res match {
            case Success(result) =>
              printer.println("Result: success (" + result.size + " values):")
              for ((v, i) <- result.zipWithIndex) {
                printer.println(i + ":" + "\t" + Conversions.toHumanReadableString(v) + " (" + (if (v != null) v.getClass.getName else "null") + ")")
              }
            case Failure(ex) =>
              printer.println("Result: error: " + ex.getMessage)
          }

          for (expects <- expectations;
               ctxExp <- expects.expectationFor(fragment);
               exp <- ctxExp.get(ctx)) {

            exp.tryMatch(res, { () => scala.Predef.print(printer.get) })(this)
          }

        }
      }
    }
  }

}
