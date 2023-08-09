package org.finos.morphir.runtime

import org.finos.morphir.naming.*

import org.finos.morphir.naming._
import org.finos.morphir.ir.{Type as T, Value as V}
import V.Value
import T.Type
import org.finos.morphir.ir.Field
import org.finos.morphir.runtime.exports.*
import org.finos.morphir.ir.Type.UType
import org.finos.morphir.ir.Module.{Specification => ModSpec}
import zio.Chunk
import org.finos.morphir.ir.sdk.Basics
import org.finos.morphir.ir.distribution.Distribution
import org.finos.morphir.ir.distribution.Distribution.Library
import org.finos.morphir.ir.sdk
import org.finos.morphir.ir.Value.{USpecification => UValueSpec, Definition => ValueDefinition}
import org.finos.morphir.ir.Type.{USpecification => UTypeSpec}

class Distributions(dists: Map[PackageName, Distribution]) {

  def lookupModuleSpecification(packageName: PackageName, module: ModuleName): Option[ModSpec.Raw] =
    dists.get(packageName) match {
      case Some(Library(_, _, packageDef)) =>
        packageDef.toSpecification.modules.get(module)
      case None => None
    }

  def lookupValueSpecification(
      packageName: PackageName,
      module: ModuleName,
      localName: Name
  ): Option[UValueSpec] =
    lookupModuleSpecification(packageName, module).flatMap(_.lookupValueSpecification(localName))

  def lookupTypeSpecification(pName: PackageName, module: ModuleName, localName: Name): Option[UTypeSpec] =
    lookupModuleSpecification(pName, module).flatMap(_.lookupTypeSpecification(localName))
}
object Distributions {
  def apply(dists: Distribution*): Distributions =
    new Distributions(dists.map { case (lib: Library) => lib.packageName -> lib }.toMap)
}

object Extractors {
  object FQString {
    def unapply(fqName: FQName): Option[String] = Some(fqName.toString())
  }
  object ListRef {
    def unapply(tpe: UType): Option[UType] =
      tpe match {
        case Type.Reference(_, FQString("Morphir.SDK:List:list"), Chunk(elementType)) =>
          Some(elementType)
        case _ => None
      }
  }
  object MaybeRef {
    def unapply(tpe: UType): Option[UType] =
      tpe match {
        case Type.Reference(_, FQString("Morphir.SDK:Maybe:maybe"), Chunk(elementType)) =>
          Some(elementType)
        case _ => None
      }
  }

  object ResultRef {
    def unapply(tpe: UType): Option[(UType, UType)] =
      tpe match {
        case Type.Reference(attributes, FQString("Morphir.SDK:Result:result"), Chunk(keyType, valType)) =>
          Some((keyType, valType))
        case _ => None
      }
  }
  object DictRef {
    def unapply(tpe: UType): Option[(UType, UType)] =
      tpe match {
        case Type.Reference(attributes, FQString("Morphir.SDK:Dict:dict"), Chunk(keyType, valType)) =>
          Some((keyType, valType))
        case _ => None
      }
  }
  trait CommonReference {
    val tpe: UType
    def unapply(argTpe: UType): Boolean =
      argTpe match {
        case Type.Reference(_, fqName, Chunk()) if fqName == tpe.asInstanceOf[Type.Reference[Unit]].typeName => true
        case _                                                                                               => false
      }
  }
  object IntRef extends CommonReference {
    final val tpe = Basics.intType
  }

  object Int32Ref extends CommonReference {
    final val tpe = sdk.Int.int32Type
  }
  object BoolRef extends CommonReference {
    final val tpe = Basics.boolType
  }
  object FloatRef extends CommonReference {
    final val tpe = Basics.floatType
  }
  object StringRef extends CommonReference {
    final val tpe = sdk.String.stringType
  }
  object CharRef extends CommonReference {
    final val tpe = sdk.Char.charType
  }
  object SimpleRef {
    def unapply(tpe: UType): Boolean = tpe match {
      case IntRef()        => true
      case Int32Ref()      => true
      case BoolRef()       => true
      case FloatRef()      => true
      case StringRef()     => true
      case CharRef()       => true
      case ListRef(_)      => true
      case MaybeRef(_)     => true
      case DictRef(_, _)   => true
      case ResultRef(_, _) => true
      case _               => false
    }
  }
  class Dealiased(dists: Distributions) {
    def unapply(tpe: UType): Option[(UType, Map[Name, UType])] = // If it's aliased we may need to grab bindings
      tpe match {
        case SimpleRef() => None
        case Type.Reference(_, typeName, typeArgs) =>
          val lookedUp = dists.lookupTypeSpecification(typeName.packagePath, typeName.modulePath, typeName.localName)
          lookedUp match {
            case Some(T.Specification.TypeAliasSpecification(typeParams, expr)) =>
              val newBindings = typeParams.zip(typeArgs).toMap
              Some(expr, newBindings)
            case _ => None
          }
        case _ => None
      }
  }
}

