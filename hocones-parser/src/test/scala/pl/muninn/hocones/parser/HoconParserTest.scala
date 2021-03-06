package pl.muninn.hocones.parser

import com.typesafe.config.{Config, ConfigList, ConfigValue, ConfigValueFactory}
import pl.muninn.hocones.common.implicits._
import pl.muninn.hocones.parser.HoconParser.CanonicalClassName
import pl.muninn.hocones.parser.`type`.SimpleValueType
import pl.muninn.hocones.parser.entity._
import pl.muninn.hocones.parser.entity.simple.EnvironmentValue

import scala.collection.JavaConverters._

class HoconParserTest extends TestSpec {

  behavior.of("HoconParser")

  it should "map java map entries to tagged tuples" in {
    val map = new java.util.HashMap[String, ConfigValue]()
    map.put("pl.muninn.test.value_1", ConfigValueFactory.fromAnyRef(1.2))
    val result = HoconParser.mapEntryToTuple(map.entrySet().asScala.head)
    result shouldBe (
      tagPath("pl.muninn.test.value_1"),
      ConfigValueFactory.fromAnyRef(1.2),
      HoconParser.tagCanonicalName("com.typesafe.config.impl.ConfigDouble")
    )
  }

  it should "render ConfigValues" in {
    val config: Config = loadConfig("commented.conf")
    val value = config.getValue("pl.muninn.test.commented.value_1")
    val expected = HoconParser.tagRenderedValue("\"some value\"")
    HoconParser.render(value) shouldBe expected
  }

  it should "parseValue for simple value types" in new ParserConfigFixture {
    simpleValues.map { value =>
      HoconParser.parseValue(value).unsafeRunSync() should matchPattern {
        case HoconValue(value._1, _, _, _) =>
      }
    }
  }

  it should "parseValue for value types" in new ParserConfigFixture {
    val concatValue = valueTuple(s"$pathPrefix.concatenation.value_1", "com.typesafe.config.impl.ConfigConcatenation")
    val mergeValue = valueTuple(s"$pathPrefix.merge.value_1", "com.typesafe.config.impl.ConfigDelayedMerge")
    val referenceValue = valueTuple(s"$pathPrefix.reference.value_1", "com.typesafe.config.impl.ConfigReference")
    HoconParser.parseValue(concatValue).unsafeRunSync() should matchPattern {
      case HoconConcatenation(concatValue._1, _, _) =>
    }
    HoconParser.parseValue(mergeValue).unsafeRunSync() should matchPattern {
      case HoconMergedValues(mergeValue._1, _, _, _) =>
    }
    HoconParser.parseValue(referenceValue).unsafeRunSync() should matchPattern {
      case HoconReferenceValue(referenceValue._1, _, _) =>
    }
  }

  it should "parseValue for list result type" in new ParserConfigFixture {
    val listValue = valueTuple(s"$pathPrefix.array.value_1", "com.typesafe.config.impl.SimpleConfigList")
    HoconParser.parseValue(listValue).unsafeRunSync() should matchPattern {
      case HoconArray(listValue._1, listValue._2, _) =>
    }
  }

  it should "parseValue for object result type" in {
    implicit val config: Config = loadConfig("object.conf")
    val path = "pl.muninn.test.object.value_1"
    val entries = getEntriesMap
    // retrieve first item of array which should be ConfigObject
    val value = entries(path).asInstanceOf[ConfigList].asScala.head
    val objectValue =
      (tagPath(path), value, HoconParser.tagCanonicalName("com.typesafe.config.impl.SimpleConfigObject"))
    HoconParser.parseValue(objectValue).unsafeRunSync() should matchPattern {
      case HoconObject(objectValue._1, objectValue._2, _) =>
    }
  }

  it should "raise error when calling parseValue for unknown type" in new ParserConfigFixture {
    val badValue = valueTuple(s"$pathPrefix.simple.value_3", "com.typesafe.config.ConfigSomething")
    assertThrows[ParsingError](HoconParser.parseValue(badValue).unsafeRunSync())
  }

  it should "parseEntrySet for multiple values" in new ParserConfigFixture {
    val result = HoconParser.parseEntrySet(simpleValues).unsafeRunSync()
    result.size shouldBe 6
    result.head should matchPattern {
      case HoconValue(_, _, _, _) =>
    }
  }

