module Morphir.Examples.App.NumberTests exposing (..)

import Morphir.Examples.App.TestUtils exposing (..)
import Morphir.SDK.Number as Number exposing (..)

-- Convert Functions

{-| Test: Number/fromInt
value = 2
expected = 2/1
-}
numberFromIntTest : Int -> Number
numberFromIntTest int =
    Number.fromInt
        int


-- Comparison Functions

{-| Test: Number/equal
a = 1/2
b = 1/2
expected = 1/2
-}
numberEqual : Number -> Number -> Number
numberEqual a b =
    Number.equal
        a
        b


{-| Test: Number/notEqual
a = 1/2
b = 3/2
expected = false
-}
numberNotEqual : Number -> Number -> Number
numberNotEqual a b =
    Number.notEqual
        a
        b


{-| Test: Number/lessThan
a = 1/2
b = 3/2
expected = true
-}
numberLessThan : Number -> Number -> Number
numberLessThan a b =
    Number.lessThan
        a
        b


{-| Test: Number/lessThanOrEqual
a = 1/2
b = 1/2
expected = true
-}
numberLessThanOrEqual : Number -> Number -> Number
numberLessThanOrEqual a b =
    Number.lessThanOrEqual
        a
        b


{-| Test: Number/greaterThan
a = 1/2
b = 3/2
expected = false
-}
numberGreaterThan : Number -> Number -> Number
numberGreaterThan a b =
    Number.greaterThan
        a
        b


{-| Test: Number/greaterThanOrEqual
a = 1/2
b = 1/2
expected = true
-}
numberGreaterThanOrEqual : Number -> Number -> Number
numberGreaterThanOrEqual a b =
    Number.greaterThanOrEqual
        a
        b


-- Arithmetic

{-| Test: Number/add
a = 1/3
b = 1/2
expected = 5/6
-}
numberAdd : Number -> Number -> Number
numberAdd a b =
    Number.add
        a
        b


{-| Test: Number/subtract
a = 1/3
b = 1/2
expected = 1/6
-}
numberSubract : Number -> Number -> Number
numberSubract a b =
    Number.subtract
        a
        b


{-| Test: Number/multiply
a = 2
b = 1/2
expected = 1
-}
numberSubract : Number -> Number -> Number
numberSubract a b =
    Number.multiply
        a
        b


{-| Test: Number/divide
a = 1
b = 1/2
expected = 2
-}
numberSubract : Number -> Number -> Number
numberSubract a b =
    Number.divide
        a
        b


{-| Test: Number/abs
value = -1/2
expected = 1/2
-}
numberAbs : Number -> Number
numberAbs num =
    Number.abs
        num


{-| Test: Number/negate
value = 1/2
expected = -1/2
-}
numberNegate : Number -> Number
numberNegate num =
    Number.negate
        num


{-| Test: Number/reciprocal
value = 1/2
expected = 2
-}
numberReciprocal : Number -> Number
numberReciprocal num =
    Number.reciprocal
        num


-- Convert To Functions

{-| Test: Number/toFractionalString
value = 1/2
expected = "1/2"
-}
numberToFractionalString : Number -> String
numberToFractionalString num =
    Number.toFractionalString
        num


{-| Test: Number/toDecimal
value = 1/2
expected = Just 0.5
-}
numberToDecimal : Number -> Maybe Decimal
numberToDecimal num =
    Number.toDecimal
        num


{-| Test: Number/coerceToDecimal
value = 1/2
expected = 0.5
-}
numberToDecimal : Decimal -> Number -> Decimal
numberToDecimal defaultDec num =
    Number.coerceToDecimal
        defaultDec
        num


-- Misc To Functions

{-| Test: Number/simplify
value = 2/4
expected = Just 1/2
-}
numberSimplify : Number -> Number
numberSimplify num =
    Number.simplify
        num


{-| Test: Number/isSimplified
value = 2/4
expected = Just 1/2
-}
numberIsSimplified : Number -> Number
numberIsSimplified num =
    Number.isSimplified
        num


-- Constants To Functions

{-| Test: Number/zero
-}
numberZero : Number
numberZero = Number.zero


{-| Test: Number/one
-}
numberOne : Number
numberOne = Number.one
