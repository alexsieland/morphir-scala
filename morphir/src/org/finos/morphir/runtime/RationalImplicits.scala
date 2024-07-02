package org.finos.morphir.runtime

import scala.util.Try

object RationalImplicits {
  trait RationalOrdering extends Ordering[spire.math.Rational] {
    def compare(x: spire.math.Rational, y: spire.math.Rational): Int = x.compare(y)
  }

  trait RationalIsFractional extends Fractional[spire.math.Rational] {
    def plus(x: spire.math.Rational, y: spire.math.Rational): spire.math.Rational = x + y

    def minus(x: spire.math.Rational, y: spire.math.Rational): spire.math.Rational = x - y

    def times(x: spire.math.Rational, y: spire.math.Rational): spire.math.Rational = x * y

    def negate(x: spire.math.Rational): spire.math.Rational = -x

    def fromInt(x: Int): spire.math.Rational = spire.math.Rational(x)

    def parseString(str: String): Option[spire.math.Rational] = Try(spire.math.Rational(str)).toOption

    def toInt(x: spire.math.Rational): Int = x.toInt

    def toLong(x: spire.math.Rational): Long = x.toLong

    def toFloat(x: spire.math.Rational): Float = x.toFloat

    def toDouble(x: spire.math.Rational): Double = x.toDouble

    def div(x: spire.math.Rational, y: spire.math.Rational): spire.math.Rational = x / y
  }

  implicit object RationalIsFractional extends RationalIsFractional with RationalOrdering
}
