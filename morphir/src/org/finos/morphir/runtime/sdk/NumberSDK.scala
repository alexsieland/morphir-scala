package org.finos.morphir.runtime.sdk

import org.finos.morphir.runtime.ErrorUtils.tryOption
import org.finos.morphir.runtime.RTValue.DivisionByZero
import org.finos.morphir.runtime.internal.{DynamicNativeFunction1, DynamicNativeFunction2, DynamicNativeFunction3, NativeContext}
import org.finos.morphir.runtime.{SDKValue, RTValue as RT}
import org.finos.morphir.runtime.RTValue.Primitive.Number as RTNumber
import org.finos.morphir.runtime.RTValue.Primitive.BigDecimal as RTDecimal
import spire.math.Rational

object NumberSDK {

  // Convert

  val fromInt = DynamicNativeFunction1("fromInt") {
    (_: NativeContext) => (int: RT.Primitive.Int) =>
      val result = Rational(int.value.toBigDecimal)
      RTNumber(result)
  }

  // Comparison

  // Arithmetic

  val add = DynamicNativeFunction2("add") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
        val result = a.value + b.value
        RTNumber(result)
  }

  val subtract = DynamicNativeFunction2("subtract") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = a.value - b.value
      RTNumber(result)
  }

  val multiply = DynamicNativeFunction2("multiply") {
    (_: NativeContext) =>
      (a: RTNumber, b: RTNumber) =>
        val result = a.value * b.value
        RTNumber(result)
  }

  val divide = DynamicNativeFunction2("divide") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = b.value match {
        case Rational.zero =>
          Left(DivisionByZero)
        case divisor =>
          val result = a.value / divisor
          Right(RTNumber(result))
      }
      ResultSDK.eitherToResult(result)
  }

  val abs = DynamicNativeFunction1("abs") {
    (_: NativeContext) => (num: RTNumber) =>
      val result = num.value.abs
      RTNumber(result)
  }

  val negate = DynamicNativeFunction1("negate") {
    (_: NativeContext) => (num: RTNumber) =>
      val result = num.value.unary_-
      RTNumber(result)
  }

  val reciprocal = DynamicNativeFunction1("reciprocal") {
    (_: NativeContext) => (num: RTNumber) =>
      val result = num.value.reciprocal
      RTNumber(result)
  }

  // Convert To

  // Misc

  // Constants

  val zero: SDKValue = SDKValue.SDKNativeValue(RTNumber(Rational.zero))

  val one: SDKValue = SDKValue.SDKNativeValue(RTNumber(Rational.one))
}
