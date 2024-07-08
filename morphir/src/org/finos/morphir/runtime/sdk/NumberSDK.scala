package org.finos.morphir.runtime.sdk

import org.finos.morphir.runtime.ErrorUtils.tryOption
import org.finos.morphir.runtime.RTValue.DivisionByZero
import org.finos.morphir.runtime.internal.{
  DynamicNativeFunction1,
  DynamicNativeFunction2,
  DynamicNativeFunction3,
  NativeContext
}
import org.finos.morphir.runtime.{SDKValue, RTValue as RT}
import org.finos.morphir.runtime.RTValue.Primitive.Number as RTNumber
import org.finos.morphir.runtime.RTValue.Primitive.BigDecimal as RTDecimal
import spire.math.{Rational, SafeLong}

import java.math.MathContext

object NumberSDK {

  // Convert

  val fromInt = DynamicNativeFunction1("fromInt") {
    (_: NativeContext) => (int: RT.Primitive.Int) =>
      val result = Rational(int.value.value)
      RTNumber(result)
  }

  // Comparison

  val equal = DynamicNativeFunction2("equal") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = a.value.equals(b.value)
      RT.Primitive.Boolean(result)
  }

  val notEqual = DynamicNativeFunction2("notEqual") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = !a.value.equals(b.value)
      RT.Primitive.Boolean(result)
  }

  val lessThan = DynamicNativeFunction2("lessThan") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = a.value < b.value
      RT.Primitive.Boolean(result)
  }

  val lessThanOrEqual = DynamicNativeFunction2("lessThanOrEqual") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = a.value <= b.value
      RT.Primitive.Boolean(result)
  }

  val greaterThan = DynamicNativeFunction2("greaterThan") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = a.value < b.value
      RT.Primitive.Boolean(result)
  }

  val greaterThanOrEqual = DynamicNativeFunction2("greaterThanOrEqual") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = a.value >= b.value
      RT.Primitive.Boolean(result)
  }

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
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = a.value * b.value
      RTNumber(result)
  }

  val divide = DynamicNativeFunction2("divide") {
    (_: NativeContext) => (a: RTNumber, b: RTNumber) =>
      val result = b.value match {
        case Rational.zero =>
          Left(DivisionByZero())
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
      val result = -num.value
      RTNumber(result)
  }

  val reciprocal = DynamicNativeFunction1("reciprocal") {
    (_: NativeContext) => (num: RTNumber) =>
      val result = num.value.reciprocal
      RTNumber(result)
  }

  // Convert To

  val toFractionalString = DynamicNativeFunction1("toFractionalString") {
    (_: NativeContext) => (num: RTNumber) =>
      val result = num.value.toString
      RT.Primitive.String(result)
  }

  val toDecimal = DynamicNativeFunction1("toDecimal") {
    (_: NativeContext) => (num: RTNumber) =>
      val result = tryOption(num.value.toBigDecimal(MathContext.UNLIMITED)).map(RTDecimal(_))
      MaybeSDK.optionToMaybe(result)
  }

  val coerceToDecimal = DynamicNativeFunction2("coerceToDecimal") {
    (_: NativeContext) => (defaultDec: RTDecimal, num: RTNumber) =>
      val result = tryOption(num.value.toBigDecimal(MathContext.UNLIMITED)).map(RTDecimal(_))
      result.getOrElse(defaultDec)
  }

  // Misc

  val simplify = DynamicNativeFunction1("simplify") {
    (_: NativeContext) => (num: RTNumber) =>
      val numerator   = num.value.numerator
      val denominator = num.value.denominator
      val (gcd, _)    = numerator.factor.gcd(denominator.factor).head
      gcd match {
        case SafeLong.one =>
          MaybeSDK.optionToMaybe(None)
        case d =>
          val result = Rational(numerator / d, denominator / d)
          MaybeSDK.optionToMaybe(Some(RTNumber(result)))
      }
  }

  val isSimplified = DynamicNativeFunction1("isSimplified") {
    (_: NativeContext) => (num: RTNumber) =>
      val nFactors = num.value.numerator.factor
      val dFactors = num.value.denominator.factor
      val (gcd, _) = nFactors.gcd(dFactors).head
      val result   = gcd.isOne
      RT.Primitive.Boolean(result)
  }

  // Constants

  val zero: SDKValue = SDKValue.SDKNativeValue(RTNumber(Rational.zero))

  val one: SDKValue = SDKValue.SDKNativeValue(RTNumber(Rational.one))
}
