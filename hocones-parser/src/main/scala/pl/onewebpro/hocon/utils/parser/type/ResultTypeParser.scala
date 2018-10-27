package pl.onewebpro.hocon.utils.parser.`type`

import cats.effect.IO
import cats.implicits._
import com.typesafe.config.{Config, ConfigList, ConfigObject, ConfigValue}
import pl.onewebpro.hocon.utils.parser.HoconParser._
import pl.onewebpro.hocon.utils.parser.ParsingError
import pl.onewebpro.hocon.utils.parser.`type`.ResultType.ResultType
import pl.onewebpro.hocon.utils.parser.entity.{HoconArray, HoconObject, HoconResultValue}

import scala.collection.JavaConverters._

object ResultTypeParser {

  import pl.onewebpro.hocon.utils.parser.ops.HoconOps._

  //TODO test me
  def parse(path: Path, value: ResultType, configValue: ConfigValue)
                                     (implicit cfg: Config): IO[HoconResultValue] = value match {
    case ResultType.LIST => configValue match {
      case list: ConfigList => for {
        values <- list.asScala.toList.zipWithIndex.map {
          case (listConfigValue, index) =>
            val (tag, canonicalName) = (tagPath(path + s".$index"), listConfigValue.canonicalName)
            parseValue(tag, listConfigValue, canonicalName)
        }.sequence
      } yield HoconArray(path, configValue, values)
      case _ => IO.raiseError(ParsingError(s"Something is wrong $path is not ConfigList"))
    }
    case ResultType.OBJECT => configValue match {
      case hoconObject: ConfigObject => for {
        entrySet <- IO(hoconObject.entrySet().asScala)
        tupled <- IO(entrySet.map(mapEntryToTuple).toSet)
        withExtraPath <- IO(tupled.map { case (objectPath, objectValue, name) => (tagPath(path + "." + objectPath), objectValue, name) })
        results <- parseEntrySet(withExtraPath)
      } yield HoconObject(path, configValue, results)
      case _ => IO.raiseError(ParsingError(s"Something is wrong $path is not ConfigList"))
    }
  }
}