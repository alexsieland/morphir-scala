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

  // Convert To

  // Misc

  // Constants

  val zero: SDKValue = SDKValue.SDKNativeValue(RTNumber(Rational.zero))

  val one: SDKValue = SDKValue.SDKNativeValue(RTNumber(Rational.one))
}
