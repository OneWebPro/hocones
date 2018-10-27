package pl.onewebpro.hocon.utils.parser.entity

import com.typesafe.config.ConfigValue
import pl.onewebpro.hocon.utils.parser.HoconParser.Path

case class HoconResolvedReference(value: Result, reference: HoconReferenceValue) extends HoconValueType {
  override lazy val path: Path = reference.path
  lazy val cfg: ConfigValue = reference.cfg
}