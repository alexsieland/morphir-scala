package org.finos.morphir.runtime.sdk

import org.finos.morphir.ir.Type
import org.finos.morphir.naming._
import org.finos.morphir.runtime.MorphirRuntimeError.UnexpectedType
import org.finos.morphir.runtime.RTValue as RT
import org.finos.morphir.runtime.internal.{DynamicNativeFunction1, DynamicNativeFunction2, DynamicNativeFunction3, DynamicNativeFunction4, DynamicNativeFunction5, NativeContext}

/**
 * The "Result" SDK functions somewhat differently from ListSDK or others, because Result is not handled as a specific
 * RTValue but through the general RTConstructor type. This is because it is treated as such in the IR - Result values
 * are initialized with constructors and pattern matched as such.
 *
 * TODO: This distinction should be refactored away. This entails:
 *   - Adding RTValue variants for Result
 *   - Adding all supporting code for such (coercers + anything else)
 *   - Adding special cases to the Apply handling for Constructors, to create these special RTValues instead of the
 *     general form
 *   - Adding special cases to Constructor pattern matching to recognize these forms
 *   - Changing the RTValue to MDM process to reflect the new form
 *   - In this file:
 *   - Remove toEither and toResult
 *   - Change the types from RT.ConstructorResult to RT.Result
 *   - Change toEither(arg) calls to arg.value and toResult(result) calls to RT.Result(result)
 */
object ResultSDK {

  /**
   * Converts a "Result" - i.e., a ConstructorResult representing a morphir-elm Result value - to a Scala Either
   *
   * @param arg
   *   A ConstructorResult representing a morphir-elm Result value (Ok or Err)
   * @return
   *   An Either representing the argument in Scala terms - `Right(x)` if arg was `Ok x`, `Left(x)` if arg was `Err x`
   */
  private[sdk] def resultToEither(arg: RT.ConstructorResult): Either[RT, RT] =
    arg match {
      case RT.ConstructorResult(fqn, List(ok)) if fqn == FQName.fromString("Morphir.SDK:Result:Ok") =>
        Right(ok)
      case RT.ConstructorResult(fqn, List(err)) if fqn == FQName.fromString("Morphir.SDK:Result:Err") => Left(err)
      case RT.ConstructorResult(_, _) =>
        throw new UnexpectedType(s"Ok(value) or Err(err)", arg, hint = "Expected due to use in a native function")
    }

  /**
   * Converts a Scala Either to a "Result" - i.e., a ConstructorResult representing a morphir-elm Result value
   *
   * @param arg
   *   A Scala Either
   * @return
   *   A ConstructorResult representing the argument in Morphir terms - `Ok x` if arg was `Right(x)`, `Err x` if arg was
   *   `Left(x)`
   */
  private[sdk] def eitherToResult(arg: Either[RT, RT]): RT.ConstructorResult =
    arg match {
      case Right(ok) => RT.ConstructorResult(FQName.fromString("Morphir.SDK:Result:Ok"), List(ok))
      case Left(err) => RT.ConstructorResult(FQName.fromString("Morphir.SDK:Result:Err"), List(err))
    }

  val map = DynamicNativeFunction2("map") {
    (ctx: NativeContext) => (f: RT.Function, resultRaw: RT.ConstructorResult) =>
      {
        val out = resultToEither(resultRaw).map(elem => ctx.evaluator.handleApplyResult(Type.variable("a"), f, elem))
        eitherToResult(out)
      }
  }

  val map2 = DynamicNativeFunction3("map2") {
    (ctx: NativeContext) => (f: RT.Function, resultRaw1: RT.ConstructorResult, resultRaw2: RT.ConstructorResult) =>
    {
      val out: Either[RT, RT] = (resultToEither(resultRaw1), resultToEither(resultRaw2)) match {
        case (Right(r1), Right(r2)) => Right(ctx.evaluator.handleApplyResult2(Type.variable("a"), f, r1, r2))
        case (e @ Left(_), _) => e
        case (_, e @ Left(_)) => e
      }
      eitherToResult(out)
    }
  }

  val map3 = DynamicNativeFunction4("map3") {
    (ctx: NativeContext) => (f: RT.Function, resultRaw1: RT.ConstructorResult, resultRaw2: RT.ConstructorResult, resultRaw3: RT.ConstructorResult) =>
    {
      val out: Either[RT, RT] = (resultToEither(resultRaw1), resultToEither(resultRaw2), resultToEither(resultRaw3)) match {
        case (Right(r1), Right(r2), Right(r3)) => Right(ctx.evaluator.handleApplyResult3(Type.variable("a"), f, r1, r2, r3))
        case (e @ Left(_), _, _) => e
        case (_, e @ Left(_), _) => e
        case (_, _, e @ Left(_)) => e
      }
      eitherToResult(out)
    }
  }

  val map4 = DynamicNativeFunction5("map4") {
    (ctx: NativeContext) => (f: RT.Function, resultRaw1: RT.ConstructorResult, resultRaw2: RT.ConstructorResult, resultRaw3: RT.ConstructorResult, resultRaw4: RT.ConstructorResult) =>
    {
      val out: Either[RT, RT] = (resultToEither(resultRaw1), resultToEither(resultRaw2), resultToEither(resultRaw3), resultToEither(resultRaw4)) match {
        case (Right(r1), Right(r2), Right(r3), Right(r4)) => Right(ctx.evaluator.handleApplyResult4(Type.variable("a"), f, r1, r2, r3))
        case (e @ Left(_), _, _, _) => e
        case (_, e @ Left(_), _, _) => e
        case (_, _, e @ Left(_), _) => e
        case (_, _, _, e @ Left(_)) => e
      }
      eitherToResult(out)
    }
  }

  val mapError = DynamicNativeFunction2("mapError") {
    (ctx: NativeContext) => (f: RT.Function, resultRaw: RT.ConstructorResult) =>
      {
        val out =
          resultToEither(resultRaw).left.map(elem => ctx.evaluator.handleApplyResult(Type.variable("a"), f, elem))
        eitherToResult(out)
      }
  }

  val withDefault = DynamicNativeFunction2("withDefault") {
    (ctx: NativeContext) => (default: RT, resultRaw: RT.ConstructorResult) =>
      resultToEither(resultRaw).getOrElse(default)
  }

  val toMaybe = DynamicNativeFunction1("toMaybe") {
    (ctx: NativeContext) => (resultRaw: RT.ConstructorResult) =>
      val out = resultToEither(resultRaw).toOption
      MaybeSDK.optionToMaybe(out)
  }

  val fromMaybe = DynamicNativeFunction2("fromMaybe") {
    (ctx: NativeContext) => (err: RT, maybeRaw: RT.ConstructorResult) =>
      val out = MaybeSDK.maybeToOption(maybeRaw) match {
        case Some(value) => Right(value)
        case None        => Left(err)
      }
      eitherToResult(out)
  }

  val andThen = DynamicNativeFunction2("andThen") {
    (ctx: NativeContext) => (callback: RT.Function, resultRaw: RT.ConstructorResult) =>
      {
        val out = resultToEither(resultRaw).flatMap { elem =>
          val fromCallbackRaw = ctx.evaluator.handleApplyResult(Type.variable("a"), callback, elem)
          val fromCallbackCr  = RT.coerceConstructorResult(fromCallbackRaw)
          resultToEither(fromCallbackCr)
        }
        eitherToResult(out)
      }
  }
}