object Utils {
  import Extractors.*

  def dealias(original_tpe: UType, dists: Distributions, bindings: Map[Name, UType]): UType = {
    def loop(tpe: UType, bindings: Map[Name, UType]): UType =
      tpe match {
        case SimpleRef() => applyBindings(tpe, bindings) // nothing further to look up
        case Type.Reference(_, typeName, typeArgs) =>
          val lookedUp = dists.lookupTypeSpecification(typeName.packagePath, typeName.modulePath, typeName.localName)
          lookedUp match {
            case Some(T.Specification.TypeAliasSpecification(typeParams, expr)) =>
              val resolvedArgs = typeArgs.map(dealias(_, dists, bindings)) // I think?
              val newBindings  = typeParams.zip(resolvedArgs).toMap
              loop(expr, bindings ++ newBindings)
            case Some(_) => applyBindings(tpe, bindings) // Can't dealias further
            case None =>
              throw new TypeNotFound(s"Unable to find $tpe while dealiasing $original_tpe") // TODO: Thread properly
          }
        case other => applyBindings(other, bindings) // Not an alias
      }
    loop(original_tpe, bindings)
  }
  def applyBindings(tpe: UType, bindings: Map[Name, UType]): UType =
    tpe match {
      case Type.Variable(_, name) if bindings.contains(name) => bindings(name)
      case Type.Tuple(_, elements)                           => T.tupleVar(elements.map(applyBindings(_, bindings)): _*)
      case DictRef(keyType, valueType) =>
        sdk.Dict.dictType(applyBindings(keyType, bindings), applyBindings(valueType, bindings))
      case ListRef(elemType)  => sdk.List.listType(applyBindings(elemType, bindings))
      case MaybeRef(elemType) => sdk.Maybe.maybeType(applyBindings(elemType, bindings))
      case Type.Record(_, argFields) =>
        T.record(argFields.map(field => Field(field.name, applyBindings(field.data, bindings))))
      case Type.Function(_, argType, retType) =>
        T.function(applyBindings(argType, bindings), applyBindings(retType, bindings))
      case Type.Reference(_, name, argTypes) => T.reference(name, argTypes.map(applyBindings(_, bindings)))
      case other                             => other // leaf nodes
    }

