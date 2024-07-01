package org.finos.morphir.runtime.sdk

import org.finos.morphir.runtime.ErrorUtils.tryOption
import org.finos.morphir.runtime.internal.{DynamicNativeFunction1, DynamicNativeFunction2, DynamicNativeFunction3, NativeContext}
import org.finos.morphir.runtime.{SDKValue, RTValue as RT}
import org.finos.morphir.runtime.RTValue.Primitive.Number as RTNumber
import org.finos.morphir.runtime.RTValue.Primitive.BigDecimal as RTDecimal
import spire.math.Rational

object NumberSDK {

  val zero: SDKValue = SDKValue.SDKNativeValue(RTNumber(Rational.zero))

  val one: SDKValue = SDKValue.SDKNativeValue(RTNumber(Rational.one))
}
