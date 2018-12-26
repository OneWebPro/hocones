package pl.onewebpro.hocones.parser.entity

import com.typesafe.config.ConfigValue
import pl.onewebpro.hocones.common.implicits.Path

case class HoconObject(path: Path, cfg: ConfigValue, values: Seq[HoconResultValue]) extends HoconResultType