  def typeCheckArg(arg: UType, param: UType, found: Map[Name, UType]): Either[TypeError, Map[Name, UType]] =
    (arg, param) match {
      case (argType, Type.Variable(_, name)) =>
        if (found.contains(name) && found(name) != argType) {
          Left(InferenceConflict(s"Both ${found(name)} and $argType bound to type variable $name"))
        } else {
          Right(found + (name -> argType))
        }
      case (Type.Unit(_), Type.Unit(_)) => Right(found)
      case (IntRef(), IntRef())         => Right(found) // Right?
      case (Int32Ref(), Int32Ref())     => Right(found)
      case (FloatRef(), FloatRef())     => Right(found)
      case (StringRef(), StringRef())   => Right(found)
      case (CharRef(), CharRef())       => Right(found)
      case (BoolRef(), BoolRef())       => Right(found)
      case (Type.Tuple(_, argElements), Type.Tuple(_, paramElements)) =>
        if (argElements.length != paramElements.length) {
          Left(new TypeMismatch(s"Different tuple arity between arg $argElements and parameter $paramElements"))
        } else {
          argElements.zip(paramElements).foldLeft(Right(found): Either[TypeError, Map[Name, UType]]) {
            case (acc, (argElement, paramElement)) =>
              acc.flatMap(found => typeCheckArg(argElement, paramElement, found))
          }
        }
      case (DictRef(argKey, argValue), DictRef(paramKey, paramValue)) =>
        for {
          keyBindings   <- typeCheckArg(argKey, paramKey, found)
          valueBindings <- typeCheckArg(argValue, paramValue, keyBindings)
        } yield valueBindings
      case (ResultRef(argErr, argOk), ResultRef(paramErr, paramOk)) =>
        for {
          errBindings <- typeCheckArg(argErr, paramErr, found)
          okBindings <- typeCheckArg(argOk, paramOk, errBindings)
        } yield okBindings
      case (ListRef(argElement), ListRef(paramElement))   => typeCheckArg(argElement, paramElement, found)
      case (MaybeRef(argElement), MaybeRef(paramElement)) => typeCheckArg(argElement, paramElement, found)
      case (Type.Record(_, argFields), Type.Record(_, paramFields)) =>
        if (argFields.length != paramFields.length) {
          Left(WrongRecordSize(s"Record lengths differ between arg : $argFields and param: $paramFields"))
        } else {
          argFields.zip(paramFields).foldLeft(Right(found): Either[TypeError, Map[Name, UType]]) {
            case (acc, (argField, paramField)) =>
              acc.flatMap(found => typeCheckArg(argField.data, paramField.data, found))
          }
        }
      case (Type.Function(_, argArg, argReturn), Type.Function(_, paramArg, paramReturn)) =>
        for {
          argBindings   <- typeCheckArg(argArg, paramArg, found)
          paramBindings <- typeCheckArg(argReturn, paramReturn, argBindings)
        } yield paramBindings
      case (Type.ExtensibleRecord(_, _, _), Type.ExtensibleRecord(_, _, _)) =>
        Left(UnsupportedType(s"Extensible record type not supported (yet)"))
      case (Type.Reference(_, argTypeName, argTypeArgs), Type.Reference(_, paramTypeName, paramTypeArgs))
          if (argTypeName == paramTypeName) =>
        argTypeArgs.zip(paramTypeArgs).foldLeft(Right(found): Either[TypeError, Map[Name, UType]]) {
          case (acc, (argTpe, paramTpe)) =>
            acc.flatMap(found => typeCheckArg(argTpe, paramTpe, found))
        }
      case (otherArg, otherParam) => Left(NotImplementedType(s"Cannot match $otherArg with $otherParam"))
    }
  def specificationToType[TA](spec: V.Specification[TA]): Type[TA] =
    curryTypeFunction(spec.output, spec.inputs)

  def unCurryTypeFunction(
      curried: UType,
      args: List[UType],
      dists: Distributions,
      knownBindings: Map[Name, UType]
  ): RTAction[Any, TypeError, UType] = {
    val dealiaser = new Dealiased(dists)
    (curried, args) match {
      case (Type.Function(attributes, parameterType, returnType), head :: tail) =>
        for {
          bindings    <- RTAction.fromEither(typeCheckArg(head, parameterType, knownBindings))
          appliedType <- unCurryTypeFunction(returnType, tail, dists, bindings)
        } yield appliedType
      case (tpe, Nil) => RTAction.succeed(applyBindings(tpe, knownBindings))
      case (dealiaser(inner, aliasBindings), args) =>
        unCurryTypeFunction(inner, args, dists, knownBindings ++ aliasBindings)
      case (nonFunction, head :: _) =>
        RTAction.fail(TooManyArgs(s"Tried to apply argument $head to non-function $nonFunction"))
    }
  }
  def isNative(fqn: FQName): Boolean = {
    val example = FQName.fromString("Morphir.SDK:Basics:equal")
    fqn.getPackagePath == example.getPackagePath
  }

  // TODO: Implement
  def typeCheck[TA](t1: Type[TA], t2: Type[TA]): RTAction[Any, TypeError, Unit] = RTAction.succeed(())
  def curryTypeFunction[TA](inner: Type[TA], params: Chunk[(Name, Type[TA])]): Type[TA] =
    params match {
      case Chunk() => inner
      case chunk =>
        curryTypeFunction(Type.Function(getattributes(inner), chunk.head._2, inner), chunk.tail)
    }

  def getattributes[TA](tpe: Type[TA]): TA =
    tpe match {
      case Type.ExtensibleRecord(attributes, _, _) => attributes
      case Type.Function(attributes, _, _)         => attributes
      case Type.Record(attributes, _)              => attributes
      case Type.Reference(attributes, _, _)        => attributes // TODO: Ignored type arguments here might be an issue
      case Type.Tuple(attributes, _)               => attributes
      case Type.Unit(attributes)                   => attributes
      case Type.Variable(attributes, _)            => attributes
    }

}