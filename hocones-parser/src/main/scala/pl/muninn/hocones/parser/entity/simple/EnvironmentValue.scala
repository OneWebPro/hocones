package pl.muninn.hocones.parser.entity.simple

import cats.effect.IO
import pl.muninn.hocones.parser.ParsingError
import pl.muninn.hocones.parser.entity.simple.EnvironmentValue.{EnvName, EnvValue}
import shapeless.tag
import shapeless.tag.@@

import scala.util.matching.Regex

case class EnvironmentValue(env: EnvValue, name: EnvName, isOptional: Boolean) extends ReferenceTypeValue

object EnvironmentValue {

  val envRegex: Regex = """(\$\{\??\w*.*?\})""".r // For finding any env name in string

  val envNameRegex: Regex =
    """\$\{\??(\w*.*?)\}""".r // For extracting env name from string

  private[EnvironmentValue] object EnvironmentValueInternal {

    trait EnvTag

    trait EnvNameTag

  }

  import EnvironmentValueInternal._

  type EnvValue = String @@ EnvTag
  type EnvName = String @@ EnvNameTag

  private def tagEnv(value: String): EnvValue = tag[EnvTag][String](value)

  private def tagEnvName(value: String): EnvName =
    tag[EnvNameTag][String](value)

  private[entity] def isOptionalEnv(value: EnvValue): Boolean =
    value(2).toString == "?"

  private[entity] def extractName(value: EnvValue): Option[EnvName] =
    envNameRegex
      .findAllMatchIn(value)
      .map(_.subgroups.last)
      .toList
      .headOption
      .map(tagEnvName)

  private[entity] def envName(value: String): Option[EnvValue] =
    if (isEnv(value)) Some(tagEnv(value)) else None

  private[entity] def containsEnv(value: String): Boolean =
    envRegex.findFirstMatchIn(value).isDefined

  private[entity] def isEnv(value: String): Boolean =
    envRegex.findAllMatchIn(value).length == 1

  def apply(value: String): IO[EnvironmentValue] =
    envName(value) match {
      case Some(env) =>
        extractName(env) match {
          case Some(name) =>
            IO(EnvironmentValue(env = env, name = name, isOptional = isOptionalEnv(env)))
          case _ =>
            IO.raiseError(ParsingError(s"Error during extracting environment from string $value"))
        }
      case None =>
        IO.raiseError(ParsingError(s"Value $value is not environment"))
    }

  def apply(notResolvedRef: NotResolvedRef): EnvironmentValue = {
    val value = notResolvedRef.env
      .replace(notResolvedRef.nameChunks.dropRight(1).mkString(".") + ".", "")
    val name = notResolvedRef.nameChunks.last
    EnvironmentValue(tagEnv(value), tagEnvName(name), notResolvedRef.isOptional)
  }
}