  it should "parse simple configurations" in {
    val config: Config = loadConfig("simple.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    result.results.size shouldBe 7

    val sortedValues = result.results.map(_.asInstanceOf[HoconValue]).sortWith(_.path < _.path)
    val expectedPaths = (1 to 7).map(i => tagPath(s"pl.muninn.test.simple.value_$i"))
    val expectedValueTypes = List(
      SimpleValueType.QUOTED_STRING,
      SimpleValueType.QUOTED_STRING,
      SimpleValueType.INT,
      SimpleValueType.DOUBLE,
      SimpleValueType.BOOLEAN,
      SimpleValueType.UNQUOTED_STRING,
      SimpleValueType.LONG
    )
    sortedValues.map(_.path) shouldBe expectedPaths
    sortedValues.map(_.valueType) shouldBe expectedValueTypes
  }
  it should "parse concatenations in configuration" in {
    val config: Config = loadConfig("concatenation.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    result.results.size shouldBe 9
    //TODO more tests
  }

  it should "parse reference values" in {
    val config: Config = loadConfig("reference.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    result.results.size shouldBe 5
    //TODO more tests
  }

  it should "parse merge values" in {
    val config: Config = loadConfig("merge.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    result.results.size shouldBe 7
    //TODO more tests
  }

  it should "parse array values" in {
    val config: Config = loadConfig("array.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    result.results.size shouldBe 3
    //TODO more tests
  }

  it should "parse object values" in {
    val config: Config = loadConfig("object.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    result.results.size shouldBe 2
    //TODO more tests
  }

  it should "parse complex example" in {
    val config: Config = loadConfig("parser.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    result.results.size shouldBe 33
    //TODO more tests
  }

  it should "return 7 results for simple.conf file as map" in {
    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("simple.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()

    val expectedPaths = (1 to 7).map(i => tagPath(s"pl.muninn.test.simple.value_$i")).toSet
    val resultMap = result.results.asMap
    resultMap.size shouldBe 7
    resultMap.keySet shouldBe expectedPaths
  }

  it should "return 7 results for simple.conf file flattened" in {
    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("simple.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()

    result.results.flattenResultValues(true).size shouldBe 7
  }

  it should "return 7 results for merge.conf file flattened" in {
    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("merge.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()

    result.results.flattenResultValues(false).size shouldBe 7
    result.results.flattenResultValues(true).size shouldBe 7
  }

  it should "return 18 results for array.conf file flattened" in {
    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("array.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()

    result.results.flattenResultValues(false).size shouldBe 18
    result.results.flattenResultValues(true).size shouldBe 23
  }

  it should "return 16 results for object.conf file flattened" in {
    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("object.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()

    result.results.flattenResultValues(false).size shouldBe 16
    result.results.flattenResultValues(true).size shouldBe 24
  }

  it should "find correctly values from array.conf file" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("array.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()

    val resultList = result.results.flattenResultValues(true)

    resultList.get("pl.muninn.test.array.value_1").isDefined shouldBe true
    resultList.get("pl.muninn.test.array.value_2.3").isDefined shouldBe true
    resultList.get("pl.muninn.test.array.value_3.1.1").isDefined shouldBe true
    resultList.get("pl.muninn.test.array.value_3.5").isDefined shouldBe false
  }

  it should "find correctly values from object.conf file" in {
    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("object.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()

    val resultList = result.results.flattenResultValues(true)

    resultList.get("pl.muninn.test.object.reference").isDefined shouldBe true
    resultList.get("pl.muninn.test.object.value_1").isDefined shouldBe true
    resultList.get("pl.muninn.test.object.value_1.0.value_1_1_1").isDefined shouldBe true
    resultList.get("pl.muninn.test.object.value_1.4.value_1_5_1").isDefined shouldBe true
    resultList.get("pl.muninn.test.object.value_1.4.value_1_5_1.0").isDefined shouldBe true

    resultList.get("pl.muninn.test.object.value_1.4.value_1_5_1.10").isDefined shouldBe false
  }

  it should "extract values containing proper env hocones values from simple.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("simple.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extractWithPath[EnvironmentValue]

    extractonResult.size shouldBe 0
  }

  it should "extract values containing proper env hocones values from reference.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("reference.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extractWithPath[EnvironmentValue]

    extractonResult.size shouldBe 2
  }

  it should "extract values containing proper env hocones values from concatenation.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("concatenation.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extractWithPath[EnvironmentValue]

    extractonResult.size shouldBe 9
  }

  it should "extract values containing proper env hocones values from merge.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("merge.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extractWithPath[EnvironmentValue]

    extractonResult.size shouldBe 6
  }

  it should "extract values containing proper env hocones values from array.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("array.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extractWithPath[EnvironmentValue]

    extractonResult.size shouldBe 4
  }

  it should "extract values containing proper env hocones values from object.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("object.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extractWithPath[EnvironmentValue]

    extractonResult.size shouldBe 5
  }

  it should "extract proper env values from simple.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("simple.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extract[EnvironmentValue]

    extractonResult.size shouldBe 0
  }

  it should "extract proper env values from reference.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("reference.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extract[EnvironmentValue]

    extractonResult.size shouldBe 2
  }

  it should "extract proper env values from concatenation.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("concatenation.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extract[EnvironmentValue]

    extractonResult.size shouldBe 2
  }

  it should "extract proper env values from merge.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("merge.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extract[EnvironmentValue]

    extractonResult.size shouldBe 2
  }

  it should "extract proper env values from array.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("array.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extract[EnvironmentValue]

    extractonResult.size shouldBe 2
  }

  it should "extract proper env values from object.conf" in {

    import pl.muninn.hocones.parser.ops.HoconOps._

    val config: Config = loadConfig("object.conf")
    val result: HoconResult = HoconParser(config).unsafeRunSync()
    val extractonResult = result.results.extract[EnvironmentValue]

    extractonResult.size shouldBe 2
  }

  trait ParserConfigFixture {
    implicit val config: Config = loadConfig("parser.conf")
    val entries: Map[String, ConfigValue] = getEntriesMap

    def valueTuple(path: String, className: String): (Path, ConfigValue, CanonicalClassName) =
      (tagPath(path), entries(path), HoconParser.tagCanonicalName(className))

    val pathPrefix = "pl.muninn.test.pl.muninn.test"

    val simpleValues = Set(
      valueTuple(s"$pathPrefix.simple.value_2", "com.typesafe.config.impl.ConfigString.Quoted"),
      valueTuple(s"$pathPrefix.simple.value_3", "com.typesafe.config.impl.ConfigInt"),
      valueTuple(s"$pathPrefix.simple.value_4", "com.typesafe.config.impl.ConfigDouble"),
      valueTuple(s"$pathPrefix.simple.value_5", "com.typesafe.config.impl.ConfigBoolean"),
      valueTuple(s"$pathPrefix.simple.value_6", "com.typesafe.config.impl.ConfigString.Unquoted"),
      valueTuple(s"$pathPrefix.simple.value_7", "com.typesafe.config.impl.ConfigLong")
    )
  }
}
