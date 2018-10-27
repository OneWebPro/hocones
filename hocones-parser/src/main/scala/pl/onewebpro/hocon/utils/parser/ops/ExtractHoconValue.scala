package pl.onewebpro.hocon.utils.parser.ops

import pl.onewebpro.hocon.utils.parser.entity._
import pl.onewebpro.hocon.utils.parser.entity.simple.{ComposedConfigValue, SimpleHoconValue}

private[ops] class ExtractHoconValue[T <: SimpleHoconValue](isType: Result => Boolean) {

  private[ops] def isValue: Result => Boolean = {
    case ComposedConfigValue(_, composedValues) => composedValues.exists(isValue)
    case value => isType(value)
  }

  private[ops] def resultContainsValue: Result => Boolean = {
    case HoconConcatenation(_, _, concatenatedValues) => concatenatedValues.values.exists(isValue)
    case HoconMergedValues(_, _, default, replace) => isValue(default) || isValue(replace)
    case HoconArray(_, _, arrayValues) => arrayValues.exists(resultContainsValue)
    case HoconObject(_, _, objectValues) => objectValues.exists(resultContainsValue)
    case HoconReferenceValue(_, _, referenceValue) => isValue(referenceValue)
    case HoconEnvironmentValue(_, _, environmentValue) => isValue(environmentValue)
    case HoconValue(_, _, _, value) => isValue(value)
    case _ => false
  }

  private[ops] def extractValues: HoconResultValue => Iterable[T] = {
    case HoconConcatenation(_, _, concatenatedValues) =>
      concatenatedValues.values.flatMap {
        case value if isType(value) => Iterable(value.asInstanceOf[T])
        case _ => Nil
      }
    case HoconMergedValues(_, _, default, replace) =>
      Iterable(default, replace).flatMap {
        case value if isType(value) => Iterable(value.asInstanceOf[T])
        case _ => Nil
      }
    case HoconArray(_, _, arrayValues) => arrayValues.flatMap(extractValues)
    case HoconObject(_, _, objectValues) => objectValues.flatMap(extractValues)
    case HoconEnvironmentValue(_, _, value) if isType(value) => Iterable(value.asInstanceOf[T])
    case HoconValue(_, _, _, value) if isType(value) => Iterable(value.asInstanceOf[T])
    case HoconReferenceValue(_, _, value) if isType(value) => Iterable(value.asInstanceOf[T])
  }


  private[ops] def toExtractedValues: HoconResultValue => Option[ExtractedValue[T]] = { value =>
    if (!resultContainsValue(value)) None else Some(ExtractedValue[T](value.cfg, value, extractValues(value)))
  }

}